# pnpn-gateway — Spring Cloud API Gateway

Port: `9090` | Status: **Minimal** (routing + fallback done; JWT filter layer planned)

## Package Structure & Classes

### `com.pnpn.gateway` (root)
- `GatewayApplication` — `@SpringBootApplication`

### `com.pnpn.gateway.config`
- `FallbackController` — `@RestController`. `@RequestMapping("/fallback")` returns `503 Service Unavailable` JSON when circuit breaker opens

### Planned (`filter/`)
- JWT global filter (validate token from header, forward userId/userType to downstream services)
- Logging filter (request/response logging)

## Route Table

| Route Pattern                                                                     | Target Service      | URI                               |
|-----------------------------------------------------------------------------------|---------------------|-----------------------------------|
| `/api/auth/**, /api/users/**`                                                     | user-service        | `http://user-service:9091`        |
| `/api/listings/**, /api/auctions/**, /api/bids/**`                                | catalog-service     | `http://catalog-service:9092`     |
| `/api/orders/**, /api/cart/**, /api/payments/**, /api/reviews/**, /api/resale/**` | transaction-service | `http://transaction-service:9093` |
| `/api/chat/**, /api/notifications/**`                                             | messaging-service   | `http://messaging-service:9094`   |

## Cross-Cutting Features

- **Rate limiting:** 100 req/min replenish, 200 burst capacity (Redis-backed `RequestRateLimiter`)
- **Circuit breaker:** `gateway-cb` on all routes, fallback to `/fallback` endpoint
- **Retry:** 3 attempts, 1s wait (Resilience4j)
- **Timeout:** 60s per request
- **Health:** `/actuator/health` + `/actuator/circuitbreakers`

## Key Contracts

- **Dependencies:** `pnpn-shared`, Spring Cloud Gateway, Resilience4j reactive, Redis reactive, JJWT
- **Downstream routing:** Uses Docker Compose service names (`user-service`, `catalog-service`, `transaction-service`, `messaging-service`) — these resolve via the internal `pnpn-network` bridge
- **No authentication at gateway level yet** — currently delegated to user-service. Planned: global JWT validation filter at gateway to reject invalid tokens before routing
- **All service communication goes through gateway** — clients never call services directly

## Configuration (application.yml)

```yaml
server.port: 9090
spring.cloud.gateway:
  routes:
    - id: user-service, uri: http://user-service:9091, predicates: Path=/api/auth/**,/api/users/**
    - id: catalog-service, uri: http://catalog-service:9092, predicates: Path=/api/listings/**,/api/auctions/**,/api/bids/**
    - id: transaction-service, uri: http://transaction-service:9093, predicates: Path=/api/orders/**,/api/cart/**,/api/payments/**,/api/reviews/**,/api/resale/**
    - id: messaging-service, uri: http://messaging-service:9094, predicates: Path=/api/chat/**,/api/notifications/**
  default-filters:
    - RequestRateLimiter (100/200)
    - CircuitBreaker (gateway-cb → /fallback)
spring.data.redis.host: ${REDIS_HOST}
jwt.secret: ${JWT_SECRET}
resilience4j:
  circuitbreaker.configs.default: slidingWindowSize=10, failureRateThreshold=50, waitDurationInOpenState=10s
  timelimiter.configs.default: timeoutDuration=60s
  retry.configs.default: maxAttempts=3, waitDuration=1s
```
