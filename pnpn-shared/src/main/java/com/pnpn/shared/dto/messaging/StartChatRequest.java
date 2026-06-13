package com.pnpn.shared.dto.messaging;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartChatRequest {
    @NotNull(message = "Listing ID is required")
    private UUID listingId;

    @NotNull(message = "Other User ID is required")
    private UUID otherUserId;
}
