#!/usr/bin/env bash
# Idempotent Phase 0 seed: 3 categories, 10 products (5 individual plants), 1 admin, 1 customer.
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

API_URL="${API_URL:-http://localhost:8080}"
ADMIN_EMAIL="${SEED_ADMIN_EMAIL:-admin@plantmarket.local}"
ADMIN_PASSWORD="${SEED_ADMIN_PASSWORD:-AdminPassword123!}"
CUSTOMER_EMAIL="${SEED_CUSTOMER_EMAIL:-customer@plantmarket.local}"
CUSTOMER_PASSWORD="${SEED_CUSTOMER_PASSWORD:-CustomerPassword123!}"
POSTGRES_USER="${POSTGRES_USER:-plantmarket}"

if ! command -v jq >/dev/null 2>&1; then
    echo "jq is required for seeding" >&2
    exit 1
fi

wait_for_gateway() {
    local attempt
    for attempt in $(seq 1 60); do
        if curl -fsS "$API_URL/actuator/health" >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
    done
    echo "api-gateway is not healthy at $API_URL" >&2
    return 1
}

register_or_login() {
    local email="$1"
    local password="$2"
    local full_name="$3"
    local status

    status="$(
        curl -sS -o /tmp/pm-seed-register.json -w '%{http_code}' \
            -H 'Content-Type: application/json' \
            -d "$(jq -n --arg email "$email" --arg password "$password" --arg name "$full_name" \
                '{email: $email, password: $password, fullName: $name}')" \
            "$API_URL/api/v1/auth/register"
    )"
    if [[ "$status" != "201" && "$status" != "409" ]]; then
        echo "Registration failed for $email: HTTP $status" >&2
        cat /tmp/pm-seed-register.json >&2
        return 1
    fi

    status="$(
        curl -sS -o /tmp/pm-seed-login.json -w '%{http_code}' \
            -H 'Content-Type: application/json' \
            -d "$(jq -n --arg email "$email" --arg password "$password" \
                '{email: $email, password: $password}')" \
            "$API_URL/api/v1/auth/login"
    )"
    if [[ "$status" != "200" ]]; then
        echo "Login failed for $email: HTTP $status" >&2
        cat /tmp/pm-seed-login.json >&2
        return 1
    fi
    jq -r '.accessToken' /tmp/pm-seed-login.json
}

ensure_category() {
    local name="$1"
    local slug="$2"
    local description="$3"
    local existing status

    existing="$(curl -sS "$API_URL/api/v1/categories" | jq -r --arg slug "$slug" '.[] | select(.slug == $slug) | .id')"
    if [[ -n "$existing" ]]; then
        echo "$existing"
        return 0
    fi

    status="$(
        curl -sS -o /tmp/pm-seed-category.json -w '%{http_code}' \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -H 'Content-Type: application/json' \
            -d "$(jq -n --arg name "$name" --arg slug "$slug" --arg description "$description" \
                '{name: $name, slug: $slug, description: $description}')" \
            "$API_URL/api/v1/categories"
    )"
    if [[ "$status" != "201" ]]; then
        echo "Category $slug failed: HTTP $status" >&2
        cat /tmp/pm-seed-category.json >&2
        return 1
    fi
    jq -r '.id' /tmp/pm-seed-category.json
}

ensure_product() {
    local sku="$1"
    local name="$2"
    local slug="$3"
    local description="$4"
    local category_id="$5"
    local price_minor="$6"
    local care_level="$7"
    local light="$8"
    local pet_safe="$9"
    local existing status

    existing="$(curl -sS "$API_URL/api/v1/products?size=100" | jq -r --arg sku "$sku" '.items[] | select(.sku == $sku) | .id')"
    if [[ -n "$existing" ]]; then
        return 0
    fi

    status="$(
        curl -sS -o /tmp/pm-seed-product.json -w '%{http_code}' \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -H 'Content-Type: application/json' \
            -d "$(jq -n \
                --arg sku "$sku" \
                --arg name "$name" \
                --arg slug "$slug" \
                --arg description "$description" \
                --arg categoryId "$category_id" \
                --argjson priceMinor "$price_minor" \
                --arg careLevel "$care_level" \
                --arg light "$light" \
                --argjson petSafe "$pet_safe" \
                '{sku: $sku, name: $name, slug: $slug, description: $description, categoryId: $categoryId,
                  priceMinor: $priceMinor, currency: "BYN", status: "PUBLISHED", careLevel: $careLevel,
                  light: $light, petSafe: $petSafe}')" \
            "$API_URL/api/v1/products"
    )"
    if [[ "$status" != "201" ]]; then
        echo "Product $sku failed: HTTP $status" >&2
        cat /tmp/pm-seed-product.json >&2
        return 1
    fi
}

