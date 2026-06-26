### Created by: Claude|Sonnet 4.6 Extended

# PnPn - Complete System Architecture, Database Schema & API Design

## Table of Contents
1. [System Overview](#system-overview)
2. [Database Schema](#database-schema)
3. [API Design & Specifications](#api-design--specifications)
4. [Spring Boot Implementation](#spring-boot-implementation)
5. [Docker & Infrastructure](#docker--infrastructure)
6. [Circuit Breaker & Resilience](#circuit-breaker--resilience)
7. [Future Cloud Migration](#future-cloud-migration)

---

## System Overview

**PnPn** is a premium influencer marketplace built on microservices architecture:
- **Frontend:** React Native (Android-first)
- **Backend:** Spring Boot 3.x microservices
- **Deployment:** Docker Compose (local/VPS), AWS-ready for future scaling
- **Key Features:** Listings, Auctions, Real-time Chat, Resale, Reviews, Notifications

### Architecture Layers
```
React Native Client
        ↓
Spring Cloud Gateway (Nginx Reverse Proxy)
        ↓
┌─────────────────────────────────────────────┐
│  Microservices (all in Docker containers)   │
├─────────────────────────────────────────────┤
│ • User Service (Auth, Profile, Verification)│
│ • Catalog Service (Listings, Auctions)      │
│ • Transaction Service (Orders, Cart, Payments)
│ • Messaging Service (Chat, Notifications)   │
└─────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────┐
│  Data Layer (Docker containers)             │
├─────────────────────────────────────────────┤
│ • PostgreSQL (Relational data)              │
│ • Redis (Cache, sessions, auction state)    │
│ • MongoDB (Chat messages)                   │
│ • MinIO (Media storage - S3-compatible)     │
└─────────────────────────────────────────────┘
```

---

## Database Schema

### PostgreSQL DDL (SQL)

#### 1. Users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK (type IN ('INFLUENCER', 'BUYER')),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    profile_pic_url VARCHAR(1024),
    bio TEXT,
    verified BOOLEAN DEFAULT FALSE,
    verification_status VARCHAR(20) DEFAULT 'UNVERIFIED' 
        CHECK (verification_status IN ('UNVERIFIED', 'PENDING', 'VERIFIED', 'REJECTED')),
    government_id_url VARCHAR(1024),
    selfie_url VARCHAR(1024),
    follower_count INT DEFAULT 0,
    reputation_score DECIMAL(3,2) DEFAULT 4.5,
    total_reviews INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_type ON users(type);
CREATE INDEX idx_users_verified ON users(verified);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
```

#### 2. Followers Table

```sql
CREATE TABLE followers (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(follower_id, following_id),
    CHECK (follower_id != following_id)
);

CREATE INDEX idx_followers_follower ON followers(follower_id);
CREATE INDEX idx_followers_following ON followers(following_id);
```

#### 3. Listings Table

```sql
CREATE TABLE listings (
    id BIGSERIAL PRIMARY KEY,
    influencer_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    category VARCHAR(100) NOT NULL,
    condition VARCHAR(50) NOT NULL 
        CHECK (condition IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR')),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SOLD', 'AUCTION', 'INACTIVE')),
    media_urls TEXT[], -- Array of URLs (images and videos)
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (influencer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_listings_influencer ON listings(influencer_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_category ON listings(category);
CREATE INDEX idx_listings_price ON listings(price);
CREATE INDEX idx_listings_created_at ON listings(created_at DESC);
CREATE INDEX idx_listings_view_count ON listings(view_count DESC);
```

#### 4. Auctions Table

```sql
CREATE TABLE auctions (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL UNIQUE,
    start_price DECIMAL(10,2) NOT NULL CHECK (start_price > 0),
    current_bid DECIMAL(10,2) NOT NULL,
    reserve_price DECIMAL(10,2),
    highest_bidder_id BIGINT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    winner_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'ENDED', 'CANCELLED')),
    extension_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id),
    CHECK (end_time > start_time)
);

CREATE INDEX idx_auctions_listing ON auctions(listing_id);
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_end_time ON auctions(end_time);
CREATE INDEX idx_auctions_winner ON auctions(winner_id);
```

#### 5. Bids Table

```sql
CREATE TABLE bids (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bids_auction ON bids(auction_id);
CREATE INDEX idx_bids_buyer ON bids(buyer_id);
CREATE INDEX idx_bids_placed_at ON bids(placed_at DESC);
```

#### 6. Orders Table

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    delivery_address TEXT,
    delivery_date TIMESTAMP,
    tracking_number VARCHAR(100),
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(id),
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_seller ON orders(seller_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_listing ON orders(listing_id);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
```

#### 7. Reviews Table

```sql
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    review_images TEXT[], -- Array of review image URLs
    status VARCHAR(50) DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'HIDDEN', 'REPORTED')),
    helpful_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(order_id)
);

CREATE INDEX idx_reviews_seller ON reviews(seller_id);
CREATE INDEX idx_reviews_buyer ON reviews(buyer_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);
```

#### 8. Resale Listings Table

```sql
CREATE TABLE resale_listings (
    id BIGSERIAL PRIMARY KEY,
    original_order_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    resale_price DECIMAL(10,2) NOT NULL CHECK (resale_price > 0),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SOLD', 'INACTIVE')),
    media_urls TEXT[], -- Additional photos of resale item
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (original_order_id) REFERENCES orders(id),
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_resale_buyer ON resale_listings(buyer_id);
CREATE INDEX idx_resale_status ON resale_listings(status);
CREATE INDEX idx_resale_original_order ON resale_listings(original_order_id);
CREATE INDEX idx_resale_created_at ON resale_listings(created_at DESC);
```

#### 9. Cart Items Table

```sql
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    quantity INT DEFAULT 1 CHECK (quantity > 0),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    UNIQUE(user_id, listing_id)
);

CREATE INDEX idx_cart_user ON cart_items(user_id);
CREATE INDEX idx_cart_listing ON cart_items(listing_id);
```

#### 10. Favourites Table

```sql
CREATE TABLE favourites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    UNIQUE(user_id, listing_id)
);

CREATE INDEX idx_favourites_user ON favourites(user_id);
CREATE INDEX idx_favourites_listing ON favourites(listing_id);
```

#### 11. Chat Rooms Table

```sql
CREATE TABLE chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(id),
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(listing_id, buyer_id, seller_id)
);

CREATE INDEX idx_chat_buyer ON chat_rooms(buyer_id);
CREATE INDEX idx_chat_seller ON chat_rooms(seller_id);
CREATE INDEX idx_chat_listing ON chat_rooms(listing_id);
```

#### 12. Notifications Table

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(100) NOT NULL 
        CHECK (type IN ('ORDER', 'BID', 'MESSAGE', 'REVIEW', 'OFFER')),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    related_order_id BIGINT,
    related_auction_id BIGINT,
    related_chat_room_id BIGINT,
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (related_order_id) REFERENCES orders(id),
    FOREIGN KEY (related_auction_id) REFERENCES auctions(id),
    FOREIGN KEY (related_chat_room_id) REFERENCES chat_rooms(id)
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(read);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
```

---

## API Design & Specifications

### Base URL
```
http://localhost/api (local Docker)
https://api.pnpn.com (production, future)
```

---

### AUTH SERVICE APIs

#### 1. Register User
```
POST /api/auth/register
Content-Type: application/json

Request:
{
  "name": "Ananya Sharma",
  "email": "ananya@example.com",
  "phone": "+919876543210",
  "password": "securePassword123!",
  "type": "INFLUENCER",
  "bio": "Fashion & lifestyle influencer"
}

Response: 201 Created
{
  "userId": 1,
  "type": "INFLUENCER",
  "email": "ananya@example.com",
  "message": "Registration successful. Please verify your phone."
}

Errors:
- 400: Email already exists, Invalid password format
- 409: Conflict - User already registered
```

#### 2. Send OTP
```
POST /api/auth/send-otp
Content-Type: application/json

Request:
{
  "phone": "+919876543210"
}

Response: 200 OK
{
  "message": "OTP sent to your phone",
  "expiresIn": 300 (seconds)
}

Errors:
- 400: Invalid phone format
- 429: Too many OTP requests (rate limited)
```

#### 3. Verify OTP
```
POST /api/auth/verify-otp
Content-Type: application/json

Request:
{
  "phone": "+919876543210",
  "otp": "123456"
}

Response: 200 OK
{
  "verified": true,
  "message": "Phone verified successfully"
}

Errors:
- 400: Invalid OTP, OTP expired
- 403: Forbidden - OTP verification failed 3 times
```

#### 4. Login
```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "email": "ananya@example.com",
  "password": "securePassword123!"
}

Response: 200 OK
{
  "userId": 1,
  "type": "INFLUENCER",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_token_uuid...",
  "expiresIn": 900 (seconds),
  "user": {
    "id": 1,
    "name": "Ananya Sharma",
    "email": "ananya@example.com",
    "type": "INFLUENCER",
    "verified": true,
    "profilePicUrl": "..."
  }
}

Errors:
- 401: Invalid credentials
- 403: Account not verified
```

#### 5. Refresh Token
```
POST /api/auth/refresh
Content-Type: application/json

Request:
{
  "refreshToken": "refresh_token_uuid..."
}

Response: 200 OK
{
  "accessToken": "new_jwt_token...",
  "refreshToken": "new_refresh_token...",
  "expiresIn": 900
}

Errors:
- 401: Invalid or expired refresh token
```

#### 6. Logout
```
POST /api/auth/logout
Authorization: Bearer {accessToken}

Request: (empty body)

Response: 200 OK
{
  "message": "Logged out successfully"
}
```

---

### LISTINGS SERVICE APIs

#### 1. Create Listing
```
POST /api/listings
Authorization: Bearer {accessToken}
Content-Type: application/json

Request: (INFLUENCER only)
{
  "title": "Limited Edition Saree",
  "description": "Handwoven silk saree, worn once for photoshoot",
  "price": 15000,
  "category": "FASHION",
  "condition": "LIKE_NEW",
  "isAuction": false,
  "mediaUrls": [
    "https://cdn.pnpn.com/listings/image1.jpg",
    "https://cdn.pnpn.com/listings/video1.mp4"
  ]
}

Response: 201 Created
{
  "id": 1,
  "influencerId": 1,
  "title": "Limited Edition Saree",
  "price": 15000,
  "status": "ACTIVE",
  "createdAt": "2025-06-09T10:00:00Z"
}

Errors:
- 400: Invalid input, Missing required fields
- 403: Forbidden (user is not INFLUENCER)
- 401: Unauthorized
```

#### 2. Get All Listings (Paginated & Filterable)
```
GET /api/listings?page=0&size=20&category=FASHION&minPrice=1000&maxPrice=50000&condition=LIKE_NEW&sort=createdAt,desc
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "title": "Limited Edition Saree",
      "price": 15000,
      "influencerName": "Ananya Sharma",
      "influencerId": 1,
      "category": "FASHION",
      "condition": "LIKE_NEW",
      "status": "ACTIVE",
      "thumbnailUrl": "...",
      "viewCount": 245,
      "createdAt": "2025-06-09T10:00:00Z"
    }
  ],
  "page": 0,
  "totalPages": 5,
  "totalElements": 100,
  "hasNext": true
}
```

#### 3. Get Listing Details
```
GET /api/listings/{id}
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "id": 1,
  "title": "Limited Edition Saree",
  "description": "Handwoven silk saree...",
  "price": 15000,
  "category": "FASHION",
  "condition": "LIKE_NEW",
  "status": "ACTIVE",
  "mediaUrls": ["image1.jpg", "video1.mp4"],
  "influencer": {
    "id": 1,
    "name": "Ananya Sharma",
    "verified": true,
    "profilePicUrl": "...",
    "rating": 4.8,
    "reviewCount": 150
  },
  "isFavourite": false,
  "viewCount": 245,
  "createdAt": "2025-06-09T10:00:00Z"
}

Errors:
- 404: Listing not found
```

#### 4. Get Listings by Influencer
```
GET /api/listings/influencer/{influencerId}?page=0&size=10
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [...],
  "page": 0,
  "totalPages": 2,
  "totalElements": 15
}
```

#### 5. Update Listing
```
PUT /api/listings/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request: (INFLUENCER only, owner of listing)
{
  "title": "Updated Title",
  "price": 14000,
  "description": "Updated description"
}

Response: 200 OK
{
  "id": 1,
  "title": "Updated Title",
  "price": 14000,
  "updatedAt": "2025-06-09T11:00:00Z"
}

Errors:
- 403: Forbidden (not owner)
- 404: Listing not found
```

#### 6. Delete Listing
```
DELETE /api/listings/{id}
Authorization: Bearer {accessToken}

Response: 204 No Content

Errors:
- 403: Forbidden (not owner)
- 409: Conflict (listing already sold)
```

#### 7. Generate Media Upload Presigned URL
```
POST /api/listings/{id}/media/presigned-url
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "fileName": "listing_photo_1.jpg",
  "fileType": "image/jpeg",
  "fileSize": 2048576
}

