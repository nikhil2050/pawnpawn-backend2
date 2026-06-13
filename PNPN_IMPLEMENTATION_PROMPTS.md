### Created by: Claude|Sonnet 4.6 Extended

# PnPn Backend - Implementation Sequence (Step-by-Step Prompts)

## Phase 1: Foundation & Core Services (Weeks 1-12)

### Week 1-2: Project Setup & Infrastructure

**Prompt 1.1: Create Maven Multi-Module Project Structure ✅**
```
Set up a Maven multi-module project for PnPn with:
- Parent pom.xml (Spring Boot 3.x, Java 21)
- pnpn-shared module (DTOs, enums, exceptions)
- pnpn-gateway module (Spring Cloud Gateway)
- pnpn-user-service module
- pnpn-catalog-service module
- pnpn-transaction-service module
- pnpn-messaging-service module

Include all dependencies: Spring Data JPA, PostgreSQL driver, Redis, 
Resilience4j, Lombok, Validation.

Output: Root pom.xml + module pom.xml files + folder structure.
```

**Prompt 1.2: Docker Compose for Local Development ✅**
```
Create docker-compose.yml with:
- All services (EXCEPT NGINX as it will be added in future scope ‼️) under a parent container name - project-pnpn
- PostgreSQL 16 container (pnpn_db, postgres:postgres)
- Redis 7 container
- MongoDB 7 container
- MinIO container (S3-compatible, minioadmin:minioadmin)
- Nginx container (reverse proxy on port 80) [FUTURE SCOPE - TO ADD LATER ‼️]

Include volumes, health checks, networking (pnpn-network).
Include .env.example with all required variables.

Use relevant credentials and update them along with how to use the services in CRED.md file 

Output: docker-compose.yml + .env.example
```

**Prompt 1.3: Nginx Reverse Proxy Configuration ‼️**
```
Create nginx.conf that:
- Routes /api/* to gateway:8080
- Routes /api/ws/* with WebSocket upgrade headers
- Routes /media/* to MinIO (CDN caching 24h, 30d expiry)
- Routes /minio/* to MinIO console (dev only)
- Includes gzip compression, keepalive, logging

Output: nginx.conf ready for production use
```

---

### Week 2-3: Database Setup

**Prompt 1.4: PostgreSQL Database Schema ✅**
```
Generate complete SQL DDL for PostgreSQL including:
- 12 tables (users, listings, auctions, bids, orders, reviews, resale_listings, 
  chat_rooms, favourites, cart_items, notifications, followers)
- Primary keys, foreign keys, constraints
- Indexes on frequently queried fields (created_at DESC, status, email, phone)
- ENUM types for UserType, OrderStatus, ListingCondition, etc.

Output: init.sql (ready to mount in docker-compose)
```

**Prompt 1.5: Shared Module - Enums & Exceptions ✅**
```
Implement in pnpn-shared:
- Enums: UserType, OrderStatus, ListingCondition, ListingStatus, 
  VerificationStatus, NotificationType
- Custom Exception: PnPnException (with message + cause)
- Global exception handler @RestControllerAdvice

Output: Complete Java classes ready to share across services.
```

**Prompt 1.6: Shared DTOs ✅**
```
Implement in pnpn-shared:
- UserDto, ListingDto, OrderDto, AuctionDto, BidDto, ReviewDto
- All request/response DTOs (LoginRequest, RegisterRequest, etc.)
- Use @Data, @Builder, validation annotations (@NotNull, @Email, etc.)

Output: All DTOs with proper annotations and documentation.
```

---

### Week 3-4: User Service (Auth)

**Prompt 1.7: User Entity & Repository**
```
Implement in pnpn-user-service:
- User JPA entity with fields: id, type, name, email, phone, passwordHash, 
  profilePicUrl, bio, verified, verificationStatus, governmentIdUrl, 
  selfieUrl, followerCount, reputationScore, totalReviews, createdAt, updatedAt
- UserRepository interface with methods: findByEmail, findByPhone, 
  findByIdAndType, findByVerified
- Add @CreationTimestamp, @UpdateTimestamp, indexes

Output: User.java + UserRepository.java ready for data access.
```

