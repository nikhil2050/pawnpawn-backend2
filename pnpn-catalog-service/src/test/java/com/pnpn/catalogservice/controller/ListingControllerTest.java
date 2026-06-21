package com.pnpn.catalogservice.controller;

import com.pnpn.catalogservice.config.SecurityConfig;
import com.pnpn.catalogservice.service.ListingService;
import com.pnpn.shared.config.JwtAuthenticationFilter;
import com.pnpn.shared.config.JwtUtil;
import com.pnpn.shared.dto.ListingDto;
import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.dto.catalog.CreateListingRequest;
import com.pnpn.shared.dto.catalog.ListingResponse;
import com.pnpn.shared.dto.catalog.MediaPresignedUrlResponse;
import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import com.pnpn.shared.exception.GlobalExceptionHandler;
import com.pnpn.shared.exception.PnPnException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ListingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationFilter.class})
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListingService listingService;

    @MockBean
    private JwtUtil jwtUtil;

    private final UUID influencerId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken influencerAuth() {
        return new UsernamePasswordAuthenticationToken(influencerId, null,
                List.of(new SimpleGrantedAuthority("ROLE_INFLUENCER")));
    }

    private ListingResponse sampleListingResponse() {
        ListingDto dto = ListingDto.builder()
                .id(listingId)
                .influencerId(influencerId)
                .title("Test Item")
                .description("Description")
                .price(BigDecimal.valueOf(29.99))
                .category("CLOTHING")
                .condition(ListingCondition.NEW)
                .status(ListingStatus.ACTIVE)
                .viewCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        UserDto influencer = UserDto.builder()
                .id(influencerId)
                .name("Alice")
                .build();
        return ListingResponse.builder().listing(dto).influencer(influencer).build();
    }

    @Test
    void createListing_shouldReturn201() throws Exception {
        when(listingService.createListing(any(UUID.class), any(CreateListingRequest.class)))
                .thenReturn(sampleListingResponse());

        mockMvc.perform(post("/api/listings")
                        .with(authentication(influencerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Item",
                                    "description": "Description",
                                    "price": 29.99,
                                    "category": "CLOTHING",
                                    "condition": "NEW",
                                    "mediaUrls": ["http://example.com/img.jpg"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listing.title").value("Test Item"))
                .andExpect(jsonPath("$.influencer.name").value("Alice"));

        verify(listingService).createListing(any(UUID.class), any(CreateListingRequest.class));
    }

    @Test
    void createListing_withoutAuth_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Item",
                                    "price": 29.99,
                                    "category": "CLOTHING",
                                    "condition": "NEW"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(listingService, never()).createListing(any(), any());
    }

    @Test
    void createListing_withInvalidBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .with(authentication(influencerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "",
                                    "price": -1.00,
                                    "category": "",
                                    "condition": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(listingService, never()).createListing(any(), any());
    }

    @Test
    void searchListings_shouldReturn200() throws Exception {
        Page<ListingResponse> page = new PageImpl<>(List.of(sampleListingResponse()));

        when(listingService.searchListings(any())).thenReturn(page);

        mockMvc.perform(get("/api/listings")
                        .param("category", "CLOTHING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].listing.title").value("Test Item"))
                .andExpect(jsonPath("$.content[0].influencer.name").value("Alice"));
    }

    @Test
    void searchListings_withAllParams_shouldReturn200() throws Exception {
        Page<ListingResponse> page = new PageImpl<>(List.of(sampleListingResponse()));

        when(listingService.searchListings(any())).thenReturn(page);

        mockMvc.perform(get("/api/listings")
                        .param("query", "test")
                        .param("category", "CLOTHING")
                        .param("influencerId", influencerId.toString())
                        .param("minPrice", "10")
                        .param("maxPrice", "100")
                        .param("condition", "NEW")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "price")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    void getListingById_shouldReturn200() throws Exception {
        when(listingService.getListingById(listingId)).thenReturn(sampleListingResponse());

        mockMvc.perform(get("/api/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listing.title").value("Test Item"));
    }

    @Test
    void getListingById_whenNotFound_shouldReturn404() throws Exception {
        when(listingService.getListingById(listingId))
                .thenThrow(new PnPnException("Listing not found: " + listingId, HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/listings/{id}", listingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Listing not found: " + listingId));
    }

    @Test
    void updateListing_shouldReturn200() throws Exception {
        when(listingService.updateListing(any(UUID.class), any(UUID.class), any(CreateListingRequest.class)))
                .thenReturn(sampleListingResponse());

        mockMvc.perform(put("/api/listings/{id}", listingId)
                        .with(authentication(influencerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Item",
                                    "price": 39.99,
                                    "category": "CLOTHING",
                                    "condition": "USED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listing.title").value("Test Item"));
    }

    @Test
    void updateListing_withoutAuth_shouldReturn403() throws Exception {
        mockMvc.perform(put("/api/listings/{id}", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Item",
                                    "price": 39.99,
                                    "category": "CLOTHING",
                                    "condition": "USED"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(listingService, never()).updateListing(any(), any(), any());
    }

    @Test
    void deleteListing_shouldReturn200() throws Exception {
        doNothing().when(listingService).deleteListing(listingId, influencerId);

        mockMvc.perform(delete("/api/listings/{id}", listingId)
                        .with(authentication(influencerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Listing deleted successfully"));

        verify(listingService).deleteListing(listingId, influencerId);
    }

    @Test
    void deleteListing_withoutAuth_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/listings/{id}", listingId))
                .andExpect(status().isForbidden());

        verify(listingService, never()).deleteListing(any(), any());
    }

    @Test
    void generatePresignedUrl_shouldReturn200() throws Exception {
        when(listingService.generatePresignedUrl(anyString(), anyString(), any(UUID.class)))
                .thenReturn(MediaPresignedUrlResponse.builder()
                        .presignedUrl("https://minio.example.com/upload")
                        .mediaUrl("/media/test-bucket/test.jpg")
                        .build());

        mockMvc.perform(post("/api/listings/{id}/media/presigned-url", listingId)
                        .with(authentication(influencerAuth()))
                        .param("bucketName", "test-bucket")
                        .param("fileName", "test.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presignedUrl").value("https://minio.example.com/upload"))
                .andExpect(jsonPath("$.mediaUrl").value("/media/test-bucket/test.jpg"));
    }

    @Test
    void generatePresignedUrl_withoutAuth_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/listings/{id}/media/presigned-url", listingId)
                        .param("bucketName", "test-bucket")
                        .param("fileName", "test.jpg"))
                .andExpect(status().isForbidden());

        verify(listingService, never()).generatePresignedUrl(any(), any(), any());
    }
}
