package com.pnpn.shared.dto.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pnpn.shared.dto.ListingDto;
import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import com.pnpn.shared.enums.UserType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListingResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void testBuilderAndGetters() {
        UUID listingId = UUID.randomUUID();
        UUID influencerId = UUID.randomUUID();

        ListingDto listingDto = ListingDto.builder()
                .id(listingId)
                .influencerId(influencerId)
                .title("Vintage Camera")
                .price(BigDecimal.valueOf(299.99))
                .condition(ListingCondition.GENTLY_USED)
                .status(ListingStatus.ACTIVE)
                .build();

        UserDto influencer = UserDto.builder()
                .id(influencerId)
                .name("Alice")
                .email("alice@example.com")
                .type(UserType.INFLUENCER)
                .build();

        ListingResponse response = ListingResponse.builder()
                .listing(listingDto)
                .influencer(influencer)
                .build();

        assertNotNull(response);
        assertEquals(listingId, response.getListing().getId());
        assertEquals(influencerId, response.getInfluencer().getId());
        assertEquals("Vintage Camera", response.getListing().getTitle());
        assertEquals("Alice", response.getInfluencer().getName());
    }

    @Test
    void testNoArgsConstructor() {
        ListingResponse response = new ListingResponse();
        assertNull(response.getListing());
        assertNull(response.getInfluencer());
    }

    @Test
    void testAllArgsConstructor() {
        UUID listingId = UUID.randomUUID();
        UUID influencerId = UUID.randomUUID();

        ListingDto listing = ListingDto.builder().id(listingId).build();
        UserDto influencer = UserDto.builder().id(influencerId).build();

        ListingResponse response = new ListingResponse(listing, influencer);

        assertEquals(listingId, response.getListing().getId());
        assertEquals(influencerId, response.getInfluencer().getId());
    }

    @Test
    void testJsonSerialization() throws Exception {
        UUID listingId = UUID.randomUUID();
        UUID influencerId = UUID.randomUUID();

        ListingDto listing = ListingDto.builder()
                .id(listingId)
                .influencerId(influencerId)
                .title("Camera")
                .price(BigDecimal.valueOf(199.99))
                .condition(ListingCondition.NEW)
                .status(ListingStatus.ACTIVE)
                .build();

        UserDto influencer = UserDto.builder()
                .id(influencerId)
                .name("Alice")
                .type(UserType.INFLUENCER)
                .build();

        ListingResponse response = ListingResponse.builder()
                .listing(listing)
                .influencer(influencer)
                .build();

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"title\":\"Camera\""));
        assertTrue(json.contains("\"name\":\"Alice\""));
        assertTrue(json.contains(listingId.toString()));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = """
                {
                    "listing": {
                        "id": "%s",
                        "title": "Camera",
                        "price": 199.99,
                        "condition": "NEW",
                        "status": "ACTIVE"
                    },
                    "influencer": {
                        "id": "%s",
                        "name": "Alice",
                        "type": "INFLUENCER"
                    }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        ListingResponse response = objectMapper.readValue(json, ListingResponse.class);
        assertNotNull(response.getListing());
        assertNotNull(response.getInfluencer());
        assertEquals("Camera", response.getListing().getTitle());
        assertEquals("Alice", response.getInfluencer().getName());
    }

    @Test
    void testSetters() {
        ListingResponse response = new ListingResponse();
        UUID listingId = UUID.randomUUID();
        UUID influencerId = UUID.randomUUID();

        ListingDto listing = ListingDto.builder().id(listingId).title("Test").build();
        UserDto influencer = UserDto.builder().id(influencerId).name("Bob").build();

        response.setListing(listing);
        response.setInfluencer(influencer);

        assertEquals(listingId, response.getListing().getId());
        assertEquals("Test", response.getListing().getTitle());
        assertEquals(influencerId, response.getInfluencer().getId());
        assertEquals("Bob", response.getInfluencer().getName());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID listingId = UUID.randomUUID();
        UUID influencerId = UUID.randomUUID();

        ListingDto listing = ListingDto.builder().id(listingId).build();
        UserDto influencer = UserDto.builder().id(influencerId).build();

        ListingResponse r1 = new ListingResponse(listing, influencer);
        ListingResponse r2 = new ListingResponse(listing, influencer);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