**Prompt 1.8: JWT Utility & Security Config**
```
Implement in pnpn-user-service:
- JwtUtil: generateAccessToken (15 min expiry), parseToken, getUserIdFromToken, 
  getUserTypeFromToken, isTokenExpired
- JwtAuthenticationFilter: extract token from Authorization header, 
  validate, set SecurityContext
- SecurityConfig: @EnableWebSecurity, filter chain, BCryptPasswordEncoder, 
  permit /api/auth/*, require auth for others

Use JJWT library (io.jsonwebtoken:jjwt).

Output: Complete JWT + Spring Security setup.
```

**Prompt 1.9: Auth Service Implementation**
```
Implement in pnpn-user-service:
- AuthService with methods: register, login, sendOtp, verifyOtp, 
  refreshToken, logout
- Password validation: min 8 chars, uppercase, lowercase, digit, symbol
- OTP generation: 4-digit random, stored in Redis with 5-min expiry
- Refresh token: UUID, stored in Redis with 7-day expiry
- Exceptions: email exists, invalid password, OTP expired, etc.

Use BCryptPasswordEncoder, RedisTemplate, Optional handling.

Output: AuthService.java with all business logic.
```

**Prompt 1.10: Auth Controller & DTOs**
```
Implement in pnpn-user-service:
- AuthController with endpoints: POST /register, /login, /refresh, /logout, 
  /send-otp, /verify-otp
- Request DTOs: RegisterRequest, LoginRequest, OtpRequest, OtpVerifyRequest
- Response DTO: LoginResponse (userId, type, accessToken, refreshToken, 
  expiresIn, user)
- Add @Valid, @NotBlank, @Email validation
- Return proper HTTP status codes (201, 200, 400, 401, 409)

Output: AuthController.java + DTO classes.
```

**Prompt 1.11: User Profile Service**
```
Implement in pnpn-user-service:
- UserService: getUserById, updateProfile, getReputation, getFollowerCount
- Methods to update: name, bio, profilePicUrl, verificationStatus
- Calculate reputation from reviews: average rating, total count
- Add circuit breaker (user-service-cb) for fallback scenarios

Output: UserService.java with profile management.
```

---

### Week 4-5: Catalog Service (Listings)

**Prompt 1.12: Listing Entity & Repository**
```
Implement in pnpn-catalog-service:
- Listing JPA entity: id, influencerId, title, description, price, category, 
  condition, status, mediaUrls (TEXT[]), viewCount, createdAt, updatedAt
- ListingRepository: findByInfluencerId, findByStatus, findByCategory, 
  findByCondition, findByPriceBetween
- CustomListingRepository: findByCriteria (dynamic query with filters)
- Implement ListingRepositoryImpl using CriteriaBuilder

Output: Listing.java + repositories with dynamic filtering.
```

**Prompt 1.13: MinIO Configuration**
```
Implement in pnpn-catalog-service:
- MinioConfig: MinioClient bean pointing to minio:9000
- Create buckets: pnpn-listings-images, pnpn-listings-videos, 
  pnpn-reviews-images, pnpn-avatars
- Test bucket creation on app startup
- Use io.minio:minio SDK

Output: MinioConfig.java + error handling.
```

**Prompt 1.14: Listing Service Implementation**
```
Implement in pnpn-catalog-service:
- ListingService: createListing (influencer only), searchListings, 
  getListingById, updateListing (owner only), deleteListing, 
  generatePresignedUrl
- Circuit breaker: @CircuitBreaker(name = "user-service-cb") for getting 
  influencer info
- Cache invalidation on create/update/delete
- View count increments via Redis (async)
- Presigned URL generation: PUT method, 1-hour expiry

Output: ListingService.java with all CRUD + media handling.
```

