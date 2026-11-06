#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# Creates a paid order with a damaged-plant claim for the admin demo (J4).
cd "$(dirname "$0")/.."

API_URL="${API_URL:-http://localhost:8080}"
CUSTOMER_EMAIL="${CUSTOMER_EMAIL:-customer@plantmarket.local}"
CUSTOMER_PASSWORD="${CUSTOMER_PASSWORD:-CustomerPassword123!}"

TOKEN="$(curl -sS -H 'Content-Type: application/json' \
    -d "$(jq -n --arg email "$CUSTOMER_EMAIL" --arg password "$CUSTOMER_PASSWORD" \
        '{email: $email, password: $password}')" \
    "$API_URL/api/v1/auth/login" | jq -r '.accessToken')"
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || {
    echo "Login failed" >&2
    exit 1
}

PRODUCT_ID="$(curl -sS "$API_URL/api/v1/products?size=100" \
    | jq -r '.items[] | select(.sku == "MON-001") | .id' | head -n 1)"
VARIANT_ID="$(curl -sS "$API_URL/api/v1/products/$PRODUCT_ID/variants" \
    | jq -r '.[0].id')"
ZONE_ID="$(curl -sS "$API_URL/api/v1/shipping/zones" \
    | jq -r '.[] | select(.city == "Минск") | .id' | head -n 1)"

ORDER_ID="$(curl -sS -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d "$(jq -n --arg variantId "$VARIANT_ID" '{items: [{variantId: $variantId, quantity: 1}]}')" \
    "$API_URL/api/v1/orders" | jq -r '.id')"

curl -sS -o /dev/null -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d "$(jq -n --arg zone "$ZONE_ID" \
        '{shippingZoneId: $zone, thermalPack: false, deliveryAddress: "Минск, Независимости 1"}')" \
    "$API_URL/api/v1/orders/$ORDER_ID/checkout"
curl -sS -o /dev/null -X POST -H "Authorization: Bearer $TOKEN" \
    "$API_URL/api/v1/orders/$ORDER_ID/pay"

CLAIM_ID="$(curl -sS -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{"reason":"Повреждён лист при доставке","photoUrls":["https://placehold.co/800x600?text=damage"]}' \
    "$API_URL/api/v1/orders/$ORDER_ID/claims" | jq -r '.id')"

echo "Order:  $ORDER_ID"
echo "Claim:  $CLAIM_ID"
echo "Admin:  http://localhost:5173 → Претензии"
