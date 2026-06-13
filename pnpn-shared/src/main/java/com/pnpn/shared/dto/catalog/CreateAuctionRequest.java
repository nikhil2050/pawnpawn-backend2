package com.pnpn.shared.dto.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuctionRequest {
    @NotNull(message = "Listing ID is required")
    private UUID listingId;

    @NotNull(message = "Start price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Start price must be non-negative")
    private BigDecimal startPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Reserve price must be non-negative")
    private BigDecimal reservePrice;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;
}
