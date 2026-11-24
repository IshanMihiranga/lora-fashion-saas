# CLAUDE.md — Lora Fashions: Fashion Customization & Multi-Tenant Commerce SaaS

> Primary instruction file for every AI coding session on this repository.
> Read it fully before writing code. If anything here conflicts with a request,
> stop and ask the project owner. The approved plan is the source of truth:
> "Fashion Customization SaaS — V1 Architecture & Development Plan" (approved 2026-11-24).
> Detailed part list: `docs/DEVELOPMENT_PARTS.md`.

| Item | Value |
| --- | --- |
| Brand | **Lora Fashions** |
| Repository | `https://github.com/IshanMihiranga/lora-fashion-saas.git` (branch `main`) |
| Local folder | `lora-fashion-saas/` |
| Java base package | `lk.lorafashions` |
| Maven coordinates | `groupId lk.lorafashions`, `artifactId lora-fashion-backend` |
| npm scope | `@lora/web`, `@lora/admin`, `@lora/ui`, `@lora/api-client` |
| Docker Compose project | `lora` |
| Database / bucket names (local) | PostgreSQL `lora`, MongoDB `lora`, S3 bucket `lora-local` (SeaweedFS, ADR-0002) |

---

## 1. Project overview

Lora Fashions is an AI-assisted fashion customization platform and multi-tenant e-commerce SaaS for
small and medium clothing / print businesses, first in Sri Lanka.

- **Sellers** subscribe to a paid plan, open a store, list products, receive orders,
  print and deliver themselves (outside the platform), and chat with customers.
- **Customers** browse a marketplace and store pages, customize products in a 2D
  editor with a live 3D preview and AI image generation, and pay each seller online.
- **Platform** earns only from seller subscriptions. No commission, no order money held,
  no tax calculation.

Version 1 runs on a zero-cost cloud stack with **sandbox** payments only, built so it
can move to production later.

---

## 2. Non-negotiable rules

1. **Modular monolith.** One Spring Boot application, one deployable backend.
   No microservices, no separate Spring Boot apps per module, no inter-service REST,
   no API gateway, no service discovery, no Kafka, no saga, no distributed transactions.
2. **No Redis in Version 1.** (Planned for Version 2.)
3. **No code outside an approved part.** Work only on the current part in
   `docs/DEVELOPMENT_PARTS.md`. Never start the next part without the owner saying
   "Next" or "Continue to the next part".
4. **Never trust the client** for tenant IDs, roles, prices, totals, discounts,
   payment status or permissions. The server recomputes and authorizes everything.
5. **No secrets in Git.** Configuration comes from environment variables only.
6. **No placeholders reported as done.** Unfinished work is reported as unfinished.
7. **Tests must pass** before a part is reported complete.
8. **Do not change the approved architecture silently.** Propose the change, explain
   why, get approval, and record an ADR in `docs/decisions/`.
9. **Do not add technologies** not listed in section 4 without approval.
10. **Do not over-engineer.** Use a pattern only for a real, current problem.

---

## 3. Finalized Version 1 scope

### In scope
| Area | Version 1 |
| --- | --- |
| Selling | Seller subscriptions: **3 monthly tiers + 1 yearly plan**, no free tier; store, storefront, products, variants, inventory, seller coupons |
| Buying | Marketplace, search, filters, compare (max 4), cart, wishlist, multi-seller checkout, online PayHere payment only |
| Orders | Seller updates status manually; customer cancellation before seller confirms; refund cases with evidence; escalation to admin |
| Design | 2D editor, uploads, seller templates, stickers/badges, 3D preview (T-shirt, hoodie), customer AI image generation |
| Community | Moderated public gallery, share links + QR + share buttons, creator points |
| Communication | Real-time customer–store chat; email + in-app notifications |
| Platform | Separate admin app, moderation, complaints, audit log, plans, reports |
| Legal | Versioned Terms + Privacy acceptance at registration and on change; data export; account deletion (PDPA No. 9 of 2022 basics) |

