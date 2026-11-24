# Development Parts — Lora Fashions, Version 1

Each part = one commit, testable on its own, followed by a stop for the owner's "Next".
Before coding a part, a full brief is posted (objective, requirements, scope, architecture,
patterns, DB changes, backend, frontend, security, validation, tests, acceptance criteria,
commit message). This file is the fixed scope; the brief adds the detail.

Legend — **Goal** · **Scope** · **Not in scope** · **Depends on** · **Done when**

---

## Phase 0 — Foundation

### Part 0.1 — Repository foundation
- **Goal:** A clean repository that anyone can start locally in under 10 minutes.
- **Scope:** Folder layout (`backend/`, `frontend/`, `infra/`, `docs/`); `README.md`; `.gitignore`, `.gitattributes`; `.env.example`; `infra/docker-compose.yml` with PostgreSQL 17, MongoDB 8, SeaweedFS S3 gateway (+ bucket init; replaced MinIO, ADR-0002), Mailpit; pre-commit config (gitleaks, formatters); `docs/architecture.md`, `docs/decisions/ADR-0001-modular-monolith.md`; `git init -b main`, add remote `origin` (https://github.com/IshanMihiranga/lora-fashion-saas.git), first commit and push after approval.
- **Not in scope:** Any application code.
- **Depends on:** —
- **Done when:** `docker compose up` shows all services healthy; gitleaks hook blocks a planted fake secret; README steps work from a clean clone.

### Part 0.2 — Backend bootstrap
- **Goal:** A running Spring Boot modular monolith skeleton.
- **Scope:** Maven project + wrapper, Java 21, pinned Spring Boot 4.x; dependencies from CLAUDE.md §4; profiles `local/dev/prod`; PostgreSQL + Flyway (baseline migration), MongoDB config; Actuator health (liveness/readiness only); JSON logging + request-ID filter; Spring Modulith `verify()` test; Testcontainers base test class; Dockerfile (layered, non-root user).
- **Not in scope:** Business modules.
- **Depends on:** 0.1
- **Done when:** App starts against Compose; `/actuator/health` UP; `./mvnw verify` green with the Modulith test; Docker image builds and runs.

### Part 0.3 — Shared kernel
- **Goal:** Common building blocks every module will use.
- **Scope:** `shared`: UUIDv7 generator, `Money` (BigDecimal, LKR), base auditable entity, `BusinessException` + error codes, global exception handler (RFC 9457 + `code` + field errors), paging request/response types with sort allow-list, `TenantContext` holder (empty for now), clock abstraction, springdoc config.
- **Not in scope:** Security.
- **Depends on:** 0.2
- **Done when:** Unit tests for `Money` and IDs; API tests prove error and paging formats; no stack traces in responses.

### Part 0.4 — Frontend bootstrap
- **Goal:** Two runnable React apps with shared foundations.
- **Scope:** npm workspaces; `apps/web`, `apps/admin` (Vite, React, JS); ESLint + Prettier; Tailwind + Radix base; `packages/ui` (Button, Input, Card, Spinner, EmptyState, ErrorState); `packages/api-client` (Axios factory, Problem Details error mapping); React Router, TanStack Query provider, route error boundaries, layout shells; Vite dev proxy to backend; Vitest + RTL + MSW setup; `vercel.json` with `/api` rewrite.
- **Not in scope:** Auth, real pages.
- **Depends on:** 0.3
- **Done when:** Both apps build and run; lint has zero warnings; sample component tests pass.

---

## Phase 1 — Identity and security

### Part 1.1 — Registration and email verification
- **Goal:** Users can register and verify their email, accepting the current legal documents.
- **Scope:** `identity` module: `users`, `email_verification_tokens`, `legal_documents`, `legal_acceptances` tables; Argon2id hashing; password policy + common-password list; registration API; verification token (hashed, single-use, 24 h); email via `notification` outbox stub → Mailpit; `UserRegistered` event; re-acceptance flag when a newer legal version exists.
- **Depends on:** 0.3
- **Done when:** Tests: duplicate email, weak password, token reuse, expired token, acceptance recorded with time/IP/version; no account-enumeration in responses.

### Part 1.2 — Login and session tokens
- **Goal:** Secure login with short access tokens and rotating refresh tokens.
- **Scope:** Login/refresh/logout endpoints; JWT signing (key from env); `refresh_tokens` with hash, family, rotation and reuse detection; `HttpOnly; Secure; SameSite=Strict` cookie; Spring Security stateless config; logout-all on password change.
- **Depends on:** 1.1
- **Done when:** Tests: valid login, wrong password, unverified email, rotation, reuse revokes family, expired tokens, logout.

### Part 1.3 — Password reset and abuse protection
- **Goal:** Safe password recovery and protection against brute force.
- **Scope:** Reset request/confirm (hashed token, 30 min); Bucket4j limits (login, register, reset); progressive login delay; security headers; CORS allow-list from config; refresh endpoint custom-header + Origin check.
- **Depends on:** 1.2
- **Done when:** Tests: rate limits return 429 with Problem Details; headers present; CORS rejects unknown origins; reset invalidates sessions.

### Part 1.4 — Roles, permissions, admin accounts
- **Goal:** Authorization framework used by all later modules.
- **Scope:** Role and permission enums (platform roles now; store roles wired in 2.1); permission evaluator; `@PreAuthorize` conventions; admin login endpoint with mandatory TOTP enrolment/verification; super admin seeded from env; `admin` module `audit_log` + `AuditService` API.
- **Depends on:** 1.3
- **Done when:** Role-access matrix tests; TOTP tests; audit entries written for logins and admin actions.

### Part 1.5 — Frontend authentication
- **Goal:** Users and admins can sign up, sign in and stay signed in.
- **Scope:** Web: register (with legal acceptance), verify email, login, forgot/reset password, profile basics; in-memory access token + silent refresh; protected routes; Admin app: login + TOTP enrolment.
- **Depends on:** 1.4, 0.4
- **Done when:** Component tests for forms and guards; Playwright: register → verify (Mailpit) → login → logout.

---

## Phase 2 — Stores and onboarding

### Part 2.1 — Stores and memberships
- **Goal:** A user can open a store and act within it.
- **Scope:** `tenancy`: `tenants`, `tenant_members`; unique slug rules; store roles (Owner/Manager/Staff) and permissions; `X-Store-Id` resolution filter → `TenantContext`; "my stores" API; `StoreCreated` event.
- **Depends on:** 1.4
- **Done when:** Tests: slug uniqueness/format, non-member gets 404, role permissions.

### Part 2.2 — Tenant isolation layers
- **Goal:** Make cross-store data access structurally impossible.
- **Scope:** Hibernate `@TenantId` base entity; transaction hook setting `app.tenant_id/app.user_id/app.scope`; separate app DB role; RLS policy template + applied to `tenant_members`; reusable isolation-test harness (store A vs B).
- **Depends on:** 2.1
- **Done when:** Isolation tests pass; a deliberately unscoped native query returns no foreign rows under RLS.

### Part 2.3 — Staff management
- **Goal:** Owners invite and manage staff with fixed roles.
- **Scope:** `staff_invitations` (hashed token, 7 days); invite, accept, change role, remove, leave; last-owner protection; audit; email notifications; plan staff limit hook (enforced in 3.1).
- **Depends on:** 2.2
- **Done when:** Role and isolation tests; invitation reuse/expiry tests.

### Part 2.4 — Files module
- **Goal:** Safe upload and delivery of images for all later features.
- **Scope:** `ObjectStorage` (S3 API: SeaweedFS locally / R2); `stored_files`; presigned PUT to `quarantine/` per purpose; confirm endpoint; validation chain (size → Tika signature → allowed type → dimension limit → re-encode → thumbnail); private presigned GET (10 min) after auth check; public path for public assets; cleanup job for abandoned uploads.
- **Depends on:** 2.2
- **Done when:** Tests: wrong signature, SVG rejected, oversized, decompression bomb, EXIF stripped, foreign file access 404.

### Part 2.5 — Storefront profile and public store page
- **Goal:** Each store has a branded public page.
- **Scope:** `storefront_profiles` (logo, banner, theme colours, about, contact, social links, shipping and return policy text); seller edit API; public API by slug (published stores only); web pages: storefront settings + `/store/:slug`.
- **Depends on:** 2.4
- **Done when:** API, validation, isolation and component tests.

### Part 2.6 — Seller PayHere account
- **Goal:** Stores connect their own PayHere merchant account securely.
- **Scope:** `seller_payment_accounts`; AES-256-GCM encryption with versioned key; masked display; owner-only access; status UNVERIFIED → VERIFIED (via sandbox test payment, completed in 5.4; manual admin verify for now); audit; `PaymentAccountVerified` event; onboarding guide text.
- **Depends on:** 2.2
- **Done when:** Tests: secret never returned or logged, only Owner can change, isolation, encryption round-trip, key-version handling.

### Part 2.7 — Seller dashboard shell
- **Goal:** Sellers have a home for all store management.
- **Scope:** Dashboard layout, store switcher, onboarding checklist (subscription, payment account, storefront, first product), staff pages, storefront settings, payment-account form.
- **Depends on:** 2.3, 2.5, 2.6
- **Done when:** Component tests; Playwright: create store → invite staff → edit storefront.

---

## Phase 3 — Subscriptions

### Part 3.1 — Plans and entitlements
- **Goal:** Plan limits enforced everywhere from one place.
- **Scope:** `plans`, `plan_limits`, `usage_counters`; seed Starter, Growth, Pro monthly + Growth Yearly with the prices and limits in CLAUDE.md §3; `EntitlementService` API (products, staff, storage, templates, analytics level, gallery fulfilment); admin plan CRUD API; wire staff limit.
- **Depends on:** 2.3
- **Done when:** Entitlement unit tests; limit-exceeded returns a clear error code.

### Part 3.2 — Subscribing with PayHere Recurring (sandbox)
- **Goal:** Stores pay for a plan and stay active automatically.
- **Scope:** `tenant_subscriptions`, `subscription_payments`; platform PayHere credentials from env; recurring checkout params + hash; notify endpoint with `md5sig` verification and idempotency; states PENDING_PAYMENT → ACTIVE → PAST_DUE (7-day grace) → SUSPENDED → CANCELLED/EXPIRED; scheduled checks; store gating (`SubscriptionStatusChanged`); emails.
- **Depends on:** 3.1
- **Done when:** Webhook suite (valid, invalid sig, duplicate, wrong amount), state-transition tests, gating tests.

### Part 3.3 — Plan changes and billing UI
- **Goal:** Sellers manage their subscription.
- **Scope:** Upgrade (immediate, new cycle, cancel old via Subscription Manager API), downgrade (next renewal, blocked if over limits), cancel; billing history; usage meters; seller UI pages.
- **Depends on:** 3.2
- **Done when:** Tests for each change path and over-limit downgrade; component tests.

---

## Phase 4 — Catalog and inventory

### Part 4.1 — Product types, zones, categories
- **Goal:** The shared vocabulary for products and customization.
- **Scope:** `product_types`, `print_zones` (mm sizes, 3D decal placement data), `platform_categories` (admin), `store_categories` (seller); seed T-shirt, hoodie, shirt, cap, bag with zones.
- **Depends on:** 3.1
- **Done when:** Admin and seller API tests; isolation tests for store categories.

### Part 4.2 — Products and variants
- **Goal:** Sellers list products with variants and customization options.
- **Scope:** `products`, `product_variants` (size, colour, SKU, price), `product_images`, `product_zone_options` (enabled zones + fee); shipping fee; customization flag; DRAFT/ACTIVE/ARCHIVED; slug; product limit from entitlements; publish requires active subscription.
- **Depends on:** 4.1, 2.4
- **Done when:** Validation, limit, isolation and optimistic-lock tests.

### Part 4.3 — Inventory
- **Goal:** Accurate stock per variant with full history.
- **Scope:** `inventory_items` (on hand, reserved, threshold, version), `inventory_movements` ledger; adjustments with reason; reservation API for checkout (used in 5.3); low-stock `StockLow` event + notification.
- **Depends on:** 4.2
- **Done when:** Concurrency test (parallel reservations never oversell); ledger consistency tests.

### Part 4.4 — Marketplace browse and search
- **Goal:** Customers find products across stores.
- **Scope:** Public APIs: list/search with Specifications (category, product type, price range, colour, size, store, text), sort allow-list, paging; product detail; compare (≤ 4); store catalog; only ACTIVE products of active, subscribed stores; `sitemap.xml`.
- **Depends on:** 4.3
- **Done when:** Query tests on fixtures; hidden products never returned; index review notes.

### Part 4.5 — Catalog UI
- **Goal:** Seller product management and customer catalog pages.
- **Scope:** Seller: product list/form, variants, images, zones, inventory screens. Customer: home, search/filter, product page, compare, store page.
- **Depends on:** 4.4
- **Done when:** Component tests; Playwright: seller adds product → customer finds it.

---

## Phase 5 — Cart, checkout, orders, payments

### Part 5.1 — Cart and wishlist
- **Scope:** `carts`, `cart_items`, `wishlist_items`; add/update/remove; server-side price/fee recalculation on every read; unavailable-item handling; customization reference placeholder (filled in 6.6).
- **Depends on:** 4.4
- **Done when:** Price-tampering tests; isolation tests.

### Part 5.2 — Seller coupons
- **Scope:** `coupons`, `coupon_redemptions`; percentage/fixed, dates, minimum spend, total and per-customer limits; validation API; seller UI.
- **Depends on:** 5.1
- **Done when:** Limit/expiry/concurrency tests.

### Part 5.3 — Checkout
- **Scope:** `checkout_sessions`, `orders`, `order_items`; checkout chain (store active & verified → stock → price → coupon); `OrderFactory` split per store with snapshots; stock reservation (20 min); `Idempotency-Key`; `OrderPlaced` event.
- **Depends on:** 5.2, 2.6
- **Done when:** Oversell, duplicate-submission and snapshot tests.

### Part 5.4 — Customer payments with seller PayHere
- **Scope:** `payments`, `payment_webhook_events`, `idempotency_keys`; payment start (hash with the store's secret); notify webhook per store (signature, amount, currency, idempotent event store); status mapping (2/0/−1/−2/−3); `OrderPaid`; reservation → deduction; expiry job; unconfirmed-payment flag job; return-page status polling API; sandbox verification for seller accounts.
- **Depends on:** 5.3
- **Done when:** Full payment workflow suite passes (see CLAUDE.md §14).

### Part 5.5 — Order lifecycle and management
- **Scope:** State machine (transition table + allowed roles); `order_status_history`; seller order list/detail/status updates; customer order list/detail/tracking; auto-complete 7 days after delivery; `OrderStatusChanged`, `OrderCompleted`.
- **Depends on:** 5.4
- **Done when:** Transition-by-role tests; optimistic-lock tests; RLS on orders verified.

### Part 5.6 — Cancellations and refund cases
- **Scope:** Customer cancel before CONFIRMED; seller cancel/reject with reason; `refund_cases`, `refund_evidence` (≤ 5 photos); accept/reject with note; record PayHere refund reference; escalate to admin; `RefundRecorded`.
- **Depends on:** 5.5
- **Done when:** Refund workflow and permission tests.

### Part 5.7 — Notifications
- **Scope:** `notifications`, `email_outbox`; Brevo adapter (Mailpit locally); retry with backoff; Thymeleaf templates; listeners for account, order, payment, refund, subscription, low stock; user email preferences; in-app list + unread count API.
- **Depends on:** 5.6
- **Done when:** Listener idempotency tests; retry tests with WireMock.

### Part 5.8 — Checkout and order UI
- **Scope:** Cart page, checkout (address, per-store totals and combined total, sequential PayHere payments), payment status page, customer orders, seller order management, refund-case screens, notification bell.
- **Depends on:** 5.7
- **Done when:** Playwright: two-store checkout with sandbox/mocked payments → seller ships → delivered.

---

## Phase 6 — Design studio

### Part 6.1 — Design documents (MongoDB)
- **Scope:** `designs`, `design_versions` (immutable), indexes; zone/layer schema validation; owner/tenant authorization; save, list, load, duplicate, delete (unpinned only); keep last 20 unpinned versions.
- **Depends on:** 4.1
- **Done when:** Repository and isolation tests; schema validation tests.

### Part 6.2 — 2D editor
- **Scope:** react-konva editor: product type and zone switcher, text (font, size, colour, outline, align), shapes, image layers, move/resize/rotate/crop, layer order/lock, undo/redo, autosave (30 s), version save/load.
- **Depends on:** 6.1
- **Done when:** Component tests for editor actions; save/load round-trip.

### Part 6.3 — Uploads in the editor
- **Scope:** Upload via files module; bounds check (blocks checkout), DPI warning (< 150 DPI), allowed types.
- **Depends on:** 6.2, 2.4
- **Done when:** Validation tests (client and server).

### Part 6.4 — Templates and sticker library
- **Scope:** `design_templates` (seller, locked/editable layers, plan-gated premium), `design_assets` (platform/store stickers, badges); seller template editor; customer template picker.
- **Depends on:** 6.3, 3.1
- **Done when:** Isolation, plan-gating and component tests.

### Part 6.5 — 3D preview
- **Scope:** R3F scene, GLB T-shirt and hoodie (licences recorded in `docs/assets.md`); colour tint; zone canvases as live decal textures; size selection; orbit + preset views; 2D fallback for other types; lazy loading.
- **Depends on:** 6.2
- **Done when:** Render smoke test; manual check on a mid-range phone (≥ 30 fps) reported.

### Part 6.6 — Customized items to order
- **Scope:** Zone PNG export (150 DPI) + upload; cart item with `designId + version`; zone fees; checkout ownership/licence check; pin on `OrderPlaced`; seller print-asset view (zone PNGs, originals, layer spec in mm).
- **Depends on:** 6.5, 5.8
- **Done when:** End-to-end design → cart → order → seller download test; pinned version cannot be deleted.

---

## Phase 7 — AI generation

### Part 7.1 — AI backend
- **Scope:** `ImageGenerationProvider` + Workers AI adapter (`flux-1-schnell`, 1024², 4 steps); prompt blocklist + moderation model; style wrapper; `GuardedImageGenerator` (5/day per customer + bonus credits, global daily cap, circuit breaker); results to storage; `ai_generations`.
- **Depends on:** 6.1, 2.4
- **Done when:** WireMock tests; quota and cap tests; blocked-prompt tests; key never exposed.

### Part 7.2 — AI panel UI
- **Scope:** Prompt input, generate, regenerate, refine, results grid, add to canvas, "remove white background" tool, quota display, friendly busy state.
- **Depends on:** 7.1
- **Done when:** Component tests with MSW.

---

## Phase 8 — Gallery and rewards

### Part 8.1 — Publishing and moderation
- **Scope:** `gallery_listings`, `listing_reviews`, `content_reports`; publish request with licence + rights confirmation; moderator queue approve/reject; reports; takedown; `GalleryListingApproved/Removed`; title/thumbnail copy refresh.
- **Depends on:** 6.1, 1.4
- **Done when:** Moderation state tests; private designs never exposed.

### Part 8.2 — Public gallery and ordering
- **Scope:** Gallery browse/search, design page, eligible-store selection (opt-in, active, verified, matching type and zones, plan allows), apply design to the chosen store's product → checkout.
- **Depends on:** 8.1, 6.6
- **Done when:** Eligibility tests; Playwright gallery order.

### Part 8.3 — Sharing and attribution
- **Scope:** `share_links` (`/d/<code>`), QR generation, share buttons (WhatsApp, Facebook, copy, Web Share), attribution carried cart → order item.
- **Depends on:** 8.2
- **Done when:** Attribution tests.

### Part 8.4 — Creator points
- **Scope:** `reward_ledger`, `reward_redemptions`; PENDING on paid item, AVAILABLE on `OrderCompleted`, reversal on `RefundRecorded`; abuse rules; redeem 50 points → 5 AI credits; history UI; creator badge.
- **Depends on:** 8.3, 7.1
- **Done when:** Abuse tests (self-purchase, repeat buyer, daily cap, refund reversal, duplicate events).

---

## Phase 9 — Communication

### Part 9.1 — Conversations (REST)
- **Scope:** `conversations`, `messages`, `conversation_reads`; customer ↔ store, optional product/order link; attachments via files; read status; RLS; rate limit; design-change-request message type.
- **Depends on:** 5.5, 2.4
- **Done when:** Access and isolation tests.

### Part 9.2 — Real-time chat
- **Scope:** WebSocket/STOMP simple broker; one-time ticket; subscription guard interceptor; push of messages and notifications; polling fallback; chat UI for customer and seller; email for new messages (max 1/hour per conversation).
- **Depends on:** 9.1
- **Done when:** Socket auth tests; component tests.

---

## Phase 10 — Reviews and analytics

### Part 10.1 — Reviews
- **Scope:** `reviews` (verified purchase, one per order item, after DELIVERED); product and store ratings; report + moderation; UI.
- **Depends on:** 5.5
- **Done when:** Eligibility and moderation tests.

### Part 10.2 — Seller analytics
- **Scope:** Read-only views; sales by period, orders by status, top products, low stock, customers; plan-gated detail; dashboard UI with charts.
- **Depends on:** 10.1, 3.1
- **Done when:** Query tests on fixtures; isolation tests.

---

## Phase 11 — Administration

### Part 11.1 — Admin core
- **Scope:** Admin app: users (view, lock), stores (view, suspend), plans, platform categories, product types/zones, PayHere account manual verification.
- **Depends on:** 1.5, 3.1, 4.1
- **Done when:** Admin role tests; every action audited.

### Part 11.2 — Moderation, complaints, reports
- **Scope:** Gallery and review moderation screens, complaints, refund escalations, audit-log viewer, platform settings, revenue/subscription and AI-usage reports.
- **Depends on:** 11.1, 8.1, 5.6
- **Done when:** Role and audit tests; component tests.

---

## Phase 12 — Hardening and release readiness

### Part 12.1 — Security review
- **Scope:** OWASP ASVS L2 checklist report; Dependency-Check + `npm audit`; CSP tightening; full isolation re-test; fixes.
- **Done when:** Checklist report in `docs/`; no high/critical findings open.

### Part 12.2 — Privacy features
- **Scope:** Data export (JSON), account deletion/anonymisation (keep legally required order data), retention jobs, Terms/Privacy pages and versioning UI.
- **Done when:** Deletion and export tests.

### Part 12.3 — Performance and end-to-end
- **Scope:** Complete Playwright suite; query/index review; k6 smoke (20 users) on catalog, cart, checkout.
- **Done when:** Reports in `docs/`; targets from plan §3 met or documented.

### Part 12.4 — Deployment and operations
- **Scope:** Render, Neon, Atlas, R2, Brevo, Workers AI, Vercel set-up (limits confirmed); `render.yaml`; backup job + restore test; `docs/deployment.md`, `docs/runbook.md`, production upgrade checklist.
- **Done when:** Live sandbox run-through of the main flows; restore test passed.

---

## Phase 13 — After Version 1

### Part 13.1 — Courier integration
- Planned separately after Version 1 is complete.
