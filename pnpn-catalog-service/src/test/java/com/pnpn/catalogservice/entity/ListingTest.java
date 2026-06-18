package com.pnpn.catalogservice.entity;

import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListingTest {

    @Test
    void shouldCreateListingUsingBuilder() {
        UUID id = UUID.randomUUID();
        UUID influencerId = UUID.randomUUID();

        Listing listing = Listing.builder()
                .id(id)
                .influencerId(influencerId)
                .title("Vintage Guitar")
                .description("A rare 1965 Fender Stratocaster")
                .price(BigDecimal.valueOf(2500.00))
                .category("Musical Instruments")
                .condition(ListingCondition.GENTLY_USED)
                .status(ListingStatus.ACTIVE)
                .mediaUrls(List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg"))
                .viewCount(42)
                .build();

        assertEquals(id, listing.getId());
        assertEquals(influencerId, listing.getInfluencerId());
        assertEquals("Vintage Guitar", listing.getTitle());
        assertEquals("A rare 1965 Fender Stratocaster", listing.getDescription());
        assertEquals(0, BigDecimal.valueOf(2500.00).compareTo(listing.getPrice()));
        assertEquals("Musical Instruments", listing.getCategory());
        assertEquals(ListingCondition.GENTLY_USED, listing.getCondition());
        assertEquals(ListingStatus.ACTIVE, listing.getStatus());
        assertEquals(2, listing.getMediaUrls().size());
        assertEquals(42, listing.getViewCount());
        assertNull(listing.getCreatedAt());
        assertNull(listing.getUpdatedAt());
    }

    @Test
    void shouldMutateFieldsViaSetters() {
        Listing listing = new Listing();
        listing.setTitle("New Title");
        listing.setDescription("New description");
        listing.setPrice(BigDecimal.valueOf(99.99));
        listing.setCategory("Electronics");
        listing.setCondition(ListingCondition.NEW);
        listing.setStatus(ListingStatus.SOLD);
        listing.setViewCount(10);

        assertEquals("New Title", listing.getTitle());
        assertEquals("New description", listing.getDescription());
        assertEquals(0, BigDecimal.valueOf(99.99).compareTo(listing.getPrice()));
        assertEquals("Electronics", listing.getCategory());
        assertEquals(ListingCondition.NEW, listing.getCondition());
        assertEquals(ListingStatus.SOLD, listing.getStatus());
        assertEquals(10, listing.getViewCount());
    }

    @Test
    void defaultViewCountShouldBeNullInPojo() {
        Listing listing = new Listing();
        assertNull(listing.getViewCount());
        assertNull(listing.getId());
        assertNull(listing.getTitle());
    }

    @Test
    void shouldHandleEquality() {
        UUID id = UUID.randomUUID();
        Listing listing1 = Listing.builder().id(id).title("Item").build();
        Listing listing2 = Listing.builder().id(id).title("Item").build();

        assertEquals(listing1, listing2);
        assertEquals(listing1.hashCode(), listing2.hashCode());
    }

    @Test
    void shouldHandleNullMediaUrls() {
        Listing listing = Listing.builder()
                .id(UUID.randomUUID())
                .influencerId(UUID.randomUUID())
                .title("Test")
                .price(BigDecimal.TEN)
                .category("Test")
                .condition(ListingCondition.NEW)
                .status(ListingStatus.ACTIVE)
                .build();

        assertNull(listing.getMediaUrls());
    }
}
