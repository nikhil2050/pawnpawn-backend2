package com.pnpn.shared.dto.transaction;

import com.pnpn.shared.dto.ListingDto;
import com.pnpn.shared.dto.OrderDto;
import com.pnpn.shared.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private OrderDto order;
    private ListingDto listing;
    private UserDto buyer;
    private UserDto seller;
}
