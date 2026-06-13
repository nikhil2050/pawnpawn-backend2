# Walkthrough - Shared Module Setup (Prompt 1.5)

We have successfully implemented the core enums, custom exceptions, and the global exception handler in the `pnpn-shared` module.

## Changes Made

### Shared Module Codebase

- Added core Enums under [com/pnpn/shared/enums](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums):
  - [UserType.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/UserType.java)
  - [OrderStatus.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/OrderStatus.java)
  - [ListingCondition.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/ListingCondition.java)
  - [ListingStatus.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/ListingStatus.java)
  - [VerificationStatus.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/VerificationStatus.java)
  - [NotificationType.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/enums/NotificationType.java)
- Configured Spring Web in [pnpn-shared/pom.xml](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/pom.xml) to support `@RestControllerAdvice`.
- Added custom exception [PnPnException.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/exception/PnPnException.java) with `HttpStatus` context.
- Added standard [ErrorResponse.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/dto/ErrorResponse.java) mapping detailed validation errors.
- Added [GlobalExceptionHandler.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/main/java/com/pnpn/shared/exception/GlobalExceptionHandler.java) utilizing `@RestControllerAdvice` to intercept exceptions globally and format JSON responses consistently.

## Verification & Testing

- Created unit tests verifying constructor arguments and default behavior of `PnPnException` in [PnPnExceptionTest.java](file:///Users/nikhilkshirsagar/AdvancedComputing/NKProjects/PawnPawnAntigravityProject/Projects/pawnpawn-backend2/pnpn-shared/src/test/java/com/pnpn/shared/exception/PnPnExceptionTest.java).
- Built and ran tests successfully via Maven using `mvn clean install` and `mvn test`.
