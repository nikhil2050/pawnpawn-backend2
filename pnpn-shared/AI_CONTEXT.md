# pnpn-shared — Shared DTOs, Enums & Exceptions

Library module (no boot application — `spring-boot-maven-plugin` skip=true). All other services depend on this jar.

## Package Structure & Classes

### Config (`com.pnpn.shared.config`)
- `JwtUtil` — HMAC-SHA256 JWT generation/parsing/validation. Reads `jwt.secret` + `jwt.access-token-expiration` from config. Methods: `generateAccessToken`, `parseToken`, `getUserIdFromToken`, `getUserTypeFromToken`, `validateToken`, `isTokenExpired`
- `JwtAuthenticationFilter` — `OncePerRequestFilter`. Extracts Bearer token from `Authorization` header, validates via `JwtUtil`, sets `SecurityContextHolder` with principal=userId(UUID), authority=`ROLE_{userType}`

### Enums (`com.pnpn.shared.enums`)
- `UserType` — INFLUENCER, FOLLOWER, ADMIN
- `ListingCondition` — NEW, LIKE_NEW, GENTLY_USED, USED, FAIR
- `ListingStatus` — ACTIVE, SOLD, WITHDRAWN, RESERVED
- `AuctionStatus` — SCHEDULED, LIVE, EXTENDED, ENDED, SOLD, WITHDRAWN
- `OrderStatus` — PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, REFUNDED
- `PaymentStatus` — PENDING, COMPLETED, FAILED, REFUNDED
- `VerificationStatus` — UNVERIFIED, PENDING, VERIFIED, REJECTED
- `NotificationType` — ORDER, BID, MESSAGE, REVIEW, OFFER, SYSTEM

### Exceptions (`com.pnpn.shared.exception`)
- `PnPnException` — base runtime exception carrying HttpStatus. Constructors: (message), (message, status), (message, cause), (message, cause, status)
- `GlobalExceptionHandler` — `@RestControllerAdvice` handling PnPnException, MethodArgumentNotValidException (validation errors with field-level details), and generic Exception

### DTOs (`com.pnpn.shared.dto`)

**Core DTOs:**
- `UserDto` — id, type, name, email, phone, profilePicUrl, bio, verified, verificationStatus, governmentIdUrl, selfieUrl, followerCount, reputationScore, totalReviews, fcmToken, createdAt, updatedAt
- `ListingDto` — id, influencerId, title, description, price, category, condition, status, mediaUrls, viewCount, createdAt, updatedAt
- `AuctionDto` — id, listingId, startPrice, currentBid, reservePrice, highestBidderId, startTime, endTime, winnerId, status, extensionCount
- `BidDto` — id, auctionId, buyerId, amount, placedAt
- `OrderDto` — id, listingId, auctionId, buyerId, sellerId, amount, status, deliveryAddress, deliveryDate, trackingNumber, cancellationReason, paymentStatus, razorpayOrderId, razorpayPaymentId, createdAt
- `ReviewDto` — id, orderId, reviewerId, sellerId, rating, text, images, createdAt
- `ErrorResponse` — timestamp, status, error, message, path, details (List<String>)
- `UpdateProfileRequest` — name, bio, profilePicUrl, verificationStatus

**Auth DTOs (`com.pnpn.shared.dto.auth`):**
- `RegisterRequest` — @NotBlank name, @Email email, @NotBlank password, phone, type (UserType)
- `LoginRequest` — @Email email, @NotBlank password
- `LoginResponse` — userId, type, accessToken, refreshToken, expiresIn, user (UserDto)
- `OtpRequest` — @Email email
- `OtpVerifyRequest` — @Email email, @NotBlank code
- `RefreshTokenRequest` — userId, refreshToken
- `LogoutRequest` — userId, refreshToken

