package com.pnpn.catalogservice.repository;

import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingSearchCriteria {
    private String category;
    private ListingCondition condition;
    private ListingStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private UUID influencerId;
    private String keyword;
}
