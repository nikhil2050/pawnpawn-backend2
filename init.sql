-- ============================================================================
-- PnPn Backend — PostgreSQL Initialization Script
-- Version: 1.0.0
-- Description: DDL for all 12 tables, ENUMs, indexes, and constraints
-- ============================================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM Types
-- ============================================================================

CREATE TYPE user_type AS ENUM (
    'INFLUENCER',
    'FOLLOWER',
    'ADMIN'
);

CREATE TYPE listing_condition AS ENUM (
    'NEW',
    'LIKE_NEW',
    'GENTLY_USED',
    'USED',
    'FAIR'
);

CREATE TYPE listing_status AS ENUM (
    'ACTIVE',
    'SOLD',
    'WITHDRAWN',
    'RESERVED'
);

CREATE TYPE auction_status AS ENUM (
    'SCHEDULED',
    'LIVE',
    'EXTENDED',
    'ENDED',
    'SOLD',
    'WITHDRAWN'
);

CREATE TYPE order_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
    'REFUNDED'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'COMPLETED',
    'FAILED',
    'REFUNDED'
);

CREATE TYPE verification_status AS ENUM (
    'UNVERIFIED',
    'PENDING',
    'VERIFIED',
    'REJECTED'
);

CREATE TYPE notification_type AS ENUM (
    'ORDER',
    'BID',
    'MESSAGE',
    'REVIEW',
    'OFFER',
    'SYSTEM'
);

-- ============================================================================
-- 1. users
-- ============================================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            user_type NOT NULL DEFAULT 'FOLLOWER',
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(20) UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    profile_pic_url TEXT,
    bio             TEXT,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    verification_status verification_status NOT NULL DEFAULT 'UNVERIFIED',
    government_id_url   TEXT,
    selfie_url          TEXT,
    follower_count      INTEGER NOT NULL DEFAULT 0,
    reputation_score    DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    total_reviews       INTEGER NOT NULL DEFAULT 0,
    fcm_token           TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 2. listings
-- ============================================================================

CREATE TABLE listings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    influencer_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    price           DECIMAL(12,2) NOT NULL CHECK (price >= 0),
    category        VARCHAR(100) NOT NULL,
    condition       listing_condition NOT NULL DEFAULT 'NEW',
    status          listing_status NOT NULL DEFAULT 'ACTIVE',
    media_urls      TEXT[] DEFAULT '{}',
    view_count      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 3. auctions
-- ============================================================================

CREATE TABLE auctions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id          UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    start_price         DECIMAL(12,2) NOT NULL CHECK (start_price >= 0),
    current_bid         DECIMAL(12,2) CHECK (current_bid >= 0),
    reserve_price       DECIMAL(12,2) CHECK (reserve_price >= 0),
    highest_bidder_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    start_time          TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time            TIMESTAMP WITH TIME ZONE NOT NULL,
    winner_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    status              auction_status NOT NULL DEFAULT 'SCHEDULED',
    extension_count     INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 4. bids
-- ============================================================================

CREATE TABLE bids (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auction_id  UUID NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
    buyer_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount      DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    placed_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 5. orders
-- ============================================================================

CREATE TABLE orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id          UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    auction_id          UUID REFERENCES auctions(id) ON DELETE SET NULL,
    buyer_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount              DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    status              order_status NOT NULL DEFAULT 'PENDING',
    delivery_address    TEXT,
    delivery_date       DATE,
    tracking_number     VARCHAR(255),
    cancellation_reason TEXT,
    payment_status      payment_status NOT NULL DEFAULT 'PENDING',
    razorpay_order_id   VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 6. reviews
-- ============================================================================

CREATE TABLE reviews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    text        TEXT,
    images      TEXT[] DEFAULT '{}',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 7. resale_listings
-- ============================================================================

CREATE TABLE resale_listings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_order_id   UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    buyer_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resale_price        DECIMAL(12,2) NOT NULL CHECK (resale_price >= 0),
    status              listing_status NOT NULL DEFAULT 'ACTIVE',
    media_urls          TEXT[] DEFAULT '{}',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 8. chat_rooms
-- ============================================================================

CREATE TABLE chat_rooms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    buyer_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_message_at TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 9. favourites
-- ============================================================================

CREATE TABLE favourites (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id  UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, listing_id)
);

