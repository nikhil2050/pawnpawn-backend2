package com.pnpn.shared.dto;

import com.pnpn.shared.enums.OrderStatus;
import com.pnpn.shared.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private UUID id;
    private UUID listingId;
    private UUID auctionId;
    private UUID buyerId;
    private UUID sellerId;
    private BigDecimal amount;
    private OrderStatus status;
    private String deliveryAddress;
    private LocalDate deliveryDate;
    private String trackingNumber;
    private String cancellationReason;
    private PaymentStatus paymentStatus;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private Instant createdAt;
    private Instant updatedAt;
}