Response: 200 OK
{
  "presignedUrl": "https://minio:9000/pnpn-listings-images/...",
  "publicUrl": "https://cdn.pnpn.com/listings/listing_photo_1.jpg",
  "expiresIn": 3600 (seconds)
}

Usage:
1. Client calls this endpoint to get presigned URL
2. Client uploads file directly to MinIO using presigned URL
3. Server receives webhook or polls MinIO to confirm upload
4. Client adds returned publicUrl to listing
```

---

### AUCTIONS SERVICE APIs (Part of Catalog Service)

#### 1. Create Auction
```
POST /api/auctions
Authorization: Bearer {accessToken}
Content-Type: application/json

Request: (INFLUENCER only)
{
  "listingId": 1,
  "startPrice": 5000,
  "reservePrice": 10000,
  "startTime": "2025-06-10T18:00:00Z",
  "endTime": "2025-06-17T18:00:00Z"
}

Response: 201 Created
{
  "id": 1,
  "listingId": 1,
  "startPrice": 5000,
  "currentBid": 5000,
  "status": "ACTIVE",
  "timeLeft": "7 days"
}
```

#### 2. Get Live Auctions
```
GET /api/auctions/live?page=0&size=20
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "listing": {
        "id": 1,
        "title": "Saree",
        "thumbnailUrl": "..."
      },
      "currentBid": 12000,
      "highestBidderId": 5,
      "startPrice": 5000,
      "endTime": "2025-06-17T18:00:00Z",
      "timeLeftSeconds": 604800,
      "bidCount": 8
    }
  ]
}
```

#### 3. Get Auction Details
```
GET /api/auctions/{id}
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "id": 1,
  "listing": {...},
  "startPrice": 5000,
  "currentBid": 12000,
  "reservePrice": 10000,
  "highestBidderId": 5,
  "status": "ACTIVE",
  "startTime": "2025-06-10T18:00:00Z",
  "endTime": "2025-06-17T18:00:00Z",
  "bidHistory": [
    {
      "buyerId": 5,
      "buyerName": "Priya",
      "amount": 12000,
      "placedAt": "2025-06-16T15:00:00Z"
    }
  ]
}
```

#### 4. Place Bid (WebSocket Real-time)
```
POST /api/bids
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "auctionId": 1,
  "amount": 13000
}

