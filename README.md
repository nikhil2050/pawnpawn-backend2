# PnPn

*A distributed backend platform for trust-mediated peer-to-peer exchange in curated digital ecosystems.*

---

## Overview

PnPn is a modular, event-driven backend platform engineered to facilitate high-value, trust-mediated interactions between participants within a carefully curated digital environment. The system is built on a foundation of identity verification, reputation scoring, and dynamic discovery, enabling secure and transparent exchange between users with distinct roles and permissions.

The platform emphasizes data integrity, fault tolerance, and horizontal scalability from the ground up. Every component is designed for independent deployment and isolated failure domains.

---

## Architecture

The system follows a microservice topology with an API gateway fronting five specialized services, each owning a distinct bounded context.

```
Clients ───► Gateway (:9090)
                │
        ┌───────┼───────┬───────┬───────┐
        │       │       │       │       │
     User   Catalog  Transact Messaging
    (:9091) (:9092)  (:9093)  (:9094)
        │       │       │       │
    ┌───┴───┐ ┌─┴──┐ ┌──┴──┐ ┌──┴───┐
    │  PG   │ │ PG │ │ PG  │ │  MG  │
    │ Redis │ │Redis│ │Redis│ │ Redis│
    │       │ │MinIO│ │     │ │  FCM │
    └───────┘ └────┘ └─────┘ └──────┘
```

### Services

| Service | Role |
|---|---|
| **Gateway** | API routing, rate limiting, circuit breaking, JWT validation, request aggregation |
| **User Service** | Identity management, authentication (JWT), OTP verification, reputation scoring, social graph |
| **Catalog Service** | Entity discovery, dynamic search, media asset management, view analytics |
| **Transaction Service** | Exchange lifecycle management, payment orchestration, review system |
| **Messaging Service** | Real-time communication, push notifications, event broadcasting |
| **Shared Library** | Cross-cutting DTOs, enums, exception handling, JWT utilities, security filters |

### Communication Patterns

- **Client-to-service:** All external traffic routes through the gateway via path-based routing
- **Service-to-service:** Synchronous HTTP calls with circuit breaker fallbacks for internal enrichment
- **Authentication propagation:** JWT validated at the gateway; principal propagated downstream via Spring Security context
- **Internal service auth:** API key header for machine-to-machine calls bypassing user authentication

---

## Technology Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.0, Spring Cloud Gateway 2023.0.2 |
| Build | Maven (multi-module) |
| Primary Database | PostgreSQL 16 |
| Cache & Sessions | Redis 7 |
| Message Store | MongoDB 7 |
| Object Storage | MinIO (S3-compatible) |
| Auth | JWT (jjwt 0.12.5), BCrypt |
| Resilience | Resilience4j (circuit breakers, retries, rate limiting) |
| Payments | Razorpay, Stripe |
| Notifications | Firebase Cloud Messaging |
| Real-time | WebSocket / STOMP |
| Containerization | Docker, Docker Compose |

---

## Features

- **Role-based identity system** — Distinct participant types with granular authorization via Spring Method Security
- **Multi-factor authentication** — Password + OTP verification with Redis-backed token lifecycle (access + refresh tokens)
- **Dynamic discovery engine** — JPA Criteria API for composable, type-safe queries with pagination, sorting, and multi-field filtering
- **Media asset management** — Presigned URL generation for direct client-to-storage uploads via S3-compatible object storage
- **View analytics pipeline** — Asynchronous view counting via Redis with scheduled batch persistence
- **Fault-tolerant inter-service communication** — Circuit breakers, retries, and graceful fallbacks for all service-to-service calls
- **Rate limiting** — Token bucket algorithm at the gateway layer (per-IP, per-route)
- **Unified error handling** — Consistent error response contract across all services via shared exception handler
- **Dual payment gateway support** — Razorpay and Stripe integration for geographic flexibility
- **Push notification infrastructure** — Firebase Cloud Messaging integration for real-time alerts
- **Containerized development topology** — Full local environment via Docker Compose with health-checked dependencies

---

## Project Structure

```
pnpn-gateway/              API gateway (routing, filtering, rate limiting)
pnpn-user-service/         Identity, auth, profiles, reputation, social
pnpn-catalog-service/      Entity management, search, media, analytics
pnpn-transaction-service/  Exchange lifecycle, payments, reviews
pnpn-messaging-service/    Chat, notifications, real-time events
pnpn-shared/               Shared DTOs, enums, exceptions, filters, utilities
├── docker-compose.yml     Local development topology
├── init.sql               Database schema and seed data
└── .env.example           Environment variable template
```

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Local Development

```bash
# Start infrastructure dependencies
docker compose up -d postgres redis mongodb minio

# Build all modules
mvn clean install -DskipTests

# Start individual services
mvn spring-boot:run -pl pnpn-user-service
mvn spring-boot:run -pl pnpn-catalog-service
```

Refer to `.env.example` for required environment configuration. Each service publishes its API documentation via Springdoc OpenAPI at `/swagger-ui.html` when running.

---

## Contributing

This project is currently in active development. Contributions, issue reports, and feature requests are welcome.

---

## License

Proprietary — All rights reserved.
