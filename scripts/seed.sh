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
    local attempt

    for attempt in $(seq 1 12); do
        status="$(
            curl -sS -o /tmp/pm-seed-register.json -w '%{http_code}' \
                -H 'Content-Type: application/json' \
                -d "$(jq -n --arg email "$email" --arg password "$password" --arg name "$full_name" \
                    '{email: $email, password: $password, fullName: $name}')" \
                "$API_URL/api/v1/auth/register"
        )"
        [[ "$status" != "429" ]] && break
        sleep 5
    done
    if [[ "$status" != "201" && "$status" != "409" ]]; then
        echo "Registration failed for $email: HTTP $status" >&2
        cat /tmp/pm-seed-register.json >&2
        return 1
    fi

    for attempt in $(seq 1 12); do
        status="$(
            curl -sS -o /tmp/pm-seed-login.json -w '%{http_code}' \
                -H 'Content-Type: application/json' \
                -d "$(jq -n --arg email "$email" --arg password "$password" \
                    '{email: $email, password: $password}')" \
                "$API_URL/api/v1/auth/login"
        )"
        [[ "$status" != "429" ]] && break
        sleep 5
    done
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
    local variegated="${10:-false}"
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
                --argjson variegated "$variegated" \
                '{sku: $sku, name: $name, slug: $slug, description: $description, categoryId: $categoryId,
                  priceMinor: $priceMinor, currency: "BYN", status: "PUBLISHED", careLevel: $careLevel,
                  light: $light, petSafe: $petSafe, variegated: $variegated}')" \
            "$API_URL/api/v1/products"
    )"
    if [[ "$status" != "201" ]]; then
        echo "Product $sku failed: HTTP $status" >&2
        cat /tmp/pm-seed-product.json >&2
        return 1
    fi
}

product_id_by_sku() {
    local sku="$1"
    curl -sS "$API_URL/api/v1/products?size=100" \
        | jq -r --arg sku "$sku" '.items[] | select(.sku == $sku) | .id' \
        | head -n 1
}

ensure_variant() {
    local product_sku="$1"
    local sku="$2"
    local name="$3"
    local size="$4"
    local pot_size="$5"
    local form="$6"
    local price_minor="$7"
    local status="$8"
    local product_id existing

    product_id="$(product_id_by_sku "$product_sku")"
    existing="$(curl -sS "$API_URL/api/v1/products/$product_id/variants" \
        | jq -r --arg sku "$sku" '.[] | select(.sku == $sku) | .id')"
    if [[ -n "$existing" ]]; then
        return 0
    fi

    curl -sS -o /tmp/pm-seed-variant.json \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H 'Content-Type: application/json' \
        -d "$(jq -n --arg sku "$sku" --arg name "$name" --arg size "$size" \
            --arg potSize "$pot_size" --arg form "$form" --argjson priceMinor "$price_minor" \
            --arg status "$status" \
            '{sku: $sku, name: $name, size: $size, potSize: $potSize, form: $form,
              priceMinor: $priceMinor, currency: "BYN", status: $status}')" \
        "$API_URL/api/v1/products/$product_id/variants" >/dev/null
}

transition_plant() {
    local plant_id="$1"
    local status="$2"
    curl -sS -o /dev/null \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H 'Content-Type: application/json' \
        -d "$(jq -n --arg status "$status" '{status: $status}')" \
        "$API_URL/api/v1/individual-plants/$plant_id/transitions"
}

