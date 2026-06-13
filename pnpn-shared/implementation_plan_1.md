# Implementation Plan - Shared Module Enums & Exceptions

Implement Prompt 1.5 in `pnpn-shared` to provide core enums, custom exceptions, and a global rest controller advice exception handler to be used by all services.

## User Review Required

> [!NOTE]
> All enums will be mapped precisely to the PostgreSQL types defined in `init.sql`.

## Proposed Changes

### `pnpn-shared` Module

Create all required enums under package `com.pnpn.shared.enums`:

#### [NEW] [UserType.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/UserType.java)
- Values: `INFLUENCER`, `FOLLOWER`, `ADMIN`

#### [NEW] [OrderStatus.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/OrderStatus.java)
- Values: `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`

#### [NEW] [ListingCondition.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/ListingCondition.java)
- Values: `NEW`, `LIKE_NEW`, `GENTLY_USED`, `USED`, `FAIR`

#### [NEW] [ListingStatus.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/ListingStatus.java)
- Values: `ACTIVE`, `SOLD`, `WITHDRAWN`, `RESERVED`

#### [NEW] [VerificationStatus.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/VerificationStatus.java)
- Values: `UNVERIFIED`, `PENDING`, `VERIFIED`, `REJECTED`

#### [NEW] [NotificationType.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/NotificationType.java)
- Values: `ORDER`, `BID`, `MESSAGE`, `REVIEW`, `OFFER`, `SYSTEM`

Create custom exceptions and handler under package `com.pnpn.shared.exception` (and dto under `com.pnpn.shared.dto`):

#### [NEW] [PnPnException.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/exception/PnPnException.java)
- Extend `RuntimeException`. Add a property for `HttpStatus` (default `INTERNAL_SERVER_ERROR` or customizable) to specify the HTTP status code directly from exception instances.

#### [NEW] [ErrorResponse.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/dto/ErrorResponse.java)
- Standard error response wrapper carrying: `timestamp`, `status` (int), `error` (String), `message` (String), `path` (String), and optional `details` (list of validation errors/fields).

#### [NEW] [GlobalExceptionHandler.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/exception/GlobalExceptionHandler.java)
- `@RestControllerAdvice` class matching `PnPnException`, `MethodArgumentNotValidException`, generic `Exception`, etc. to return clean `ErrorResponse` structures with corresponding status codes.

---

## Verification Plan

### Automated Tests
- Run `mvn clean install` from the root directory to ensure `pnpn-shared` builds successfully and is ready to be resolved by other modules.
- We will add a simple unit test in `pnpn-shared/src/test/java/com/pnpn/shared` to verify enums and Exception/Handler behavior (since we want robust testing coverage).

### Manual Verification
- Verify the compilation of dependent projects to ensure no type conflicts.
