package com.pnpn.shared.dto.transaction;

import com.pnpn.shared.dto.ListingDto;
import com.pnpn.shared.dto.OrderDto;
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
public class ResaleResponse {
    private UUID id;
    private UUID originalOrderId;
    private UUID buyerId;
    private BigDecimal resalePrice;
    private ListingStatus status;
    private List<String> mediaUrls;
    private Instant createdAt;
    private Instant updatedAt;
    private OrderDto originalOrder;
    private ListingDto originalListing;
}
