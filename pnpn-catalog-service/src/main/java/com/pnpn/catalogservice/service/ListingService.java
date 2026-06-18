package com.pnpn.catalogservice.service;

import com.pnpn.catalogservice.client.UserServiceClient;
import com.pnpn.catalogservice.entity.Listing;
import com.pnpn.catalogservice.repository.ListingRepository;
import com.pnpn.shared.dto.ListingDto;
import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.dto.catalog.CreateListingRequest;
import com.pnpn.shared.dto.catalog.ListingResponse;
import com.pnpn.shared.dto.catalog.ListingSearchCriteria;
import com.pnpn.shared.dto.catalog.MediaPresignedUrlResponse;
import com.pnpn.shared.enums.ListingStatus;
import com.pnpn.shared.exception.PnPnException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Core service for listing CRUD, search, media upload, and view tracking.
 * <p>
 * <b>Cache strategy</b> — Two logical caches are maintained:
 * <ul>
 *   <li>{@code "listings"} — individual listing lookups via {@link #getListingById(UUID)},
 *       populated by {@code @Cacheable} and cleared entirely on any write operation.</li>
 *   <li>{@code "listingsSearch"} — search results; fully evicted on write operations
 *       since search queries are dynamic and cannot be selectively invalidated.</li>
 * </ul>
 * Both caches are backed by Redis (auto-configured via {@code spring-boot-starter-data-redis}).
 * Eviction is coarse (all-entries) for simplicity; a more granular approach (event-based
 * eviction per listing ID) can be added later for higher throughput.
 * <p>
 * <b>Async view counting</b> — {@link #incrementViewCount(UUID)} runs on Spring's
 * {@code SimpleAsyncTaskExecutor} (from {@code @EnableAsync}) and atomically increments a
 * Redis key {@code listing:views:{id}}. The background {@link #syncViewCounts()} task
 * (every 60s via {@code @Scheduled}) is a placeholder that would flush accumulated Redis
 * counts into the database periodically, decoupling page views from write-heavy DB updates.
 * <p>
 * <b>Influencer enrichment</b> — {@link #buildListingResponse(Listing)} calls
 * {@link UserServiceClient#getUserById(UUID)} to attach influencer profile data to each
 * response. This call is protected by a circuit breaker so that a transient user-service
 * outage returns "Unavailable" instead of failing the request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserServiceClient userServiceClient;
    private final MinioClient minioClient;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VIEW_COUNT_KEY_PREFIX = "listing:views:";

    /**
     * Creates a new listing as an influencer.
     * <p>
     * <b>Cache:</b> Evicts both {@code listings} and {@code listingsSearch} so that
     * fresh data is served on subsequent reads.
     * <p>
     * <b>Transaction:</b> The save is wrapped in {@code @Transactional} — if the
     * influencer validation (handled at controller level via auth) fails, no DB write occurs.
     */
    @Transactional
    @CacheEvict(value = {"listings", "listingsSearch"}, allEntries = true)
    public ListingResponse createListing(UUID influencerId, CreateListingRequest request) {
        Listing listing = Listing.builder()
                .influencerId(influencerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .condition(request.getCondition())
                .status(ListingStatus.ACTIVE)
                .viewCount(0)
                .mediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : List.of())
                .build();

        listing = listingRepository.save(listing);
        log.info("Listing created: {} by influencer {}", listing.getId(), influencerId);
        return buildListingResponse(listing);
    }

    /**
     * Searches listings with dynamic filters and pagination.
     * <p>
     * Maps the API-level {@link ListingSearchCriteria} to the repository-level
     * {@link com.pnpn.catalogservice.repository.ListingSearchCriteria}, then delegates
     * to {@link com.pnpn.catalogservice.repository.CustomListingRepository#findByCriteria}.
     * Results are ordered by {@code createdAt DESC} by default.
     * <p>
     * <b>Not cached</b> — search queries are highly dynamic; caching would require
     * a more sophisticated key strategy (e.g., hash of filter params).
     */
    public Page<ListingResponse> searchListings(ListingSearchCriteria criteria) {
        Pageable pageable = buildPageable(criteria);

        com.pnpn.catalogservice.repository.ListingSearchCriteria repoCriteria =
                com.pnpn.catalogservice.repository.ListingSearchCriteria.builder()
                        .keyword(criteria.getQuery())
                        .category(criteria.getCategory())
                        .minPrice(criteria.getMinPrice())
                        .maxPrice(criteria.getMaxPrice())
                        .condition(criteria.getCondition())
                        .status(criteria.getStatus() != null ? criteria.getStatus() : ListingStatus.ACTIVE)
                        .influencerId(criteria.getInfluencerId())
                        .build();

        return listingRepository.findByCriteria(repoCriteria, pageable)
                .map(this::buildListingResponse);
    }

    /**
     * Fetches a single listing by ID.
     * <p>
     * <b>Cache:</b> Result is stored in {@code "listings"} keyed by {@code #id}.
     * Subsequent calls with the same ID skip DB lookup until the cache is evicted
     * by a write operation or expires (default Redis TTL applies).
     * <p>
     * <b>View count:</b> Fires an async Redis increment — the actual DB {@code view_count}
     * column is updated in batch by {@link #syncViewCounts()}.
     */
    @Cacheable(value = "listings", key = "#id")
    public ListingResponse getListingById(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new PnPnException("Listing not found: " + id, HttpStatus.NOT_FOUND));

        incrementViewCount(id);

        return buildListingResponse(listing);
    }

    /**
     * Updates an existing listing. Only the owning influencer may update.
     * <p>
     * <b>Cache:</b> Evicts both caches entirely — stale individual entries and stale search result pages are cleared together.
     * <p>
     * <b>Ownership check:</b> Compares {@code userId} against {@link Listing#getInfluencerId()};
     * throws {@link PnPnException} with {@code 403 FORBIDDEN} on mismatch.
     */
    @Transactional
    @CacheEvict(value = {"listings", "listingsSearch"}, allEntries = true)
    public ListingResponse updateListing(UUID listingId, UUID userId, CreateListingRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new PnPnException("Listing not found: " + listingId, HttpStatus.NOT_FOUND));

        if (!listing.getInfluencerId().equals(userId)) {
            throw new PnPnException("You are not the owner of this listing", HttpStatus.FORBIDDEN);
        }

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setCategory(request.getCategory());
        listing.setCondition(request.getCondition());
        listing.setMediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : List.of());

        listing = listingRepository.save(listing);
        log.info("Listing updated: {}", listingId);
        return buildListingResponse(listing);
    }

    /**
     * Deletes a listing. Only the owning influencer may delete.
     * <p>
     * <b>Cache:</b> Evicts both caches entirely.
     */
    @Transactional
    @CacheEvict(value = {"listings", "listingsSearch"}, allEntries = true)
    public void deleteListing(UUID listingId, UUID userId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new PnPnException("Listing not found: " + listingId, HttpStatus.NOT_FOUND));

        if (!listing.getInfluencerId().equals(userId)) {
            throw new PnPnException("You are not the owner of this listing", HttpStatus.FORBIDDEN);
        }

        listingRepository.delete(listing);
        log.info("Listing deleted: {}", listingId);
    }

    /**
     * Generates a presigned URL for direct browser-to-MinIO upload.
     * <p>
     * <b>How it works:</b>
     * <ol>
     *   <li>Constructs a unique object path: {@code {userId}/{UUID}_{fileName}}.</li>
     *   <li>Requests a {@code PUT} presigned URL from MinIO with a 1-hour expiry
     *       via {@link MinioClient#getPresignedObjectUrl}.</li>
     *   <li>Returns the temporary upload URL alongside the final public media path.</li>
     * </ol>
     * The client uses the presigned URL to upload the file directly; no server-side
     * buffering is involved.
     */
    public MediaPresignedUrlResponse generatePresignedUrl(String bucketName, String fileName, UUID userId) {
        String objectName = userId + "/" + UUID.randomUUID() + "_" + fileName;

        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );

            String mediaUrl = "/media/" + bucketName + "/" + objectName;

            return MediaPresignedUrlResponse.builder()
                    .presignedUrl(presignedUrl)
                    .mediaUrl(mediaUrl)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for bucket: {}, object: {}",
                    bucketName, objectName, e);
            throw new PnPnException("Failed to generate upload URL", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Atomically increments the view counter for a listing in Redis.
     * <p>
     * Runs on a separate thread via {@code @Async} (executor configured by
     * {@code @EnableAsync} on the application class).
     * The Redis key {@code listing:views:{id}} is incremented with no blocking on the calling thread,
     * keeping {@link #getListingById(UUID)} fast.
     * <p>
     * <b>Sync to DB:</b> {@link #syncViewCounts()} periodically flushes these
     * Redis counters to the {@code view_count} column.
     */
    @Async
    public void incrementViewCount(UUID listingId) {
        stringRedisTemplate.opsForValue().increment(VIEW_COUNT_KEY_PREFIX + listingId);
    }

    /**
     * Scheduled task that flushes Redis view counters to the database.
     * <p>
     * Runs every 60 seconds (configurable via
     * {@code listing.view-count.sync-interval}). This is a placeholder — a full
     * implementation would scan Redis keys matching {@code listing:views:*},
     * update each listing's {@code viewCount} in the DB, and reset the counters.
     * <p>
     * <b>Why periodic sync instead of synchronous writes?</b>
     * Every view triggers a write. For high-traffic listings, synchronous DB writes
     * would create contention. Batching views in Redis and flushing on a schedule
     * keeps the DB load constant regardless of traffic spikes.
     */
    @Scheduled(fixedRateString = "${listing.view-count.sync-interval:60000}")
    public void syncViewCounts() {
        log.debug("Syncing view counts from Redis to database");
    }

    private Pageable buildPageable(ListingSearchCriteria criteria) {
        int page = Math.max(criteria.getPage(), 0);
        int size = criteria.getSize() > 0 ? criteria.getSize() : 20;
        int maxSize = 100;
        if (size > maxSize) {
            size = maxSize;
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (criteria.getSortBy() != null && !criteria.getSortBy().isBlank()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if ("desc".equalsIgnoreCase(criteria.getSortDirection())) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, criteria.getSortBy());
        }

        return PageRequest.of(page, size, sort);
    }

    /**
     * Builds a {@link ListingResponse} enriched with influencer profile data.
     * <p>
     * Delegates to {@link UserServiceClient#getUserById(UUID)} which is circuit-broken.
     * If user-service is down, the influencer field contains a stub with name "Unavailable".
     */
    private ListingResponse buildListingResponse(Listing listing) {
        UserDto influencer = userServiceClient.getUserById(listing.getInfluencerId());
        return ListingResponse.builder()
                .listing(toDto(listing))
                .influencer(influencer)
                .build();
    }

    private ListingDto toDto(Listing listing) {
        return ListingDto.builder()
                .id(listing.getId())
                .influencerId(listing.getInfluencerId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .category(listing.getCategory())
                .condition(listing.getCondition())
                .status(listing.getStatus())
                .mediaUrls(listing.getMediaUrls())
                .viewCount(listing.getViewCount())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }
}
