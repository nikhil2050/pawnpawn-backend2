package com.pnpn.catalogservice.repository;

import com.pnpn.catalogservice.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomListingRepository {
    Page<Listing> findByCriteria(ListingSearchCriteria criteria, Pageable pageable);
}