ensure_individual_plant() {
    local product_sku="$1"
    local code="$2"
    local price_minor="$3"
    local care_level="$4"
    local light="$5"
    local pet_safe="$6"
    local product_id existing_id current_status

    product_id="$(product_id_by_sku "$product_sku")"
    existing_id="$(curl -sS "$API_URL/api/v1/individual-plants?search=$code&size=100" \
        | jq -r --arg code "$code" '.items[] | select(.code == $code) | .id' \
        | head -n 1)"

    if [[ -z "$existing_id" ]]; then
        existing_id="$(curl -sS \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -H 'Content-Type: application/json' \
            -d "$(jq -n --arg code "$code" --argjson priceMinor "$price_minor" \
                --arg careLevel "$care_level" --arg light "$light" --argjson petSafe "$pet_safe" \
                '{code: $code, description: "Индивидуальное растение. Фото: placeholder.",
                  photoUrl: "https://placehold.co/600x800?text=plant", priceMinor: $priceMinor,
                  currency: "BYN", careLevel: $careLevel, light: $light, petSafe: $petSafe}')" \
            "$API_URL/api/v1/products/$product_id/individual-plants" \
            | jq -r '.id')"
    fi

    current_status="$(curl -sS "$API_URL/api/v1/individual-plants/$existing_id" | jq -r '.status')"
    if [[ "$current_status" == "ACQUIRED" ]]; then
        transition_plant "$existing_id" "QUARANTINE"
        current_status="QUARANTINE"
    fi
    if [[ "$current_status" == "QUARANTINE" ]]; then
        transition_plant "$existing_id" "AVAILABLE"
    fi
}

ensure_stock_batch() {
    local product_sku="$1"
    local variant_sku="$2"
    local supplier="$3"
    local quantity="$4"
    local unit_cost_minor="$5"
    local product_id variant_id existing batch_id

    product_id="$(product_id_by_sku "$product_sku")"
    variant_id="$(curl -sS "$API_URL/api/v1/products/$product_id/variants" \
        | jq -r --arg sku "$variant_sku" '.[] | select(.sku == $sku) | .id')"
    existing="$(curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$API_URL/api/v1/stock-batches?variantId=$variant_id" \
        | jq -r --arg supplier "$supplier" '.[] | select(.supplier == $supplier) | .id')"
    if [[ -n "$existing" ]]; then
        return 0
    fi

    batch_id="$(curl -sS \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H 'Content-Type: application/json' \
        -d "$(jq -n --arg variantId "$variant_id" --arg supplier "$supplier" \
            --argjson quantity "$quantity" --argjson unitCostMinor "$unit_cost_minor" \
            '{productVariantId: $variantId, supplier: $supplier, quantity: $quantity,
              unitCostMinor: $unitCostMinor, currency: "BYN",
              quarantineUntil: "2030-01-01T00:00:00Z"}')" \
        "$API_URL/api/v1/stock-batches" | jq -r '.id')"
    curl -sS -o /dev/null -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$API_URL/api/v1/stock-batches/$batch_id/release"
}

ensure_available_stock() {
    local product_sku="$1"
    local variant_sku="$2"
    local threshold="$3"
    local quantity="$4"
    local product_id variant_id available batch_id

    product_id="$(product_id_by_sku "$product_sku")"
    variant_id="$(curl -sS "$API_URL/api/v1/products/$product_id/variants" \
        | jq -r --arg sku "$variant_sku" '.[] | select(.sku == $sku) | .id')"
    available="$(curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$API_URL/api/v1/stock-batches?variantId=$variant_id" \
        | jq -r '[.[] | select(.status == "AVAILABLE") | .remainingQuantity] | add // 0')"
    if (( available >= threshold )); then
        return 0
    fi

    batch_id="$(curl -sS \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H 'Content-Type: application/json' \
        -d "$(jq -n --arg variantId "$variant_id" --argjson quantity "$quantity" \
            '{productVariantId: $variantId, supplier: "Seed Top-up", quantity: $quantity,
              unitCostMinor: 5000, currency: "BYN",
              quarantineUntil: "2030-01-01T00:00:00Z"}')" \
        "$API_URL/api/v1/stock-batches" | jq -r '.id')"
    curl -sS -o /dev/null -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$API_URL/api/v1/stock-batches/$batch_id/release"
}

