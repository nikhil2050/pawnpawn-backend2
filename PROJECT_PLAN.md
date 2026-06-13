### Created by: OpenCode|Deepseek V4 Flash

# PawnPawn Backend — Project Plan

PawnPawn is a premium, dual-themed luxury marketplace and live bidding platform that connects top-tier influencers selling their exclusive wardrobes directly to their followers. 

---

## 1. Project Overview

**Goal**
Build a scalable, secure backend for a luxury fashion marketplace where
influencers auction wardrobe items to their followers in real time.

**Target Users**
- **Influencers** — list items, run live auctions, manage sales
- **Followers** — browse, bid live, purchase
- **Admins** — moderate listings, manage disputes, platform ops

**Key Differentiators**
- Dual-theme luxury experience
- Live bidding with real-time engagement
- Exclusive influencer-follower interaction

---

## 2. Tech Stack

| Layer           | Technology                                  |
| --------------- | ------------------------------------------- |
| Language        | Java 17                                     |
| Framework       | Spring Boot 3.x                             |
| Build Tool      | Maven                                       |
| API Gateway     | Spring Cloud Gateway                        |
| Inter-service   | Kafka / RabbitMQ (event-driven)             |
| Database (core) | PostgreSQL                                  |
| Database (chat) | MongoDB                                     |
| Cache           | Redis                                       |
| Object Storage  | MinIO (dev) → AWS S3 / GCS (prod)           |
| Container       | Docker / Docker Compose                     |
| Orchestration   | (TBD — Kubernetes if scaling demands it)    |
| CI/CD           | (TBD — GitHub Actions / Jenkins)            |
| API Docs        | OpenAPI / Swagger                           |
| Testing         | JUnit, Mockito, REST Assured, (TBD - Testcontainers) |

---

## 3. Microservice Architecture

All services run in Docker containers orchestrated by Docker Compose for local development and single-machine deployment.
All containers of services are categorized under the container - project-pnpn-ag2 for easy grouping and management.

| Service |  Rationale |
|---------|------------|
| pnpn-user-service | Auth + User are tightly coupled; every operation starts with identifying the user. Verification & reputation are user attributes. |
| pnpn-catalog-service | Listings + Auctions + Bidding are all product domain; scaling independently from transactions. |
| pnpn-transaction-service | Orders + Reviews + Resale form the commerce transaction flow. A review is tied to an order; resale creates a new order. Single responsibility: "manage commerce lifecycle." |
| pnpn-messaging-service | Chat + Notifications are both async communication channels. Both benefit from the same infrastructure (WebSocket, async workers, event-driven).

### Database Allocation (Clean Boundaries)
| Service | Primary DB | Secondary |
|---------|------------|-----------|
| pnpn-user-service | PostgreSQL (users, followers) | Redis (session cache) |
| pnpn-catalog-service | PostgreSQL (listings, auctions, bids) | Redis (live auction state, bid queue) |
| pnpn-transaction-service | PostgreSQL (orders, reviews, resale) | — | 
| pnpn-messaging-service | MongoDB (chat messages) | Redis (unread counts, typing indicators) |

### Current Service Dependencies & Call Graph
```
Gateway
  │
  ├─→ User Service (AUTH, Profile, Verification)
  │   └─ External API: OTP provider
  │       └─ Circuit Breaker: ✅ (3 failures → OPEN → retry after 30s)
  │
  ├─→ Catalog Service (Listings, Auctions, Bids)
  │   ├─ Service Call (Sync): User Service (get seller reputation)
  │   │   └─ Circuit Breaker: ✅ (fallback: use cached reputation)
  │   │
  │   └─ External API: MinIO
  │       └─ Circuit Breaker: ✅ (3 failures → OPEN)
  │
  ├─→ Transaction Service (Orders, Payments, Reviews)
  │   ├─ Service Call (Sync): Catalog Service (get listing details)
  │   │   └─ Circuit Breaker: ✅ (CRITICAL — fail fast if down)
  │   │
  │   ├─ Service Call (Sync): User Service (get buyer/seller)
  │   │   └─ Circuit Breaker: ✅ (CRITICAL — fail fast if down)
  │   │
  │   ├─ External API: Razorpay (payment)
  │   │   └─ Circuit Breaker: ✅ (CRITICAL — fail fast)
  │   │
  │   └─ Event Publishing (Async): Order Created, Payment Done, etc.
  │       └─ Messaging Service listens (no blocking call)
  │
  └─→ Messaging Service (Chat, Notifications)
      ├─ Event Listener: Order events (async)
      ├─ Event Listener: Bid events (async)
      │
      └─ External API: FCM
          └─ Circuit Breaker: ✅ (non-critical — queue offline messages)
```