### Out of scope for Version 1 (do not build)
Redis · courier APIs and tracking numbers (planned at the very end) · cash on delivery ·
SMS / WhatsApp API · Google / social login · custom staff roles · store subdomains and
custom domains (V1 uses `/store/<slug>`) · seller AI business assistant · cash or
subscription-discount rewards · automated refunds via PayHere API · ClamAV · CI/CD ·
Next.js / SSR · TypeScript · languages other than English · currencies other than LKR.

### Business rules fixed by the owner
- Each seller connects **their own PayHere account** (Merchant ID + Merchant Secret).
  Customer money goes directly to the seller. A store cannot take orders until its
  PayHere account is **verified** and its subscription is **active**.
- Sellers do not need a business registration to join the platform (PayHere itself
  may require one; checkout simply stays disabled until the account is verified).
- A multi-seller cart is split into **one order per store**, each paid with **its own
  PayHere payment**. The combined total is shown, then each store is paid in turn.
- Prices entered by sellers are final (tax, discounts and delivery are their concern);
  each product/store has its own shipping fee shown to the customer.
- Printing and delivery are done by sellers outside the platform.
- Refunds: the seller decides with evidence. Seller fault → refund owed; customer fault
  → no refund. The seller refunds in the PayHere dashboard and records the reference.
  The platform never moves money.
- Gallery publishing always requires **platform moderation**.
- AI generation is **customer-only**; preview quality matters more than print quality.
- The 2D design is what the seller receives; 3D is preview only.

### Subscription plans (decided — seeded by migration, editable later by admins)
Every plan includes the core features: store, storefront, products, inventory, orders,
online payments, refund cases, chat, notifications, coupons and gallery browsing.

| Code | Name | Billing | Price (LKR) | Products | Staff (besides owner) | Storage | Extras |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `STARTER_M` | Starter | Monthly | 2,490 | 25 | 1 | 500 MB | Basic sales summary |
| `GROWTH_M` | Growth | Monthly | 4,990 | 200 | 3 | 2 GB | Store templates (10), standard analytics, gallery fulfilment opt-in |
| `PRO_M` | Pro | Monthly | 9,990 | 1,000 | 10 | 5 GB | Unlimited templates, premium template library, advanced analytics, featured placement in store listings |
| `GROWTH_Y` | Growth Yearly | Yearly | 49,900 | Same as Growth | Same as Growth | Same as Growth | Two months free compared with monthly |

The one yearly plan is **Growth Yearly**: Growth is the tier most small and medium
sellers are expected to choose, and a yearly option there keeps them committed. Prices are
starting values for sandbox testing and can be changed in the admin app.

### Other decided values
| Item | Value |
| --- | --- |
| Reward rates | 10 points per qualifying order item; 50 points = 5 extra AI generations; 7-day hold after delivery |
| Customer cancellation | Allowed until the seller confirms the order |
| Refund case window | Up to 7 days after delivery |
| Plan changes | Upgrade: immediate, new billing cycle, no proration. Downgrade: next renewal, blocked while usage exceeds the lower plan |
| Past-due grace period | 7 days, then the store is suspended (existing orders can still be fulfilled) |
| Stock reservation at checkout | 20 minutes |
| AI generation quota | 5 per customer per day + bonus credits; global daily cap 150 (config) |
| Upload limits | PNG, JPEG, WebP; max 10 MB; max 5 refund-evidence photos |
| Order number format | `LF-<year>-<6-digit sequence>`, e.g. `LF-2026-000123` |

---

## 4. Approved technology stack

### Backend
- **Java 21 (LTS)**, **Spring Boot 4.x** (latest stable, pinned in Part 0.2), Maven wrapper.
- Spring Web, Spring Security, Spring Validation, Spring Data JPA (Hibernate),
  Spring Data MongoDB, Spring WebSocket (STOMP, in-memory simple broker).
- **Spring Modulith** — module verification test + event publication registry (outbox).
- Flyway (PostgreSQL migrations), MapStruct, springdoc-openapi, Resilience4j,
  Bucket4j (in-memory), AWS SDK v2 S3 client, Apache Tika, Thymeleaf (email templates),
  Logback JSON encoder.
