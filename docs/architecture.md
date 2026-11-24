# Architecture — Lora Fashions Version 1

> Summary for orientation. `CLAUDE.md` is the source of truth; if this file and
> `CLAUDE.md` ever disagree, `CLAUDE.md` wins and this file is corrected.
> Decisions are recorded in [`decisions/`](decisions/).

## 1. System context

```
  Customers ─┐                                   ┌─► PayHere (each seller's own merchant account)
             ├─► apps/web  ──┐                   ├─► Cloudflare Workers AI (image generation)
  Sellers ───┘               │  same-origin      ├─► Brevo (email)
                             ├─ /api/* ───► Spring Boot modular monolith ─┼─► Cloudflare R2 (files)
  Platform staff ─► apps/admin┘  (+ WebSocket)   ├─► PostgreSQL (all business facts)
                                                 └─► MongoDB (design content, AI records)
```

- **One backend deployable**: a Spring Boot 4 application (Java 21) split into modules
  by package and verified by Spring Modulith ([ADR-0001](decisions/ADR-0001-modular-monolith.md)).
- **Two frontends**: `apps/web` (customers and sellers) and `apps/admin` (platform staff),
  React + Vite, JavaScript, sharing `packages/ui` and `packages/api-client`.
- The platform never holds customer money: each store is paid directly through its own
  PayHere account. The platform earns only from seller subscriptions.

## 2. Modules

Top-level packages under `lk.lorafashions`:

| Group | Modules |
| --- | --- |
| Kernel | `shared` |
| Accounts and stores | `identity`, `tenancy`, `storefront`, `subscription` |
| Commerce | `catalog`, `inventory`, `cart`, `promotion`, `ordering`, `payment` |
| Creation | `design`, `ai`, `gallery`, `rewards` |
| Communication | `messaging`, `notification`, `review` |
| Platform | `files`, `analytics`, `admin` |

Every module has the same layout: `api/` (public facade, DTOs, events) and the internal
`domain/`, `application/`, `infrastructure/`, `web/` packages.

### Dependency rules (summary)
1. Modules depend on `shared`; `shared` depends on nothing.
2. Cross-module access only through the other module's `api` package.
3. Synchronous calls go downstream only; upstream reactions use domain events
   (`@ApplicationModuleListener`, persisted in the event publication table, idempotent).
4. No cycles; `ApplicationModules.verify()` runs in the test suite.
5. No module reads or writes another module's tables or collections.

## 3. Data ownership

| Store | Owns |
| --- | --- |
| PostgreSQL | Everything involving money, stock, status, permissions or identity |
| MongoDB | Designs, immutable design versions, templates, sticker library, AI generation records |
| Object storage | All binary files (databases keep only object keys) |

Cross-database consistency without 2-phase commit: write the immutable design version
first, commit the order in PostgreSQL second, pin the version from the `OrderPlaced`
event, and reconcile nightly.

## 4. Multi-tenancy

Tenant = store, in a shared schema with a `tenant_id` column. Four layers:
1. **Resolution** — `X-Store-Id` + membership check sets `TenantContext`; body tenant IDs ignored.
2. **Query scoping** — Hibernate `@TenantId` on store-owned entities.
3. **PostgreSQL RLS** on the most sensitive tables, with `FORCE ROW LEVEL SECURITY`.
4. **Tests** — store-A-vs-store-B isolation tests for every seller and customer endpoint.

## 5. Environments

| Concern | Local | Dev (zero cost) | Prod (later) |
| --- | --- | --- | --- |
| PostgreSQL | Docker `postgres:17` | Neon free | Neon paid |
| MongoDB | Docker `mongo:8` | Atlas free | Atlas paid |
| Object storage | SeaweedFS S3 gateway ([ADR-0002](decisions/ADR-0002-local-object-storage-seaweedfs.md)) | Cloudflare R2 | Cloudflare R2 |
| Email | Mailpit | Brevo free | Brevo paid |
| Payments | PayHere sandbox | PayHere sandbox | PayHere live |
| Backend | Local JVM | Render free | Always-on instance |
| Frontend | Vite dev server | Vercel Hobby | Vercel Pro / Cloudflare Pages |

Local infrastructure is defined in [`infra/docker-compose.yml`](../infra/docker-compose.yml);
see the [README](../README.md) for start-up steps.

## 6. Repository layout

```
backend/    Spring Boot application (Part 0.2 onward)
frontend/   npm workspaces: apps/web, apps/admin, packages/ui, packages/api-client (Part 0.4 onward)
infra/      docker-compose.yml (local); render.yaml (Part 12.4)
docs/       DEVELOPMENT_PARTS.md, architecture.md, decisions/ (ADRs), later runbook and deployment guides
```
