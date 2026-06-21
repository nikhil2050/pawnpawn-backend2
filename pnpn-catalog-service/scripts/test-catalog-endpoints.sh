#!/usr/bin/env bash

# =============================================================================
# PnPn Backend - API Endpoint Test Script
# Tests all implemented endpoints in pnpn-catalog-service
# Prerequisites: service running on localhost:9092, PostgreSQL + Redis + MinIO up
# Usage: bash pnpn-catalog-service/scripts/test-catalog-endpoints.sh
# =============================================================================

BASE="http://localhost:9091/api/auth"
CATALOG="http://localhost:9092/api/listings"
ACCESS=""
LISTING_ID=""

echo "═══════════════════════════════════════════════════════════════"
echo " PnPn Backend - Catalog API Test Suite"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# ──────────────────────────────────────────────────────────────────
# 0. Register / login an INFLUENCER to get a valid token
# ──────────────────────────────────────────────────────────────────
echo "─── 0a. POST /api/auth/register (influencer) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Influencer Test",
    "email": "influencer@example.com",
    "password": "Secure@123",
    "phone": "+1234567890",
    "type": "INFLUENCER"
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
ACCESS=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
echo ""

echo "─── 0b. POST /api/auth/login (influencer) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "influencer@example.com",
    "password": "Secure@123"
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
ACCESS=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
echo ""

# ──────────────────────────────────────────────────────────────────
# 1. CREATE LISTING - Success (INFLUENCER)
# ──────────────────────────────────────────────────────────────────
echo "─── 1. POST /api/listings (INFLUENCER → 201) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$CATALOG" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS" \
  -d '{
    "title": "Vintage Guitar",
    "description": "A rare 1965 Fender Stratocaster in sunburst finish.",
    "category": "MUSIC",
    "price": 2500.00,
    "condition": "USED_GOOD",
    "mediaUrls": ["https://example.com/guitar1.jpg"]
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
LISTING_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('listing',{}).get('id',''))" 2>/dev/null)
echo ""

# ──────────────────────────────────────────────────────────────────
# 2. CREATE LISTING - Forbidden without auth
# ──────────────────────────────────────────────────────────────────
echo "─── 2. POST /api/listings (no auth → 403) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$CATALOG" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Unauthorized Item",
    "description": "Should not be created",
    "category": "OTHER",
    "price": 10.00,
    "condition": "NEW"
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo ""

# ──────────────────────────────────────────────────────────────────
# 3. CREATE LISTING - Validation failure (should 400)
# ──────────────────────────────────────────────────────────────────
echo "─── 3. POST /api/listings (invalid data → 400) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$CATALOG" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS" \
  -d '{
    "title": "",
    "price": -5
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# 4. SEARCH LISTINGS - Success (public)
# ──────────────────────────────────────────────────────────────────
echo "─── 4. GET /api/listings (public) ───"
RESP=$(curl -s -w "\n%{http_code}" -X GET "$CATALOG?page=0&size=10&sortBy=createdAt&sortDirection=desc")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# 5. SEARCH LISTINGS - With filters
# ──────────────────────────────────────────────────────────────────
echo "─── 5. GET /api/listings?category=MUSIC&minPrice=1000 (public) ───"
RESP=$(curl -s -w "\n%{http_code}" -X GET "$CATALOG?category=MUSIC&minPrice=1000&page=0&size=5")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'totalElements={d.get(\"totalElements\",\"?\")}')" 2>/dev/null
echo ""

# ──────────────────────────────────────────────────────────────────
# 6. GET LISTING BY ID - Success (public)
# ──────────────────────────────────────────────────────────────────
echo "─── 6. GET /api/listings/{id} (public) ───"
RESP=$(curl -s -w "\n%{http_code}" -X GET "$CATALOG/$LISTING_ID")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# 7. GET LISTING BY ID - Not found (should 404)
# ──────────────────────────────────────────────────────────────────
echo "─── 7. GET /api/listings/00000000-0000-0000-0000-000000000000 (→ 404) ───"
RESP=$(curl -s -w "\n%{http_code}" -X GET "$CATALOG/00000000-0000-0000-0000-000000000000")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo ""

# ──────────────────────────────────────────────────────────────────
# 8. UPDATE LISTING - Success (INFLUENCER, owner)
# ──────────────────────────────────────────────────────────────────
echo "─── 8. PUT /api/listings/{id} (INFLUENCER owner → 200) ───"
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$CATALOG/$LISTING_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS" \
  -d '{
    "title": "Vintage Guitar (Price Reduced)",
    "description": "A rare 1965 Fender Stratocaster — now cheaper!",
    "category": "MUSIC",
    "price": 1999.99,
    "condition": "USED_GOOD",
    "mediaUrls": ["https://example.com/guitar1.jpg", "https://example.com/guitar2.jpg"]
  }')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# 9. UPDATE LISTING - No auth (should 403)
# ──────────────────────────────────────────────────────────────────
echo "─── 9. PUT /api/listings/{id} (no auth → 403) ───"
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$CATALOG/$LISTING_ID" \
  -H "Content-Type: application/json" \
  -d '{"title": "Hacked", "category": "OTHER", "price": 0.01, "condition": "NEW"}')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo ""

# ──────────────────────────────────────────────────────────────────
# 10. DELETE LISTING - No auth (should 403)
# ──────────────────────────────────────────────────────────────────
echo "─── 10. DELETE /api/listings/{id} (no auth → 403) ───"
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$CATALOG/$LISTING_ID")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo ""

# ──────────────────────────────────────────────────────────────────
# 11. DELETE LISTING - Success (INFLUENCER, owner)
# ──────────────────────────────────────────────────────────────────
echo "─── 11. DELETE /api/listings/{id} (INFLUENCER owner → 200) ───"
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$CATALOG/$LISTING_ID" \
  -H "Authorization: Bearer $ACCESS")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""

# ──────────────────────────────────────────────────────────────────
# 12. PRESIGNED URL - No auth (should 403)
# ──────────────────────────────────────────────────────────────────
echo "─── 12. POST /api/listings/{id}/media/presigned-url (no auth → 403) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$CATALOG/$LISTING_ID/media/presigned-url?bucketName=pnpn-listings-images&fileName=test.jpg")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
echo "Status: $CODE"
echo ""

# ──────────────────────────────────────────────────────────────────
# 13. PRESIGNED URL - Success (INFLUENCER)
# ──────────────────────────────────────────────────────────────────
echo "─── 13. POST /api/listings/{id}/media/presigned-url (INFLUENCER → 200) ───"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$CATALOG/$LISTING_ID/media/presigned-url?bucketName=pnpn-listings-images&fileName=test.jpg" \
  -H "Authorization: Bearer $ACCESS")
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
echo "   0a. 201 (Created, register influencer)"
echo "   0b. 200 (OK, login)"
echo "   1.  201 (Created, listing)"
echo "   2.  403 (Forbidden, no token)"
echo "   3.  400 (Bad Request, validation)"
echo "   4.  200 (OK, search)"
echo "   5.  200 (OK, filtered search)"
echo "   6.  200 (OK, listing detail)"
echo "   7.  404 (Not Found)"
echo "   8.  200 (OK, updated)"
echo "   9.  403 (Forbidden, no token)"
echo "  10.  403 (Forbidden, no token)"
echo "  11.  200 (OK, deleted)"
echo "  12.  403 (Forbidden, no token)"
echo "  13.  200 (OK, presigned URL)"
echo "═══════════════════════════════════════════════════════════════"
