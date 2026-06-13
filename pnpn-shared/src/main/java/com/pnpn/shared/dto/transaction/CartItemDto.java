package com.pnpn.shared.dto.transaction;

import com.pnpn.shared.dto.ListingDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private UUID id;
    private UUID userId;
    private UUID listingId;
    private int quantity;
    private Instant addedAt;
    private ListingDto listing;
}