- Tests: JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL, MongoDB), WireMock,
  Spring Security Test, Spring Modulith test, JaCoCo.

### Frontend
- **JavaScript** (no TypeScript), React (latest stable), Vite, npm workspaces.
- React Router, TanStack Query, Zustand (editor state), React Hook Form + Zod,
  Axios, Tailwind CSS + Radix UI, react-konva (2D), React Three Fiber + drei (3D), `qrcode`.
- Tests: Vitest, React Testing Library, MSW, Playwright.
- ESLint (strict) + Prettier; JSDoc on the API layer.

### Data and services
| Purpose | Local | Dev (zero cost) | Prod (later, paid) |
| --- | --- | --- | --- |
| Relational DB | Docker `postgres:17` | Neon free | Neon paid + PITR |
| Document DB | Docker `mongo:8` | MongoDB Atlas free | Atlas paid + backups |
| Object storage | SeaweedFS S3 gateway (ADR-0002) | Cloudflare R2 free | R2 |
| Email | Mailpit | Brevo free | Brevo paid |
| AI images | WireMock stub / Workers AI | Cloudflare Workers AI `flux-1-schnell` | Workers AI paid or other provider |
| Payments | PayHere sandbox + cloudflared tunnel | PayHere sandbox | PayHere live |
| Backend host | local JVM | Render free web service (Docker) | Always-on instance |
| Frontend host | Vite dev server | Vercel Hobby (dev only, non-commercial) | Vercel Pro or Cloudflare Pages |

---

## 5. Architecture — modular monolith

### Modules (top-level packages under `lk.lorafashions`)
| Module | Owns | Store |
| --- | --- | --- |
| `shared` | IDs (UUIDv7), `Money`, errors, paging, `TenantContext`, security helpers | — |
| `identity` | Users, credentials, refresh/verification/reset tokens, legal acceptance, admin TOTP | PostgreSQL |
| `tenancy` | Stores (tenants), memberships, fixed roles, staff invitations | PostgreSQL |
| `storefront` | Store branding, policies, public store data | PostgreSQL |
| `subscription` | Plans, limits, entitlements, seller subscriptions, billing records, usage | PostgreSQL |
| `catalog` | Product types, print zones, categories, products, variants, images | PostgreSQL |
| `inventory` | Stock levels, movements, reservations | PostgreSQL |
| `cart` | Carts, cart items, wishlists | PostgreSQL |
| `promotion` | Seller coupons, redemptions | PostgreSQL |
| `ordering` | Checkout sessions, orders, items, status history, refund cases | PostgreSQL |
| `payment` | Seller PayHere accounts, payments, webhook events, idempotency keys | PostgreSQL |
| `design` | Designs, immutable versions, templates, sticker library | MongoDB |
| `ai` | AI generations, provider adapter, quotas | MongoDB |
| `gallery` | Listings, moderation, licences, share links, reports | PostgreSQL |
| `rewards` | Points ledger, redemptions | PostgreSQL |
| `messaging` | Conversations, messages, read markers, WebSocket endpoint | PostgreSQL |
| `notification` | In-app notifications, email outbox, templates | PostgreSQL |
| `review` | Product and store reviews | PostgreSQL |
| `files` | Object storage adapter, upload validation, file records | PostgreSQL + object storage |
| `analytics` | Read-only reports | Reads PostgreSQL views |
| `admin` | Platform settings, complaints, audit log | PostgreSQL |

### Module layout (same in every module)
```
<module>/
  api/             PUBLIC: <Module>Api facade interface, DTOs, events (@NamedInterface)
  domain/          entities, value objects, domain services, state rules (internal)
  application/     use-case services, @Transactional, @PreAuthorize (internal)
  infrastructure/  repositories, external adapters (internal)
  web/             controllers, request/response DTOs, web mappers (internal)
```

