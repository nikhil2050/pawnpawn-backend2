package com.pnpn.catalogservice.repository;

import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListingSearchCriteriaTest {

    @Test
    void shouldBuildWithAllFields() {
        UUID influencerId = UUID.randomUUID();

        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .category("Electronics")
                .condition(ListingCondition.NEW)
                .status(ListingStatus.ACTIVE)
                .minPrice(BigDecimal.valueOf(10))
                .maxPrice(BigDecimal.valueOf(100))
                .influencerId(influencerId)
                .keyword("camera")
                .build();

        assertEquals("Electronics", criteria.getCategory());
        assertEquals(ListingCondition.NEW, criteria.getCondition());
        assertEquals(ListingStatus.ACTIVE, criteria.getStatus());
        assertEquals(0, BigDecimal.valueOf(10).compareTo(criteria.getMinPrice()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(criteria.getMaxPrice()));
        assertEquals(influencerId, criteria.getInfluencerId());
        assertEquals("camera", criteria.getKeyword());
    }

    @Test
    void shouldBuildWithPartialFields() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .category("Books")
                .minPrice(BigDecimal.valueOf(5))
                .build();

        assertEquals("Books", criteria.getCategory());
        assertEquals(0, BigDecimal.valueOf(5).compareTo(criteria.getMinPrice()));
        assertNull(criteria.getCondition());
        assertNull(criteria.getStatus());
        assertNull(criteria.getMaxPrice());
        assertNull(criteria.getInfluencerId());
        assertNull(criteria.getKeyword());
    }

    @Test
    void shouldBuildWithEmptyBuilder() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder().build();

        assertNull(criteria.getCategory());
        assertNull(criteria.getCondition());
        assertNull(criteria.getStatus());
        assertNull(criteria.getMinPrice());
        assertNull(criteria.getMaxPrice());
        assertNull(criteria.getInfluencerId());
        assertNull(criteria.getKeyword());
    }

    @Test
    void shouldUseNoArgsConstructor() {
        ListingSearchCriteria criteria = new ListingSearchCriteria();

        assertNull(criteria.getCategory());
        assertNull(criteria.getCondition());
    }

    @Test
    void shouldUseAllArgsConstructor() {
        UUID influencerId = UUID.randomUUID();

        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "Music", ListingCondition.USED, ListingStatus.SOLD,
                BigDecimal.ONE, BigDecimal.TEN, influencerId, "guitar");

        assertEquals("Music", criteria.getCategory());
        assertEquals(ListingCondition.USED, criteria.getCondition());
        assertEquals(ListingStatus.SOLD, criteria.getStatus());
        assertEquals(0, BigDecimal.ONE.compareTo(criteria.getMinPrice()));
        assertEquals(0, BigDecimal.TEN.compareTo(criteria.getMaxPrice()));
        assertEquals(influencerId, criteria.getInfluencerId());
        assertEquals("guitar", criteria.getKeyword());
    }

    @Test
    void shouldMutateFieldsViaSetters() {
        UUID influencerId = UUID.randomUUID();
        ListingSearchCriteria criteria = new ListingSearchCriteria();

        criteria.setCategory("Clothing");
        criteria.setCondition(ListingCondition.LIKE_NEW);
        criteria.setStatus(ListingStatus.RESERVED);
        criteria.setMinPrice(BigDecimal.valueOf(20));
        criteria.setMaxPrice(BigDecimal.valueOf(200));
        criteria.setInfluencerId(influencerId);
        criteria.setKeyword("shirt");

        assertEquals("Clothing", criteria.getCategory());
        assertEquals(ListingCondition.LIKE_NEW, criteria.getCondition());
        assertEquals(ListingStatus.RESERVED, criteria.getStatus());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(criteria.getMinPrice()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(criteria.getMaxPrice()));
        assertEquals(influencerId, criteria.getInfluencerId());
        assertEquals("shirt", criteria.getKeyword());
    }

    @Test
    void shouldHandleEquality() {
        UUID influencerId = UUID.randomUUID();

        ListingSearchCriteria criteria1 = ListingSearchCriteria.builder()
                .category("Toys").minPrice(BigDecimal.TEN).influencerId(influencerId).build();

        ListingSearchCriteria criteria2 = ListingSearchCriteria.builder()
                .category("Toys").minPrice(BigDecimal.TEN).influencerId(influencerId).build();

        assertEquals(criteria1, criteria2);
        assertEquals(criteria1.hashCode(), criteria2.hashCode());
    }

    @Test
    void shouldHandleInequality() {
        ListingSearchCriteria criteria1 = ListingSearchCriteria.builder().category("A").build();
        ListingSearchCriteria criteria2 = ListingSearchCriteria.builder().category("B").build();

        assertNotEquals(criteria1, criteria2);
    }

    @Test
    void shouldHandleToString() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .keyword("test").build();

        String str = criteria.toString();
        assertTrue(str.contains("test"));
        assertTrue(str.contains("keyword"));
    }
}
