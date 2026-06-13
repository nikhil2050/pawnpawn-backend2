package com.pnpn.shared.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResaleRequest {
    @NotNull(message = "Original Order ID is required")
    private UUID orderId;

    @NotNull(message = "Resale price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Resale price must be non-negative")
    private BigDecimal resalePrice;

    private List<String> mediaUrls;
}