### Dependency rules
1. Every module may depend on `shared`; `shared` depends on no module.
2. A module uses another module **only through its `api` package**. Never import
   another module's `domain`, `application`, `infrastructure` or `web` classes.
3. Synchronous calls go downstream only (e.g. `ordering` → `catalog`, `inventory`,
   `promotion`, `payment`). Upstream reactions use **domain events**.
4. No circular dependencies. `ApplicationModules.of(Application.class).verify()` runs
   in the test suite and must pass.
5. A module never reads or writes another module's tables or collections.
   Cross-module reporting uses read-only SQL views owned by the source module.
6. Entities never leave their module. Other modules receive DTOs/records.
7. Event listeners use `@ApplicationModuleListener` (after commit, async, persisted in
   the event publication table) and **must be idempotent**.

### Core events (extend via `docs/event-catalogue.md`)
`UserRegistered`, `StoreCreated`, `SubscriptionStatusChanged`, `PaymentAccountVerified`,
`OrderPlaced`, `OrderPaid`, `OrderStatusChanged`, `OrderCompleted`, `OrderCancelled`,
`RefundRecorded`, `GalleryListingApproved`, `GalleryListingRemoved`, `MessageSent`,
`StockLow`.

---

## 6. Database responsibilities

### Ownership
- **PostgreSQL** owns every fact involving money, stock, status, permissions or identity.
- **MongoDB** owns only design content (designs, immutable versions, templates,
  sticker/badge library) and AI generation records.
- **Object storage** owns all binary files. Databases store only object keys.
- Never duplicate business data across databases. The only allowed copy: a gallery
  listing stores the design title and thumbnail key for browsing, refreshed by event.

### PostgreSQL conventions
- UUIDv7 primary keys (`id uuid`). Human order numbers (`LF-2026-000123`) for display.
- `tenant_id uuid not null` on every store-owned table.
- Audit columns: `created_at`, `updated_at` (timestamptz, UTC), `created_by`.
- `version bigint` (optimistic lock) on orders, inventory items, subscriptions, coupons, products.
- Money: `numeric(12,2)` + `currency char(3)` (always `LKR` in V1). Java uses `Money`
  (BigDecimal) — never `double`/`float`.
- Foreign keys only inside a module; cross-module references are plain UUID columns.
- Index every foreign key; composite `(tenant_id, status, created_at)` for list screens.
- Check constraints for non-negative stock and amounts; unique constraints for slugs, SKUs per tenant, coupon codes per tenant.
- Soft delete by status (`ARCHIVED`), not row deletion, for products, stores, users.
- Orders snapshot product title, variant, unit price, fees, discount, address and
  design reference (`design_id`, `design_version`).
- Migrations: Flyway, `backend/src/main/resources/db/migration/`,
  name `V<yyyyMMddHHmm>__<module>_<description>.sql`. Never edit an applied migration.

### MongoDB conventions
- Collections: `designs`, `design_versions`, `design_templates`, `design_assets`, `ai_generations`.
- Every document has `ownerUserId`; store-owned documents also have `tenantId`.
- `design_versions` are **immutable**; `(designId, version)` unique. Versions referenced
  by an order are **pinned** and never deleted.
- Indexes are created on startup by code (`IndexOperations`), listed per collection.
- Repository methods always take the caller identity and add it to the query.
  Never build queries from raw user strings; reject `$`-prefixed or dotted keys in free-form maps.

### Cross-database consistency (no 2-phase commit)
1. Save the design version in MongoDB first (immutable).
2. Checkout verifies the version exists and the buyer may use it, then commits the order in PostgreSQL.
3. `OrderPlaced` → listener pins the version (idempotent, retried from the event table).
4. Gallery visibility source of truth = `gallery_listings.status` (PostgreSQL).
5. Nightly reconciliation job reports and fixes orphans.

---

## 7. Multi-tenancy

- Tenant = store. Shared PostgreSQL database and schema, `tenant_id` column.
- **Layer 1 — resolution:** seller APIs require `X-Store-Id`; a filter loads the caller's
  membership (404 if none) and sets `TenantContext` (tenant + role). Public storefront
  APIs resolve the tenant from the URL slug and expose published data only.
  Tenant IDs in request bodies are ignored.
