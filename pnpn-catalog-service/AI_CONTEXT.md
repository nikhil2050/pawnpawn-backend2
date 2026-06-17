# pnpn-catalog-service — Listings, Auctions & Media Storage

Port: `9092` | Status: **Partially implemented** (entity, repo, MinIO done; controller/service/dto layers planned) | Tests: 4 test classes

## Package Structure & Classes

### `com.pnpn.catalogservice` (root)
- `CatalogServiceApplication` — `@SpringBootApplication`

### `com.pnpn.catalogservice.config`
- `MinioConfig` — `@ConfigurationProperties(prefix="minio")`. Creates `MinioClient` bean pointing to `http://{minio.host}:9000`. On `@PostConstruct`, ensures 4 buckets exist:
  - `pnpn-listings-images`, `pnpn-listings-videos`, `pnpn-reviews-images`, `pnpn-avatars`

### `com.pnpn.catalogservice.entity`
- `Listing` — JPA entity mapping `listings` table. Fields: id(UUID), influencerId, title, description, price(BigDecimal), category, condition(ListingCondition), status(ListingStatus), mediaUrls(List<String>, `TEXT[]` via `@JdbcTypeCode(SqlTypes.ARRAY)`), viewCount, createdAt, updatedAt
- Planned: `Auction`, `Bid`, `Favourite` entities

### `com.pnpn.catalogservice.repository`
- `ListingRepository` — extends `JpaRepository<Listing, UUID> + CustomListingRepository`. Query methods: `findByInfluencerId`, `findByStatus`, `findByCategory`, `findByCondition`, `findByPriceBetween`
- `CustomListingRepository` — interface defining `Page<Listing> findByCriteria(ListingSearchCriteria, Pageable)`
- `ListingRepositoryImpl` — implements `CustomListingRepository` using JPA CriteriaBuilder. Dynamic predicates: category, condition, status, minPrice, maxPrice, influencerId, keyword (title + description LIKE). Returns paginated results via `PageImpl`
- `ListingSearchCriteria` — POJO: category, condition, status, minPrice, maxPrice, influencerId, keyword
- Planned: `AuctionRepository`, `BidRepository`, `FavouriteRepository`

### Planned (`controller/`, `service/`, `dto/`)
- `ListingService` — createListing, searchListings, getListingById, updateListing, deleteListing, generatePresignedUrl. Circuit breaker for user-service. Cache invalidation. View count via Redis
- `ListingController` — CRUD endpoints, `@PreAuthorize("hasRole('INFLUENCER')")` for write ops
- `AuctionService` — createAuction, placeBid (atomic Redis ops), autoCloseExpiredAuctions, snipe extension (last 60s, max 3)
- `AuctionController` — auction CRUD, bid placement, live auction WebSocket
- `FavouriteService` — toggleFavourite, getFavourites (Redis cached)
- WebSocket config for `/ws/auction/{auctionId}` — STOMP broadcast on bid

### Test classes
- `ListingTest`, `ListingRepositoryTest`, `ListingSearchCriteriaTest`, `MinioConfigTest`

## Key Contracts

- **Dependencies:** `pnpn-shared`, PostgreSQL (listings table), Redis (view count, auction bids, favourites cache), MinIO (media upload presigned URLs), Resilience4j (`user-service-cb`)
- **Dynamic search:** Full-text + filter search via JPA Criteria API. Supports: category, condition, price range, keyword search on title+description, influencer filter, pagination
- **Media upload flow:** Client requests `POST /listings/{id}/media/presigned-url` → gets a presigned MinIO URL (PUT, 1hr expiry) → client uploads directly to MinIO
- **Auctions:** Scheduled `@Scheduled` task to auto-close expired auctions. Snipe protection extends auction by 60s if bid placed in last 60s (max 3 extensions)
- **Bid validation:** Amount >= minimumBid (increment calculator: 5%/3%/2%/1% based on price tiers)

## Configuration (application.yml)

```yaml
server.port: 9092
spring.datasource.url: jdbc:postgresql://${DB_HOST}:5432/pnpn_db
spring.jpa.hibernate.ddl-auto: update
spring.data.redis.host: ${REDIS_HOST}
minio:
  url: http://${MINIO_HOST}:9000
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  buckets:
    listings-images: pnpn-listings-images
    listings-videos: pnpn-listings-videos
    reviews-images: pnpn-reviews-images
    avatars: pnpn-avatars
```

## Planned REST Endpoints

| Method | Path                                   | Auth       | Description              |
|--------|----------------------------------------|------------|--------------------------|
| POST   | /api/listings                          | INFLUENCER | Create listing           |
| GET    | /api/listings                          | Public     | Search/paginate listings |
| GET    | /api/listings/{id}                     | Public     | Get listing detail       |
| PUT    | /api/listings/{id}                     | Owner      | Update listing           |
| DELETE | /api/listings/{id}                     | Owner      | Delete listing           |
| POST   | /api/listings/{id}/media/presigned-url | INFLUENCER | Get upload URL           |
| POST   | /api/auctions                          | INFLUENCER | Create auction           |
| GET    | /api/auctions/live                     | Public     | Get live auctions        |
| GET    | /api/auctions/{id}                     | Public     | Auction detail + bids    |
| POST   | /api/bids                              | Auth       | Place bid                |
| POST   | /api/favourites/{listingId}            | Auth       | Toggle favourite         |
| GET    | /api/favourites                        | Auth       | List favourites          |