Response: 200 OK
{
  "success": true,
  "currentBid": 13000,
  "message": "Bid placed successfully"
}

WebSocket Broadcast (to all subscribers of /ws/auction/{auctionId}):
{
  "type": "BID_PLACED",
  "auctionId": 1,
  "currentBid": 13000,
  "highestBidderId": 10,
  "bidCount": 9,
  "timeLeftSeconds": 580000
}

Errors:
- 400: Bid amount must be higher than current bid
- 409: Auction has ended
```

---

### CART & CHECKOUT APIs

#### 1. Add to Cart
```
POST /api/cart/add
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "listingId": 1,
  "quantity": 1
}

Response: 201 Created
{
  "id": 1,
  "listing": {...},
  "quantity": 1,
  "addedAt": "2025-06-09T10:00:00Z"
}

Errors:
- 404: Listing not found
- 409: Listing already in cart
```

#### 2. Get Cart
```
GET /api/cart
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "items": [
    {
      "id": 1,
      "listing": {
        "id": 1,
        "title": "Saree",
        "price": 15000,
        "thumbnailUrl": "..."
      },
      "quantity": 1
    }
  ],
  "subtotal": 15000,
  "tax": 2700,
  "total": 17700
}
```

#### 3. Remove from Cart
```
DELETE /api/cart/remove/{cartItemId}
Authorization: Bearer {accessToken}

Response: 204 No Content

Errors:
- 404: Cart item not found
```

#### 4. Checkout (Create Order)
```
POST /api/cart/checkout
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "deliveryAddress": "123 Main St, Mumbai, 400001",
  "cartItems": [1, 2, 3] (cart item IDs)
}

Response: 200 OK
{
  "orders": [
    {
      "id": 1,
      "listingId": 1,
      "amount": 17700,
      "status": "PENDING",
      "createdAt": "2025-06-09T10:00:00Z"
    }
  ]
}

Errors:
- 400: Empty cart, Invalid address
- 409: Listing already sold
```

---

### ORDER MANAGEMENT APIs

#### 1. Get Buyer's Orders
```
GET /api/orders/buyer?page=0&size=10&status=DELIVERED
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "listing": {
        "title": "Saree",
        "thumbnailUrl": "..."
      },
      "seller": {
        "id": 1,
        "name": "Ananya"
      },
      "amount": 17700,
      "status": "DELIVERED",
      "deliveryDate": "2025-06-15T10:00:00Z",
      "createdAt": "2025-06-09T10:00:00Z",
      "canReview": true
    }
  ],
  "page": 0,
  "totalPages": 1,
  "totalElements": 5
}
```

#### 2. Get Seller's Orders
```
GET /api/orders/seller?page=0&size=10&status=CONFIRMED
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "listing": {...},
      "buyer": {
        "id": 5,
        "name": "Priya"
      },
      "amount": 17700,
      "status": "CONFIRMED",
      "deliveryAddress": "123 Main St, Mumbai",
      "createdAt": "2025-06-09T10:00:00Z"
    }
  ]
}
```

#### 3. Get Order Details
```
GET /api/orders/{orderId}
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "id": 1,
  "listing": {...},
  "buyer": {...},
  "seller": {...},
  "amount": 17700,
  "status": "SHIPPED",
  "deliveryAddress": "123 Main St, Mumbai, 400001",
  "trackingNumber": "TRK12345",
  "deliveryDate": null,
  "timeline": [
    {
      "status": "CONFIRMED",
      "timestamp": "2025-06-09T10:30:00Z"
    },
    {
      "status": "SHIPPED",
      "timestamp": "2025-06-10T09:00:00Z"
    }
  ],
  "createdAt": "2025-06-09T10:00:00Z"
}
```

#### 4. Update Order Status
```
PUT /api/orders/{orderId}/status
Authorization: Bearer {accessToken}
Content-Type: application/json

Request: (SELLER only)
{
  "status": "SHIPPED",
  "trackingNumber": "TRK12345"
}

Response: 200 OK
{
  "id": 1,
  "status": "SHIPPED",
  "updatedAt": "2025-06-10T09:00:00Z"
}

Valid transitions:
PENDING → CONFIRMED
CONFIRMED → SHIPPED
SHIPPED → DELIVERED

Errors:
- 403: Forbidden (not seller)
- 409: Invalid status transition
```

#### 5. Cancel Order
```
POST /api/orders/{orderId}/cancel
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "reason": "Changed my mind"
}

Response: 200 OK
{
  "id": 1,
  "status": "CANCELLED",
  "refundAmount": 17700,
  "updatedAt": "2025-06-09T11:00:00Z"
}

Errors:
- 403: Forbidden (can only cancel if PENDING/CONFIRMED)
- 409: Cannot cancel delivered order
```

---

### PAYMENT APIs (Razorpay Integration)

#### 1. Create Payment Order
```
POST /api/payments/create-order
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "orderId": 1,
  "amount": 17700
}

Response: 200 OK
{
  "razorpayOrderId": "order_1A2B3C4D5E6F",
  "amount": 1770000, (in paise)
  "currency": "INR",
  "key": "rzp_live_XXXXXXXXXXX"
}
```

#### 2. Verify Payment
```
POST /api/payments/verify
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "razorpayOrderId": "order_1A2B3C4D5E6F",
  "razorpayPaymentId": "pay_XXXXXXXXXXX",
  "razorpaySignature": "signature_xxxxx"
}

