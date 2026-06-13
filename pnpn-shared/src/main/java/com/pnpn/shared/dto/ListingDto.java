package com.pnpn.shared.dto;

import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDto {
    private UUID id;
    private UUID influencerId;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private ListingCondition condition;
    private ListingStatus status;
    private List<String> mediaUrls;
    private int viewCount;
    private Instant createdAt;
    private Instant updatedAt;
}
