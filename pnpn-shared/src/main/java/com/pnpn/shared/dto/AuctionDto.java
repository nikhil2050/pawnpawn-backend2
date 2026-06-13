package com.pnpn.shared.dto;

import com.pnpn.shared.enums.AuctionStatus;
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
public class AuctionDto {
    private UUID id;
    private UUID listingId;
    private BigDecimal startPrice;
    private BigDecimal currentBid;
    private BigDecimal reservePrice;
    private UUID highestBidderId;
    private Instant startTime;
    private Instant endTime;
    private UUID winnerId;
    private AuctionStatus status;
    private int extensionCount;
    private Instant createdAt;
    private Instant updatedAt;
}