**Prompt 1.15: Listing Controller & DTOs**
```
Implement in pnpn-catalog-service:
- ListingController: POST /listings, GET /listings (paginated + filters), 
  GET /listings/{id}, PUT /listings/{id}, DELETE /listings/{id}, 
  POST /listings/{id}/media/presigned-url
- Request DTOs: CreateListingRequest, ListingSearchCriteria
- Response DTO: ListingResponse (include influencer info), 
  MediaPresignedUrlResponse
- Add @PreAuthorize("hasRole('INFLUENCER')") for creation/update/delete
- Support query params: category, minPrice, maxPrice, condition, page, size

Output: ListingController.java + DTOs with full validation.
```

---

### Week 5-6: Auction Service (Part of Catalog)

**Prompt 1.16: Auction Entity & Bidding**
```
Implement in pnpn-catalog-service:
- Auction JPA entity: id, listingId, startPrice, currentBid, reservePrice, 
  highestBidderId, startTime, endTime, winnerId, status, extensionCount
- Bid JPA entity: id, auctionId, buyerId, amount, placedAt
- AuctionRepository + BidRepository with proper indexes
- BidIncrementCalculator: 5%/3%/2%/1% based on price ranges

Output: Auction.java + Bid.java + repositories + calculator.
```

**Prompt 1.17: Auction Service Implementation**
```
Implement in pnpn-catalog-service:
- AuctionService: createAuction, getAuctionDetails, getActiveAuctions, 
  placeBid (with atomic operations), autoCloseExpiredAuctions, 
  extendAuctionIfSnipe (last 60s rule, max 3 extensions)
- Use Redis for atomic bid operations (INCR, compare-and-set)
- Validate bid amount >= minimumBid (from calculator)
- Scheduled task @Scheduled to close auctions at end time
- Publish events: BidPlaced (WebSocket), AuctionEnded

Output: AuctionService.java with real-time logic.
```

**Prompt 1.18: Auction WebSocket Endpoints**
```
Implement in pnpn-catalog-service:
- Spring WebSocket + STOMP configuration
- Endpoint: /ws/auction/{auctionId}
- Broadcast on bid: {auctionId, currentBid, highestBidderId, bidCount, 
  timeLeftSeconds}
- Notify outbid subscribers
- Handle client disconnect gracefully

Use spring-boot-starter-websocket + spring-messaging.

Output: WebSocket config + message handling.
```

**Prompt 1.19: Auction Controller & DTOs**
```
Implement in pnpn-catalog-service:
- AuctionController: POST /auctions, GET /auctions/{id}, 
  GET /auctions/live, POST /bids
- Request DTOs: CreateAuctionRequest (listingId, startPrice, reservePrice, 
  startTime, endTime), PlaceBidRequest
- Response DTO: AuctionDetailResponse (include bidding history)
- Add @PreAuthorize("hasRole('INFLUENCER')") for creation
- Error responses: invalid bid, auction ended, snipe prevention extended

Output: AuctionController.java + DTOs.
```

---

### Week 6-7: Transaction Service (Orders, Cart, Payments)

**Prompt 1.20: Order & Cart Entities**
```
Implement in pnpn-transaction-service:
- Order JPA entity: id, listingId, buyerId, sellerId, amount, status, 
  deliveryAddress, deliveryDate, trackingNumber, cancellationReason, 
  createdAt, updatedAt
- CartItem JPA entity: id, userId, listingId, quantity, addedAt
- OrderRepository: findByBuyerId, findBySellerId, findByStatus
- CartRepository: findByUserId, findByUserIdAndListingId

Output: Order.java + CartItem.java + repositories.
```

**Prompt 1.21: Cart Service**
```
Implement in pnpn-transaction-service:
- CartService: addToCart, removeFromCart, getCart, clearCart, checkout
- Validations: listing not already in cart, listing not sold, not self-purchase
- Checkout: create orders for each cart item, update listing status to SOLD, 
  clear cart, publish OrderCreatedEvent
- Exceptions: empty cart, listing sold, insufficient stock

Output: CartService.java with cart management.
```

**Prompt 1.22: Order Service**
```
Implement in pnpn-transaction-service:
- OrderService: createOrder (internal, called by checkout), getOrdersByBuyer, 
  getOrdersBySeller, getOrderById, updateOrderStatus (seller only), 
  cancelOrder (buyer, if PENDING/CONFIRMED)
- Status transitions: PENDING → CONFIRMED → SHIPPED → DELIVERED
- Publish events: OrderConfirmed, OrderShipped, OrderDelivered
- Handle refunds on cancellation
- Calculate seller earnings (with PnPn commission: 5%)

Output: OrderService.java with order lifecycle.
```

