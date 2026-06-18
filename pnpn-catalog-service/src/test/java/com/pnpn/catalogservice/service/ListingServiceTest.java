package com.pnpn.catalogservice.service;

import com.pnpn.catalogservice.client.UserServiceClient;
import com.pnpn.catalogservice.entity.Listing;
import com.pnpn.catalogservice.repository.ListingRepository;
import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.dto.catalog.CreateListingRequest;
import com.pnpn.shared.dto.catalog.ListingResponse;
import com.pnpn.shared.dto.catalog.ListingSearchCriteria;
import com.pnpn.shared.dto.catalog.MediaPresignedUrlResponse;
import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.exception.PnPnException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private MinioClient minioClient;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private ListingService listingService;

    private final UUID influencerId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();
    private final String bucketName = "pnpn-listings-images";

    private Listing activeListing;

    @BeforeEach
    void setUp() {
        listingService = new ListingService(
                listingRepository, userServiceClient, minioClient, stringRedisTemplate);

        activeListing = Listing.builder()
                .id(listingId)
                .influencerId(influencerId)
                .title("Vintage Camera")
                .description("A classic 35mm film camera")
                .price(BigDecimal.valueOf(299.99))
                .category("Electronics")
                .condition(ListingCondition.GENTLY_USED)
                .status(ListingStatus.ACTIVE)
                .mediaUrls(List.of("https://example.com/photo.jpg"))
                .viewCount(42)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ═══════════════════════════════════════════════════════════════════
    // createListing
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void createListing_shouldSucceed() {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("New Item")
                .description("Brand new item description")
                .price(BigDecimal.valueOf(49.99))
                .category("Books")
                .condition(ListingCondition.NEW)
                .mediaUrls(List.of("https://example.com/img.jpg"))
                .build();

        UserDto influencer = UserDto.builder().id(influencerId).name("Alice").build();
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> {
            Listing saved = inv.getArgument(0);
            return Listing.builder()
                    .id(UUID.randomUUID())
                    .influencerId(saved.getInfluencerId())
                    .title(saved.getTitle())
                    .description(saved.getDescription())
                    .price(saved.getPrice())
                    .category(saved.getCategory())
                    .condition(saved.getCondition())
                    .status(saved.getStatus())
                    .viewCount(0)
                    .mediaUrls(saved.getMediaUrls())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });
        when(userServiceClient.getUserById(influencerId)).thenReturn(influencer);

        ListingResponse response = listingService.createListing(influencerId, request);

        assertNotNull(response);
        assertEquals("New Item", response.getListing().getTitle());
        assertEquals(influencerId, response.getListing().getInfluencerId());
        assertEquals(ListingStatus.ACTIVE, response.getListing().getStatus());
        assertEquals(0, response.getListing().getViewCount());
        assertEquals("Alice", response.getInfluencer().getName());

        ArgumentCaptor<Listing> captor = ArgumentCaptor.forClass(Listing.class);
        verify(listingRepository).save(captor.capture());
        assertEquals("New Item", captor.getValue().getTitle());
        assertEquals(ListingStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void createListing_shouldHandleNullMediaUrls() {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("No Media")
                .description("Item without images")
                .price(BigDecimal.valueOf(10.00))
                .category("Other")
                .condition(ListingCondition.NEW)
                .build();

        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> {
            Listing saved = inv.getArgument(0);
            return Listing.builder()
                    .id(UUID.randomUUID())
                    .influencerId(influencerId)
                    .title(saved.getTitle())
                    .price(saved.getPrice())
                    .status(ListingStatus.ACTIVE)
                    .viewCount(0)
                    .mediaUrls(List.of())
                    .build();
        });
        when(userServiceClient.getUserById(influencerId))
                .thenReturn(UserDto.builder().id(influencerId).name("Alice").build());

        ListingResponse response = listingService.createListing(influencerId, request);

        assertNotNull(response);
        assertTrue(response.getListing().getMediaUrls().isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // searchListings
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void searchListings_shouldReturnResults() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .category("Electronics")
                .page(0)
                .size(10)
                .build();

        Page<Listing> listingPage = new PageImpl<>(List.of(activeListing));
        when(listingRepository.findByCriteria(any(com.pnpn.catalogservice.repository.ListingSearchCriteria.class),
                any(PageRequest.class)))
                .thenReturn(listingPage);
        when(userServiceClient.getUserById(influencerId))
                .thenReturn(UserDto.builder().id(influencerId).name("Alice").build());

        Page<ListingResponse> result = listingService.searchListings(criteria);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Vintage Camera", result.getContent().getFirst().getListing().getTitle());
    }

    @Test
    void searchListings_shouldDefaultStatusToActive() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .query("camera")
                .page(0)
                .size(20)
                .build();

        Page<Listing> emptyPage = Page.empty();
        when(listingRepository.findByCriteria(any(com.pnpn.catalogservice.repository.ListingSearchCriteria.class),
                any(PageRequest.class)))
                .thenReturn(emptyPage);

        Page<ListingResponse> result = listingService.searchListings(criteria);

        assertTrue(result.isEmpty());

        ArgumentCaptor<com.pnpn.catalogservice.repository.ListingSearchCriteria> captor =
                ArgumentCaptor.forClass(com.pnpn.catalogservice.repository.ListingSearchCriteria.class);
        verify(listingRepository).findByCriteria(captor.capture(), any());
        assertEquals(ListingStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals("camera", captor.getValue().getKeyword());
    }

    @Test
    void searchListings_shouldApplySortAndPagination() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .page(1)
                .size(5)
                .sortBy("price")
                .sortDirection("asc")
                .build();

        Page<Listing> emptyPage = Page.empty();
        when(listingRepository.findByCriteria(any(com.pnpn.catalogservice.repository.ListingSearchCriteria.class),
                any(PageRequest.class)))
                .thenReturn(emptyPage);

        listingService.searchListings(criteria);

        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(listingRepository).findByCriteria(any(), pageCaptor.capture());
        assertEquals(1, pageCaptor.getValue().getPageNumber());
        assertEquals(5, pageCaptor.getValue().getPageSize());
    }

    @Test
    void searchListings_shouldCapPageSize() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .page(0)
                .size(200)
                .build();

        Page<Listing> emptyPage = Page.empty();
        when(listingRepository.findByCriteria(any(com.pnpn.catalogservice.repository.ListingSearchCriteria.class),
                any(PageRequest.class)))
                .thenReturn(emptyPage);

        listingService.searchListings(criteria);

        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(listingRepository).findByCriteria(any(), pageCaptor.capture());
        assertEquals(100, pageCaptor.getValue().getPageSize());
    }

    // ═══════════════════════════════════════════════════════════════════
    // getListingById
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getListingById_shouldReturnListing() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(activeListing));
        when(userServiceClient.getUserById(influencerId))
                .thenReturn(UserDto.builder().id(influencerId).name("Alice").build());

        ListingResponse response = listingService.getListingById(listingId);

        assertNotNull(response);
        assertEquals("Vintage Camera", response.getListing().getTitle());
        assertEquals(influencerId, response.getListing().getInfluencerId());
        assertEquals(42, response.getListing().getViewCount());
        verify(stringRedisTemplate.opsForValue()).increment("listing:views:" + listingId);
    }

    @Test
    void getListingById_shouldThrowWhenNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class,
                () -> listingService.getListingById(listingId));
        assertEquals("Listing not found: " + listingId, ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // updateListing
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void updateListing_shouldSucceedWhenOwner() {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Updated Camera")
                .description("Updated description")
                .price(BigDecimal.valueOf(349.99))
                .category("Electronics")
                .condition(ListingCondition.LIKE_NEW)
                .mediaUrls(List.of("https://example.com/new.jpg"))
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(activeListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userServiceClient.getUserById(influencerId))
                .thenReturn(UserDto.builder().id(influencerId).name("Alice").build());

        ListingResponse response = listingService.updateListing(listingId, influencerId, request);

        assertNotNull(response);
        assertEquals("Updated Camera", response.getListing().getTitle());
        assertEquals("Updated description", response.getListing().getDescription());
        assertEquals(BigDecimal.valueOf(349.99), response.getListing().getPrice());
        assertEquals(ListingCondition.LIKE_NEW, response.getListing().getCondition());
    }

    @Test
    void updateListing_shouldThrowWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Hijacked").price(BigDecimal.TEN)
                .category("X").condition(ListingCondition.NEW).build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(activeListing));

        PnPnException ex = assertThrows(PnPnException.class,
                () -> listingService.updateListing(listingId, otherUserId, request));
        assertEquals("You are not the owner of this listing", ex.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void updateListing_shouldThrowWhenListingNotFound() {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Ghost").price(BigDecimal.TEN)
                .category("X").condition(ListingCondition.NEW).build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class,
                () -> listingService.updateListing(listingId, influencerId, request));
        assertEquals("Listing not found: " + listingId, ex.getMessage());
    }

    @Test
    void updateListing_shouldHandleNullMediaUrls() {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("No Media Update")
                .description("Desc")
                .price(BigDecimal.valueOf(25.00))
                .category("X")
                .condition(ListingCondition.NEW)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(activeListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userServiceClient.getUserById(influencerId))
                .thenReturn(UserDto.builder().id(influencerId).name("Alice").build());

        listingService.updateListing(listingId, influencerId, request);
    }

    // ═══════════════════════════════════════════════════════════════════
    // deleteListing
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void deleteListing_shouldSucceedWhenOwner() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(activeListing));

        listingService.deleteListing(listingId, influencerId);

        verify(listingRepository).delete(activeListing);
    }

    @Test
    void deleteListing_shouldThrowWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(activeListing));

        PnPnException ex = assertThrows(PnPnException.class,
                () -> listingService.deleteListing(listingId, otherUserId));
        assertEquals("You are not the owner of this listing", ex.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(listingRepository, never()).delete(any());
    }

    @Test
    void deleteListing_shouldThrowWhenListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class,
                () -> listingService.deleteListing(listingId, influencerId));
        assertEquals("Listing not found: " + listingId, ex.getMessage());
        verify(listingRepository, never()).delete(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // generatePresignedUrl
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void generatePresignedUrl_shouldReturnUrl() throws Exception {
        String presignedUrl = "https://minio.example.com/upload-url";
        String fileName = "photo.jpg";

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(presignedUrl);

        MediaPresignedUrlResponse response = listingService.generatePresignedUrl(
                bucketName, fileName, influencerId);

        assertNotNull(response);
        assertEquals(presignedUrl, response.getPresignedUrl());
        assertTrue(response.getMediaUrl().startsWith("/media/" + bucketName + "/"));
        assertTrue(response.getMediaUrl().endsWith("_" + fileName));

        ArgumentCaptor<GetPresignedObjectUrlArgs> captor =
                ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(captor.capture());
        assertEquals(bucketName, captor.getValue().bucket());
        assertEquals(io.minio.http.Method.PUT, captor.getValue().method());
    }

    @Test
    void generatePresignedUrl_shouldThrowWhenMinioFails() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("MinIO connection refused"));

        PnPnException ex = assertThrows(PnPnException.class,
                () -> listingService.generatePresignedUrl(bucketName, "test.jpg", influencerId));
        assertEquals("Failed to generate upload URL", ex.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // incrementViewCount
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void incrementViewCount_shouldIncrementRedisCounter() {
        listingService.incrementViewCount(listingId);

        verify(stringRedisTemplate.opsForValue()).increment("listing:views:" + listingId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // syncViewCounts
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void syncViewCounts_shouldRunWithoutError() {
        assertDoesNotThrow(() -> listingService.syncViewCounts());
    }

    // ═══════════════════════════════════════════════════════════════════
    // buildListingResponse / influencer enrichment
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getListingById_shouldEnrichWithInfluencerInfo() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(activeListing));
        when(userServiceClient.getUserById(influencerId))
                .thenReturn(UserDto.builder()
                        .id(influencerId)
                        .name("Alice")
                        .email("alice@example.com")
                        .type(UserType.INFLUENCER)
                        .build());

        ListingResponse response = listingService.getListingById(listingId);

        assertNotNull(response.getInfluencer());
        assertEquals("Alice", response.getInfluencer().getName());
        assertEquals(influencerId, response.getInfluencer().getId());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Edge cases
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getListingById_shouldHandleNullMediaUrlsInEntity() {
        Listing listingWithNullMedia = Listing.builder()
                .id(listingId)
                .influencerId(influencerId)
                .title("No Media")
                .description("Desc")
                .price(BigDecimal.valueOf(10))
                .category("X")
                .condition(ListingCondition.NEW)
                .status(ListingStatus.ACTIVE)
                .viewCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listingWithNullMedia));
        when(userServiceClient.getUserById(influencerId))
                .thenReturn(UserDto.builder().id(influencerId).name("Alice").build());

        ListingResponse response = listingService.getListingById(listingId);
        assertNotNull(response);
        assertNull(response.getListing().getMediaUrls());
    }
}