ensure_shipping_zone() {
    local name="$1"
    local city="$2"
    local price_minor="$3"
    local free_from_minor="$4"
    local min_days="$5"
    local max_days="$6"
    local winter="$7"
    local thermal_minor="$8"
    local existing

    existing="$(curl -sS "$API_URL/api/v1/shipping/zones" \
        | jq -r --arg name "$name" '.[] | select(.name == $name) | .id')"
    if [[ -n "$existing" ]]; then
        return 0
    fi

    curl -sS -o /tmp/pm-seed-zone.json \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H 'Content-Type: application/json' \
        -d "$(jq -n --arg name "$name" --arg city "$city" --argjson priceMinor "$price_minor" \
            --argjson freeFromMinor "$free_from_minor" --argjson minDays "$min_days" \
            --argjson maxDays "$max_days" --argjson winter "$winter" \
            --argjson thermalMinor "$thermal_minor" \
            '{name: $name, city: $city, priceMinor: $priceMinor, freeFromMinor: $freeFromMinor,
              minDays: $minDays, maxDays: $maxDays, winterRequiresThermal: $winter,
              thermalPackMinor: $thermalMinor, active: true}')" \
        "$API_URL/api/v1/shipping/zones" >/dev/null
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

ensure_product "PLANT-001" "Philodendron Pink Princess" "philodendron-pink-princess" "Индивидуальное растение. Фото: placeholder." "$MONSTERA_ID" 259900 "MEDIUM" "BRIGHT" false true
ensure_product "PLANT-002" "Anthurium Clarinervium" "anthurium-clarinervium" "Индивидуальное растение. Фото: placeholder." "$MONSTERA_ID" 189900 "HARD" "MEDIUM" false true
ensure_product "PLANT-003" "Alocasia Frydek" "alocasia-frydek" "Индивидуальное растение. Фото: placeholder." "$MONSTERA_ID" 89900 "MEDIUM" "BRIGHT" false
ensure_product "PLANT-004" "Syngonium Albo" "syngonium-albo" "Индивидуальное растение. Фото: placeholder." "$MONSTERA_ID" 159900 "EASY" "MEDIUM" false true
ensure_product "PLANT-005" "Scindapsus Treubii Moonlight" "scindapsus-treubii-moonlight" "Индивидуальное растение. Фото: placeholder." "$MONSTERA_ID" 74900 "EASY" "MEDIUM" false

ensure_variant "MON-001" "MON-001-12" "12 cm pot" "MEDIUM" "12cm" "POTTED" 139900 "ACTIVE"
ensure_variant "MON-002" "MON-002-12" "12 cm pot" "MEDIUM" "12cm" "POTTED" 359900 "ACTIVE"

ensure_individual_plant "PLANT-001" "IP-001" 12000 "MEDIUM" "MEDIUM" false
ensure_individual_plant "PLANT-002" "IP-002" 189900 "HARD" "MEDIUM" false
ensure_individual_plant "PLANT-003" "IP-003" 89900 "MEDIUM" "BRIGHT" false
ensure_individual_plant "PLANT-004" "IP-004" 159900 "EASY" "MEDIUM" false
ensure_individual_plant "PLANT-005" "IP-005" 74900 "EASY" "MEDIUM" false

ensure_stock_batch "MON-001" "MON-001-12" "Seed Supplier" 10 5000
ensure_available_stock "MON-001" "MON-001-12" 5 10

ensure_shipping_zone "Минск" "Минск" 500 10000 1 2 false 0
ensure_shipping_zone "Гомель (зима)" "Гомель" 700 15000 2 4 true 500

CATEGORY_COUNT="$(curl -sS "$API_URL/api/v1/categories" | jq 'length')"
PRODUCT_COUNT="$(curl -sS "$API_URL/api/v1/products?size=100" | jq '.totalElements')"

echo "Seed complete: categories=$CATEGORY_COUNT products=$PRODUCT_COUNT"
echo "Admin: $ADMIN_EMAIL / $ADMIN_PASSWORD"
echo "Customer: $CUSTOMER_EMAIL / $CUSTOMER_PASSWORD"
