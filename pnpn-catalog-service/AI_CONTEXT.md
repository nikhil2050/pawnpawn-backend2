# pnpn-catalog-service — Listings, Auctions & Media Storage

Port: `9092` | Status: **Partially implemented** (entity, repo, MinIO, service, controller done; auctions & bidding planned) | Tests: 8 test classes (+1 in pnpn-shared)

## Package Structure & Classes

### `com.pnpn.catalogservice` (root)
- `CatalogServiceApplication` — `@SpringBootApplication`, `@EnableCaching`, `@EnableAsync`, `@EnableScheduling`

### `com.pnpn.catalogservice.config`
- `MinioProperties` — `@ConfigurationProperties(prefix="minio")`. Fields: url, accessKey, secretKey, buckets (`Map<String, String>`)
- `MinioConfig` — Creates `MinioClient` bean from `MinioProperties`
- `MinioInitializer` — `@EventListener(ApplicationReadyEvent.class)`. Ensures 4 buckets exist via `MinioClient.bucketExists()` + `makeBucket()`:
  - `pnpn-listings-images`, `pnpn-listings-videos`, `pnpn-reviews-images`, `pnpn-avatars`
- `SecurityConfig` — Stateless JWT security, registers `JwtAuthenticationFilter` (from `pnpn-shared`) before `UsernamePasswordAuthenticationFilter`. `/api/listings` GET public, `/actuator/**` public, all else authenticated
- `AppConfig` — exposes `RestTemplate` bean for inter-service HTTP calls

### `com.pnpn.catalogservice.client`
- `UserServiceClient` — HTTP client that calls `pnpn-user-service` to fetch `UserDto` by ID. Protected by `@CircuitBreaker(name = "user-service-cb")` with fallback returning name "Unavailable"

### `com.pnpn.catalogservice.entity`
- `Listing` — JPA entity mapping `listings` table. Fields: id(UUID), influencerId, title, description, price(BigDecimal), category, condition(ListingCondition), status(ListingStatus), mediaUrls(List<String>, `TEXT[]` via `@JdbcTypeCode(SqlTypes.ARRAY)`), viewCount, createdAt, updatedAt
- Planned: `Auction`, `Bid`, `Favourite` entities

### `com.pnpn.catalogservice.repository`
- `ListingRepository` — extends `JpaRepository<Listing, UUID> + CustomListingRepository`. Query methods: `findByInfluencerId`, `findByStatus`, `findByCategory`, `findByCondition`, `findByPriceBetween`
- `CustomListingRepository` — interface defining `Page<Listing> findByCriteria(ListingSearchCriteria, Pageable)`
- `ListingRepositoryImpl` — implements `CustomListingRepository` using JPA CriteriaBuilder. Dynamic predicates: category, condition, status, minPrice, maxPrice, influencerId, keyword (title + description LIKE). Returns paginated results via `PageImpl`
- `ListingSearchCriteria` — POJO: category, condition, status, minPrice, maxPrice, influencerId, keyword
- Planned: `AuctionRepository`, `BidRepository`, `FavouriteRepository`

### `com.pnpn.catalogservice.controller`
- `ListingController` — `@RestController @RequestMapping("/api/listings")`. Endpoints: POST `/`, GET `/`, GET `/{id}`, PUT `/{id}`, DELETE `/{id}`, POST `/{id}/media/presigned-url`. Uses `@PreAuthorize("hasRole('INFLUENCER')")` for write ops. Extracts `userId` from JWT principal

### `com.pnpn.catalogservice.service`
- `ListingService` — createListing, searchListings, getListingById, updateListing, deleteListing, generatePresignedUrl. Features:
  - `@Cacheable("listings")` + `@CacheEvict({"listings", "listingsSearch"})` on writes — Redis-backed
  - `@Async` view count increment via Redis (`listing:views:{id}`), synced to DB by `@Scheduled` task
  - `@CircuitBreaker` on user-service calls for influencer enrichment
  - Presigned URL: `Method.PUT`, 1-hour expiry via MinioClient

### Implemented REST Endpoints

| Method | Path                                   | Auth       | Description              |
|--------|----------------------------------------|------------|--------------------------|
| POST   | /api/listings                          | INFLUENCER | Create listing           |
| GET    | /api/listings                          | Public     | Search/paginate listings |
| GET    | /api/listings/{id}                     | Public     | Get listing detail       |
| PUT    | /api/listings/{id}                     | Owner      | Update listing           |
| DELETE | /api/listings/{id}                     | Owner      | Delete listing           |
| POST   | /api/listings/{id}/media/presigned-url | INFLUENCER | Get upload URL           |

### Planned
- `AuctionService` — createAuction, placeBid (atomic Redis ops), autoCloseExpiredAuctions, snipe extension (last 60s, max 3)
- `AuctionController` — auction CRUD, bid placement, live auction WebSocket
- `FavouriteService` — toggleFavourite, getFavourites (Redis cached)
- WebSocket config for `/ws/auction/{auctionId}` — STOMP broadcast on bid

### Test classes (8 in catalog-service, 1 in pnpn-shared)
- `ListingTest`, `ListingRepositoryTest`, `ListingSearchCriteriaTest`, `MinioConfigTest`
- `MinioInitializerTest`, `AppConfigTest`, `UserServiceClientTest`, `ListingServiceTest`
- Shared module: `ListingResponseTest`

## Key Contracts

- **Dependencies:** `pnpn-shared`, PostgreSQL (listings table), Redis (caching, view count, auction bids, favourites cache), MinIO (media upload presigned URLs), Resilience4j (`user-service-cb`)
- **Inter-service:** `UserServiceClient` calls `pnpn-user-service:9091/api/users/{id}` via `RestTemplate` — circuit-broken with fallback
- **Caching:** Two logical Redis-backed caches — `"listings"` (per-ID) and `"listingsSearch"` (dynamic queries). Cleared entirely on create/update/delete
- **Dynamic search:** Full-text + filter search via JPA Criteria API. Supports: category, condition, price range, keyword search on title+description, influencer filter, pagination, sort
- **Media upload flow:** Client requests presigned URL → gets a presigned MinIO URL (PUT, 1hr expiry) + public media path → client uploads directly to MinIO (no server-side buffering)
- **View counting:** Async Redis increment on each `getListingById` call. Background `@Scheduled` task (60s interval) flushes accumulated counts to DB view_count column
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
| POST   | /api/auctions                          | INFLUENCER | Create auction           |
| GET    | /api/auctions/live                     | Public     | Get live auctions        |
| GET    | /api/auctions/{id}                     | Public     | Auction detail + bids    |
| POST   | /api/bids                              | Auth       | Place bid                |
| POST   | /api/favourites/{listingId}            | Auth       | Toggle favourite         |
| GET    | /api/favourites                        | Auth       | List favourites          |