```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    React Native Client (Android)                            │
│            Influencer UI (Feminine) | Buyer UI (Dark Premium)               │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    API Gateway (Spring Cloud Gateway)                       │
│          Nginx Reverse Proxy | Rate Limiting | JWT Validation               │
└──────┬──────────────────┬──────────────────────┬────────────────────────────┘
       │                  │                      │
       ▼                  ▼                      ▼
   ┌─────────────┐  ┌─────────────┐       ┌─────────────┐
   │   User      │  │  Catalog    │       │ Transaction │
   │  Service    │  │  Service    │       │  Service    │
   └─────────────┘  └─────────────┘       └─────────────┘
                          │                      │
                          │                      │
                          ▼                      │
                       ┌──────────────┐          │
                       │Auction Engine│◀─────────┘
                       │(Futurescope) │
                       └──────────────┘
                          │
                          ▼ (Event-driven)
                    ┌─────────────┐
                    │  Messaging  │
                    │  Service    │
                    └─────────────┘

DATABASE LAYER:
┌──────────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐
│ PostgreSQL   │  │  Redis   │  │ MongoDB  │  │ MinIO   │
│ (Relational) │  │ (Cache)  │  │ (Chat)   │  │ (Media) │
└──────────────┘  └──────────┘  └──────────┘  └─────────┘

EXTERNAL SERVICES (with Circuit Breakers):
┌──────────────┐  ┌──────────┐  ┌──────────┐
│  Razorpay    │  │Firebase  │  │   OTP    │
│  (Payments)  │  │  (FCM)   │  │ Provider │
└──────────────┘  └──────────┘  └──────────┘

DEPLOYMENT:
┌─────────────────────────────────────────────────────────────┐
│             Docker Compose Network                           │
│  (All services + DBs + Redis on single machine/VPS)         │
└─────────────────────────────────────────────────────────────┘
```

### Service Responsibilities

#### pnpn-user-service
- Auth (login, register, OTP, JWT refresh, Logout)
- JWT issuance & refresh
- OAuth2 integration (Google, Instagram)
- Role management (INFLUENCER, FOLLOWER, ADMIN)
- User Profile (CRUD)
- Follower-Following relationships
- Influencer Verification & Reputation scores

#### pnpn-catalog-service
- Listings (CRUD, filtering, search, Categories, tags, condition, pricing)
- Wardrobe item CRUD
- Media upload (images/videos) → Choose between MinIO or S3 using Config → CDN
- [FUTURESCOPE] Bidding (place bid, bid history, Redis atomic ops)
- [FUTURESCOPE] Live auction sessions (WebSocket STOMP)
- [FUTURESCOPE] Bid validation & conflict resolution
- [FUTURESCOPE] Time extension logic (if bid placed near end)
- [FUTURESCOPE] Leaderboard / current highest bid
- [FUTURESCOPE] Auction state: SCHEDULED → LIVE → EXTENDED → SOLD / WITHDRAWN

#### pnpn-transaction-service
- Orders (create, status tracking, shipping)
- Escrow / payment hold until delivery confirmed
- Invoicing and refunds
- Cart (add/remove items)
- Checkout & Payments (Stripe and Razorpay integration)
- Reviews & Ratings
- [FUTURESCOPE] Resale Listings (buyer resell purchased items)

#### pnpn-messaging-service
- Notifications (FCM push, order updates, bid alerts)
   - (Listens to Kafka events from other services)
   - Push notifications (FCM / APNs)
   - Email notifications (order confirmations, outbid alerts)
   - In-app notification feed
- [FUTURESCOPE] Chat (WebSocket, STOMP, MongoDB persistence) 
   - (Stored in MongoDB for high volume / denormalized schema)
   - 1-on-1 and group chat
   - Real-time messaging via WebSocket
   - Message history, read receipts, typing indicators
   - Chat moderation / reporting
   - Unread counts & read receipts

#### pnpn-gateway
- API Gateway (reverse proxy, routing, rate limiting)
   - Route requests to appropriate services
   - Rate limiting, request validation, logging
   - JWT validation (auth passthrough)

