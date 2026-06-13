package com.pnpn.shared.dto;

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
public class BidDto {
    private UUID id;
    private UUID auctionId;
    private UUID buyerId;
    private BigDecimal amount;
    private Instant placedAt;
}