- **Layer 2 — query scoping:** store-owned entities use Hibernate `@TenantId`.
  Cross-store reads (marketplace, admin) use separate read-only repositories with public fields only.
- **Layer 3 — PostgreSQL RLS** on `orders`, `order_items`, `refund_cases`, `payments`,
  `seller_payment_accounts`, `tenant_members`, `conversations`, `messages`, `coupons`.
  The app sets `app.tenant_id`, `app.user_id`, `app.scope` via `set_config(..., true)` per
  transaction; the app DB role is not the table owner; tables use `FORCE ROW LEVEL SECURITY`.
- **Layer 4 — tests:** every seller endpoint has a store-A-vs-store-B test expecting 404;
  customer resources and MongoDB designs have equivalent tests.

---

## 8. Security rules (apply in every part)

- **Passwords:** Argon2id; minimum 12 characters; common-password check.
- **Tokens:** access JWT 15 min (frontend memory only); refresh token random, stored as
  SHA-256 hash, rotated on every use, reuse → revoke family; cookie
  `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`.
- **Same-origin API:** Vercel rewrites `/api/*` to the backend. WebSocket uses a
  one-time 60-second ticket.
- **Admins:** separate app and login, mandatory TOTP, 8-hour max session, all actions audited.
  First super admin is seeded from environment variables.
- **Authorization:** deny by default; `@PreAuthorize` on every application-service method;
  custom permission evaluator reads `TenantContext`. Foreign or missing resources → 404.
- **Fixed roles:** Customer · Store Owner · Store Manager · Store Staff · Moderator · Admin · Super Admin
  (permission matrix in the plan, section 9; implement as an enum-based mapping).
- **Rate limits** (Bucket4j): login, register, password reset, AI generation, uploads,
  chat send, checkout, webhooks.
- **Input:** Bean Validation on every request DTO; size limits on strings/collections;
  JPA parameters/Specifications only (no string-built SQL).
- **XSS:** React escaping; `dangerouslySetInnerHTML` only with DOMPurify; strict CSP;
  **no SVG uploads**.
- **CSRF:** API uses bearer header; refresh endpoint needs custom header + Origin check.
- **CORS:** explicit allow-list per environment; never `*` with credentials.
- **Files:** presigned upload (5 min, fixed type and max size) to `quarantine/`; server
  validates signature (Tika), type per purpose, dimensions, then **re-encodes** images
  and moves them to the final key. Private files via 10-minute presigned GET after an
  authorization check. Images only in V1 (PNG, JPEG, WebP ≤ 10 MB).
- **Payments:** merchant secrets encrypted AES-256-GCM with a versioned key from env;
  masked in UI; changes audited. Order is Paid **only** after a PayHere notification with a
  valid `md5sig`, matching amount and currency, and an unseen payment ID. The return
  page never marks anything paid. No card data touches our servers.
- **Rewards:** immutable ledger entries; award only on `COMPLETED`; reverse on refund;
  self-purchase excluded; one award per buyer per design per 30 days; 200 points/day cap.
- **Secrets:** env vars only; `.env.example` with fake values; gitleaks pre-commit hook.
- **Headers:** HSTS, `X-Content-Type-Options: nosniff`, `frame-ancestors 'none'`,
  `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`.
- **Logging:** never log passwords, tokens, secrets, full addresses or card data.
  Audit log for logins, role/staff changes, payment-account changes, refunds, moderation, admin actions.
- **Errors:** no stack traces, SQL or internal class names in responses.
- **Dependencies:** Dependabot alerts on; OWASP Dependency-Check + `npm audit` before each phase tag.

---

## 9. Design pattern guidelines

Use these where listed; anything else needs a stated reason in the part report.

