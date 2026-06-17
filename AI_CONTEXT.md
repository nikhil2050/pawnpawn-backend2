# PnPn Backend — Premium Luxury Dual-Theme Marketplace

## System Topology

```
┌─────────────┐     ┌────────────────────────────────────────────────────┐
│   Clients   │────▶│  pnpn-gateway (9090)  Spring Cloud Gateway         │
│ (Mobile/Web)│     │  Rate limit: 100 req/min/IP, CB, retry 3x, timeout │
└─────────────┘     └──────┬─────────┬──────────┬─────────┬──────────────┘
                           │         │          │         │
                    ┌──────▼──┐ ┌────▼────┐ ┌───▼────┐ ┌──▼──────────┐
                    │  User   │ │ Catalog │ │Transact│ │ Messaging   │
                    │ Service │ │ Service │ │ Service│ │ Service     │
                    │ :9091   │ │ :9092   │ │ :9093  │ │ :9094       │
                    └────┬────┘ └────┬────┘ └────┬───┘ └──────┬──────┘
                         │           │           │            │
              ┌──────────┴───┬───────┴──────┬────┴──────┬─────┴───────────┐
              │  PostgreSQL  │    Redis     │   MinIO   │     MongoDB     │
              │  :5432       │   :6379      │  :9000    │    :27017       │
              │  (data)      │  (sessions,  │ (media)   │  (chat msgs)    │
              │              │   cache,     │           │                 │
              │              │   rate-limit)│           │                 │
              └──────────────┴──────────────┴───────────┴─────────────────┘
```

## Microservice Boundaries

| Service                    | Port | Status                | Responsibility                                                                 |
|----------------------------|------|-----------------------|--------------------------------------------------------------------------------|
| `pnpn-gateway`             | 9090 | Minimal               | API routing, rate limiting, circuit breaker, JWT validation, fallback          |
| `pnpn-user-service`        | 9091 | Fully implemented     | Register/login/OTP/refresh/logout, user profiles, JWT issuing                  |
| `pnpn-catalog-service`     | 9092 | Partially implemented | Listings CRUD, dynamic search, auctions, bidding, MinIO media                  |
| `pnpn-transaction-service` | 9093 | Skeleton only         | Orders lifecycle, cart management, payments (Razorpay/Stripe), reviews, resale |
| `pnpn-messaging-service`   | 9094 | Skeleton only         | Real-time chat (WebSocket), notifications (FCM)                                |
| `pnpn-shared`              | —    | Fully implemented     | Shared DTOs (39), enums (8), exceptions (2), global handler                    |

## Database Architecture

- **PostgreSQL** — 12 tables: `users`, `listings`, `auctions`, `bids`, `orders`, `reviews`, `resale_listings`, `chat_rooms`, `favourites`, `cart_items`, `notifications`, `followers`. All enums defined as PostgreSQL ENUM types.
- **Redis** — 4 use cases: auth sessions (OTP TTL 5min, refresh token TTL 7d), auction bid atomic ops, favourites cache, rate limiter bucket.
- **MongoDB** — chat messages (high write volume, flexible schema per room).
- **MinIO** — S3-compatible media storage (listing images/videos, review images, avatars). 4 auto-created buckets.

## Tech Stack

- Java 21, Spring Boot 3.3.0, Maven multi-module
- Spring Cloud Gateway 2023.0.2, Spring Security, Resilience4j
- JPA/Hibernate + PostgreSQL, Spring Data Redis, Spring Data MongoDB
- JWT (jjwt 0.12.5), BCrypt
- MinIO SDK, Razorpay SDK, Firebase Admin SDK
- WebSocket + STOMP (for chat & auction real-time)
- Lombok, Validation, Actuator
- JaCoCo (code coverage)
- **Planned Phase 2:** Kafka event bus, Prometheus/Grafana, React Admin Panel

## Service Communication Pattern

- **Synchronous:** Clients → Gateway (REST) → Service. Gateway handles routing, rate limiting, circuit breakers, retries.
- **Asynchronous (planned):** Redis Pub/Sub for events (BidPlaced, OrderCreated, PaymentVerified, etc.). Phase 2 upgrades to Kafka.
- **JWT propagation:** Gateway validates JWT, passes `userId` + `userType` to downstream services via headers.

## shared DTO Contracts

Every service depends on `pnpn-shared`. Key DTO domain groups:
- `shared.dto.auth.*` — RegisterRequest, LoginRequest, LoginResponse, OtpRequest, OtpVerifyRequest, RefreshTokenRequest, LogoutRequest
- `shared.dto.catalog.*` — CreateListingRequest, ListingSearchCriteria, CreateAuctionRequest, PlaceBidRequest, AuctionDetailResponse, MediaPresignedUrlResponse
- `shared.dto.transaction.*` — AddToCartRequest, CheckoutRequest, OrderDetailResponse, CreatePaymentRequest, VerifyPaymentRequest, CreateReviewRequest, CreateResaleRequest
- `shared.dto.messaging.*` — StartChatRequest, ChatRoomResponse, ChatMessageResponse, NotificationResponse
- `shared.dto.*` — UserDto, ListingDto, AuctionDto, BidDto, OrderDto, ReviewDto, ErrorResponse, UpdateProfileRequest

## Quick Start

```bash
docker compose up -d          # Start all infra + services
mvn clean install -DskipTests # Build all modules
```

Test endpoints via `PnPn.postman_collection.json` or `pnpn-user-service/scripts/test-auth-endpoints.sh`.

## Phase 2 Roadmap

- Kafka event bus (replaces Redis Pub/Sub)
- React admin dashboard (influencer verification, listing moderation, analytics)
- Prometheus + Grafana monitoring
- AWS ECS Fargate + RDS + ElastiCache + S3 + CloudFront
- GitHub Actions CI/CD

See `PNPN_IMPLEMENTATION_PROMPTS.md` for the full step-by-step plan.
