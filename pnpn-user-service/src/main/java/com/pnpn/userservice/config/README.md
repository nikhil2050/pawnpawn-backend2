**Registration** → `POST /api/auth/register` (public, no JWT needed)
No JWT methods involved. SampleAuthService.register() hashes password via BCryptPasswordEncoder.encode(), saves user.

**Login** → `POST /api/auth/login` (public)
1. AuthService.login(email, password) verifies password via BCryptPasswordEncoder.matches()
2. JwtUtil.generateAccessToken(userId, userType) → creates signed JWT with sub=userId, userType=INFLUENCER, exp=now+15min
   
Every subsequent request (any authenticated endpoint)
3. JwtAuthenticationFilter.doFilterInternal() fires before every request
4. Calls extractToken(request) → reads Authorization: Bearer <token> header
5. Calls JwtUtil.validateToken(token) → verifies HMAC-SHA256 signature, checks expiry
6. Calls JwtUtil.getUserIdFromToken(token) → parses sub claim → UUID
7. Calls JwtUtil.getUserTypeFromToken(token) → parses userType claim → INFLUENCER / FOLLOWER
8. Creates UsernamePasswordAuthenticationToken(userId, null, [ROLE_INFLUENCER]) → sets it on SecurityContextHolder

Controller / Service layer uses auth info
9. SecurityContextHolder.getContext().getAuthentication().getPrincipal() → the userId UUID
10. @PreAuthorize("hasRole('INFLUENCER')") checks the ROLE_ authority derived from the userType claim
---

File	Endpoints
service/AuthService.java	register(), login() — validates credentials, generates JWT via JwtUtil
controller/AuthController.java	POST /api/auth/register (201), POST /api/auth/login (200)
controller/UserController.java	GET /api/users/me — reads userId from Authentication principal
cURL demo (start the service first: mvn spring-boot:run -pl pnpn-user-service):

---
Method call chain per request:
```text
REGISTER    → AuthController.register()
            → AuthService.register()
                → BCryptPasswordEncoder.encode(password)
                → userRepository.save(user)
                → JwtUtil.generateAccessToken(userId, userType)
                → returns LoginResponse (with JWT)

LOGIN       → AuthController.login()
            → AuthService.login()
                → userRepository.findByEmail(email)
                → BCryptPasswordEncoder.matches(password, passwordHash)
                → JwtUtil.generateAccessToken(userId, userType)
                → returns LoginResponse (with JWT)

AUTH'd      → JwtAuthenticationFilter.doFilterInternal()        ← EVERY request
            → extractToken(request)                            ← reads Bearer
            → JwtUtil.validateToken(token)                     ← verifies sig + expiry
            → JwtUtil.getUserIdFromToken(token)                ← parses sub claim
            → JwtUtil.getUserTypeFromToken(token)              ← parses userType claim
            → sets SecurityContext(principal=userId, role=ROLE_INFLUENCER)
            → UserController.getCurrentUser()
                → Authentication.getPrincipal()               ← UUID from SecurityContext
                → userRepository.findById(userId)
                → returns UserDto
```

---

Here are the cURL commands to test all endpoints. \
Save as `scripts/test-auth-endpoints.sh` and run after starting the service.

Prerequisites: Service running on `localhost:9091`, PostgreSQL + Redis up.

```bash scripts/test-auth-endpoints.sh```

## Individual cURLs

### 1. Register an Influencer:✅
```shell
curl -s -X POST http://localhost:9091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Britney","email":"britney@example.com","password":"Secure@123","phone":"+1234567890","type":"INFLUENCER"}'
```

### 2. Login:✅
```shell
curl -s -X POST http://localhost:9091/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"britney@example.com","password":"Secure@123"}'
```

### 3. Send OTP: ✅
```shell
curl -s -X POST http://localhost:9091/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"britney@example.com"}'
```

### 4. Verify OTP: (replace 1234 with actual code from send-otp response) ✅
```shell
curl -s -X POST http://localhost:9091/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"britney@example.com","code":"1234"}'
```

### 5. Refresh token: ✅
```shell
curl -s -X POST http://localhost:9091/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"userId":"<uuid-from-login>","refreshToken":"<token-from-login>"}'
```

### 6. Logout: ✅
```shell
curl -s -X POST http://localhost:9091/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"userId":"<uuid>","refreshToken":"<token>"}'
```

### 7. Get user profile (authenticated): ✅
```shell
curl -s http://localhost:9091/api/users/me \
  -H "Authorization: Bearer <access-token>"
```

### Error cases:
| Expect | Command                                              | Working |
|--------|------------------------------------------------------|---------|
| 400    | Register with empty name, bad email, weak password   | ✅       |
| 401    | Login with wrong password                            | ✅       |
| 409    | Register with same email again                       | ✅       |
| 404    | Send OTP to unknown email                            | ✅       |
| 401    | Verify OTP with wrong code "0000"                    | ✅       |
| 401    | Refresh with invalid token                           | ✅       |
| 401    | GET /api/users/me without Authorization header       | ✅       |

The script runs all 14 test cases (7 happy + 7 error) and prints status codes for each.

---

OTP Send Flow
The OTP flow is entirely in pnpn-user-service:
1. POST `/api/auth/send-otp` → `AuthController.java`:41 → `AuthService.java`:94
2. Validates user exists in PostgreSQL via `UserRepository.findByEmail()`
3. Generates a 4-digit code using `SecureRandom` (`AuthService.java`:98)
4. Stores it in Redis with key otp:<email>, 5-minute TTL (`AuthService.java:103`)
5. Returns the code in the HTTP response body — no external sending happens (`AuthController.java:46`)
   
Verification: `POST /api/auth/verify-otp` → `AuthController.java`:50 → `AuthService.java`:111 — looks up Redis key, compares code, deletes on match.

SMS Send Code
   There is no SMS send code. The OTP is returned directly to the client in the API response.
   pnpn-messaging-service/ is a skeleton — only a main() method, no controllers or services. 
   No Twilio, AWS SNS, or any SMS provider dependency exists anywhere in the project. 
   The User entity has a phone field and UserRepository has findByPhone(), but they are unused by the OTP flow (which only uses email).

---
**Key details:**
- Tokens are stateless — no server-side session, no DB lookup on each request
- HMAC-SHA256 signed using jwt.secret from config (shared across all microservices)
- 15 min expiry — refresh handled via a separate refresh token (Redis-backed, 7-day expiry, implemented in Prompt 1.9)
- UserType is embedded as a custom claim to derive ROLE_INFLUENCER / ROLE_FOLLOWER for Spring Security authorization