**Prompt 1.23: Payment Service (Razorpay)**
```
Implement in pnpn-transaction-service:
- PaymentService: createRazorpayOrder, verifyPayment
- On creation: call Razorpay API, store orderId in order table
- On verification: verify signature using Razorpay secret key, 
  update order status to CONFIRMED, publish PaymentVerified event
- Circuit breaker: @CircuitBreaker(name = "razorpay-cb", 
  failbackMethod = "paymentFallback")
- Fallback: mark order as PENDING, retry manually

Use com.razorpay:razorpay-java SDK.

Output: PaymentService.java with Razorpay integration.
```

**Prompt 1.24: Order Controller & DTOs**
```
Implement in pnpn-transaction-service:
- OrderController: GET /orders/buyer, GET /orders/seller, GET /orders/{id}, 
  PUT /orders/{id}/status, POST /orders/{id}/cancel
- Request DTOs: UpdateOrderStatusRequest, CancelOrderRequest
- Response DTO: OrderDetailResponse (include timeline, buyer, seller, items)
- Pagination: page, size, sort by createdAt DESC
- Proper authorization: buyer can only see own orders, seller can update own orders

Output: OrderController.java + DTOs.
```

**Prompt 1.25: Cart Controller**
```
Implement in pnpn-transaction-service:
- CartController: POST /cart/add, DELETE /cart/{itemId}, GET /cart, 
  POST /cart/checkout
- Request DTOs: AddToCartRequest, CheckoutRequest
- Response DTO: CartResponse (items, subtotal, tax, total)
- Calculate tax: 18% GST in India
- Validate before checkout: all items still available, seller info, prices

Output: CartController.java.
```

**Prompt 1.26: Payment Controller**
```
Implement in pnpn-transaction-service:
- PaymentController: POST /payments/create-order, POST /payments/verify
- Request DTO: CreatePaymentRequest (orderId, amount), 
  VerifyPaymentRequest (razorpayOrderId, razorpayPaymentId, razorpaySignature)
- Response DTO: PaymentResponse (success, message, orderId, status)
- Error handling: invalid signature, payment already processed, order not found

Output: PaymentController.java.
```

---

### Week 7-8: Messaging Service (Chat)

**Prompt 1.27: MongoDB Chat Schema**
```
Implement in pnpn-messaging-service:
- ChatRoom JPA entity: id, listingId, buyerId, sellerId, lastMessageAt, 
  createdAt, updatedAt
- ChatMessage MongoDB document: _id, roomId, senderId, text, imageUrl, 
  sentAt, read, readAt
- ChatRoomRepository: findByBuyerId, findBySellerId, 
  findByListingIdAndBuyerIdAndSellerId
- ChatMessageRepository (MongoDB): findByRoomIdOrderBySentAtDesc

Output: JPA + MongoDB entities/repositories.
```

**Prompt 1.28: WebSocket Chat Configuration**
```
Implement in pnpn-messaging-service:
- WebSocketConfig: @EnableWebSocketMessageBroker, enable STOMP
- Endpoint: /ws/chat (SockJS fallback)
- In-memory message broker: /topic/chat/{roomId}, /user/queue/notifications
- Allowed origins: "*" (dev), restrict to domain (prod)

Use spring-boot-starter-websocket.

Output: WebSocketConfig.java.
```

**Prompt 1.29: Chat Service & Message Handler**
```
Implement in pnpn-messaging-service:
- ChatService: startChatRoom, getChatRooms, getChatMessages, sendMessage
- ChatMessageHandler: @MessageMapping("/chat.send"), process & broadcast
- Validation: user can only chat in rooms they belong to
- Store message in MongoDB, publish to /topic/chat/{roomId}
- Track read receipts: senderId marks message as read
- Unread count: cached in Redis

Output: ChatService.java + ChatMessageHandler.java.
```