| Pattern | Use for |
| --- | --- |
| Facade | Each module's `api` interface (`CatalogApi`, `InventoryApi`, …) |
| Repository / Service layer / DTO / Mapper (MapStruct) | Every module |
| Domain events + transactional outbox | Cross-module reactions (Spring Modulith) |
| State | Order, subscription, refund case, gallery listing lifecycles (transition table + allowed roles) |
| Adapter / Anti-corruption layer | PayHere, Workers AI, Brevo, R2/SeaweedFS (S3 API) |
| Strategy | `ImageGenerationProvider`, `ObjectStorage` implementations |
| Factory | `OrderFactory` (cart → per-store orders with snapshots) |
| Specification | Marketplace product search filters |
| Chain of Responsibility | Upload validation chain; checkout checks |
| Decorator | `GuardedImageGenerator` (quota + circuit breaker around the provider) |
| Builder | Test fixtures, complex value objects |
| Idempotency / Retry / Circuit breaker | Webhooks, payments, AI, email, storage |
| Optimistic locking | Orders, inventory, subscriptions, coupons, products |

Not used in V1: CQRS (beyond read-only analytics queries), Saga, distributed
transactions, Template Method (unless a real need appears).

Principles: SOLID, KISS, DRY for real duplication only, high cohesion / low coupling,
encapsulated entities (behaviour methods, no public setters), dependency inversion at
module and adapter boundaries.

---

## 10. Coding standards

### Java
- Java 21 features welcome (records for DTOs/events, sealed types, pattern matching).
- Constructor injection only; no field injection; classes `final` where practical.
- Controllers: validate → call one application service → map response. No business logic.
- `@Transactional` only in the `application` layer; read-only transactions for queries.
- Domain entities protect invariants; state changes via methods (`order.confirm(actor)`).
- Checked business errors → `BusinessException` subclasses with stable error codes.
- No `Optional` fields or parameters; `Optional` only as return type.
- No `System.out`; SLF4J with structured key-values; request ID in MDC.
- Formatting by Spotless (Google Java Format). No wildcard imports.
- Public module APIs have Javadoc.

### JavaScript / React
- Function components and hooks only. One component per file.
- Server state only via TanStack Query hooks in `features/<x>/api/`.
- Every screen handles loading, empty and error states.
- Forms: React Hook Form + Zod schema in `features/<x>/schemas/`.
- No secrets or business rules that matter for security in the frontend.
- Accessibility: labels for inputs, keyboard navigation, visible focus, alt text.
- ESLint must pass with zero warnings; Prettier formatting.

---

## 11. Naming conventions

| Thing | Convention | Example |
| --- | --- | --- |
| Java package | lowercase | `lk.lorafashions.ordering.application` |
| Module facade | `<Module>Api` | `InventoryApi` |
| Application service | `<Thing>Service` | `CheckoutService` |
| Controller | `<Thing>Controller` | `SellerOrderController` |
| Web DTOs | `<Action><Thing>Request` / `<Thing>Response` | `CreateProductRequest` |
| Module DTO | `<Thing>Dto` (record) | `ProductSnapshotDto` |
| Event | past tense record | `OrderPaid` |
| Exception | `<Problem>Exception` | `InsufficientStockException` |
| Error code | UPPER_SNAKE | `STOCK_INSUFFICIENT` |
| Table / column | snake_case, plural tables | `order_items.unit_price` |
| Constraint / index | `pk_`, `fk_<table>_<ref>`, `ux_<table>_<cols>`, `ix_<table>_<cols>`, `ck_` | `ux_products_tenant_id_slug` |
| Mongo collection / field | snake_case plural / camelCase | `design_versions.designId` |
| REST path | kebab-case plural nouns | `/api/v1/seller/refund-cases/{id}` |
| JSON fields | camelCase | `shippingFee` |
| Env variables | `APP_` + UPPER_SNAKE | `APP_JWT_SIGNING_KEY` |
| React component file | PascalCase `.jsx` | `ProductCard.jsx` |
| Hook | `useXxx.js` | `useCart.js` |
| Other JS files | camelCase `.js` | `formatMoney.js` |
| Test class | `<Class>Test` / `<Thing>IT` (integration) | `CheckoutServiceTest`, `PayHereWebhookIT` |