**Catalog DTOs (`com.pnpn.shared.dto.catalog`):**
- `CreateListingRequest` — @NotBlank title, description, @NotNull price, @NotBlank category, @NotNull condition, mediaUrls
- `ListingSearchCriteria` — query, category, minPrice, maxPrice, condition, status, influencerId, page, size, sortBy, sortDirection
- `CreateAuctionRequest` — @NotNull listingId, @NotNull startPrice, reservePrice, @NotNull startTime, @NotNull endTime
- `AuctionDetailResponse` — auction (AuctionDto), listing (ListingDto), bidHistory (List<BidDto>)
- `PlaceBidRequest` — @NotNull auctionId, @NotNull buyerId, @NotNull amount
- `MediaPresignedUrlResponse` — presignedUrl, mediaUrl
- `ListingResponse` — listing (ListingDto), influencer (UserDto)

**Transaction DTOs (`com.pnpn.shared.dto.transaction`):**
- `AddToCartRequest` — @NotNull listingId, quantity
- `CartItemDto` — id, listingId, title, price, quantity, imageUrl, sellerId, sellerName
- `CartResponse` — items (List<CartItemDto>), subtotal, tax (18% GST), total
- `CheckoutRequest` — cartItemIds (List<UUID>), deliveryAddress
- `OrderDetailResponse` — order (OrderDto), buyer (UserDto), seller (UserDto), items (List<CartItemDto>), timeline
- `CreatePaymentRequest` — @NotNull orderId, @NotNull amount
- `VerifyPaymentRequest` — razorpayOrderId, razorpayPaymentId, razorpaySignature
- `PaymentResponse` — success, message, orderId, status
- `UpdateOrderStatusRequest` — @NotNull status
- `CancelOrderRequest` — cancellationReason
- `CreateReviewRequest` — @NotNull orderId, @Min(1) @Max(5) rating, text, images (List<String>)
- `ReviewResponse` — id, orderId, reviewerId, sellerId, rating, text, images, reviewerName, createdAt
- `CreateResaleRequest` — @NotNull orderId, @NotNull resalePrice, mediaUrls
- `ResaleResponse` — id, originalOrderId, buyerId, resalePrice, status, mediaUrls, originalListing, createdAt

**Messaging DTOs (`com.pnpn.shared.dto.messaging`):**
- `StartChatRequest` — @NotNull listingId, @NotNull otherUserId
- `ChatRoomResponse` — id, listingId, listingTitle, buyerId, sellerId, lastMessage, lastMessageAt, unreadCount
- `ChatMessageResponse` — id, roomId, senderId, text, imageUrl, sentAt, read, readAt
- `NotificationResponse` — id, type, title, body, read, createdAt

## Key Contracts

- Every service depends on `com.pnpn:pnpn-shared:1.0.0-SNAPSHOT` via Maven
- `GlobalExceptionHandler`, `JwtUtil`, `JwtAuthenticationFilter` are picked up automatically by any service scanning `com.pnpn.shared` via `@SpringBootApplication(scanBasePackages = {"com.pnpn.xxx", "com.pnpn.shared"})`
- All DTOs use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` from Lombok
- Validation annotations (`@NotNull`, `@NotBlank`, `@Email`, `@Min`, `@Max`) on request DTOs

## Test classes (8)
- `JwtUtilTest` (15 tests) — token generation, parsing, validation, expiry, malformed/signature edge cases
- `JwtAuthenticationFilterTest` (14 tests) — all user type roles, missing/invalid/expired tokens, exception handling
- `DtoSerializationTest` — JSON serde for all 40 DTOs across all domains
- `ErrorResponseTest` — builder, JSON serde, equals/hashCode
- `ListingResponseTest` — builder, JSON serde, setters
- `EnumsTest` — values, ordinals, name/tostring for all 8 enums
- `GlobalExceptionHandlerTest` — PnPnException, validation errors, generic exceptions
- `PnPnExceptionTest` — all 4 constructor variants

## Configuration

- Spring Boot Starter Validation, Starter Web, Starter Security (for JwtAuthenticationFilter), Jackson
