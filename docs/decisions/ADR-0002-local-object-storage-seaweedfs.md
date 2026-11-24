# ADR-0002 — SeaweedFS replaces MinIO for local object storage

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** Project owner (approved during Part 0.1)

## Context

The approved plan used **MinIO** as the local S3-compatible object store (dev and prod
use Cloudflare R2). While implementing Part 0.1 on 2026-11-24, no official MinIO image
could be pulled:

- `minio/minio` and `minio/mc` no longer exist on Docker Hub ("object not found").
- `quay.io/minio/minio` and `quay.io/minio/mc` require authentication.
- Even the last known release tag could not be resolved.

MinIO's community edition moved to source-only distribution and maintenance mode, so a
pinned, trustworthy, pullable image is no longer available.

## Decision

Use **SeaweedFS** (`chrislusf/seaweedfs`, version pinned in `infra/docker-compose.yml`)
with its built-in **S3 gateway** as the local object store. The local bucket
(`lora-local`) is created by a one-shot official `amazon/aws-cli` container.

Scope of the change:
- Local environment only. Dev and prod keep **Cloudflare R2**.
- The application still talks to object storage only through the AWS SDK v2 S3 client
  behind the `ObjectStorage` port (Part 2.4), so no application code depends on SeaweedFS.
- CLAUDE.md §4 (data and services table) and the related MinIO mentions are updated.

## Consequences

**Positive**
- Actively maintained, Apache-2.0 licensed, single container, supports the S3 features
  the platform needs (path-style access, presigned PUT/GET, copy, delete).
- Keeps local development fully offline and free.

**Negative / risks**
- SeaweedFS's S3 compatibility differs from R2 in small ways; behaviour that matters
  (presigned URLs, content-type and size conditions) must also be verified against R2
  in the dev environment (Part 2.4 and Part 12.4).
- No MinIO web console; files are inspected with the AWS CLI or the SeaweedFS filer UI.

## Alternatives considered

- **RustFS** — MinIO drop-in, but only recently reached 1.0; less proven.
- **Garage** — stable but AGPL and needs cluster-layout setup on every fresh start.
- **Build MinIO from source** — slow first start and no further security fixes.
