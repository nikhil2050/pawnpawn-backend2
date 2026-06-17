package com.pnpn.shared.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pnpn.shared.dto.auth.LoginRequest;
import com.pnpn.shared.dto.auth.LoginResponse;
import com.pnpn.shared.dto.auth.LogoutRequest;
import com.pnpn.shared.dto.auth.OtpRequest;
import com.pnpn.shared.dto.auth.OtpVerifyRequest;
import com.pnpn.shared.dto.auth.RefreshTokenRequest;
import com.pnpn.shared.dto.auth.RegisterRequest;
import com.pnpn.shared.dto.catalog.AuctionDetailResponse;
import com.pnpn.shared.dto.catalog.CreateAuctionRequest;
import com.pnpn.shared.dto.catalog.CreateListingRequest;
import com.pnpn.shared.dto.catalog.ListingSearchCriteria;
import com.pnpn.shared.dto.catalog.MediaPresignedUrlResponse;
import com.pnpn.shared.dto.catalog.PlaceBidRequest;
import com.pnpn.shared.dto.messaging.ChatMessageResponse;
import com.pnpn.shared.dto.messaging.ChatRoomResponse;
import com.pnpn.shared.dto.messaging.NotificationResponse;
import com.pnpn.shared.dto.messaging.StartChatRequest;
import com.pnpn.shared.dto.transaction.AddToCartRequest;
import com.pnpn.shared.dto.transaction.CancelOrderRequest;
import com.pnpn.shared.dto.transaction.CartItemDto;
import com.pnpn.shared.dto.transaction.CartResponse;
import com.pnpn.shared.dto.transaction.CheckoutRequest;
import com.pnpn.shared.dto.transaction.CreatePaymentRequest;
import com.pnpn.shared.dto.transaction.CreateResaleRequest;
import com.pnpn.shared.dto.transaction.CreateReviewRequest;
import com.pnpn.shared.dto.transaction.OrderDetailResponse;
import com.pnpn.shared.dto.transaction.PaymentResponse;
import com.pnpn.shared.dto.transaction.ResaleResponse;
import com.pnpn.shared.dto.transaction.ReviewResponse;
import com.pnpn.shared.dto.transaction.UpdateOrderStatusRequest;
import com.pnpn.shared.dto.transaction.VerifyPaymentRequest;
import com.pnpn.shared.enums.AuctionStatus;
import com.pnpn.shared.enums.ListingCondition;
import com.pnpn.shared.enums.ListingStatus;
import com.pnpn.shared.enums.NotificationType;
import com.pnpn.shared.enums.OrderStatus;
import com.pnpn.shared.enums.PaymentStatus;
import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.enums.VerificationStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void testUserDtoSerialization() throws IOException {
        UserDto dto = UserDto.builder()
                .id(UUID.randomUUID())
                .name("Jane Doe")
                .email("jane@example.com")
                .type(UserType.INFLUENCER)
                .reputationScore(BigDecimal.valueOf(4.85))
                .createdAt(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);

        UserDto deserialized = objectMapper.readValue(json, UserDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getName(), deserialized.getName());
        assertEquals(dto.getEmail(), deserialized.getEmail());
        assertEquals(dto.getType(), deserialized.getType());
        assertEquals(dto.getReputationScore(), deserialized.getReputationScore());
        assertEquals(dto.getCreatedAt(), deserialized.getCreatedAt());
    }

    @Test
    void testUserDtoFullSerialization() throws IOException {
        Instant now = Instant.now();
        UserDto dto = UserDto.builder()
                .id(UUID.randomUUID())
                .type(UserType.FOLLOWER)
                .name("John")
                .email("john@test.com")
                .phone("+1234567890")
                .profilePicUrl("https://example.com/pic.jpg")
                .bio("Bio text")
                .verified(true)
                .verificationStatus(VerificationStatus.VERIFIED)
                .governmentIdUrl("https://example.com/id.jpg")
                .selfieUrl("https://example.com/selfie.jpg")
                .followerCount(100)
                .reputationScore(BigDecimal.valueOf(4.5))
                .totalReviews(10)
                .fcmToken("token123")
                .createdAt(now)
                .updatedAt(now)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        UserDto deserialized = objectMapper.readValue(json, UserDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getType(), deserialized.getType());
        assertEquals(dto.getName(), deserialized.getName());
        assertEquals(dto.getEmail(), deserialized.getEmail());
        assertEquals(dto.getPhone(), deserialized.getPhone());
        assertEquals(dto.getProfilePicUrl(), deserialized.getProfilePicUrl());
        assertEquals(dto.getBio(), deserialized.getBio());
        assertEquals(dto.isVerified(), deserialized.isVerified());
        assertEquals(dto.getVerificationStatus(), deserialized.getVerificationStatus());
        assertEquals(dto.getGovernmentIdUrl(), deserialized.getGovernmentIdUrl());
        assertEquals(dto.getSelfieUrl(), deserialized.getSelfieUrl());
        assertEquals(dto.getFollowerCount(), deserialized.getFollowerCount());
        assertEquals(dto.getReputationScore(), deserialized.getReputationScore());
        assertEquals(dto.getTotalReviews(), deserialized.getTotalReviews());
        assertEquals(dto.getFcmToken(), deserialized.getFcmToken());
        assertEquals(dto.getCreatedAt(), deserialized.getCreatedAt());
        assertEquals(dto.getUpdatedAt(), deserialized.getUpdatedAt());
    }

    @Test
    void testListingDtoSerialization() throws IOException {
        Instant now = Instant.now();
        ListingDto dto = ListingDto.builder()
                .id(UUID.randomUUID())
                .influencerId(UUID.randomUUID())
                .title("Test Listing")
                .description("Description")
                .price(BigDecimal.valueOf(99.99))
                .category("Electronics")
                .condition(ListingCondition.NEW)
                .status(ListingStatus.ACTIVE)
                .mediaUrls(List.of("https://example.com/img1.jpg"))
                .viewCount(42)
                .createdAt(now)
                .updatedAt(now)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ListingDto deserialized = objectMapper.readValue(json, ListingDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getInfluencerId(), deserialized.getInfluencerId());
        assertEquals(dto.getTitle(), deserialized.getTitle());
        assertEquals(dto.getDescription(), deserialized.getDescription());
        assertEquals(dto.getPrice(), deserialized.getPrice());
        assertEquals(dto.getCategory(), deserialized.getCategory());
        assertEquals(dto.getCondition(), deserialized.getCondition());
        assertEquals(dto.getStatus(), deserialized.getStatus());
        assertEquals(dto.getMediaUrls(), deserialized.getMediaUrls());
        assertEquals(dto.getViewCount(), deserialized.getViewCount());
        assertEquals(dto.getCreatedAt(), deserialized.getCreatedAt());
        assertEquals(dto.getUpdatedAt(), deserialized.getUpdatedAt());
    }

    @Test
    void testOrderDtoSerialization() throws IOException {
        Instant now = Instant.now();
        OrderDto dto = OrderDto.builder()
                .id(UUID.randomUUID())
                .listingId(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(199.99))
                .status(OrderStatus.CONFIRMED)
                .deliveryAddress("123 Main St")
                .deliveryDate(LocalDate.of(2025, 6, 15))
                .trackingNumber("TRACK123")
                .paymentStatus(PaymentStatus.COMPLETED)
                .razorpayOrderId("order_id")
                .razorpayPaymentId("payment_id")
                .createdAt(now)
                .updatedAt(now)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        OrderDto deserialized = objectMapper.readValue(json, OrderDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getListingId(), deserialized.getListingId());
        assertEquals(dto.getBuyerId(), deserialized.getBuyerId());
        assertEquals(dto.getSellerId(), deserialized.getSellerId());
        assertEquals(dto.getAmount(), deserialized.getAmount());
        assertEquals(dto.getStatus(), deserialized.getStatus());
        assertEquals(dto.getDeliveryAddress(), deserialized.getDeliveryAddress());
        assertEquals(dto.getDeliveryDate(), deserialized.getDeliveryDate());
        assertEquals(dto.getTrackingNumber(), deserialized.getTrackingNumber());
        assertEquals(dto.getPaymentStatus(), deserialized.getPaymentStatus());
        assertEquals(dto.getRazorpayOrderId(), deserialized.getRazorpayOrderId());
        assertEquals(dto.getRazorpayPaymentId(), deserialized.getRazorpayPaymentId());
        assertEquals(dto.getCreatedAt(), deserialized.getCreatedAt());
        assertEquals(dto.getUpdatedAt(), deserialized.getUpdatedAt());
        assertNull(deserialized.getCancellationReason());
    }

    @Test
    void testAuctionDtoSerialization() throws IOException {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        AuctionDto dto = AuctionDto.builder()
                .id(id)
                .listingId(UUID.randomUUID())
                .startPrice(BigDecimal.valueOf(100))
                .currentBid(BigDecimal.valueOf(150))
                .reservePrice(BigDecimal.valueOf(200))
                .highestBidderId(UUID.randomUUID())
                .startTime(now)
                .endTime(now.plusSeconds(3600))
                .winnerId(UUID.randomUUID())
                .status(AuctionStatus.LIVE)
                .extensionCount(2)
                .createdAt(now)
                .updatedAt(now)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        AuctionDto deserialized = objectMapper.readValue(json, AuctionDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getListingId(), deserialized.getListingId());
        assertEquals(dto.getStartPrice(), deserialized.getStartPrice());
        assertEquals(dto.getCurrentBid(), deserialized.getCurrentBid());
        assertEquals(dto.getReservePrice(), deserialized.getReservePrice());
        assertEquals(dto.getHighestBidderId(), deserialized.getHighestBidderId());
        assertEquals(dto.getStartTime(), deserialized.getStartTime());
        assertEquals(dto.getEndTime(), deserialized.getEndTime());
        assertEquals(dto.getWinnerId(), deserialized.getWinnerId());
        assertEquals(dto.getStatus(), deserialized.getStatus());
        assertEquals(dto.getExtensionCount(), deserialized.getExtensionCount());
    }

    @Test
    void testBidDtoSerialization() throws IOException {
        Instant now = Instant.now();
        BidDto dto = BidDto.builder()
                .id(UUID.randomUUID())
                .auctionId(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(250.00))
                .placedAt(now)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        BidDto deserialized = objectMapper.readValue(json, BidDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getAuctionId(), deserialized.getAuctionId());
        assertEquals(dto.getBuyerId(), deserialized.getBuyerId());
        assertEquals(dto.getAmount(), deserialized.getAmount());
        assertEquals(dto.getPlacedAt(), deserialized.getPlacedAt());
    }

    @Test
    void testReviewDtoSerialization() throws IOException {
        Instant now = Instant.now();
        ReviewDto dto = ReviewDto.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .reviewerId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .rating(5)
                .text("Great product!")
                .images(List.of("img1.jpg", "img2.jpg"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ReviewDto deserialized = objectMapper.readValue(json, ReviewDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getOrderId(), deserialized.getOrderId());
        assertEquals(dto.getReviewerId(), deserialized.getReviewerId());
        assertEquals(dto.getSellerId(), deserialized.getSellerId());
        assertEquals(dto.getRating(), deserialized.getRating());
        assertEquals(dto.getText(), deserialized.getText());
        assertEquals(dto.getImages(), deserialized.getImages());
        assertEquals(dto.getCreatedAt(), deserialized.getCreatedAt());
        assertEquals(dto.getUpdatedAt(), deserialized.getUpdatedAt());
    }

    @Test
    void testUpdateProfileRequestSerialization() throws IOException {
        UpdateProfileRequest dto = UpdateProfileRequest.builder()
                .name("New Name")
                .bio("New bio")
                .profilePicUrl("https://example.com/new.jpg")
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        UpdateProfileRequest deserialized = objectMapper.readValue(json, UpdateProfileRequest.class);
        assertEquals(dto.getName(), deserialized.getName());
        assertEquals(dto.getBio(), deserialized.getBio());
        assertEquals(dto.getProfilePicUrl(), deserialized.getProfilePicUrl());
        assertEquals(dto.getVerificationStatus(), deserialized.getVerificationStatus());
    }

    // --- Auth DTOs ---

    @Test
    void testLoginRequestSerialization() throws IOException {
        LoginRequest dto = LoginRequest.builder()
                .email("test@example.com")
                .password("secret123")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        LoginRequest deserialized = objectMapper.readValue(json, LoginRequest.class);
        assertEquals(dto.getEmail(), deserialized.getEmail());
        assertEquals(dto.getPassword(), deserialized.getPassword());
    }

    @Test
    void testLoginResponseSerialization() throws IOException {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .name("User")
                .email("u@example.com")
                .type(UserType.INFLUENCER)
                .build();

        LoginResponse dto = LoginResponse.builder()
                .userId(UUID.randomUUID())
                .type(UserType.ADMIN)
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .expiresIn(3600L)
                .user(user)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        LoginResponse deserialized = objectMapper.readValue(json, LoginResponse.class);
        assertEquals(dto.getUserId(), deserialized.getUserId());
        assertEquals(dto.getType(), deserialized.getType());
        assertEquals(dto.getAccessToken(), deserialized.getAccessToken());
        assertEquals(dto.getRefreshToken(), deserialized.getRefreshToken());
        assertEquals(dto.getExpiresIn(), deserialized.getExpiresIn());
        assertEquals(dto.getUser().getId(), deserialized.getUser().getId());
        assertEquals(dto.getUser().getName(), deserialized.getUser().getName());
    }

    @Test
    void testRegisterRequestSerialization() throws IOException {
        RegisterRequest dto = RegisterRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .phone("+1234567890")
                .password("password123")
                .type(UserType.FOLLOWER)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        RegisterRequest deserialized = objectMapper.readValue(json, RegisterRequest.class);
        assertEquals(dto.getName(), deserialized.getName());
        assertEquals(dto.getEmail(), deserialized.getEmail());
        assertEquals(dto.getPhone(), deserialized.getPhone());
        assertEquals(dto.getPassword(), deserialized.getPassword());
        assertEquals(dto.getType(), deserialized.getType());
    }

    @Test
    void testLogoutRequestSerialization() throws IOException {
        LogoutRequest dto = LogoutRequest.builder()
                .userId(UUID.randomUUID())
                .refreshToken("some_refresh_token")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        LogoutRequest deserialized = objectMapper.readValue(json, LogoutRequest.class);
        assertEquals(dto.getUserId(), deserialized.getUserId());
        assertEquals(dto.getRefreshToken(), deserialized.getRefreshToken());
    }

    @Test
    void testOtpRequestSerialization() throws IOException {
        OtpRequest dto = OtpRequest.builder()
                .email("user@example.com")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        OtpRequest deserialized = objectMapper.readValue(json, OtpRequest.class);
        assertEquals(dto.getEmail(), deserialized.getEmail());
    }

    @Test
    void testOtpVerifyRequestSerialization() throws IOException {
        OtpVerifyRequest dto = OtpVerifyRequest.builder()
                .email("user@example.com")
                .code("1234")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        OtpVerifyRequest deserialized = objectMapper.readValue(json, OtpVerifyRequest.class);
        assertEquals(dto.getEmail(), deserialized.getEmail());
        assertEquals(dto.getCode(), deserialized.getCode());
    }

    @Test
    void testRefreshTokenRequestSerialization() throws IOException {
        RefreshTokenRequest dto = RefreshTokenRequest.builder()
                .userId(UUID.randomUUID())
                .refreshToken("refresh_token")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        RefreshTokenRequest deserialized = objectMapper.readValue(json, RefreshTokenRequest.class);
        assertEquals(dto.getUserId(), deserialized.getUserId());
        assertEquals(dto.getRefreshToken(), deserialized.getRefreshToken());
    }

    // --- Catalog DTOs ---

    @Test
    void testCreateListingRequestSerialization() throws IOException {
        CreateListingRequest dto = CreateListingRequest.builder()
                .title("My Listing")
                .description("Great item")
                .price(BigDecimal.valueOf(49.99))
                .category("Books")
                .condition(ListingCondition.GENTLY_USED)
                .mediaUrls(List.of("url1", "url2"))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CreateListingRequest deserialized = objectMapper.readValue(json, CreateListingRequest.class);
        assertEquals(dto.getTitle(), deserialized.getTitle());
        assertEquals(dto.getDescription(), deserialized.getDescription());
        assertEquals(dto.getPrice(), deserialized.getPrice());
        assertEquals(dto.getCategory(), deserialized.getCategory());
        assertEquals(dto.getCondition(), deserialized.getCondition());
        assertEquals(dto.getMediaUrls(), deserialized.getMediaUrls());
    }

    @Test
    void testListingSearchCriteriaSerialization() throws IOException {
        ListingSearchCriteria dto = ListingSearchCriteria.builder()
                .query("camera")
                .category("Electronics")
                .minPrice(BigDecimal.valueOf(10))
                .maxPrice(BigDecimal.valueOf(1000))
                .condition(ListingCondition.NEW)
                .status(ListingStatus.ACTIVE)
                .influencerId(UUID.randomUUID())
                .page(0)
                .size(20)
                .sortBy("price")
                .sortDirection("asc")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ListingSearchCriteria deserialized = objectMapper.readValue(json, ListingSearchCriteria.class);
        assertEquals(dto.getQuery(), deserialized.getQuery());
        assertEquals(dto.getCategory(), deserialized.getCategory());
        assertEquals(dto.getMinPrice(), deserialized.getMinPrice());
        assertEquals(dto.getMaxPrice(), deserialized.getMaxPrice());
        assertEquals(dto.getCondition(), deserialized.getCondition());
        assertEquals(dto.getStatus(), deserialized.getStatus());
        assertEquals(dto.getInfluencerId(), deserialized.getInfluencerId());
        assertEquals(dto.getPage(), deserialized.getPage());
        assertEquals(dto.getSize(), deserialized.getSize());
        assertEquals(dto.getSortBy(), deserialized.getSortBy());
        assertEquals(dto.getSortDirection(), deserialized.getSortDirection());
    }

    @Test
    void testCreateAuctionRequestSerialization() throws IOException {
        Instant now = Instant.now();
        CreateAuctionRequest dto = CreateAuctionRequest.builder()
                .listingId(UUID.randomUUID())
                .startPrice(BigDecimal.valueOf(50))
                .reservePrice(BigDecimal.valueOf(100))
                .startTime(now)
                .endTime(now.plusSeconds(86400))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CreateAuctionRequest deserialized = objectMapper.readValue(json, CreateAuctionRequest.class);
        assertEquals(dto.getListingId(), deserialized.getListingId());
        assertEquals(dto.getStartPrice(), deserialized.getStartPrice());
        assertEquals(dto.getReservePrice(), deserialized.getReservePrice());
        assertEquals(dto.getStartTime(), deserialized.getStartTime());
        assertEquals(dto.getEndTime(), deserialized.getEndTime());
    }

    @Test
    void testPlaceBidRequestSerialization() throws IOException {
        PlaceBidRequest dto = PlaceBidRequest.builder()
                .auctionId(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(75.50))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        PlaceBidRequest deserialized = objectMapper.readValue(json, PlaceBidRequest.class);
        assertEquals(dto.getAuctionId(), deserialized.getAuctionId());
        assertEquals(dto.getBuyerId(), deserialized.getBuyerId());
        assertEquals(dto.getAmount(), deserialized.getAmount());
    }

    @Test
    void testAuctionDetailResponseSerialization() throws IOException {
        AuctionDto auction = AuctionDto.builder()
                .id(UUID.randomUUID()).status(AuctionStatus.LIVE).build();
        ListingDto listing = ListingDto.builder()
                .id(UUID.randomUUID()).title("Listing").build();
        BidDto bid = BidDto.builder()
                .id(UUID.randomUUID()).amount(BigDecimal.TEN).build();

        AuctionDetailResponse dto = AuctionDetailResponse.builder()
                .auction(auction)
                .listing(listing)
                .bidHistory(List.of(bid))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        AuctionDetailResponse deserialized = objectMapper.readValue(json, AuctionDetailResponse.class);
        assertEquals(dto.getAuction().getId(), deserialized.getAuction().getId());
        assertEquals(dto.getListing().getId(), deserialized.getListing().getId());
        assertEquals(1, deserialized.getBidHistory().size());
        assertEquals(dto.getBidHistory().get(0).getId(), deserialized.getBidHistory().get(0).getId());
    }

    @Test
    void testMediaPresignedUrlResponseSerialization() throws IOException {
        MediaPresignedUrlResponse dto = MediaPresignedUrlResponse.builder()
                .presignedUrl("https://presigned.example.com/upload")
                .mediaUrl("https://media.example.com/img.jpg")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        MediaPresignedUrlResponse deserialized = objectMapper.readValue(json, MediaPresignedUrlResponse.class);
        assertEquals(dto.getPresignedUrl(), deserialized.getPresignedUrl());
        assertEquals(dto.getMediaUrl(), deserialized.getMediaUrl());
    }

    // --- Messaging DTOs ---

    @Test
    void testStartChatRequestSerialization() throws IOException {
        StartChatRequest dto = StartChatRequest.builder()
                .listingId(UUID.randomUUID())
                .otherUserId(UUID.randomUUID())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        StartChatRequest deserialized = objectMapper.readValue(json, StartChatRequest.class);
        assertEquals(dto.getListingId(), deserialized.getListingId());
        assertEquals(dto.getOtherUserId(), deserialized.getOtherUserId());
    }

    @Test
    void testChatRoomResponseSerialization() throws IOException {
        ChatRoomResponse dto = ChatRoomResponse.builder()
                .id(UUID.randomUUID())
                .listingId(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .lastMessageAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .listing(ListingDto.builder().id(UUID.randomUUID()).title("Item").build())
                .otherUser(UserDto.builder().id(UUID.randomUUID()).name("Other").build())
                .unreadCount(3)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ChatRoomResponse deserialized = objectMapper.readValue(json, ChatRoomResponse.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getListingId(), deserialized.getListingId());
        assertEquals(dto.getBuyerId(), deserialized.getBuyerId());
        assertEquals(dto.getSellerId(), deserialized.getSellerId());
        assertEquals(dto.getListing().getId(), deserialized.getListing().getId());
        assertEquals(dto.getOtherUser().getId(), deserialized.getOtherUser().getId());
        assertEquals(dto.getUnreadCount(), deserialized.getUnreadCount());
    }

    @Test
    void testChatMessageResponseSerialization() throws IOException {
        ChatMessageResponse dto = ChatMessageResponse.builder()
                .id("msg_1")
                .roomId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .text("Hello!")
                .imageUrl("https://example.com/img.jpg")
                .sentAt(Instant.now())
                .read(true)
                .readAt(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ChatMessageResponse deserialized = objectMapper.readValue(json, ChatMessageResponse.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getRoomId(), deserialized.getRoomId());
        assertEquals(dto.getSenderId(), deserialized.getSenderId());
        assertEquals(dto.getText(), deserialized.getText());
        assertEquals(dto.getImageUrl(), deserialized.getImageUrl());
        assertEquals(dto.isRead(), deserialized.isRead());
    }

    @Test
    void testNotificationResponseSerialization() throws IOException {
        NotificationResponse dto = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.ORDER)
                .title("Order Update")
                .body("Your order has shipped")
                .data(Map.of("orderId", "12345"))
                .read(false)
                .createdAt(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        NotificationResponse deserialized = objectMapper.readValue(json, NotificationResponse.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getType(), deserialized.getType());
        assertEquals(dto.getTitle(), deserialized.getTitle());
        assertEquals(dto.getBody(), deserialized.getBody());
        assertEquals(dto.getData(), deserialized.getData());
        assertEquals(dto.isRead(), deserialized.isRead());
    }

    // --- Transaction DTOs ---

    @Test
    void testAddToCartRequestSerialization() throws IOException {
        AddToCartRequest dto = AddToCartRequest.builder()
                .listingId(UUID.randomUUID())
                .quantity(2)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        AddToCartRequest deserialized = objectMapper.readValue(json, AddToCartRequest.class);
        assertEquals(dto.getListingId(), deserialized.getListingId());
        assertEquals(dto.getQuantity(), deserialized.getQuantity());
    }

    @Test
    void testCartItemDtoSerialization() throws IOException {
        CartItemDto dto = CartItemDto.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .listingId(UUID.randomUUID())
                .quantity(1)
                .addedAt(Instant.now())
                .listing(ListingDto.builder().id(UUID.randomUUID()).title("Item").build())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CartItemDto deserialized = objectMapper.readValue(json, CartItemDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getUserId(), deserialized.getUserId());
        assertEquals(dto.getListingId(), deserialized.getListingId());
        assertEquals(dto.getQuantity(), deserialized.getQuantity());
        assertEquals(dto.getListing().getId(), deserialized.getListing().getId());
    }

    @Test
    void testCartResponseSerialization() throws IOException {
        CartResponse dto = CartResponse.builder()
                .items(List.of(
                    CartItemDto.builder().id(UUID.randomUUID()).quantity(1).build()
                ))
                .subtotal(BigDecimal.valueOf(100))
                .tax(BigDecimal.valueOf(10))
                .total(BigDecimal.valueOf(110))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CartResponse deserialized = objectMapper.readValue(json, CartResponse.class);
        assertEquals(1, deserialized.getItems().size());
        assertEquals(dto.getSubtotal(), deserialized.getSubtotal());
        assertEquals(dto.getTax(), deserialized.getTax());
        assertEquals(dto.getTotal(), deserialized.getTotal());
    }

    @Test
    void testCheckoutRequestSerialization() throws IOException {
        CheckoutRequest dto = CheckoutRequest.builder()
                .deliveryAddress("456 Oak Ave")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CheckoutRequest deserialized = objectMapper.readValue(json, CheckoutRequest.class);
        assertEquals(dto.getDeliveryAddress(), deserialized.getDeliveryAddress());
    }

    @Test
    void testCancelOrderRequestSerialization() throws IOException {
        CancelOrderRequest dto = CancelOrderRequest.builder()
                .reason("Changed my mind")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CancelOrderRequest deserialized = objectMapper.readValue(json, CancelOrderRequest.class);
        assertEquals(dto.getReason(), deserialized.getReason());
    }

    @Test
    void testCreatePaymentRequestSerialization() throws IOException {
        CreatePaymentRequest dto = CreatePaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(99.99))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CreatePaymentRequest deserialized = objectMapper.readValue(json, CreatePaymentRequest.class);
        assertEquals(dto.getOrderId(), deserialized.getOrderId());
        assertEquals(dto.getAmount(), deserialized.getAmount());
    }

    @Test
    void testVerifyPaymentRequestSerialization() throws IOException {
        VerifyPaymentRequest dto = VerifyPaymentRequest.builder()
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_456")
                .razorpaySignature("sig_789")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        VerifyPaymentRequest deserialized = objectMapper.readValue(json, VerifyPaymentRequest.class);
        assertEquals(dto.getRazorpayOrderId(), deserialized.getRazorpayOrderId());
        assertEquals(dto.getRazorpayPaymentId(), deserialized.getRazorpayPaymentId());
        assertEquals(dto.getRazorpaySignature(), deserialized.getRazorpaySignature());
    }

    @Test
    void testPaymentResponseSerialization() throws IOException {
        PaymentResponse dto = PaymentResponse.builder()
                .success(true)
                .message("Payment completed")
                .orderId(UUID.randomUUID())
                .status("COMPLETED")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        PaymentResponse deserialized = objectMapper.readValue(json, PaymentResponse.class);
        assertEquals(dto.isSuccess(), deserialized.isSuccess());
        assertEquals(dto.getMessage(), deserialized.getMessage());
        assertEquals(dto.getOrderId(), deserialized.getOrderId());
        assertEquals(dto.getStatus(), deserialized.getStatus());
    }

    @Test
    void testCreateResaleRequestSerialization() throws IOException {
        CreateResaleRequest dto = CreateResaleRequest.builder()
                .orderId(UUID.randomUUID())
                .resalePrice(BigDecimal.valueOf(150))
                .mediaUrls(List.of("url1"))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CreateResaleRequest deserialized = objectMapper.readValue(json, CreateResaleRequest.class);
        assertEquals(dto.getOrderId(), deserialized.getOrderId());
        assertEquals(dto.getResalePrice(), deserialized.getResalePrice());
        assertEquals(dto.getMediaUrls(), deserialized.getMediaUrls());
    }

    @Test
    void testResaleResponseSerialization() throws IOException {
        ResaleResponse dto = ResaleResponse.builder()
                .id(UUID.randomUUID())
                .originalOrderId(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .resalePrice(BigDecimal.valueOf(200))
                .status(ListingStatus.ACTIVE)
                .mediaUrls(List.of("img.jpg"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .originalOrder(OrderDto.builder().id(UUID.randomUUID()).build())
                .originalListing(ListingDto.builder().id(UUID.randomUUID()).build())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ResaleResponse deserialized = objectMapper.readValue(json, ResaleResponse.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getOriginalOrderId(), deserialized.getOriginalOrderId());
        assertEquals(dto.getBuyerId(), deserialized.getBuyerId());
        assertEquals(dto.getResalePrice(), deserialized.getResalePrice());
        assertEquals(dto.getStatus(), deserialized.getStatus());
        assertEquals(dto.getMediaUrls(), deserialized.getMediaUrls());
        assertEquals(dto.getOriginalOrder().getId(), deserialized.getOriginalOrder().getId());
        assertEquals(dto.getOriginalListing().getId(), deserialized.getOriginalListing().getId());
    }

    @Test
    void testCreateReviewRequestSerialization() throws IOException {
        CreateReviewRequest dto = CreateReviewRequest.builder()
                .orderId(UUID.randomUUID())
                .rating(4)
                .text("Good product")
                .images(List.of("img1.jpg"))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CreateReviewRequest deserialized = objectMapper.readValue(json, CreateReviewRequest.class);
        assertEquals(dto.getOrderId(), deserialized.getOrderId());
        assertEquals(dto.getRating(), deserialized.getRating());
        assertEquals(dto.getText(), deserialized.getText());
        assertEquals(dto.getImages(), deserialized.getImages());
    }

    @Test
    void testReviewResponseSerialization() throws IOException {
        ReviewResponse dto = ReviewResponse.builder()
                .review(ReviewDto.builder().id(UUID.randomUUID()).rating(5).build())
                .reviewer(UserDto.builder().id(UUID.randomUUID()).name("Reviewer").build())
                .seller(UserDto.builder().id(UUID.randomUUID()).name("Seller").build())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ReviewResponse deserialized = objectMapper.readValue(json, ReviewResponse.class);
        assertEquals(dto.getReview().getId(), deserialized.getReview().getId());
        assertEquals(dto.getReviewer().getId(), deserialized.getReviewer().getId());
        assertEquals(dto.getSeller().getId(), deserialized.getSeller().getId());
    }

    @Test
    void testOrderDetailResponseSerialization() throws IOException {
        OrderDetailResponse dto = OrderDetailResponse.builder()
                .order(OrderDto.builder().id(UUID.randomUUID()).build())
                .listing(ListingDto.builder().id(UUID.randomUUID()).build())
                .buyer(UserDto.builder().id(UUID.randomUUID()).build())
                .seller(UserDto.builder().id(UUID.randomUUID()).build())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        OrderDetailResponse deserialized = objectMapper.readValue(json, OrderDetailResponse.class);
        assertEquals(dto.getOrder().getId(), deserialized.getOrder().getId());
        assertEquals(dto.getListing().getId(), deserialized.getListing().getId());
        assertEquals(dto.getBuyer().getId(), deserialized.getBuyer().getId());
        assertEquals(dto.getSeller().getId(), deserialized.getSeller().getId());
    }

    @Test
    void testUpdateOrderStatusRequestSerialization() throws IOException {
        UpdateOrderStatusRequest dto = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.SHIPPED)
                .trackingNumber("TRACK123")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        UpdateOrderStatusRequest deserialized = objectMapper.readValue(json, UpdateOrderStatusRequest.class);
        assertEquals(dto.getStatus(), deserialized.getStatus());
        assertEquals(dto.getTrackingNumber(), deserialized.getTrackingNumber());
    }

    @Test
    void testDtoWithNullFields() throws IOException {
        ListingDto dto = ListingDto.builder()
                .id(UUID.randomUUID())
                .title("Minimal")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"title\":\"Minimal\""));
        assertTrue(json.contains("\"id\":\"" + dto.getId().toString() + "\""));

        ListingDto deserialized = objectMapper.readValue(json, ListingDto.class);
        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getTitle(), deserialized.getTitle());
        assertNull(deserialized.getDescription());
        assertNull(deserialized.getMediaUrls());
    }

    @Test
    void testDtoWithBigDecimalPrecision() throws IOException {
        BigDecimal precise = new BigDecimal("1234.56789");
        BidDto dto = BidDto.builder()
                .id(UUID.randomUUID())
                .auctionId(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .amount(precise)
                .placedAt(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        BidDto deserialized = objectMapper.readValue(json, BidDto.class);
        assertEquals(0, precise.compareTo(deserialized.getAmount()));
    }

    @Test
    void testDtoWithNegativeValues() throws IOException {
        UUID id = UUID.randomUUID();
        ListingDto dto = objectMapper.readValue(
            "{\"id\":\"" + id + "\",\"viewCount\":-5}",
            ListingDto.class
        );
        assertEquals(id, dto.getId());
        assertEquals(-5, dto.getViewCount());
    }
}
