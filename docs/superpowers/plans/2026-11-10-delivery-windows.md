# Delivery Windows Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let customers choose a validated delivery date and time window during checkout.

**Architecture:** Add a date and a `DeliveryWindow` enum to the order, validate explicit values against the shipping zone's `minDays`/`maxDays`, and default omitted values for backward compatibility. The storefront sends both through the BFF.

**Tech Stack:** Spring Boot/JPA/PostgreSQL, Next.js 16, Playwright Java.

**Spec:** `docs/superpowers/specs/2026-11-10-delivery-windows-design.md`

## Global Constraints

- Money remains in minor units; no shipping-price changes.
- Existing checkout callers that omit date/window must keep working.
- Dates are validated in UTC.
- Storefront must not send a date outside the selected zone's range.

## Review Focus

- Explicit date before `today + minDays` or after `today + maxDays` returns 400.
- Omitted date/window gets `today + minDays` and `DAY`.
- Guest checkout and authenticated checkout behave the same.
- Changing the zone updates the date input bounds and does not keep an invalid date.
- Order responses expose the chosen date/window.

---

### Task 1: Backend delivery date and window

**Files:**

- Create: `services/order-service/src/main/resources/db/migration/V9__add_delivery_window.sql`
- Create: `services/order-service/src/main/java/com/plantmarket/order/domain/DeliveryWindow.java`
- Modify: `services/order-service/src/main/java/com/plantmarket/order/domain/OrderEntity.java`
- Modify: `services/order-service/src/main/java/com/plantmarket/order/web/dto/CheckoutRequest.java`
- Modify: `services/order-service/src/main/java/com/plantmarket/order/web/dto/GuestCheckoutRequest.java`
- Modify: `services/order-service/src/main/java/com/plantmarket/order/web/dto/OrderResponse.java`
- Modify: `services/order-service/src/main/java/com/plantmarket/order/service/OrderService.java`
- Test: `services/order-service/src/test/java/com/plantmarket/order/OrderFlowIntegrationTest.java`

**Interfaces:**

- Produces: `enum DeliveryWindow { MORNING, DAY, EVENING }`.
- Produces: `CheckoutRequest(UUID shippingZoneId, Boolean thermalPack, String deliveryAddress, LocalDate deliveryDate, DeliveryWindow deliveryWindow)`.
- Produces: `OrderResponse` fields `deliveryDate: LocalDate`, `deliveryWindow: DeliveryWindow`.
- Consumes: `ShippingZoneEntity.getMinDays()/getMaxDays()`.

- [ ] **Step 1: Write the failing integration test**

Add to `OrderFlowIntegrationTest`:

```java
@Test
void checkoutStoresDeliveryWindowAndRejectsOutOfRangeDate() {
  UUID customerId = UUID.randomUUID();
  HttpHeaders headers = headers(customerId);
  ShippingZoneResponse zone = createZone(headers, 2, 5);
  UUID orderId = createVariantOrder(headers);

  CheckoutRequest valid =
      new CheckoutRequest(zone.id(), false, "Minsk, Nezavisimosti 1",
          LocalDate.now(ZoneOffset.UTC).plusDays(3), DeliveryWindow.EVENING);
  ResponseEntity<OrderResponse> checkedOut =
      restTemplate.exchange("/api/v1/orders/" + orderId + "/checkout",
          HttpMethod.POST, new HttpEntity<>(valid, headers), OrderResponse.class);
  assertThat(checkedOut.getStatusCode()).isEqualTo(HttpStatus.OK);
  assertThat(checkedOut.getBody().deliveryDate())
      .isEqualTo(LocalDate.now(ZoneOffset.UTC).plusDays(3));
  assertThat(checkedOut.getBody().deliveryWindow()).isEqualTo(DeliveryWindow.EVENING);

  UUID secondId = createVariantOrder(headers);
  CheckoutRequest tooEarly =
      new CheckoutRequest(zone.id(), false, "Minsk, Nezavisimosti 2",
          LocalDate.now(ZoneOffset.UTC).plusDays(1), DeliveryWindow.DAY);
  ResponseEntity<String> rejected =
      restTemplate.exchange("/api/v1/orders/" + secondId + "/checkout",
          HttpMethod.POST, new HttpEntity<>(tooEarly, headers), String.class);
  assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}
```

