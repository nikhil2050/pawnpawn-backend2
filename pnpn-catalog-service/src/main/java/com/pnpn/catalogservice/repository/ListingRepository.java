package com.pnpn.catalogservice.repository;

import com.pnpn.catalogservice.entity.Listing;
import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID>, CustomListingRepository {

    List<Listing> findByInfluencerId(UUID influencerId);

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findByCategory(String category);

    List<Listing> findByCondition(ListingCondition condition);

    List<Listing> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
}