Response: 200 OK
{
  "success": true,
  "message": "Payment verified",
  "orderId": 1,
  "status": "CONFIRMED"
}

Errors:
- 400: Invalid signature
- 409: Payment already processed
```

---

## Spring Boot Implementation

### Project Structure
```
pnpn-backend/
├── pnpn-gateway/
│   ├── Dockerfile
│   ├── application.yml
│   └── src/main/java/com/pnpn/gateway/...
│
├── pnpn-user-service/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── application.yml
│   └── src/main/java/com/pnpn/userservice/
│       ├── controller/AuthController.java
│       ├── service/AuthService.java
│       ├── entity/User.java
│       ├── repository/UserRepository.java
│       ├── dto/RegisterRequest.java
│       ├── config/SecurityConfig.java
│       └── util/JwtUtil.java
│
├── pnpn-catalog-service/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── application.yml
│   └── src/main/java/com/pnpn/catalogservice/
│       ├── controller/ListingController.java
│       ├── controller/AuctionController.java
│       ├── service/ListingService.java
│       ├── service/AuctionService.java
│       ├── entity/Listing.java
│       ├── entity/Auction.java
│       ├── repository/ListingRepository.java
│       ├── config/MinioConfig.java
│       └── util/MediaUploadUtil.java
│
├── pnpn-transaction-service/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── application.yml
│   └── src/main/java/com/pnpn/transactionservice/
│       ├── controller/OrderController.java
│       ├── controller/CartController.java
│       ├── controller/PaymentController.java
│       ├── service/OrderService.java
│       ├── service/CartService.java
│       ├── service/PaymentService.java
│       ├── entity/Order.java
│       ├── repository/OrderRepository.java
│       └── config/RazorpayConfig.java
│
├── pnpn-messaging-service/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── application.yml
│   └── src/main/java/com/pnpn/messagingservice/
│       ├── controller/ChatController.java
│       ├── service/ChatService.java
│       ├── config/WebSocketConfig.java
│       └── handler/ChatMessageHandler.java
│
├── pnpn-shared/
│   ├── pom.xml
│   └── src/main/java/com/pnpn/shared/
│       ├── dto/UserDto.java
│       ├── dto/ListingDto.java
│       ├── exception/PnPnException.java
│       ├── enums/UserType.java
│       └── enums/OrderStatus.java
│
├── docker-compose.yml
├── nginx.conf
└── README.md
```

---

### 1. Shared DTO Classes (pnpn-shared)

```java
// UserType.java
public enum UserType {
    INFLUENCER, BUYER
}

// OrderStatus.java
public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

// ListingCondition.java
public enum ListingCondition {
    NEW, LIKE_NEW, GOOD, FAIR
}

// UserDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserType type;
    private String profilePicUrl;
    private Boolean verified;
    private Integer followerCount;
    private Double reputationScore;
}

// ListingDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingDto {
    private Long id;
    private Long influencerId;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String condition;
    private String status;
    private List<String> mediaUrls;
    private Integer viewCount;
    private LocalDateTime createdAt;
}

// PnPnException.java
public class PnPnException extends RuntimeException {
    public PnPnException(String message) {
        super(message);
    }
    