**Prompt 1.30: Chat Controller**
```
Implement in pnpn-messaging-service:
- ChatController: POST /chat/start, GET /chat/rooms, 
  GET /chat/rooms/{roomId}/messages, PUT /chat/rooms/{roomId}/read
- Request DTO: StartChatRequest (listingId, otherUserId)
- Response DTO: ChatRoomResponse (list with unread counts), 
  ChatMessageResponse (paginated)
- Pagination: fetch last 50 messages, older on scroll

Output: ChatController.java + DTOs.
```

---

### Week 8-9: Reviews & Notifications

**Prompt 1.31: Review Service**
```
Implement in pnpn-transaction-service:
- ReviewService: createReview (after delivery), getReviewsBySeller, 
  updateReview, deleteReview
- Validation: can only review if order delivered, one review per order
- Calculate seller reputation: average rating from all reviews
- Publish ReviewCreated event
- Update user.reputationScore and user.totalReviews

Output: ReviewService.java.
```

**Prompt 1.32: Review Controller**
```
Implement in pnpn-transaction-service:
- ReviewController: POST /reviews, GET /reviews/seller/{sellerId}, 
  PUT /reviews/{id}, DELETE /reviews/{id}
- Request DTO: CreateReviewRequest (orderId, rating 1-5, text, images[])
- Response DTO: ReviewResponse (all fields)
- Authorization: buyer can only review own orders

Output: ReviewController.java + DTOs.
```

**Prompt 1.33: Notification Service & FCM**
```
Implement in pnpn-messaging-service:
- NotificationService: sendOrderNotification, sendBidNotification, 
  sendMessageNotification, sendOfferNotification
- FCM integration: Firebase Admin SDK, send to user's FCM token
- Notification storage: save in PostgreSQL notifications table
- Circuit breaker: @CircuitBreaker(name = "fcm-cb", fallback to queue)
- Offline queue: if FCM fails, store in Redis queue, retry later

Use com.google.firebase:firebase-admin SDK.

Output: NotificationService.java with FCM + fallback.
```

**Prompt 1.34: Notification Controller**
```
Implement in pnpn-messaging-service:
- NotificationController: GET /notifications (paginated, unread first), 
  PUT /notifications/{id}/read, PUT /notifications/read-all
- Response DTO: NotificationResponse (id, type, title, body, read, createdAt)
- Filtering: by type (ORDER, BID, MESSAGE, REVIEW, OFFER)
- Badge count: unread notifications count

Output: NotificationController.java.
```

---

### Week 9-10: Favourites & Resale

**Prompt 1.35: Favourites Service**
```
Implement in pnpn-catalog-service:
- FavouriteService: toggleFavourite, isFavourite, getFavourites
- Cache in Redis: user:{userId}:favourites (set of listing IDs)
- Invalidate cache on toggle
- Return paginated list of favourite listings with details

Output: FavouriteService.java.
```

**Prompt 1.36: Resale Listing Service**
```
Implement in pnpn-transaction-service:
- ResaleListing entity: id, originalOrderId, buyerId, resalePrice, status, 
  mediaUrls, createdAt
- ResaleService: createResaleListing, getMyResales, getResaleById, 
  updateResalePrice, markAsSold
- Validation: can only resell if order delivered, not already reselling
- When sold: create new order, notify original seller (PnPn gets 5% of 
  resale price)

Output: ResaleListing.java + ResaleService.java.
```

**Prompt 1.37: Resale Controller**
```
Implement in pnpn-transaction-service:
- ResaleController: POST /resale, GET /resale/my, GET /resale/{id}, 
  PUT /resale/{id}/price
- Request DTO: CreateResaleRequest (orderId, resalePrice, mediaUrls[])
- Response DTO: ResaleResponse (include original listing, resale details)
- Show resale history with earnings

Output: ResaleController.java + DTOs.
```

---

### Week 10-11: API Gateway & Spring Cloud Config

