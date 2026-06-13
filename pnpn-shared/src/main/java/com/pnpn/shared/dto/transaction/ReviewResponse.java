package com.pnpn.shared.dto.transaction;

import com.pnpn.shared.dto.ReviewDto;
import com.pnpn.shared.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private ReviewDto review;
    private UserDto reviewer;
    private UserDto seller;
}