#### pnpn-shared
- Common DTOs
- Enums (UserType, OrderStatus, AuctionStatus, etc.)
- Utilities (JWT utils, validation helpers)
- Constants
- Exception handlers

---

## 4. Data Storage Strategy

### 4.1 PostgreSQL Tables (Primary Relational DB, per service — database-per-service pattern)

Docker Container: postgres:16
Volume: postgres_data:/var/lib/postgresql/data
Port: 5432 (internal to Docker network)
Tables:

```sql
`users` (id, role, name, email, phone, profile_pic, bio, verified, reputation_score, password_hash, oauth_provider, oauth_id, created_at, updated_at)

`listings` (id, influencer_id, title, description, price, category, condition, status, media_urls, images[])
`listing_media` (id, listing_id, url, type, sort_order)
`auctions` (id, listing_id, start_price, current_bid, reserve_price, start_time, end_time, winner_id, winning_bid, status)
`bids` (id, auction_id, buyer_id, amount, placed_at)
`orders` (id, listing_id, buyer_id, seller_id, auction_id, amount, status, payment_method, created_at)
`payments` (id, order_id, gateway, transaction_id, amount, status, created_at)
`refunds` (id, order_id, reason, amount, status)
`reviews` (id, order_id, buyer_id, seller_id, rating, text, created_at)
`chat_rooms` (id, listing_id, buyer_id, seller_id, created_at)
`favourites` (user_id, listing_id, added_at)
`cart_items` (user_id, listing_id, added_at)
`followers` (follower_id, following_id, created_at)
`notifications` (id, user_id, type, title, body, read, created_at)
`resale_listings` (id, original_order_id, buyer_id, resale_price, status, created_at)
```

Access:
User Service: postgresql://postgres:password@postgres:5432/pnpn_user_db
Catalog Service: postgresql://postgres:password@postgres:5432/pnpn_catalog_db
Transaction Service: postgresql://postgres:password@postgres:5432/pnpn_transaction_db


### 4.2 Redis (Cache + Session Store)
Docker Container: redis:7
Port: 6379 (internal to Docker network)
Persistence: RDB snapshot
Use Cases:
- Session Cache: JWT refresh tokens, user session data, JWT blacklist
- Rate Limiting: API rate limit counters
- Listing Cache: Hot listings feed, category facets
- Auction Bidding: Live bid state (current highest bid per auction), atomic bid operations
- Real-time leaderboard (sorted sets)
- Chat Cache: Unread message counts, active WebSocket connections

Access: redis://redis:6379

### 4.3 MinIO / S3
Docker Container: minio/minio:latest
Port: 9000 (API), 9001 (Console)
Volume: minio_data:/data
Initial Credentials: minioadmin:minioadmin
Buckets:
- pnpn-listings-images — listing photos (will be moved to s3 bucket later)
- pnpn-listings-videos — listing videos (will be moved to s3 bucket later)
- pnpn-reviews-images — review/rating images (will be moved to s3 bucket later)
- pnpn-avatars — user profile pictures (will be moved to s3 bucket later)

Access (via Nginx proxy) [FUTURESCOPE]:
- Backend: http://minio:9000 (internal)
- Client: http://192.168.x.x/media/ (public CDN URL via Nginx)

Flow:
- Catalog Service generates presigned URL via MinIO SDK
- React Native client uploads directly to MinIO
- Nginx proxies CDN requests from http://host/media/{bucket}/{key}

### 4.4 MongoDB Collections (Chat History) [FUTURESCOPE]
Docker Container: mongo:7
Port: 27017 (internal to Docker network)
Volume: mongo_data:/data/db
Collections:
chat_messages {
  _id, room_id, sender_id, text, image_url, sent_at, read
}

Access: mongodb://mongo:27017/pnpn_chat

[To Be Decided]
- `chat_rooms` (participants[], last_message, unread_count)
- `messages` (room_id, sender_id, content, type, timestamp, read_by[])



---

[PENDING VERIFICATION] 

## 5. Event-Driven Communication 

Services communicate asynchronously via Kafka topics:

