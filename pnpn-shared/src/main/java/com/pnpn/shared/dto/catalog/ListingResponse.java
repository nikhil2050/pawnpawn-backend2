package com.pnpn.shared.dto.catalog;

import com.pnpn.shared.dto.ListingDto;
import com.pnpn.shared.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private ListingDto listing;
    private UserDto influencer;
}