---

## 12. Folder structure

```
lora-fashion-saas/
  CLAUDE.md
  README.md
  .gitignore  .gitattributes  .env.example  .pre-commit-config.yaml
  backend/
    pom.xml  mvnw  Dockerfile
    src/main/java/lk/lorafashions/
      Application.java
      shared/ identity/ tenancy/ storefront/ subscription/ catalog/ inventory/
      cart/ promotion/ ordering/ payment/ design/ ai/ gallery/ rewards/
      messaging/ notification/ review/ files/ analytics/ admin/
    src/main/resources/
      application.yml  application-local.yml  application-dev.yml  application-prod.yml
      db/migration/  templates/email/
    src/test/java/lk/lorafashions/...
  frontend/
    package.json (npm workspaces)
    apps/web/      src/{app,features,shared}  vercel.json
    apps/admin/    src/{app,features,shared}  vercel.json
    packages/ui/   packages/api-client/
    e2e/           Playwright tests
  infra/
    docker-compose.yml  render.yaml
  docs/
    DEVELOPMENT_PARTS.md  architecture.md  event-catalogue.md  deployment.md
    assets.md  runbook.md  decisions/ (ADR-NNNN-title.md)
```

Feature folder (frontend): `features/<feature>/{api,components,hooks,pages,schemas}`.

---

## 13. API standards

- Base path `/api/v1`. Prefix by caller:
  `/auth/**` (anyone) · `/public/**` (anyone, read-only) · `/me/**` (signed-in user) ·
  `/seller/**` (store member + `X-Store-Id`) · `/admin/**` (platform staff + TOTP) ·
  `/webhooks/**` (signature-verified).
- Plural nouns, kebab-case; actions as sub-resources (`POST /orders/{id}/cancel`).
- Money `{"amount": "1500.00", "currency": "LKR"}` (amount as string). Time ISO-8601 UTC.
- Errors: RFC 9457 Problem Details + `code` + `errors[]` for fields. 404 for foreign resources.
- Paging: `page`, `size` (default 20, max 100), `sort` from an allow-list;
  response `{ "content": [...], "page": { "number", "size", "totalElements", "totalPages" } }`.
- `Idempotency-Key` header required on checkout, payment start, refund-case creation (stored 24 h).
- Optimistic concurrency: updates send `version`; stale → 409.
- Breaking change → `/api/v2` for that resource. OpenAPI via springdoc; Swagger UI off in `prod`.

---

## 14. Testing requirements

- A part is complete only when `./mvnw verify` (backend) and `npm run lint && npm test`
  (frontend, when touched) pass, and its acceptance criteria are checked.
- Coverage: ≥ 80% lines on `domain` and `application` packages (JaCoCo gate).
- Required per part where relevant: unit tests for rules; Testcontainers repository tests;
  MockMvc API tests (status, validation, Problem Details); Spring Security tests per role;
  **tenant-isolation tests for every new seller/customer endpoint**; WireMock for external
  services; component tests for new UI; Modulith verify test always green.
- Payment work must cover: success, failure, cancel, duplicate notification,
  wrong signature, wrong amount, out-of-order notification, expiry, chargeback.
- Playwright end-to-end suite grows with Phases 1, 5, 6, 8 and is completed in Phase 12.
- Test data via builders; no shared SQL dumps; tests independent of execution order.

---

## 15. Git conventions

- Remote `origin` = `https://github.com/IshanMihiranga/lora-fashion-saas.git`; branch `main`;
  **one commit per approved part**; push after the owner says "Next".
- Conventional Commits with the part number:
  `feat(ordering): split checkout into per-store orders [P5.3]`
  Types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `build`.
  Body: what changed, why, how it was tested.
- Fix inside the current, unpushed part → amend. After push → separate `fix(...)` commit.
- Tag `v0.<phase>.0` when a phase completes.
- Pre-commit: gitleaks, Spotless, Prettier, ESLint on changed files.
- Never commit `.env`, keys, dumps, `node_modules`, `target`, IDE files.