    public PnPnException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

### 2. User Service Implementation

#### User Entity

```java
// User.java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true)
    private String phone;
    
    @Column(nullable = false)
    private String passwordHash;
    
    private String profilePicUrl;
    private String bio;
    
    @Column(nullable = false)
    private Boolean verified = false;
    
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    
    private String governmentIdUrl;
    private String selfieUrl;
    
    @Column(nullable = false)
    private Integer followerCount = 0;
    
    @Column(nullable = false)
    private BigDecimal reputationScore = new BigDecimal("4.50");
    
    @Column(nullable = false)
    private Integer totalReviews = 0;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// VerificationStatus.java
public enum VerificationStatus {
    UNVERIFIED, PENDING, VERIFIED, REJECTED
}
```

#### DTO Classes for Auth

```java
// RegisterRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String phone;
    
    @NotBlank
    @Size(min = 8)
    private String password;
    
    @NotNull
    private UserType type;
    
    private String bio;
}

// LoginRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    private String password;
}

// LoginResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private UserType type;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserDto user;
}

// OtpRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    @NotBlank
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String phone;
}

// OtpVerifyRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {
    @NotBlank
    private String phone;
    
    @NotBlank
    @Length(min = 4, max = 6)
    private String otp;
}
```

#### User Repository

```java
// UserRepository.java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByIdAndType(Long id, UserType type);
    List<User> findByVerified(Boolean verified);
}
```

#### Auth Service

```java
// AuthService.java
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final OtpProvider otpProvider; // Twilio/MSG91
    
    public void register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new PnPnException("Email already registered");
        }
        
        // Validate password strength
        if (!isStrongPassword(request.getPassword())) {
            throw new PnPnException("Password must be at least 8 characters with letters, numbers, and symbols");
        }
        
        // Create user
        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .type(request.getType())
            .bio(request.getBio())
            .verified(false)
            .verificationStatus(VerificationStatus.UNVERIFIED)
            .build();
        
        userRepository.save(user);
    }
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new PnPnException("User not found"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new PnPnException("Invalid credentials");
        }
        
        // Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getType());
        String refreshToken = UUID.randomUUID().toString();
        
        // Store refresh token in Redis (7 days expiry)
        redisTemplate.opsForValue().set(
            "refresh_token:" + refreshToken,
            user.getId().toString(),
            Duration.ofDays(7)
        );
        
        return LoginResponse.builder()
            .userId(user.getId())
            .type(user.getType())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(900L) // 15 minutes
            .user(mapToUserDto(user))
            .build();
    }
    
    public void sendOtp(OtpRequest request) {
        // Check if user exists
        User user = userRepository.findByPhone(request.getPhone())
            .orElseThrow(() -> new PnPnException("User not found"));
        
        // Generate 4-digit OTP
        String otp = generateOtp();
        
        // Store OTP in Redis (5 minutes expiry)
        redisTemplate.opsForValue().set(
            "otp:" + request.getPhone(),
            otp,
            Duration.ofMinutes(5)
        );
        
        // Send OTP via SMS
        try {
            otpProvider.sendOtp(request.getPhone(), otp);
        } catch (Exception e) {
            throw new PnPnException("Failed to send OTP: " + e.getMessage());
        }
    }
    
    public void verifyOtp(OtpVerifyRequest request) {
        String storedOtp = redisTemplate.opsForValue()
            .get("otp:" + request.getPhone());
        
        if (storedOtp == null) {
            throw new PnPnException("OTP expired");
        }
        
        if (!storedOtp.equals(request.getOtp())) {
            throw new PnPnException("Invalid OTP");
        }
        
        // Mark user as verified
        User user = userRepository.findByPhone(request.getPhone())
            .orElseThrow(() -> new PnPnException("User not found"));
        
        user.setVerified(true);
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        userRepository.save(user);
        
        // Delete OTP from Redis
        redisTemplate.delete("otp:" + request.getPhone());
    }
    
    public LoginResponse refreshToken(String refreshToken) {
        String userId = redisTemplate.opsForValue()
            .get("refresh_token:" + refreshToken);
        
        if (userId == null) {
            throw new PnPnException("Invalid or expired refresh token");
        }
        
        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> new PnPnException("User not found"));
        
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getType());
        String newRefreshToken = UUID.randomUUID().toString();
        
        // Update refresh token in Redis
        redisTemplate.delete("refresh_token:" + refreshToken);
        redisTemplate.opsForValue().set(
            "refresh_token:" + newRefreshToken,
            user.getId().toString(),
            Duration.ofDays(7)
        );
        
        return LoginResponse.builder()
            .userId(user.getId())
            .type(user.getType())
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(900L)
            .user(mapToUserDto(user))
            .build();
    }
    
    public void logout(String refreshToken) {
        redisTemplate.delete("refresh_token:" + refreshToken);
    }
    
    private boolean isStrongPassword(String password) {
        // At least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 symbol
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }
    
    private String generateOtp() {
        return String.format("%04d", new Random().nextInt(10000));
    }
    
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .type(user.getType())
            .profilePicUrl(user.getProfilePicUrl())
            .verified(user.getVerified())
            .followerCount(user.getFollowerCount())
            .reputationScore(user.getReputationScore().doubleValue())
            .build();
    }
}
```

#### Auth Controller

```java
// AuthController.java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Please verify your phone."));
        } catch (PnPnException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (PnPnException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody OtpRequest request) {
        try {
            authService.sendOtp(request);
            return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your phone",
                "expiresIn", 300
            ));
        } catch (PnPnException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        try {
            authService.verifyOtp(request);
            return ResponseEntity.ok(Map.of(
                "verified", true,
                "message", "Phone verified successfully"
            ));
        } catch (PnPnException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            LoginResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (PnPnException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
```

#### JWT Utility

```java
// JwtUtil.java
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:your-secret-key-min-32-characters-long}")
    private String secret;
    
    @Value("${jwt.expiry-minutes:15}")
    private int expiryMinutes;
    
    private final SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
    
    public String generateAccessToken(Long userId, UserType type) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiryMinutes * 60 * 1000);
        
        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("type", type.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), algorithm)
            .compact();
    }
    
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new PnPnException("Invalid JWT token");
        }
    }
    
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
    
    public UserType getUserTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return UserType.valueOf(claims.get("type", String.class));
    }
    
    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().before(new Date());
    }
}
```

#### Security Configuration

```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtUtil jwtUtil;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// JwtAuthenticationFilter.java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.getUserIdFromToken(token);
                UserType userType = jwtUtil.getUserTypeFromToken(token);
                
                // Create authentication object
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(userId, null, 
                        List.of(new SimpleGrantedAuthority("ROLE_" + userType.toString())));
                
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (PnPnException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

### 3. Catalog Service - Listings Implementation

#### Listing Entity

```java
// Listing.java
@Entity
@Table(name = "listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long influencerId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ListingCondition condition;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.ACTIVE;
    
    @Column(columnDefinition = "TEXT[]")
    private List<String> mediaUrls;
    
    @Column(nullable = false)
    private Integer viewCount = 0;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// ListingCondition.java
public enum ListingCondition {
    NEW, LIKE_NEW, GOOD, FAIR
}

// ListingStatus.java
public enum ListingStatus {
    ACTIVE, SOLD, AUCTION, INACTIVE
}
```

#### Listing DTOs

```java
// CreateListingRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateListingRequest {
    @NotBlank
    private String title;
    
    @NotBlank
    private String description;
    
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;
    
    @NotBlank
    private String category;
    
    @NotNull
    private ListingCondition condition;
    
    private boolean isAuction;
    
    @NotEmpty
    private List<String> mediaUrls;
}

// ListingResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingResponse {
    private Long id;
    private Long influencerId;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private ListingCondition condition;
    private ListingStatus status;
    private List<String> mediaUrls;
    private Integer viewCount;
    private UserDto influencer;
    private boolean isFavourite;
    private LocalDateTime createdAt;
}

// MediaPresignedUrlResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaPresignedUrlResponse {
    private String presignedUrl;
    private String publicUrl;
    private Long expiresIn;
}
```

#### Listing Repository

```java
// ListingRepository.java
@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, CustomListingRepository {
    List<Listing> findByInfluencerId(Long influencerId);
    List<Listing> findByStatus(ListingStatus status);
    Page<Listing> findByCategory(String category, Pageable pageable);
    Page<Listing> findByCondition(ListingCondition condition, Pageable pageable);
    Page<Listing> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}

// CustomListingRepository.java
public interface CustomListingRepository {
    Page<Listing> findByCriteria(ListingSearchCriteria criteria, Pageable pageable);
}

// ListingRepositoryImpl.java
@RequiredArgsConstructor
public class ListingRepositoryImpl implements CustomListingRepository {
    
    private final EntityManager em;
    
    @Override
    public Page<Listing> findByCriteria(ListingSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
        Root<Listing> listing = query.from(Listing.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (criteria.getCategory() != null) {
            predicates.add(cb.equal(listing.get("category"), criteria.getCategory()));
        }
        if (criteria.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(listing.get("price"), criteria.getMinPrice()));
        }
        if (criteria.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(listing.get("price"), criteria.getMaxPrice()));
        }
        if (criteria.getCondition() != null) {
            predicates.add(cb.equal(listing.get("condition"), criteria.getCondition()));
        }
        
        predicates.add(cb.equal(listing.get("status"), ListingStatus.ACTIVE));
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(listing.get("createdAt")));
        
        TypedQuery<Listing> typedQuery = em.createQuery(query);
        int totalRows = typedQuery.getResultList().size();
        
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        return new PageImpl<>(typedQuery.getResultList(), pageable, totalRows);
    }
}

// ListingSearchCriteria.java
@Data
public class ListingSearchCriteria {
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private ListingCondition condition;
    private Long influencerId;
    private String searchTerm;
}
```

#### Listing Service

```java
// ListingService.java
@Service
@RequiredArgsConstructor
public class ListingService {
    
    private final ListingRepository listingRepository;
    private final MinioClient minioClient;
    private final UserServiceClient userServiceClient;
    private final RedisTemplate<String, String> redisTemplate;
    
