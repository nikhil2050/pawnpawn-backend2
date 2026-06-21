package com.pnpn.catalogservice.controller;

import com.pnpn.catalogservice.service.ListingService;
import com.pnpn.shared.dto.catalog.CreateListingRequest;
import com.pnpn.shared.dto.catalog.ListingResponse;
import com.pnpn.shared.dto.catalog.ListingSearchCriteria;
import com.pnpn.shared.dto.catalog.MediaPresignedUrlResponse;
import com.pnpn.shared.exception.PnPnException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestBody CreateListingRequest request) {
        UUID influencerId = getCurrentUserId();
        ListingResponse response = listingService.createListing(influencerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ListingResponse>> searchListings(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID influencerId,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .query(query)
                .category(category)
                .influencerId(influencerId)
                .minPrice(minPrice != null ? new java.math.BigDecimal(minPrice) : null)
                .maxPrice(maxPrice != null ? new java.math.BigDecimal(maxPrice) : null)
                .condition(condition != null
                        ? com.pnpn.shared.enums.ListingCondition.valueOf(condition.toUpperCase())
                        : null)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        return ResponseEntity.ok(listingService.searchListings(criteria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.getListingById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable UUID id,
            @Valid @RequestBody CreateListingRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(listingService.updateListing(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<Map<String, String>> deleteListing(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        listingService.deleteListing(id, userId);
        return ResponseEntity.ok(Map.of("message", "Listing deleted successfully"));
    }

    @PostMapping("/{id}/media/presigned-url")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<MediaPresignedUrlResponse> generatePresignedUrl(
            @PathVariable UUID id,
            @RequestParam String bucketName,
            @RequestParam String fileName) {
        UUID userId = getCurrentUserId();
        MediaPresignedUrlResponse response = listingService.generatePresignedUrl(
                bucketName, fileName, userId);
        return ResponseEntity.ok(response);
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new PnPnException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return (UUID) authentication.getPrincipal();
    }
}
