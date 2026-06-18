package com.pnpn.shared.dto.catalog;

import com.pnpn.shared.dto.ListingDto;
import com.pnpn.shared.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {
    private ListingDto listing;
    private UserDto influencer;
}