    @CircuitBreaker(name = "user-service-cb", fallbackMethod = "getUserFallback")
    public ListingResponse createListing(CreateListingRequest request, Long influencerId) {
        // Verify influencer
        UserDto influencer = userServiceClient.getUserById(influencerId);
        if (!influencer.getType().equals(UserType.INFLUENCER)) {
            throw new PnPnException("Only influencers can create listings");
        }
        
        // Create listing
        Listing listing = Listing.builder()
            .influencerId(influencerId)
            .title(request.getTitle())
            .description(request.getDescription())
            .price(request.getPrice())
            .category(request.getCategory())
            .condition(request.getCondition())
            .status(request.isAuction() ? ListingStatus.AUCTION : ListingStatus.ACTIVE)
            .mediaUrls(request.getMediaUrls())
            .viewCount(0)
            .build();
        
        listing = listingRepository.save(listing);
        
        // Invalidate cache
        redisTemplate.delete("listings:cache");
        
        return mapToResponse(listing, influencer, false);
    }
    
    public Page<ListingResponse> searchListings(ListingSearchCriteria criteria, Pageable pageable) {
        Page<Listing> listings = listingRepository.findByCriteria(criteria, pageable);
        
        return listings.map(listing -> {
            UserDto influencer = userServiceClient.getUserById(listing.getInfluencerId());
            return mapToResponse(listing, influencer, false);
        });
    }
    
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
            .orElseThrow(() -> new PnPnException("Listing not found"));
        
        // Increment view count (async via Redis)
        redisTemplate.opsForValue().increment("listing:views:" + id);
        
