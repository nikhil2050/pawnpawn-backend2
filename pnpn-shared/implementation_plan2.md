# Implementation Plan - Shared DTOs (Prompt 1.6)

Implement DTOs, request objects, and response wrappers in the `pnpn-shared` module to provide a standard data contract across microservices.

## User Review Required

> [!NOTE]
> We will also introduce two additional enums: `AuctionStatus` and `PaymentStatus`, which were not explicitly mentioned in Prompt 1.5 but are defined in `init.sql` and required for `AuctionDto` and `OrderDto` fields.

## Proposed Changes

### `pnpn-shared` Module

#### [NEW] Enums
- `com.pnpn.shared.enums.AuctionStatus`: `SCHEDULED`, `LIVE`, `EXTENDED`, `ENDED`, `SOLD`, `WITHDRAWN`
- `com.pnpn.shared.enums.PaymentStatus`: `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`

#### [NEW] Core DTOs
Create package `com.pnpn.shared.dto` containing:
- `UserDto.java`
- `ListingDto.java`
- `OrderDto.java`
- `AuctionDto.java`
- `BidDto.java`
- `ReviewDto.java`

#### [NEW] Request/Response DTOs
Create package `com.pnpn.shared.dto.auth` / `com.pnpn.shared.dto.catalog` / `com.pnpn.shared.dto.transaction` / `com.pnpn.shared.dto.messaging` containing:
- **Auth**: `RegisterRequest.java`, `LoginRequest.java`, `OtpRequest.java`, `OtpVerifyRequest.java`, `LoginResponse.java`
- **Catalog**: `CreateListingRequest.java`, `ListingSearchCriteria.java`, `MediaPresignedUrlResponse.java`
- **Auction**: `CreateAuctionRequest.java`, `PlaceBidRequest.java`, `AuctionDetailResponse.java`
- **Order/Cart**: `UpdateOrderStatusRequest.java`, `CancelOrderRequest.java`, `OrderDetailResponse.java`, `AddToCartRequest.java`, `CheckoutRequest.java`, `CartResponse.java`, `CartItemDto.java`
- **Payment**: `CreatePaymentRequest.java`, `VerifyPaymentRequest.java`, `PaymentResponse.java`
- **Chat**: `StartChatRequest.java`, `ChatRoomResponse.java`, `ChatMessageResponse.java`
- **Review**: `CreateReviewRequest.java`, `ReviewResponse.java`
- **Notification**: `NotificationResponse.java`
- **Resale**: `CreateResaleRequest.java`, `ResaleResponse.java`

---

## Verification Plan

### Automated Tests
- Run `mvn clean install` to ensure code compiles and lombok/validation annotations work correctly.
- Implement a test to verify serialization/deserialization of these DTOs using Jackson.
