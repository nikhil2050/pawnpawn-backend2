package com.pnpn.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private UUID id;
    private UUID orderId;
    private UUID reviewerId;
    private UUID sellerId;
    private int rating;
    private String text;
    private List<String> images;
    private Instant createdAt;
    private Instant updatedAt;
}
