package com.pnpn.shared.dto.messaging;

import com.pnpn.shared.dto.ListingDto;
import com.pnpn.shared.dto.UserDto;
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
public class ChatRoomResponse {
    private UUID id;
    private UUID listingId;
    private UUID buyerId;
    private UUID sellerId;
    private Instant lastMessageAt;
    private Instant createdAt;
    private Instant updatedAt;
    private ListingDto listing;
    private UserDto otherUser;
    private long unreadCount;
}
