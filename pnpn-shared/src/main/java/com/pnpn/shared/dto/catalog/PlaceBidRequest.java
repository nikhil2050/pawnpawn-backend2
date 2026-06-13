package com.pnpn.shared.dto.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class PlaceBidRequest {
    @NotNull(message = "Auction ID is required")
    private UUID auctionId;

    @NotNull(message = "Buyer ID is required")
    private UUID buyerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Bid amount must be positive")
    private BigDecimal amount;
}
