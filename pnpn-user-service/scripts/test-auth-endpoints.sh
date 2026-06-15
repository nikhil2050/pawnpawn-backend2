#!/usr/bin/env bash
# =============================================================================
# PnPn Backend - API Endpoint Test Script
# Tests all implemented endpoints in pnpn-user-service
# Prerequisites: service running on localhost:9091, PostgreSQL + Redis up
# Usage: bash pnpn-user-service/scripts/test-auth-endpoints.sh
# =============================================================================

BASE="http://localhost:9091/api/auth"
USER_API="http://localhost:9091/api/users"
ACCESS=""
REFRESH=""
USER_ID=""

echo "═══════════════════════════════════════════════════════════════"
echo " PnPn Backend - API Test Suite"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# ──────────────────────────────────────────────────────────────────
# 1. REGISTER - Success
# ──────────────────────────────────────────────────────────────────
echo "─── 1. POST /api/auth/register (success) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Test",
    "email": "alice@example.com",
    "password": "Secure@123",
    "phone": "+1234567890",
    "type": "BUYER"
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# Extract tokens for subsequent tests
ACCESS=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
REFRESH=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('refreshToken',''))" 2>/dev/null)
USER_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('userId',''))" 2>/dev/null)

# ──────────────────────────────────────────────────────────────────
# 2. REGISTER - Duplicate (should 409)
# ──────────────────────────────────────────────────────────────────
echo "─── 2. POST /api/auth/register (duplicate email → 409) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Clone",
    "email": "alice@example.com",
    "password": "Secure@123",
    "type": "SELLER"
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"

# ──────────────────────────────────────────────────────────────────
# 3. REGISTER - Validation failure (should 400)
# ──────────────────────────────────────────────────────────────────
echo "─── 3. POST /api/auth/register (invalid data → 400) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "email": "not-an-email",
    "password": "short",
    "type": null
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# 4. LOGIN - Success
# ──────────────────────────────────────────────────────────────────
echo "─── 4. POST /api/auth/login (success) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "Secure@123"
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# Update tokens from login
ACCESS=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
REFRESH=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('refreshToken',''))" 2>/dev/null)
USER_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('userId',''))" 2>/dev/null)

# ──────────────────────────────────────────────────────────────────
# 5. LOGIN - Wrong password (should 401)
# ──────────────────────────────────────────────────────────────────
echo "─── 5. POST /api/auth/login (wrong password → 401) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "WrongPass1!"
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"

# ──────────────────────────────────────────────────────────────────
# 6. SEND OTP - Success
# ──────────────────────────────────────────────────────────────────
echo "─── 6. POST /api/auth/send-otp (success) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/send-otp" \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com"}')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
# Extract OTP code for next step
OTP_CODE=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
echo ""

# ──────────────────────────────────────────────────────────────────
# 7. SEND OTP - Unknown email (should 404)
# ──────────────────────────────────────────────────────────────────
echo "─── 7. POST /api/auth/send-otp (unknown user → 404) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/send-otp" \
  -H "Content-Type: application/json" \
  -d '{"email": "ghost@example.com"}')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"

# ──────────────────────────────────────────────────────────────────
# 8. VERIFY OTP - Success
# ──────────────────────────────────────────────────────────────────
echo "─── 8. POST /api/auth/verify-otp (success) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/verify-otp" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"alice@example.com\", \"code\": \"$OTP_CODE\"}")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# 9. VERIFY OTP - Wrong code (should 401)
# ──────────────────────────────────────────────────────────────────
echo "─── 9. POST /api/auth/verify-otp (wrong code → 401) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/verify-otp" \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com", "code": "0000"}')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"

# ──────────────────────────────────────────────────────────────────
# 10. REFRESH TOKEN - Success
# ──────────────────────────────────────────────────────────────────
echo "─── 10. POST /api/auth/refresh (success) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"$USER_ID\", \"refreshToken\": \"$REFRESH\"}")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
# Update tokens from refresh
ACCESS=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
echo ""

# ──────────────────────────────────────────────────────────────────
# 11. REFRESH TOKEN - Invalid token (should 401)
# ──────────────────────────────────────────────────────────────────
echo "─── 11. POST /api/auth/refresh (invalid token → 401) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"$USER_ID\", \"refreshToken\": \"bad-token\"}")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"

# ──────────────────────────────────────────────────────────────────
# 12. GET /api/users/me - Authenticated
# ──────────────────────────────────────────────────────────────────
echo "─── 12. GET /api/users/me (authenticated) ───"
RESP=$(curl -s -w "\n%{http_code}" -X GET "$USER_API/me" \
  -H "Authorization: Bearer $ACCESS")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# 13. GET /api/users/me - No auth (should 401)
# ──────────────────────────────────────────────────────────────────
echo "─── 13. GET /api/users/me (no auth → 401) ───"
RESP=$(curl -s -w "\n%{http_code}" -X GET "$USER_API/me")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"

# ──────────────────────────────────────────────────────────────────
# 14. LOGOUT
# ──────────────────────────────────────────────────────────────────
echo "─── 14. POST /api/auth/logout (success) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/logout" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"$USER_ID\", \"refreshToken\": \"$REFRESH\"}")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# SUMMARY
# ──────────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo " Tests complete — check status codes above"
echo " Expected:"
echo "   1. 201 (Created, register)"
echo "   2. 409 (Conflict, duplicate email)"
echo "   3. 400 (Bad Request, validation)"
echo "   4. 200 (OK, login)"
echo "   5. 401 (Unauthorized, wrong password)"
echo "   6. 200 (OK, send OTP)"
echo "   7. 404 (Not Found, unknown email)"
echo "   8. 200 (OK, verify OTP)"
echo "   9. 401 (Unauthorized, wrong OTP)"
echo "  10. 200 (OK, refresh)"
echo "  11. 401 (Unauthorized, bad refresh)"
echo "  12. 200 (OK, user profile)"
echo "  13. 401 (Unauthorized, no token)"
echo "  14. 200 (OK, logout)"
echo "═══════════════════════════════════════════════════════════════"
