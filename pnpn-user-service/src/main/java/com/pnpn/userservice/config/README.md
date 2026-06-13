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

## 1. REGISTER a new influencer 
```shell
curl -s -X POST http://localhost:9091/api/auth/register \
-H "Content-Type: application/json" \
-d '{
"name": "Alice",
"email": "alice@example.com",
"password": "Pass1234!",
"type": "INFLUENCER"
}' | jq .
```
```json
# Response: 201 Created
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "INFLUENCER",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",   ← JWT (exp: 15 min)
  "refreshToken": "...",
  "expiresIn": 900000,
  "user": { "id": "...", "name": "Alice", ... }
}
```

## 2. LOGIN with same credentials
```shell
curl -s -X POST http://localhost:9091/api/auth/login \
-H "Content-Type: application/json" \
-d '{
"email": "alice@example.com",
"password": "Pass1234!"
}' | jq .
```
```json
Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",   ← fresh JWT
  ...
}
```

## 3. ACCESS PROTECTED ENDPOINT with JWT
TOKEN="<paste-accessToken-from-step-2>"

```shell
curl -s http://localhost:9091/api/users/me \
-H "Authorization: Bearer $TOKEN" | jq .
```
```json
Response: 200 OK
{
  "id": "550e8400-...",
  "name": "Alice",
  "email": "alice@example.com",
  "type": "INFLUENCER",
  ...
}
```

## 4. WITHOUT TOKEN → 401
```shell
curl -s http://localhost:9091/api/users/me | jq .
```
```json
Response: 401 Unauthorized
{
  "error": "Unauthorized",
  "status": 401,
  ...
}
```

## 5. WITH EXPIRED / TAMPERED TOKEN → 401
```shell
curl -s http://localhost:9091/api/users/me \
-H "Authorization: Bearer invalid.jwt.here" | jq .
```
```json
Response: 403 Forbidden
(JWT parsing fails in filter → no auth set → security rejects)
```

## 6. REGISTER WITH DUPLICATE EMAIL → 409
```shell
curl -s -X POST http://localhost:9091/api/auth/register \
-H "Content-Type: application/json" \
-d '{
"name": "Alice",
"email": "alice@example.com",
"password": "Pass1234!",
"type": "INFLUENCER"
}' | jq .
```
```json
Response: 409 Conflict
{ "message": "Email already registered", ... }
```

## 7. LOGIN WITH WRONG PASSWORD → 401
```shell
curl -s -X POST http://localhost:9091/api/auth/login \
-H "Content-Type: application/json" \
-d '{
"email": "alice@example.com",
"password": "wrongpassword"
}' | jq .
```
```json
Response: 401 Unauthorized
{ "message": "Invalid email or password", ... }
```
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
**Key details:**
- Tokens are stateless — no server-side session, no DB lookup on each request
- HMAC-SHA256 signed using jwt.secret from config (shared across all microservices)
- 15 min expiry — refresh handled via a separate refresh token (Redis-backed, 7-day expiry, implemented in Prompt 1.9)
- UserType is embedded as a custom claim to derive ROLE_INFLUENCER / ROLE_FOLLOWER for Spring Security authorization
