# ADR-0001 — Modular monolith

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** Project owner

## Context

Lora Fashions Version 1 is built and run by a very small team on a zero-cost cloud
stack (one free Render web service, free Neon and Atlas tiers). The domain is broad —
identity, stores, subscriptions, catalog, inventory, ordering, payments, design, AI,
gallery, rewards, messaging, notifications, reviews, files, analytics and admin — but
traffic in Version 1 is small. Several business flows (checkout, payment notifications,
refunds) need strong consistency across areas that would be separate services in a
distributed design.

## Decision

Build **one Spring Boot application** organised as a **modular monolith**:

- Each business area is a top-level package under `lk.lorafashions` with a public
  `api` package (facade, DTOs, events) and internal `domain`, `application`,
  `infrastructure` and `web` packages.
- Modules talk to each other only through `api` facades (synchronous, downstream only)
  or domain events (upstream reactions).
- **Spring Modulith** enforces the boundaries: `ApplicationModules.verify()` runs in the
  test suite, and its event publication registry acts as a transactional outbox.
- Each module owns its tables or collections exclusively.
- No microservices, inter-service REST, API gateway, service discovery, message broker,
  saga or distributed transactions in Version 1.

## Consequences

**Positive**
- One deployable fits the free hosting tier and is simple to run, debug and test.
- Local ACID transactions cover checkout and payment flows; no distributed consistency.
- Boundaries are still explicit and machine-checked, so a module can be extracted later
  if real scaling needs appear.

**Negative / risks**
- All modules scale together; a heavy feature (e.g. AI calls) shares resources with the rest.
  Mitigated by circuit breakers, quotas and async event listeners.
- Boundary discipline depends on the Modulith verification test staying green.
- One bad deployment affects the whole platform. Mitigated by tests and image rollback.

## Alternatives considered

- **Microservices** — rejected: operational cost and complexity far exceed Version 1 needs,
  and free hosting tiers cannot run many services.
- **Unstructured monolith** — rejected: boundaries would erode, making later change and
  extraction expensive.
