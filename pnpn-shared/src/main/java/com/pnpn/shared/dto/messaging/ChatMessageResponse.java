package com.pnpn.shared.dto.messaging;

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
public class ChatMessageResponse {
    private String id;
    private UUID roomId;
    private UUID senderId;
    private String text;
    private String imageUrl;
    private Instant sentAt;
    private boolean read;
    private Instant readAt;
}
