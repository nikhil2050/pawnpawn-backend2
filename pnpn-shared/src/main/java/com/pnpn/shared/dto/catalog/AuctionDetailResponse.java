package com.pnpn.shared.dto.catalog;

import com.pnpn.shared.dto.AuctionDto;
import com.pnpn.shared.dto.BidDto;
import com.pnpn.shared.dto.ListingDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionDetailResponse {
    private AuctionDto auction;
    private ListingDto listing;
    private List<BidDto> bidHistory;
}
