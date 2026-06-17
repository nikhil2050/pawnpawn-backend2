# pnpn-messaging-service — Real-time Chat & Push Notifications

Port: `9094` | Status: **Skeleton only** (only Application class exists; all business logic planned)

## Package Structure & Classes

### `com.pnpn.messagingservice` (root)
- `MessagingServiceApplication` — `@SpringBootApplication`

### Planned Packages

**Entities:**
- `ChatRoom` — JPA entity mapping `chat_rooms` table. Fields: id(UUID), listingId, buyerId, sellerId, lastMessageAt, createdAt, updatedAt
- `ChatMessage` — MongoDB document. Fields: id, roomId, senderId, text, imageUrl, sentAt, read, readAt

**Repositories:**
- `ChatRoomRepository` (JPA) — findByBuyerId, findBySellerId, findByListingIdAndBuyerIdAndSellerId
- `ChatMessageRepository` (MongoDB `MongoRepository`) — findByRoomIdOrderBySentAtDesc

**WebSocket:**
- `WebSocketConfig` — `@EnableWebSocketMessageBroker`, STOMP endpoint `/ws/chat` (SockJS fallback), broker `/topic/chat/{roomId}`, `/user/queue/notifications`
- `ChatMessageHandler` — `@MessageMapping("/chat.send")`. Validates room membership, persists to MongoDB, broadcasts to `/topic/chat/{roomId}`

**Services:**
- `ChatService` — startChatRoom (create or find existing), getChatRooms (with unread counts), getChatMessages (paginated, latest 50), sendMessage, markAsRead
- `NotificationService` — sendOrderNotification, sendBidNotification, sendMessageNotification, sendOfferNotification. FCM integration via Firebase Admin SDK. Circuit breaker `fcm-cb` with offline queue fallback (Redis, retry later)

**Controllers:**
- `ChatController` — `POST /api/chat/start`, `GET /api/chat/rooms`, `GET /api/chat/rooms/{roomId}/messages?page&size`, `PUT /api/chat/rooms/{roomId}/read`
- `NotificationController` — `GET /api/notifications?page&size&type`, `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`

## Key Contracts

- **Dependencies:** `pnpn-shared`, PostgreSQL (chat_rooms table), MongoDB (chat messages — high write volume), Redis (unread counts, offline notification queue), Firebase Admin SDK (FCM push), WebSocket + STOMP, Resilience4j (`fcm-cb`)
- **Chat flow:** Client opens WebSocket connection to `/ws/chat`. Sends message via STOMP `/app/chat.send` → handler stores in MongoDB → broadcasts to `/topic/chat/{roomId}`. Offline users get FCM push notification
- **Read receipts:** `PUT /chat/rooms/{roomId}/read` marks all messages in room as read for the requesting user. Unread count cached in Redis
- **Chat room uniqueness:** One room per (listingId, buyerId, sellerId) — `startChatRoom` reuses existing room if found
- **Notification types:** ORDER, BID, MESSAGE, REVIEW, OFFER, SYSTEM. Stored in PostgreSQL `notifications` table. Push via FCM with circuit breaker fallback to Redis queue
- **FCM failure handling:** If Firebase send fails, notification is queued in Redis and retried later via scheduled task

## Configuration (application.yml)

```yaml
server.port: 9094
spring.datasource.url: jdbc:postgresql://${DB_HOST}:5432/pnpn_db
spring.data.mongodb:
  host: ${MONGO_HOST}
  port: ${MONGO_PORT}
  database: pnpn_chat
  username: ${MONGO_USERNAME}
  password: ${MONGO_PASSWORD}
spring.data.redis.host: ${REDIS_HOST}
firebase.config-path: ${FIREBASE_CONFIG_PATH}
resilience4j.circuitbreaker.instances.fcm-cb:
  slidingWindowSize: 10, failureRateThreshold: 50, waitDurationInOpenState: 10s
```

**Note:** JPA, MongoDB, and Redis configs are currently commented out in application.yml. Uncomment when entities/services are implemented.