        UserDto influencer = userServiceClient.getUserById(listing.getInfluencerId());
        return mapToResponse(listing, influencer, false);
    }
    
    public void updateListing(Long id, CreateListingRequest request, Long influencerId) {
        Listing listing = listingRepository.findById(id)
            .orElseThrow(() -> new PnPnException("Listing not found"));
        
        if (!listing.getInfluencerId().equals(influencerId)) {
            throw new PnPnException("You can only edit your own listings");
        }
        
        if (listing.getStatus() == ListingStatus.SOLD) {
            throw new PnPnException("Cannot edit a sold listing");
        }
        
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setCategory(request.getCategory());
        listing.setCondition(request.getCondition());
        listing.setMediaUrls(request.getMediaUrls());
        
        listingRepository.save(listing);
        
        // Invalidate cache
        redisTemplate.delete("listings:cache");
    }
    
    public void deleteListing(Long id, Long influencerId) {
        Listing listing = listingRepository.findById(id)
            .orElseThrow(() -> new PnPnException("Listing not found"));
        
        if (!listing.getInfluencerId().equals(influencerId)) {
            throw new PnPnException("You can only delete your own listings");
        }
        
        if (listing.getStatus() == ListingStatus.SOLD) {
            throw new PnPnException("Cannot delete a sold listing");
        }
        
        listingRepository.delete(listing);
        
        // Delete media from MinIO
        for (String mediaUrl : listing.getMediaUrls()) {
            // Extract bucket and object name from URL
            deleteMediaFromMinio(mediaUrl);
        }
        
        // Invalidate cache
        redisTemplate.delete("listings:cache");
    }
    
    public MediaPresignedUrlResponse generatePresignedUrl(Long listingId, String fileName, String fileType) {
        // Verify listing exists
        listingRepository.findById(listingId)
            .orElseThrow(() -> new PnPnException("Listing not found"));
        
        try {
            String bucketName = getBucketForFileType(fileType);
            String objectName = "listings/" + listingId + "/" + fileName;
            
            String presignedUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(HttpMethod.PUT)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiration(3600) // 1 hour
                    .build()
            );
            
            String publicUrl = String.format("https://cdn.pnpn.com/%s/%s", bucketName, objectName);
            
            return new MediaPresignedUrlResponse(presignedUrl, publicUrl, 3600L);
        } catch (Exception e) {
            throw new PnPnException("Failed to generate presigned URL: " + e.getMessage());
        }
    }
    
    @CircuitBreaker(name = "user-service-cb", fallbackMethod = "getUserFallback")
    private UserDto getUserFallback(Long userId, Exception e) {
        // Return cached or default user info
        String cached = redisTemplate.opsForValue().get("user:" + userId);
        if (cached != null) {
            return objectMapper.readValue(cached, UserDto.class);
        }
        
        // Return minimal user info
        return UserDto.builder()
            .id(userId)
            .name("Unknown")
            .reputationScore(4.5)
            .build();
    }
    
    private String getBucketForFileType(String fileType) {
        if (fileType.startsWith("image/")) {
            return "pnpn-listings-images";
        } else if (fileType.startsWith("video/")) {
            return "pnpn-listings-videos";
        } else {
            throw new PnPnException("Unsupported file type: " + fileType);
        }
    }
    
    private void deleteMediaFromMinio(String mediaUrl) {
        try {
            // Extract bucket and object name
            // Example: https://cdn.pnpn.com/pnpn-listings-images/listings/1/photo.jpg
            // TODO: Implement deletion logic
        } catch (Exception e) {
            log.error("Failed to delete media from MinIO: " + e.getMessage());
        }
    }
    
    private ListingResponse mapToResponse(Listing listing, UserDto influencer, boolean isFavourite) {
        return ListingResponse.builder()
            .id(listing.getId())
            .influencerId(listing.getInfluencerId())
            .title(listing.getTitle())
            .description(listing.getDescription())
            .price(listing.getPrice())
            .category(listing.getCategory())
            .condition(listing.getCondition())
            .status(listing.getStatus())
            .mediaUrls(listing.getMediaUrls())
            .viewCount(listing.getViewCount())
            .influencer(influencer)
            .isFavourite(isFavourite)
            .createdAt(listing.getCreatedAt())
            .build();
    }
}
```

#### Listing Controller

```java
// ListingController.java
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {
    
    private final ListingService listingService;
    private final JwtUtil jwtUtil;
    
    @PostMapping
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<ListingResponse> createListing(
            @RequestBody @Valid CreateListingRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long influencerId = extractUserIdFromToken(authHeader);
        ListingResponse response = listingService.createListing(request, influencerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<Page<ListingResponse>> searchListings(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        
        ListingSearchCriteria criteria = new ListingSearchCriteria();
        criteria.setCategory(category);
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);
        if (condition != null) {
            criteria.setCondition(ListingCondition.valueOf(condition));
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        Page<ListingResponse> response = listingService.searchListings(criteria, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        ListingResponse response = listingService.getListingById(id);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<Void> updateListing(
            @PathVariable Long id,
            @RequestBody @Valid CreateListingRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long influencerId = extractUserIdFromToken(authHeader);
        listingService.updateListing(id, request, influencerId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<Void> deleteListing(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Long influencerId = extractUserIdFromToken(authHeader);
        listingService.deleteListing(id, influencerId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/media/presigned-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaPresignedUrlResponse> generatePresignedUrl(
            @PathVariable Long id,
            @RequestBody MediaPresignedUrlRequest request) {
        MediaPresignedUrlResponse response = 
            listingService.generatePresignedUrl(id, request.getFileName(), request.getFileType());
        return ResponseEntity.ok(response);
    }
    
    private Long extractUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        return jwtUtil.getUserIdFromToken(token);
    }
}
```

---

### 4. MinIO Configuration

```java
// MinioConfig.java
@Configuration
public class MinioConfig {
    
    @Value("${minio.url:http://minio:9000}")
    private String minioUrl;
    
    @Value("${minio.access-key:minioadmin}")
    private String accessKey;
    
    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;
    
    @Bean
    public MinioClient minioClient() throws Exception {
        MinioClient client = MinioClient.builder()
            .endpoint(minioUrl)
            .credentials(accessKey, secretKey)
            .build();
        
        // Create buckets if they don't exist
        createBucketIfNotExists(client, "pnpn-listings-images");
        createBucketIfNotExists(client, "pnpn-listings-videos");
        createBucketIfNotExists(client, "pnpn-reviews-images");
        createBucketIfNotExists(client, "pnpn-avatars");
        
        return client;
    }
    
    private void createBucketIfNotExists(MinioClient client, String bucketName) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
            
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bucket: " + bucketName, e);
        }
    }
}
```

---

## Docker & Infrastructure

### docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: pnpn-postgres
    environment:
      POSTGRES_DB: pnpn_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U postgres" ]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - pnpn-network

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: pnpn-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: [ "CMD", "redis-cli", "ping" ]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - pnpn-network

  # MongoDB Chat
  mongo:
    image: mongo:7
    container_name: pnpn-mongo
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
    healthcheck:
      test: [ "CMD", "mongosh", "--eval", "db.adminCommand('ping')" ]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - pnpn-network

  # MinIO S3-compatible Storage
  minio:
    image: minio/minio:latest
    container_name: pnpn-minio
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:9000/minio/health/live" ]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - pnpn-network

  # Spring Cloud Gateway
  gateway:
    build:
      context: ../pnpn-gateway
      dockerfile: pnpn-gateway/Dockerfile
    container_name: pnpn-gateway
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pnpn_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - pnpn-network

  # User Service
  user-service:
    build:
      context: ../pnpn-user-service
      dockerfile: pnpn-user-service/Dockerfile
    container_name: pnpn-user-service
    ports:
      - "8001:8001"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pnpn_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      JWT_SECRET: your-secret-key-min-32-characters-long-secure-key
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - pnpn-network

  # Catalog Service
  catalog-service:
    build:
      context: ../pnpn-catalog-service
      dockerfile: pnpn-catalog-service/Dockerfile
    container_name: pnpn-catalog-service
    ports:
      - "8002:8002"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pnpn_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      MINIO_URL: http://minio:9000
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      minio:
        condition: service_healthy
    networks:
      - pnpn-network

  # Transaction Service
  transaction-service:
    build:
      context: ../pnpn-transaction-service
      dockerfile: pnpn-transaction-service/Dockerfile
    container_name: pnpn-transaction-service
    ports:
      - "8003:8003"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pnpn_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      RAZORPAY_KEY_ID: ${RAZORPAY_KEY_ID}
      RAZORPAY_KEY_SECRET: ${RAZORPAY_KEY_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - pnpn-network

  # Messaging Service
  messaging-service:
    build:
      context: ../pnpn-messaging-service
      dockerfile: pnpn-messaging-service/Dockerfile
    container_name: pnpn-messaging-service
    ports:
      - "8004:8004"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATA_MONGODB_URI: mongodb://admin:password@mongo:27017/pnpn_chat?authSource=admin
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      FIREBASE_PROJECT_ID: ${FIREBASE_PROJECT_ID}
      FIREBASE_PRIVATE_KEY: ${FIREBASE_PRIVATE_KEY}
      FIREBASE_CLIENT_EMAIL: ${FIREBASE_CLIENT_EMAIL}
    depends_on:
      redis:
        condition: service_healthy
      mongo:
        condition: service_healthy
    networks:
      - pnpn-network

  # Nginx Reverse Proxy
  nginx:
    image: nginx:alpine
    container_name: pnpn-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - gateway
      - minio
    networks:
      - pnpn-network

volumes:
  postgres_data:
  redis_data:
  mongo_data:
  minio_data:

networks:
  pnpn-network:
    driver: bridge
```

### nginx.conf

```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    gzip on;

    # API Gateway Upstream
    upstream api_gateway {
        server gateway:8080;
    }

    # MinIO Upstream
    upstream minio_upstream {
        server minio:9000;
    }

    # MinIO Console Upstream
    upstream minio_console {
        server minio:9001;
    }

    # HTTP Server
    server {
        listen 80;
        server_name _;

        # API Routes
        location /api/ {
            proxy_pass http://api_gateway;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_read_timeout 60s;
        }

        # WebSocket for Chat
        location /api/ws/ {
            proxy_pass http://api_gateway;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_read_timeout 86400;
        }

        # Media CDN (MinIO)
        location /media/ {
            proxy_pass http://minio_upstream/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_cache_valid 200 24h;
            expires 30d;
        }

        # MinIO Console (for development only)
        location /minio/ {
            proxy_pass http://minio_console/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        # Health check
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
```

### Dockerfile for Spring Boot Services

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Circuit Breaker & Resilience

### Resilience4j Configuration

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50.0
        slowCallRateThreshold: 50.0
        slowCallDurationThreshold: 2000ms
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 30000ms
        permittedNumberOfCallsInHalfOpenState: 3
        
    instances:
      user-service-cb:
        baseConfig: default
        failureRateThreshold: 50
        waitDurationInOpenState: 30000ms
        
      catalog-service-cb:
        baseConfig: default
        failureRateThreshold: 50
        waitDurationInOpenState: 30000ms
        
      razorpay-cb:
        baseConfig: default
        failureRateThreshold: 30
        waitDurationInOpenState: 60000ms
        
      fcm-cb:
        baseConfig: default
        failureRateThreshold: 70
        waitDurationInOpenState: 10000ms
        
      minio-cb:
        baseConfig: default
        failureRateThreshold: 60
        waitDurationInOpenState: 20000ms
  
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1000
        retryExceptions:
          - java.net.ConnectException
          - java.io.IOException
    
    instances:
      user-service-retry:
        baseConfig: default
      
      catalog-service-retry:
        baseConfig: default
  
  timelimiter:
    configs:
      default:
        timeoutDuration: 5s
        cancelRunningFuture: true
    
    instances:
      user-service-timeout:
        baseConfig: default
        timeoutDuration: 5s
      
      catalog-service-timeout:
        baseConfig: default
        timeoutDuration: 5s
```

---

## Future Cloud Migration

When ready to migrate to AWS (Phase 4+):

| Current (Docker) | AWS Equivalent |
|---|---|
| PostgreSQL container | RDS Multi-AZ |
| Redis container | ElastiCache |
| MongoDB container | DocumentDB or Atlas |
| MinIO | S3 + CloudFront CDN |
| Nginx container | ALB + Route 53 |
| Docker Compose | ECS Fargate + ECR |
| .env file | Secrets Manager |

**Zero code changes required** — only Docker images need to be pushed to AWS ECR and services deployed to ECS.

---

## Quick Start Guide

```bash
# 1. Clone repository
git clone <repo-url>
cd pnpn-backend

# 2. Create .env file
cp .env.example .env

# 3. Build all services
docker-compose build

# 4. Start all services
docker-compose up -d

# 5. Verify services are running
docker-compose ps

# 6. Check logs
docker-compose logs -f gateway

# 7. Verify database schema
docker exec pnpn-postgres psql -U postgres -d pnpn_db -f /docker-entrypoint-initdb.d/init.sql

# 8. Access services:
#    - API: http://localhost/api
#    - MinIO Console: http://localhost/minio
#    - PostgreSQL: localhost:5432
#    - Redis: localhost:6379
#    - MongoDB: localhost:27017
```

---

---

## Architectural Decisions & Technology Choices

### Repository Structure
- **Decision:** Monorepo with Maven multi-module
- **Rationale:** 
  - Single Git repo, easier dependency management
  - Shared `pnpn-shared` module for DTOs, enums, exceptions
  - Independent service builds (each service has own `pom.xml`)
  - Clear folder structure (`pnpn-[service-name]`)
- **Deployment:** Each service builds to separate Docker image
- **CI/CD:** GitHub Actions builds all modules on push

### OAuth & Social Login

**Phase 1 (MVP):**
- Email + Password (BCrypt hashing)
- OTP via SMS (Twilio/MSG91)

**Phase 2 (Post-launch):**
- Google OAuth (influencer signup)
- Instagram OAuth (auto-import follower count)

**Future Scope (Phase 3+):**
- Apple Sign-In (iOS app)
- Facebook (follower import)

---

### Event Streaming: Kafka vs RabbitMQ

**Decision: Kafka (Phase 2+)**

**Why Kafka?**
1. **Audit Trail:** All events persisted (replay capability)
2. **Scalability:** Better for high-throughput bidding events
3. **Consumer Groups:** Multiple services consume same event independently
4. **Retention:** Keep 30-day history for compliance/debugging

**Phase 1 (MVP - Without Kafka):**
- Synchronous service calls with Circuit Breaker + Fallback
- Redis Pub/Sub for light event publishing (in-memory, no persistence)

**Phase 2 (Add Kafka):**
- Replace Redis Pub/Sub with Kafka topics
- Each service has consumer group
- 30-day retention policy
- Dead-letter topics for failed messages

---

### API Versioning Strategy

**Decision: URL Path Versioning**

**Why?**
- Path versioning is cacheable by CDN
- Clear separation per version
- Visible in browser/Swagger

**Strategy:**
```
/api/v1/listings
/api/v2/listings (if breaking change)
```

---

### Service Discovery

**Phase 1 (Docker Compose):** Docker DNS resolution
**Phase 3 (AWS/K8s):** Kubernetes-native CoreDNS (not Eureka)

---

### Testing Strategy

**Target Coverage:**
- Unit Tests: 40%
- Integration Tests: 30%
- Contract Tests: 10%
- E2E Tests: 20%

**Tools:** JUnit 5, Mockito, Pact, RestAssured

---

### Documentation

**Decision: SpringDoc OpenAPI + Aggregated Swagger UI**

**Per Service:**
- Each service has own OpenAPI spec (`/api/v1/api-docs`)
- Auto-generated from annotations

**Aggregated:**
- Single Swagger UI at `http://localhost/swagger-ui.html`
- Gateway pulls specs from all services and merges them

---

### Monitoring & Observability

**Phase 1:** Basic health checks + stdout logging
**Phase 2:** Prometheus + Grafana + ELK Stack + Jaeger
**Phase 3 (AWS):** CloudWatch Logs, CloudWatch Metrics, X-Ray

---

### Deployment Target

**Decision: AWS (primary), GCP as future option**

**Why AWS?**
1. Razorpay partnership (payments ecosystem)
2. Largest India region (Mumbai)
3. Startup credits available
4. ECS + RDS + ElastiCache well-integrated

**Phase 1:** Local Docker Compose
**Phase 2:** AWS ECS Fargate + RDS + ElastiCache
**Phase 3+:** Multi-region (Mumbai + N. Virginia)

---

### Mobile App vs Web First

**Decision: Mobile (React Native Android) first**

**Why?**
1. Target market (India) — 80% mobile-only
2. Influencer use case — phones are primary
3. Real-time features need push notifications
4. Android first (80% India market share)

**API Design Implications:**
- Paginated responses (mobile bandwidth)
- Presigned URLs (direct media uploads)
- Push notifications (FCM required)
- WebSocket support (real-time auctions)

---

### Auction Time Extensions

**Decision: Dynamic Extension (Snipe Prevention)**

**Rule:**
- If bid placed in **last 60 seconds** → extend by 60 seconds
- Maximum **3 extensions** per auction
- Counter resets if no bids for 5 minutes

---

### Minimum Bid Increment Rules

**Decision: Percentage-based, Configurable**

**Rule:**
- **₹1-₹1,000:** 5% increment
- **₹1,001-₹10,000:** 3% increment
- **₹10,001-₹100,000:** 2% increment
- **₹100,000+:** 1% increment

**Database-driven:** `bid_increment_rules` table for future adjustments

---

## Decision Summary

| Decision | Choice | Phase | Notes |
|---|---|---|---|
| Repository | Monorepo (Maven) | 1 | Single source |
| OAuth | Email + OTP (v1), Google + Instagram (v2) | 1-2 | Phased rollout |
| Service Discovery | Docker DNS (v1), k8s CoreDNS (v3) | 1-3 | Minimal overhead |
| **Events** | **Redis Pub/Sub (v1), Kafka (v2)** | **1-2** | **Audit trail + scalability** |
| API Versioning | URL path (`/api/v1`) | 1 | CDN-friendly |
| Testing | Pyramid (Unit 40%, Int 30%, Contract 10%, E2E 20%) | 1-2 | Balanced coverage |
| Documentation | SpringDoc OpenAPI + Swagger UI | 1 | Auto-generated |
| Monitoring | Prometheus+Grafana (v2), CloudWatch (v3) | 1-3 | Progressive |
| Deployment | Docker Compose (v1), AWS ECS (v2) | 1-2 | Vendor-agnostic |
| Platform | Mobile-first (React Native Android) | 1 | India market |
| Auction Extension | Dynamic snipe prevention (60s, max 3) | 1 | User experience |
| Bid Increment | Percentage-based (5%/3%/2%/1%) | 1 | Fair across prices |

---

**Document Version:** 2.1  
**Last Updated:** 2025-06-09  
**Status:** Complete with Architectural Decisions + Technology Choices
