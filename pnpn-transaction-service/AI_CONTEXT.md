# pnpn-transaction-service — Orders, Cart, Payments, Reviews & Resale

Port: `9093` | Status: **Skeleton only** (only Application class exists; all business logic planned)

## Package Structure & Classes

### `com.pnpn.transactionservice` (root)
- `TransactionServiceApplication` — `@SpringBootApplication`

### Planned Packages

**Entities (JPA, PostgreSQL `orders`, `cart_items`, `reviews`, `resale_listings` tables):**
- `Order` — id, listingId, auctionId, buyerId, sellerId, amount, status(OrderStatus), deliveryAddress, deliveryDate, trackingNumber, cancellationReason, paymentStatus(PaymentStatus), razorpayOrderId, razorpayPaymentId, createdAt, updatedAt
- `CartItem` — id, userId, listingId, quantity, addedAt. Unique constraint on (userId, listingId)

**Repositories:**
- `OrderRepository` — findByBuyerId, findBySellerId, findByStatus, paginated
- `CartRepository` — findByUserId, findByUserIdAndListingId

**Services:**
- `CartService` — addToCart, removeFromCart, getCart, clearCart, checkout. Validations: no self-purchase, listing not sold, listing not already in cart. Checkout creates orders from cart items, updates listing status to SOLD, publishes OrderCreatedEvent
- `OrderService` — createOrder, getOrdersByBuyer, getOrdersBySeller, getOrderById, updateOrderStatus (seller only: PENDING→CONFIRMED→SHIPPED→DELIVERED), cancelOrder (buyer, if PENDING/CONFIRMED). PnPn commission: 5%
- `PaymentService` — Razorpay integration: createRazorpayOrder, verifyPayment (signature verification). Circuit breaker `razorpay-cb` with paymentFallback (marks order PENDING, retry manually)
- `ReviewService` — createReview (after delivery, one per order), getReviewsBySeller, updateReview, deleteReview. Updates seller reputationScore + totalReviews on user-service
- `ResaleService` — createResaleListing, getMyResales, getResaleById, updateResalePrice, markAsSold. PnPn gets 5% of resale price

**Controllers:**
- `CartController` — `POST /api/cart/add`, `DELETE /api/cart/{itemId}`, `GET /api/cart`, `POST /api/cart/checkout`
- `OrderController` — `GET /api/orders/buyer`, `GET /api/orders/seller`, `GET /api/orders/{id}`, `PUT /api/orders/{id}/status`, `POST /api/orders/{id}/cancel`
- `PaymentController` — `POST /api/payments/create-order`, `POST /api/payments/verify`
- `ReviewController` — `POST /api/reviews`, `GET /api/reviews/seller/{sellerId}`, `PUT /api/reviews/{id}`, `DELETE /api/reviews/{id}`
- `ResaleController` — `POST /api/resale`, `GET /api/resale/my`, `GET /api/resale/{id}`, `PUT /api/resale/{id}/price`

## Key Contracts

- **Dependencies:** `pnpn-shared` (all DTOs), PostgreSQL (transactional data), Redis (cart sessions), Razorpay SDK (payments), Stripe SDK (payments), Resilience4j (`razorpay-cb`, `stripe-cb`)
- **Order lifecycle:** PENDING → CONFIRMED (on payment verify) → SHIPPED → DELIVERED. Cancellation allowed in PENDING/CONFIRMED states
- **Payment flow:** Client → createPaymentRequest → Razorpay order created → Client pays via Razorpay SDK → Client → verifyPayment → signature verified → order confirmed
- **Checkout flow:** Validate cart items → verify all available → create orders → clear cart → update listing status to SOLD → publish OrderCreatedEvent
- **Reviews:** Can only be created after DELIVERED. Exactly one review per order
- **Resale:** Original buyer can resell after DELIVERED. PnPn earns 5% commission on resale
- **Tax:** 18% GST on all orders (calculated in CartResponse)

## Configuration (application.yml)

```yaml
server.port: 9093
razorpay:
  key-id: ${RAZORPAY_KEY_ID}
  key-secret: ${RAZORPAY_KEY_SECRET}
stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}
resilience4j.circuitbreaker.instances:
  razorpay-cb: slidingWindowSize=10, failureRateThreshold=50, waitDurationInOpenState=10s
  stripe-cb: slidingWindowSize=10, failureRateThreshold=50, waitDurationInOpenState=10s
```

**Note:** PostgreSQL datasource and JPA config are currently commented out in application.yml. Uncomment when entities are implemented. Similarly, Redis config is commented.
