package com.pnpn.shared.dto.catalog;

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
    private String query;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private ListingCondition condition;
    private ListingStatus status;
    private UUID influencerId;
    private int page;
    private int size;
    private String sortBy;
    private String sortDirection;
}
