package com.pnpn.catalogservice.repository;

import com.pnpn.catalogservice.entity.Listing;
import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ListingRepositoryTest {

    @Autowired
    private ListingRepository listingRepository;

    private Listing activeGuitar;
    private Listing activeCamera;
    private Listing soldGuitar;
    private UUID influencerA;
    private UUID influencerB;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();

        influencerA = UUID.randomUUID();
        influencerB = UUID.randomUUID();

        activeGuitar = listingRepository.save(Listing.builder()
                .influencerId(influencerA)
                .title("Fender Stratocaster")
                .description("Vintage 1965 guitar")
                .price(BigDecimal.valueOf(2500.00))
                .category("Musical Instruments")
                .condition(ListingCondition.GENTLY_USED)
                .status(ListingStatus.ACTIVE)
                .mediaUrls(List.of("https://example.com/guitar.jpg"))
                .viewCount(100)
                .build());

        activeCamera = listingRepository.save(Listing.builder()
                .influencerId(influencerA)
                .title("Sony A7III")
                .description("Mirrorless camera body")
                .price(BigDecimal.valueOf(1800.00))
                .category("Electronics")
                .condition(ListingCondition.LIKE_NEW)
                .status(ListingStatus.ACTIVE)
                .viewCount(50)
                .build());

        soldGuitar = listingRepository.save(Listing.builder()
                .influencerId(influencerB)
                .title("Gibson Les Paul")
                .description("Used electric guitar")
                .price(BigDecimal.valueOf(1500.00))
                .category("Musical Instruments")
                .condition(ListingCondition.USED)
                .status(ListingStatus.SOLD)
                .viewCount(200)
                .build());
    }

    @Test
    void shouldSaveAndFindById() {
        Optional<Listing> found = listingRepository.findById(activeGuitar.getId());

        assertTrue(found.isPresent());
        assertEquals("Fender Stratocaster", found.get().getTitle());
        assertEquals(influencerA, found.get().getInfluencerId());
    }

    @Test
    void shouldFindByInfluencerId() {
        List<Listing> listings = listingRepository.findByInfluencerId(influencerA);

        assertEquals(2, listings.size());
        assertTrue(listings.stream().allMatch(l -> l.getInfluencerId().equals(influencerA)));
    }

    @Test
    void shouldReturnEmptyForUnknownInfluencer() {
        List<Listing> listings = listingRepository.findByInfluencerId(UUID.randomUUID());

        assertTrue(listings.isEmpty());
    }

    @Test
    void shouldFindByStatus() {
        List<Listing> active = listingRepository.findByStatus(ListingStatus.ACTIVE);

        assertEquals(2, active.size());
        assertTrue(active.stream().allMatch(l -> l.getStatus() == ListingStatus.ACTIVE));
    }

    @Test
    void shouldFindByCategory() {
        List<Listing> instruments = listingRepository.findByCategory("Musical Instruments");

        assertEquals(2, instruments.size());
        assertTrue(instruments.stream().allMatch(l -> l.getCategory().equals("Musical Instruments")));
    }

    @Test
    void shouldReturnEmptyForUnknownCategory() {
        List<Listing> results = listingRepository.findByCategory("Books");

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFindByCondition() {
        List<Listing> used = listingRepository.findByCondition(ListingCondition.USED);

        assertEquals(1, used.size());
        assertEquals("Gibson Les Paul", used.getFirst().getTitle());
    }

    @Test
    void shouldFindByPriceBetween() {
        List<Listing> results = listingRepository.findByPriceBetween(
                BigDecimal.valueOf(1000.00), BigDecimal.valueOf(2000.00));

        assertEquals(2, results.size());
    }

    @Test
    void shouldFindByPriceBetween_ExclusiveBounds() {
        List<Listing> results = listingRepository.findByPriceBetween(
                BigDecimal.valueOf(2500.00), BigDecimal.valueOf(3000.00));

        assertEquals(1, results.size());
        assertEquals("Fender Stratocaster", results.getFirst().getTitle());
    }

    @Test
    void shouldDeleteListing() {
        listingRepository.delete(activeGuitar);

        Optional<Listing> found = listingRepository.findById(activeGuitar.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNonExistentId() {
        Optional<Listing> found = listingRepository.findById(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // findByCriteria (CustomListingRepository — CriteriaBuilder)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void findByCriteria_shouldReturnAllWhenNoFilters() {
        Page<Listing> page = listingRepository.findByCriteria(
                new ListingSearchCriteria(), PageRequest.of(0, 10));

        assertEquals(3, page.getTotalElements());
        assertEquals(3, page.getContent().size());
    }

    @Test
    void findByCriteria_shouldFilterByCategory() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .category("Electronics")
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Sony A7III", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldFilterByStatus() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .status(ListingStatus.SOLD)
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Gibson Les Paul", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldFilterByCondition() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .condition(ListingCondition.GENTLY_USED)
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Fender Stratocaster", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldFilterByInfluencerId() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .influencerId(influencerB)
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Gibson Les Paul", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldFilterByMinPrice() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .minPrice(BigDecimal.valueOf(2000.00))
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Fender Stratocaster", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldFilterByMaxPrice() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .maxPrice(BigDecimal.valueOf(1600.00))
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Gibson Les Paul", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldFilterByPriceRange() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .minPrice(BigDecimal.valueOf(1000.00))
                .maxPrice(BigDecimal.valueOf(2000.00))
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findByCriteria_shouldFilterByKeywordInTitle() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .keyword("stratocaster")
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Fender Stratocaster", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldFilterByKeywordInDescription() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .keyword("mirrorless")
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Sony A7III", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldFilterByKeywordCaseInsensitive() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .keyword("FENDER")
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findByCriteria_shouldReturnEmptyForNonMatchingKeyword() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .keyword("nonexistent")
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findByCriteria_shouldCombineMultipleFilters() {
        ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .category("Musical Instruments")
                .status(ListingStatus.ACTIVE)
                .condition(ListingCondition.GENTLY_USED)
                .build();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Fender Stratocaster", page.getContent().getFirst().getTitle());
    }

    @Test
    void findByCriteria_shouldSupportPagination() {
        ListingSearchCriteria criteria = new ListingSearchCriteria();

        Page<Listing> firstPage = listingRepository.findByCriteria(criteria, PageRequest.of(0, 2));
        Page<Listing> secondPage = listingRepository.findByCriteria(criteria, PageRequest.of(1, 2));

        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
    }

    @Test
    void findByCriteria_shouldOrderByCreatedAtDesc() {
        ListingSearchCriteria criteria = new ListingSearchCriteria();

        Page<Listing> page = listingRepository.findByCriteria(criteria, PageRequest.of(0, 10));

        assertTrue(page.getContent().get(0).getCreatedAt()
                .isAfter(page.getContent().get(2).getCreatedAt())
                || page.getContent().get(0).getCreatedAt()
                .equals(page.getContent().get(2).getCreatedAt()));
    }
}