**Prompt 1.38: Spring Cloud Gateway Setup**
```
Implement in pnpn-gateway:
- GatewayConfig: route /api/auth/* → user-service, /api/listings/* → 
  catalog-service, /api/orders/* → transaction-service, 
  /api/chat/* → messaging-service
- Add global filters: JWT validation, logging, rate limiting
- Circuit breaker filter: wrap routes with circuit breakers
- Timeout: 60s for API calls, 86400s for WebSocket

Use spring-cloud-starter-gateway.

Output: GatewayConfig.java + route definitions.
```

**Prompt 1.39: Gateway Resilience & Rate Limiting**
```
Implement in pnpn-gateway:
- Rate limiting: 100 requests per minute per IP (use Redis)
- Circuit breaker: wrap service routes, fail fast if service down
- Retry: 3 attempts with 1s backoff
- Timeout: 60s per request (configurable per service)
- Health check: /actuator/health exposes service health

Output: Resilience4j + RateLimiting configuration.
```

---

### Week 11-12: Testing & Documentation

**Prompt 1.40: Unit Tests for Services**
```
Write unit tests for:
- AuthService: register, login, sendOtp, verifyOtp, refreshToken
- ListingService: createListing, searchListings, updateListing
- CartService: addToCart, removeFromCart, checkout
- Use Mockito to mock repositories and external services
- Aim for 40% code coverage

Output: Test classes with @Test methods + assertions.
```

**Prompt 1.41: Integration Tests**
```
Write integration tests using @SpringBootTest:
- Test full flow: Register → Create Listing → Add to Cart → Checkout
- Test database: @DataJpaTest with H2 in-memory DB
- Test controllers: @WebMvcTest with MockMvc
- Verify circuit breaker behavior: fail fast when service down
- Aim for 30% code coverage

Output: Integration test suites for each service.
```

**Prompt 1.42: OpenAPI Documentation**
```
Add SpringDoc annotations to all controllers:
- @OpenAPIDefinition on main controller
- @Operation on each endpoint
- @Schema on request/response DTOs
- @Parameter on query/path parameters
- Generate /api/v1/api-docs (JSON), access via /swagger-ui.html

Use org.springdoc:springdoc-openapi-starter-webmvc-ui.

Output: Fully documented APIs with Swagger UI.
```

---

## Phase 2: Auctions, Resale, Kafka (Weeks 13-18)

### Prompt 2.1: Replace Redis Pub/Sub with Kafka
**Add Kafka topics (orders.created, auction.bid-placed, payment.verified). Refactor event publishing from Redis to Kafka. Update consumers (OrderService → send notification event).**

### Prompt 2.2: Add Redis Pub/Sub → Kafka Migration
**Implement KafkaTemplate in OrderService, OrderCreatedEvent producer. Implement @KafkaListener in NotificationService.**

### Prompt 2.3: Admin Panel (React Web)
**Create React dashboard (separate from mobile). Pages: Influencer verification, listing moderation, order management, user banning, analytics dashboard.**

### Prompt 2.4: Prometheus + Grafana Monitoring
**Add micrometer-prometheus to all services. Create Grafana dashboards: request latency, error rates, circuit breaker status, database connections.**

### Prompt 2.5: E2E Testing
**Write end-to-end tests: Register → List → Bid → Checkout → Verify Payment. Use RestAssured or Postman collections.**

---

## Phase 3: Cloud Migration (Weeks 19+)

### Prompt 3.1: AWS Infrastructure (IaC - Terraform)
**Create Terraform scripts for: ECS Fargate task definitions, RDS PostgreSQL (Multi-AZ), ElastiCache Redis, S3, CloudFront, ALB, Route 53.**

### Prompt 3.2: GitHub Actions CI/CD
**Create GitHub Actions workflow: build all services → run tests → push Docker images to ECR → deploy to ECS.**

### Prompt 3.3: AWS Secrets Manager Integration
**Replace .env file with AWS Secrets Manager. Update application.yml to fetch secrets from Secrets Manager at startup.**

### Prompt 3.4: CloudWatch Logs & Monitoring
**Remove ELK stack. Configure CloudWatch Logs for all services. Create CloudWatch Alarms for high error rates, high latency, circuit breaker open.**

---

**Implementation Order:** Follow prompts sequentially. Each prompt builds on previous. Test after each service before moving to next.
