# pnpn-user-service — Authentication & User Profile Management

Port: `9091` | Status: **Fully implemented** | Tests: 9 test classes

## Package Structure & Classes

### `com.pnpn.userservice` (root)
- `UserServiceApplication` — `@SpringBootApplication(scanBasePackages = {"com.pnpn.userservice", "com.pnpn.shared"})`

### `com.pnpn.userservice.config`
- `SecurityConfig` — Stateless JWT security: CSRF disabled, sessionless, `/api/auth/**` and `/actuator/**` public, all else authenticated, BCryptPasswordEncoder bean. Registers `JwtAuthenticationFilter` (from `pnpn-shared`) before `UsernamePasswordAuthenticationFilter`
- `InternalApiKeyFilter` — `OncePerRequestFilter`. Validates `X-Internal-Api-Key` header against `services.internal-api-key` for inter-service calls

### `com.pnpn.userservice.entity`
- `User` — JPA entity mapping `users` table. Fields: id(UUID), type(UserType), name, email, phone, passwordHash, profilePicUrl, bio, verified, verificationStatus, governmentIdUrl, selfieUrl, followerCount, reputationScore, totalReviews, fcmToken, createdAt, updatedAt

### `com.pnpn.userservice.repository`
- `UserRepository` — extends `JpaRepository<User, UUID>`. Methods: `findByEmail(email)`, `findByPhone(phone)`, `findByIdAndType(id, type)`, `findByVerified(verified)`

### `com.pnpn.userservice.service`
- `AuthService` — registration (password validation: 8+ chars, upper/lower/digit/special), login (BCrypt verify), OTP (4-digit, 5min Redis TTL), refresh token (UUID, 7d Redis TTL), logout. Builds LoginResponse with JWT + refresh + UserDto
- `UserService` — `getUserById`, `updateProfile` (name/bio/profilePicUrl/verificationStatus), `getReputation`, `getFollowerCount`. All wrapped with `@CircuitBreaker(name = "user-service-cb")`

### `com.pnpn.userservice.controller`
- `AuthController` — `@RequestMapping("/api/auth")`:
  - `POST /register` — 201, registers new user
  - `POST /login` — 200, returns JWT + refresh token
  - `POST /send-otp` — 200, sends 4-digit OTP (returns code in dev)
  - `POST /verify-otp` — 200, verifies OTP
  - `POST /refresh` — 200, issues new token pair
  - `POST /logout` — 200, invalidates refresh token
- `UserController` — `@RequestMapping("/api/users")`:
  - `GET /me` — current user profile
  - `GET /{id}` — user by ID
  - `PUT /{id}/profile` — update profile (self only, 403 otherwise)
  - `GET /{id}/reputation` — reputation score
  - `GET /{id}/followers/count` — follower count

### Test classes (`com.pnpn.userservice.*`)
- `AuthServiceTest`, `UserServiceTest`, `AuthControllerTest`, `UserControllerTest`
- `SecurityConfigTest`, `UserTest`, `UserRepositoryTest`
- `JwtUtilTest`, `JwtAuthenticationFilterTest` — integration tests using shared `JwtUtil` / `JwtAuthenticationFilter` from `pnpn-shared`

## Key Contracts

- **Dependencies:** `pnpn-shared` (DTOs, enums, exceptions, `JwtUtil`, `JwtAuthenticationFilter`), PostgreSQL (users table), Redis (OTP + refresh tokens)
- **JWT structure:** `{sub: userId, userType: "INFLUENCER"|"FOLLOWER"|"ADMIN", iat, exp}` — signed with HMAC-SHA256. JWT utils live in `pnpn-shared`; `SecurityConfig` registers the shared `JwtAuthenticationFilter`
- **Auth flow:** Register/Login → returns `{accessToken(15min), refreshToken(7d), user(UserDto)}`. Refresh token stored in Redis keyed by `refresh:{userId}`. OTP stored in Redis keyed by `otp:{email}`
- **Self-service validation:** Profile update checks `currentUserId == targetUserId`
- **Inter-service:** No direct calls to other services; other services call user-service via gateway to resolve user info

## Configuration (application.yml)

```yaml
server.port: 9091
spring.datasource.url: jdbc:postgresql://${DB_HOST}:5432/pnpn_db
spring.jpa.hibernate.ddl-auto: update
spring.data.redis.host: ${REDIS_HOST}
jwt.secret: ${JWT_SECRET}
jwt.access-token-expiration: 900000   # 15 min
jwt.refresh-token-expiration: 604800000 # 7 days
resilience4j.circuitbreaker.instances.user-service-cb:
  slidingWindowSize: 10, failureRateThreshold: 50, waitDurationInOpenState: 10s
```