| | Event                        | Producer        | Consumers                |
|-| ---------------------------- | --------------- | ------------------------ |
| TBD | `user.registered`        | pawn-auth       | pawn-user, pawn-notif    |
| TBD | `listing.created`        | pawn-listing    | pawn-notif               |
| TBD | `auction.started`        | pawn-bidding    | pawn-notif, pawn-gateway |
| TBD | `auction.ended`          | pawn-bidding    | pawn-order, pawn-notif   |
| TBD | `bid.placed`             | pawn-bidding    | pawn-notif               |
| TBD | `order.created`          | pawn-order      | pawn-notif               |
| TBD | `order.completed`        | pawn-order      | pawn-notif               |
| TBD | `payment.received`       | pawn-order      | pawn-bidding             |

---

[PENDING VERIFICATION] 

## 6. API Design

### RESTful Endpoints (examples)

**Auth**
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/oauth/{provider}`

**Users**
- `GET /api/v1/users/{id}`
- `PUT /api/v1/users/{id}`
- `POST /api/v1/users/{id}/follow`
- `DELETE /api/v1/users/{id}/follow`

**Listings**
- `GET /api/v1/listings` (search, filter, paginate)
- `POST /api/v1/listings`
- `GET /api/v1/listings/{id}`
- `PUT /api/v1/listings/{id}`
- `DELETE /api/v1/listings/{id}`
- `POST /api/v1/listings/{id}/media`

**Auctions / Bidding**
- `GET /api/v1/auctions` (active, upcoming, past)
- `POST /api/v1/auctions`
- `GET /api/v1/auctions/{id}`
- `WS /ws/bid/{auctionId}` (STOMP)

**Orders**
- `POST /api/v1/orders` (checkout)
- `GET /api/v1/orders/{id}`
- `GET /api/v1/orders/user/{userId}`

**Chat**
- `GET /api/v1/chat/rooms`
- `POST /api/v1/chat/rooms`
- `GET /api/v1/chat/rooms/{id}/messages`
- `WS /ws/chat/{roomId}` (STOMP)

### WebSocket Topics

```
/ws/bid/{auctionId}       — bid placement & live updates
/ws/chat/{roomId}         — messaging
/topic/auction/{id}       — auction state changes
/topic/notifications/{userId} — push notifications
```

---

## 7. Infrastructure

### Docker Compose (Local Development)

All containers of services are categorized under the container - project-pnpn-ag2 for easy grouping and management.

```yaml
services:
  postgres:      # port 5432
  mongodb:       # port 27017
  redis:         # port 6379
  minio:         # port 9000 (API), 9001 (console)
  zookeeper:     # Kafka dependency
  kafka:         # port 9092  [TBD]
  pawn-gateway:  # port 9090
  pawn-auth:     # port 9091
  pawn-user:     # port 9092
  pawn-listing:  # port 9093
  pawn-bidding:  # port 9094
  pawn-order:    # port 9095
  pawn-chat:     # port 9096
  pawn-notif:    # port 9097
