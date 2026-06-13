# PnPn Backend — Credentials & Usage Guide

> **Container prefix:** `project-pnpn-*`
> **Network:** `pnpn-network`

---

## 1. Starting the Stack

```bash
# Copy environment file and edit as needed
cp .env.example .env

# Start all services (infra + microservices)
docker compose up -d

# Start only infrastructure (for local Java dev)
docker compose up -d postgres redis mongo minio

# View logs for a specific service
docker compose logs -f user-service

# Rebuild and restart a single service after code changes
docker compose up -d --build catalog-service
```

---

## 2. Infrastructure Services

| Service    | Container Name            | Port(s)  | Credentials                        |
| ---------- | ------------------------- | -------- | ---------------------------------- |
| PostgreSQL | `project-pnpn-postgres`   | `5432`   | `postgres` / `postgres`            |
| Redis      | `project-pnpn-redis`      | `6379`   | (none)                             |
| MongoDB    | `project-pnpn-mongo`      | `27017`  | `admin` / `password`               |
| MinIO      | `project-pnpn-minio`      | `9000`   | `minioadmin` / `minioadmin`        |
| MinIO Console | (same container)       | `9001`   | `minioadmin` / `minioadmin`        |

### Connecting from host machine (outside Docker):

| Service         | Host & Port                                |
| --------------- | ------------------------------------------ |
| PostgreSQL      | `localhost:5432` — `jdbc:postgresql://localhost:5432/pnpn_db` |
| Redis           | `localhost:6379`                           |
| MongoDB         | `localhost:27017` — `mongodb://admin:password@localhost:27017/pnpn_chat?authSource=admin` |
| MinIO API       | `http://localhost:9000`                    |
| MinIO Console   | `http://localhost:9001`                    |

---

## 3. Microservice Endpoints

| Service             | Container Name                  | Internal Port | Host Port | Health Check                    |
| ------------------- | ------------------------------- | ------------- | --------- | ------------------------------- |
| Gateway             | `project-pnpn-gateway`         | `9090`        | `9090`    | `/actuator/health`              |
| User Service        | `project-pnpn-user-service`    | `9091`        | `9091`    | `/actuator/health`              |
| Catalog Service     | `project-pnpn-catalog-service` | `9092`        | `9092`    | `/actuator/health`              |
| Transaction Service | `project-pnpn-transaction-service` | `9093`    | `9093`    | `/actuator/health`              |
| Messaging Service   | `project-pnpn-messaging-service`   | `9094`    | `9094`    | `/actuator/health`              |

All external API calls go through the Gateway at `http://localhost:9090`.  
Service-to-service calls use internal Docker hostnames (e.g. `http://user-service:9091`).

### Route Map

| Gateway Path                          | Target Service       |
| ------------------------------------- | -------------------- |
| `/api/auth/**`, `/api/users/**`       | User Service (`:9091`) |
| `/api/listings/**`, `/api/auctions/**`, `/api/bids/**` | Catalog Service (`:9092`) |
| `/api/orders/**`, `/api/cart/**`, `/api/payments/**`, `/api/reviews/**`, `/api/resale/**` | Transaction Service (`:9093`) |
| `/api/chat/**`, `/api/notifications/**` | Messaging Service (`:9094`) |

---

## 4. MinIO Buckets

The catalog service creates the following buckets on startup:

- `pnpn-listings-images`
- `pnpn-listings-videos`
- `pnpn-reviews-images`
- `pnpn-avatars`

Access the MinIO console at `http://localhost:9001` to browse files.

---

## 5. Useful Commands

```bash
# Check status of all containers
docker compose ps

# Tail logs for all services
docker compose logs -f

# Stop everything
docker compose down

# Stop everything and delete volumes (⚠️ destroys data)
docker compose down -v

# Rebuild all microservices
docker compose build

# Run a specific service with a debugger (attach port 5005)
# Add this to docker-compose service env:
#   JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

---

## 6. Local Development (without Docker for Java)

For faster development iteration, run infrastructure in Docker and Java services locally:

```bash
# Start only infra
docker compose up -d postgres redis mongo minio

# Run a service locally (IntelliJ or terminal)
cd pnpn-user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or with env vars inline
DB_HOST=localhost REDIS_HOST=localhost mvn spring-boot:run -pl pnpn-user-service -am
```

When running locally, the `application.yml` defaults (`localhost`) will connect to Docker-published ports.

---

## 7. Credential Management

- **Development:** All credentials are in `.env` (which is `.gitignore`-d).
- **CI/CD:** Inject credentials via CI/CD secrets (GitHub Actions / Jenkins).
- **Production:** Use AWS Secrets Manager / GCP Secret Manager.
- **Production Gateways:** `RAZORPAY_KEY_ID`, `STRIPE_SECRET_KEY`, `JWT_SECRET` must be strong random values.

**Never commit `.env` or `firebase-service-account.json` to version control.**