wait_for_gateway

ADMIN_TOKEN="$(register_or_login "$ADMIN_EMAIL" "$ADMIN_PASSWORD" "Plant Market Admin")"
CUSTOMER_TOKEN="$(register_or_login "$CUSTOMER_EMAIL" "$CUSTOMER_PASSWORD" "Plant Market Customer")"

ADMIN_ID="$(curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" "$API_URL/api/v1/auth/me" | jq -r '.id')"
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d pm_auth -v ON_ERROR_STOP=1 -q \
    -c "INSERT INTO user_roles (user_id, role_name) VALUES ('$ADMIN_ID', 'ADMIN') ON CONFLICT DO NOTHING;"
# Re-login so the token carries the ADMIN role claim.
ADMIN_TOKEN="$(register_or_login "$ADMIN_EMAIL" "$ADMIN_PASSWORD" "Plant Market Admin")"

MONSTERA_ID="$(ensure_category "Монстеры" "monstera" "Крупнолистные тропические растения")"
FERNS_ID="$(ensure_category "Папоротники" "ferns" "Влажные и тенистые помещения")"
SUCCULENTS_ID="$(ensure_category "Суккуленты" "succulents" "Свет и редкий полив")"

ensure_product "MON-001" "Monstera Deliciosa" "monstera-deliciosa" "Классическая монстера с резными листьями" "$MONSTERA_ID" 129900 "EASY" "BRIGHT" false
ensure_product "MON-002" "Monstera Albo Variegata" "monstera-albo-variegata" "Пестролистная форма, редкая" "$MONSTERA_ID" 349900 "MEDIUM" "BRIGHT" false
ensure_product "FERN-001" "Nephrolepis Boston" "nephrolepis-boston" "Пышный папоротник для ванной" "$FERNS_ID" 4500 "EASY" "MEDIUM" true
ensure_product "SUCC-001" "Echeveria Elegans" "echeveria-elegans" "Розетка с голубоватым налётом" "$SUCCULENTS_ID" 3800 "EASY" "DIRECT" true
ensure_product "SUCC-002" "Haworthia Fasciata" "haworthia-fasciata" "Полосатая хавортия, компактная" "$SUCCULENTS_ID" 2900 "EASY" "BRIGHT" true

ensure_product "PLANT-001" "Philodendron Pink Princess" "philodendron-pink-princess" "Индивидуальное растение. Фото: placeholder (Phase 0)." "$MONSTERA_ID" 259900 "MEDIUM" "BRIGHT" false
ensure_product "PLANT-002" "Anthurium Clarinervium" "anthurium-clarinervium" "Индивидуальное растение. Фото: placeholder (Phase 0)." "$MONSTERA_ID" 189900 "HARD" "MEDIUM" false
ensure_product "PLANT-003" "Alocasia Frydek" "alocasia-frydek" "Индивидуальное растение. Фото: placeholder (Phase 0)." "$MONSTERA_ID" 89900 "MEDIUM" "BRIGHT" false
ensure_product "PLANT-004" "Syngonium Albo" "syngonium-albo" "Индивидуальное растение. Фото: placeholder (Phase 0)." "$MONSTERA_ID" 159900 "EASY" "MEDIUM" false
ensure_product "PLANT-005" "Scindapsus Treubii Moonlight" "scindapsus-treubii-moonlight" "Индивидуальное растение. Фото: placeholder (Phase 0)." "$MONSTERA_ID" 74900 "EASY" "MEDIUM" false

CATEGORY_COUNT="$(curl -sS "$API_URL/api/v1/categories" | jq 'length')"
PRODUCT_COUNT="$(curl -sS "$API_URL/api/v1/products?size=100" | jq '.totalElements')"

echo "Seed complete: categories=$CATEGORY_COUNT products=$PRODUCT_COUNT"
echo "Admin: $ADMIN_EMAIL / $ADMIN_PASSWORD"
echo "Customer: $CUSTOMER_EMAIL / $CUSTOMER_PASSWORD"