---

## 16. Development workflow (mandatory)

For every part:
1. **Brief first.** Post the part brief (objective, requirements, scope, architecture,
   patterns, database changes, backend, frontend, security, validation and errors, tests,
   acceptance criteria, commit message) and wait for the owner's go-ahead.
2. Implement only that scope.
3. Run all relevant tests; verify existing functionality still passes.
4. Report: what was implemented, files created/changed, key decisions, test results
   (counts + coverage), known limitations, the commit message.
5. **Stop.** Continue only when the owner says "Next" / "Continue to the next part".
   Then commit, push, update section 19 of this file, and post the next part's brief.

If a problem is found: explain it and fix it inside the approved scope before continuing.
If the plan must change: propose, get approval, write an ADR, update this file.

---

## 17. Development phases (summary — details in `docs/DEVELOPMENT_PARTS.md`)

| Phase | Name | Parts |
| --- | --- | --- |
| 0 | Foundation | 0.1–0.4 |
| 1 | Identity and security | 1.1–1.5 |
| 2 | Stores and onboarding | 2.1–2.7 |
| 3 | Subscriptions | 3.1–3.3 |
| 4 | Catalog and inventory | 4.1–4.5 |
| 5 | Cart, checkout, orders, payments | 5.1–5.8 |
| 6 | Design studio | 6.1–6.6 |
| 7 | AI generation | 7.1–7.2 |
| 8 | Gallery and rewards | 8.1–8.4 |
| 9 | Communication | 9.1–9.2 |
| 10 | Reviews and analytics | 10.1–10.2 |
| 11 | Administration | 11.1–11.2 |
| 12 | Hardening and release readiness | 12.1–12.4 |
| 13 | After V1 — courier integration | 13.1 |

---

## 18. Production best practices (target state)

- Twelve-factor config; one Docker image for all environments; Spring profiles `local`, `dev`, `prod`.
- Flyway migrations backward-compatible (expand → migrate → contract); prod migration runs before app start.
- Actuator liveness/readiness public; all other actuator endpoints closed.
- JSON logs with request IDs; external uptime monitor; error tracking (Sentry) in prod.
- Graceful shutdown; JVM tuned for container memory; HikariCP pool sized to the database plan.
- Backups: weekly `pg_dump` + `mongodump` to private R2 in dev; managed PITR/backups in prod;
  restore tested before launch.
- Before real payments: always-on backend, Vercel Pro / Cloudflare Pages, own domain,
  ClamAV, CI/CD, PayHere live credentials, legal pages reviewed.
- Rollback: redeploy previous image; never ship a migration the previous version cannot run with.

---

## 19. Current project status

**Phase:** 0 — Foundation (in progress).
**Current step:** Part 0.1 committed and pushed 2026-11-24.
**Next part:** 0.2 — Backend bootstrap (brief posted, awaiting owner approval).

### Completed
- [x] Phase 1 — Requirement analysis
- [x] Phase 2 — Architecture and development plan
- [x] Phase 3 — Plan approved (with owner edits: 1 yearly plan, Java 21, assistant commits and pushes)
- [x] Phase 4 — `CLAUDE.md` and `docs/DEVELOPMENT_PARTS.md` confirmed by owner
- [x] Owner decisions recorded: brand Lora Fashions, package `lk.lorafashions`,
      remote `https://github.com/IshanMihiranga/lora-fashion-saas.git`, plans and values in §3
- [x] Part 0.1 — Repository foundation (local object storage changed to SeaweedFS, ADR-0002)

### Pending
- [ ] Parts 0.2 → 12.4 (see `docs/DEVELOPMENT_PARTS.md`)
- [ ] Part 13.1 courier integration (after V1)

### Part log
Commit column: the commit subject's part tag; find the hash with `git log --grep "<tag>"`.

| Part | Status | Commit | Date |
| --- | --- | --- | --- |
| 0.1 Repository foundation | Done — services healthy, gitleaks blocks planted secrets, README verified from clean copy | `[P0.1]` | 2026-11-24 |