Add a helper that creates a zone with explicit min/max days, or reuse the existing zone fixture.

- [ ] **Step 2: Run the test and verify it fails**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./gradlew --no-daemon :services:order-service:test --tests '*OrderFlowIntegrationTest'`
Expected: FAIL because the request record has no date/window fields.

- [ ] **Step 3: Implement the backend**

Migration:

```sql
ALTER TABLE orders ADD COLUMN delivery_date date;
ALTER TABLE orders ADD COLUMN delivery_window varchar(20);
```

`OrderEntity.applyShipping` accepts and stores both; getters expose them. `OrderService.checkoutOrder`:

```java
LocalDate date = request.deliveryDate();
DeliveryWindow window = request.deliveryWindow();
if (date == null && window == null) {
  date = LocalDate.now(ZoneOffset.UTC).plusDays(zone.getMinDays());
  window = DeliveryWindow.DAY;
}
if (date == null || window == null) {
  throw ApiException.badRequest("deliveryDate and deliveryWindow are required together");
}
LocalDate earliest = LocalDate.now(ZoneOffset.UTC).plusDays(zone.getMinDays());
LocalDate latest = LocalDate.now(ZoneOffset.UTC).plusDays(zone.getMaxDays());
if (date.isBefore(earliest) || date.isAfter(latest)) {
  throw ApiException.badRequest("deliveryDate is outside the zone window");
}
order.applyShipping(zone.getId(), shipping, thermal, address, date, window);
```

Guest checkout forwards both fields. `OrderResponse`/`toResponse` include them.

- [ ] **Step 4: Run the order test and verify it passes**

Run the same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add services/order-service
git commit -m "feat(orders): add validated delivery date and window"
```

---

### Task 2: Storefront checkout date/window

**Files:**

- Modify: `clients/storefront/src/lib/api.ts`
- Modify: `clients/storefront/src/app/checkout/page.tsx`
- Modify: `clients/storefront/src/app/api/checkout/route.ts`
- Test: `tests/e2e/src/test/java/com/plantmarket/e2e/CheckoutFlowE2ETest.java`

**Interfaces:**

- Consumes: `DeliveryWindow` string union and `CheckoutRequest` fields from Task 1.
- Produces: `deliveryDate`/`deliveryWindow` in the BFF checkout body.

- [ ] **Step 1: Extend the E2E test**

In `CheckoutFlowE2ETest`, after selecting the zone, select the first enabled delivery date and
`EVENING`, submit, and assert the order is paid. If the date input is empty, fill it with the
input's `min` value.

- [ ] **Step 2: Run it and verify it fails**

Run the checkout E2E test. Expected: FAIL because the form has no date/window controls.

- [ ] **Step 3: Implement the form**

Add `DeliveryWindow` type, `deliveryDate`/`deliveryWindow` to the checkout payload, a date input with
`min`/`max` derived from the selected zone (`today + minDays`/`maxDays`), and a window select with
`MORNING`/`DAY`/`EVENING` labels. Default to the earliest date and `DAY`; reset an invalid date when
the zone changes. Pass both fields through `POST /api/checkout`.

- [ ] **Step 4: Run the checkout E2E and verify it passes**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clients/storefront tests/e2e
git commit -m "feat(storefront): choose delivery date and window"
```

---

### Task 3: Docs and full verification

**Files:**

- Modify: `contracts/openapi/order-service.yaml`
- Modify: `docs/status.md`
- Modify: `docs/technical-guide.html`
- Create: `docs/adr/0016-delivery-windows.md`

- [ ] **Step 1: Document the contract and limitations**

Add `deliveryDate`/`deliveryWindow` to checkout/order schemas, note the default and UTC range, and
record that capacity slots are still future work. Update status/guide.

- [ ] **Step 2: Full build and E2E**

Run `./gradlew build`, rebuild the demo stack, reset data, and run the full E2E suite. Expected:
all green.

- [ ] **Step 3: Commit**

```bash
git add contracts docs
git commit -m "docs: document delivery windows"
```
