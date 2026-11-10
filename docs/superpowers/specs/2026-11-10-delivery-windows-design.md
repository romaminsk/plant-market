# Delivery windows — design

- **Date:** 2026-11-10
- **Status:** Accepted (autonomous owner mode)
- **Scope:** order-service checkout + storefront checkout form

## Intent

Checkout currently collects a zone and a free-text address, but the customer cannot say when the
plants should arrive. The roadmap asks for delivery slots; this iteration adds a concrete delivery
date and time window, validated against the zone lead time.

## Goals

- `CheckoutRequest`/`GuestCheckoutRequest` accept `deliveryDate` and `deliveryWindow`.
- Order stores both and returns them in the API response.
- Date must fall between `today + zone.minDays` and `today + zone.maxDays` (UTC).
- Storefront shows a date input and a window select driven by the selected zone.
- Existing clients that omit the fields get a deterministic default (earliest date, `DAY`).

## Non-goals

- Per-window capacity, courier routing or an admin slot calendar.
- Changing shipping price or the thermal-pack rule.
- Time zones other than UTC for the date boundary.

## Approach

Add `delivery_date date` and `delivery_window varchar(20)` to `orders` (V9). The window enum is
`MORNING`, `DAY`, `EVENING`. `OrderService.checkoutOrder` validates an explicit date/window against
the zone range; when both are omitted it uses `today + minDays` and `DAY`, so existing API callers
keep working. The storefront checkout form computes `min`/`max` from the selected zone, defaults to
the earliest date and `DAY`, and sends both through the BFF.

## Consequences

- No capacity limit yet: any number of orders can choose the same window. A `delivery_slots` table
  with capacity can replace the enum later without changing the public date/window shape.
- Orders created by old clients get a valid default instead of failing.
- The date boundary is UTC; a future change can make it zone-aware.