-- ============================================================================
-- 10. cart_items
-- ============================================================================

CREATE TABLE cart_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id  UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    quantity    INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    added_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, listing_id)
);

-- ============================================================================
-- 11. notifications
-- ============================================================================

CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        notification_type NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT,
    data        JSONB,
    read        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 12. followers
-- ============================================================================

CREATE TABLE followers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    influencer_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (follower_id, influencer_id),
    CHECK (follower_id <> influencer_id)
);

-- ============================================================================
-- Indexes
-- ============================================================================

-- users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_type ON users(type);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_verified ON users(verified) WHERE verified = TRUE;

-- listings
CREATE INDEX idx_listings_influencer_id ON listings(influencer_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_category ON listings(category);
CREATE INDEX idx_listings_condition ON listings(condition);
CREATE INDEX idx_listings_price ON listings(price);
CREATE INDEX idx_listings_created_at ON listings(created_at DESC);
CREATE INDEX idx_listings_influencer_status ON listings(influencer_id, status);

-- auctions
CREATE INDEX idx_auctions_listing_id ON auctions(listing_id);
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_end_time ON auctions(end_time) WHERE status IN ('SCHEDULED', 'LIVE', 'EXTENDED');
CREATE INDEX idx_auctions_highest_bidder ON auctions(highest_bidder_id);

-- bids
CREATE INDEX idx_bids_auction_id ON bids(auction_id);
CREATE INDEX idx_bids_buyer_id ON bids(buyer_id);
CREATE INDEX idx_bids_placed_at ON bids(placed_at DESC);
CREATE INDEX idx_bids_auction_amount ON bids(auction_id, amount DESC);

-- orders
CREATE INDEX idx_orders_buyer_id ON orders(buyer_id);
CREATE INDEX idx_orders_seller_id ON orders(seller_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_buyer_status ON orders(buyer_id, status);
CREATE INDEX idx_orders_seller_status ON orders(seller_id, status);

-- reviews
CREATE INDEX idx_reviews_seller_id ON reviews(seller_id);
CREATE INDEX idx_reviews_order_id ON reviews(order_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);

-- resale_listings
CREATE INDEX idx_resale_listings_buyer_id ON resale_listings(buyer_id);
CREATE INDEX idx_resale_listings_status ON resale_listings(status);

-- chat_rooms
CREATE INDEX idx_chat_rooms_buyer_id ON chat_rooms(buyer_id);
CREATE INDEX idx_chat_rooms_seller_id ON chat_rooms(seller_id);
CREATE INDEX idx_chat_rooms_listing ON chat_rooms(listing_id);
CREATE INDEX idx_chat_rooms_last_message ON chat_rooms(last_message_at DESC);

-- favourites
CREATE INDEX idx_favourites_user_id ON favourites(user_id);
CREATE INDEX idx_favourites_listing_id ON favourites(listing_id);

-- cart_items
CREATE INDEX idx_cart_items_user_id ON cart_items(user_id);
CREATE INDEX idx_cart_items_listing_id ON cart_items(listing_id);

-- notifications
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, read) WHERE read = FALSE;
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- followers
CREATE INDEX idx_followers_follower ON followers(follower_id);
CREATE INDEX idx_followers_influencer ON followers(influencer_id);
CREATE INDEX idx_followers_created_at ON followers(created_at DESC);

-- ============================================================================
-- Updated_at triggers (auto-update on row modification)
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_listings_updated_at
    BEFORE UPDATE ON listings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_auctions_updated_at
    BEFORE UPDATE ON auctions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_resale_listings_updated_at
    BEFORE UPDATE ON resale_listings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_chat_rooms_updated_at
    BEFORE UPDATE ON chat_rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