```

### Production (Future)
- Container orchestration (Kubernetes — EKS / GKE)
- Managed PostgreSQL (RDS / Cloud SQL)
- Managed MongoDB (Atlas / Cosmos DB)
- Managed Redis (ElastiCache / Memorystore)
- S3 / GCS for media
- CloudFront / Cloud CDN

---

## 8. Security & Compliance

- JWT-based stateless auth (access + refresh tokens)
- OAuth2 for social login (Google, Instagram)
- Role-based access (RBAC) on every endpoint
- Input validation & sanitization (OWASP Top 10)
- Rate limiting per user/IP via Redis
- CORS configuration
- Payment PCI DSS compliance (Stripe + Razorpay handle card data)
- GDPR compliance (data export, right to deletion)
- Logging & audit trail (ELK stack future)

---

[PENDING VERIFICATION] 

## 9. Feature Roadmap / Milestones

### Milestone 1: Foundation
Project scaffolding for all microservices
Docker Compose with all infra services
Spring Cloud Gateway setup
Eureka / Consul for service discovery (optional initially)
Shared libraries (DTOs, exceptions, utils)

### Milestone 2: Auth & Users
pawn-auth: register, login, JWT, OAuth2
pawn-user: profiles, follow system
API Gateway route rules & JWT filter

### Milestone 3: Listings & Media
pawn-listing: CRUD items, categories, search
Media upload to MinIO
Image optimization & CDN path

### Milestone 4: Live Bidding
WebSocket server + STOMP broker
pawn-bidding: auction lifecycle, bid engine
Time extension logic
Real-time bid feed to frontend
Redis-backed bid state

### Milestone 5: Orders & Payments
pawn-order: checkout flow
Stripe integration
Razorpay integration
Escrow / payment hold
Refund flow

### Milestone 6: Chat & Notifications
pawn-chat: MongoDB schema, WebSocket messaging
pawn-notif: Kafka consumer, push/email dispatch
Notification preferences

### Milestone 7: Admin Panel & Operations
Admin endpoints for moderation
Dashboard metrics
Dispute resolution

### Milestone 8: Production Hardening
Kubernetes manifests
Monitoring (Prometheus + Grafana)
Distributed tracing (Jaeger)
Load testing
Security audit

---

## 10. Open Questions / Decisions Needed

- [x] Build tool: **Maven**
- [x] Architecture: **Full Microservices**
- [x] Payment gateways: **Stripe + Razorpay**
- [ ] Monorepo vs multi-repo? **(leaning toward monorepo with Maven multi-module)**
- [x] OAuth providers beyond Google/Instagram? (Apple, Facebook?) **Google, Instagram. (Futurescope-Apple, Facebook)**
- [x] Service discovery: Eureka vs Consul vs k8s-native? **k8s-native**
- [ ] Kafka vs RabbitMQ for events? **(Kafka preferred for audit trail)**
- [ ] API versioning strategy? (URL path vs header?)
- [ ] Testing strategy per service? (unit → integration → contract → e2e)
- [ ] Documentation approach? (SpringDoc OpenAPI per service + aggregated)
- [ ] Monitoring / observability tools?
- [ ] Deployment target? (AWS vs GCP vs both?)
- [ ] Mobile app vs web first? (affects API design)
- [ ] How handle auction time extensions? (fixed increment? dynamic?)
- [ ] Minimum bid increment rules? (fixed %? configurable?)

---

## 11. Task Tracker

| #  | Task                              | Service         | Milestone | Status    |
| -- | --------------------------------- | --------------- | --------- | --------- |
| 1  | Init Maven multi-module project   | All             | M1        | Pending   |
| 2  | Docker Compose config             | Infra           | M1        | Pending   |
| 3  | Spring Cloud Gateway setup        | pawn-gateway    | M1        | Pending   |
| 4  | Shared library (DTOs, exceptions) | common          | M1        | Pending   |
| 5  | Auth: register/login              | pawn-auth       | M2        | Pending   |
| 6  | Auth: JWT & refresh tokens        | pawn-auth       | M2        | Pending   |
| 7  | Auth: OAuth2 (Google, Insta)      | pawn-auth       | M2        | Pending   |
| 8  | User: profile CRUD                | pawn-user       | M2        | Pending   |
| 9  | User: follow system               | pawn-user       | M2        | Pending   |
| 10 | Listing: CRUD                     | pawn-listing    | M3        | Pending   |
| 11 | Listing: search/filter            | pawn-listing    | M3        | Pending   |
| 12 | Media: upload → MinIO             | pawn-listing    | M3        | Pending   |
| 13 | Bidding: WebSocket setup          | pawn-bidding    | M4        | Pending   |
| 14 | Bidding: auction lifecycle        | pawn-bidding    | M4        | Pending   |
| 15 | Bidding: bid engine + validation  | pawn-bidding    | M4        | Pending   |
| 16 | Bidding: time extension logic     | pawn-bidding    | M4        | Pending   |
| 17 | Bidding: Redis bid state          | pawn-bidding    | M4        | Pending   |
| 18 | Order: checkout flow              | pawn-order      | M5        | Pending   |
| 19 | Payment: Stripe integration       | pawn-order      | M5        | Pending   |
| 20 | Payment: Razorpay integration     | pawn-order      | M5        | Pending   |
| 21 | Payment: escrow/hold              | pawn-order      | M5        | Pending   |
| 22 | Chat: MongoDB schema              | pawn-chat       | M6        | Pending   |
| 23 | Chat: WebSocket messaging         | pawn-chat       | M6        | Pending   |
| 24 | Notification: Kafka consumer      | pawn-notif      | M6        | Pending   |
| 25 | Notification: push + email        | pawn-notif      | M6        | Pending   |
| 26 | Admin: moderation endpoints       | pawn-gateway    | M7        | Pending   |
| 27 | Admin: dashboard metrics          | all             | M7        | Pending   |
| 28 | K8s deployment manifests         | Infra           | M8        | Pending   |
| 29 | Monitoring & tracing             | Infra           | M8        | Pending   |
| 30 | Load testing + security audit    | All             | M8        | Pending   |

---

*Last updated: 2026-06-09*
