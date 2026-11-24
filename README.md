# Lora Fashions

AI-assisted fashion customization platform and multi-tenant e-commerce SaaS for small
and medium clothing and print businesses in Sri Lanka. Sellers subscribe, open a store
and sell; customers design products in a 2D editor with a 3D preview and pay each
seller directly through PayHere.

- Architecture: [`docs/architecture.md`](docs/architecture.md)
- Development plan: [`docs/DEVELOPMENT_PARTS.md`](docs/DEVELOPMENT_PARTS.md)
- Decisions: [`docs/decisions/`](docs/decisions/)
- Rules for contributors and AI sessions: [`CLAUDE.md`](CLAUDE.md)

> **Status:** Phase 0 — foundation. The backend skeleton (`backend/`) runs; the
> frontends (`frontend/`) are added in Part 0.4.

---

## Prerequisites

| Tool | Version | Check |
| --- | --- | --- |
| Git | 2.40+ | `git --version` |
| Docker Desktop (running) with Compose v2+ | current | `docker compose version` |
| Python | 3.9+ (only for the pre-commit hooks) | `python --version` |
| Java (JDK) | 21 LTS | `java -version` |
| Node.js + npm | current LTS (from Part 0.4) | `node -v` |

## Quick start (about 5 minutes after images are downloaded)

All commands run from the repository root. They work in PowerShell, Git Bash, macOS and Linux.

```bash
# 1. Clone
git clone https://github.com/IshanMihiranga/lora-fashion-saas.git
cd lora-fashion-saas

# 2. Create your local environment file (fake local values; never commit .env)
cp .env.example .env            # PowerShell: Copy-Item .env.example .env

# 3. Start local infrastructure and wait until every service is healthy
docker compose --env-file .env -f infra/docker-compose.yml up -d --wait

# 4. Check status (all services "healthy"; s3-init "exited (0)")
docker compose --env-file .env -f infra/docker-compose.yml ps -a

# 5. Install the git hooks (secret scanning and file hygiene)
python -m pip install --user pre-commit
python -m pre_commit install
python -m pre_commit run --all-files
```

The first hook run downloads the gitleaks toolchain and takes a few minutes; later runs are fast.

## Local services

Every port is bound to `127.0.0.1` only. Credentials come from your `.env`.

| Service | Purpose | Address |
| --- | --- | --- |
| PostgreSQL 17 | Business data | `localhost:5432`, database `lora` |
| MongoDB 8.0 | Design content, AI records | `mongodb://localhost:27017/lora?authSource=admin` |
| SeaweedFS S3 gateway | Object storage (replaces MinIO, see ADR-0002) | `http://localhost:8333`, bucket `lora-local` |
| Mailpit | Catches all outgoing email | SMTP `localhost:1025`, web UI <http://localhost:8025> |

Quick checks:

```bash
# PostgreSQL
docker compose --env-file .env -f infra/docker-compose.yml exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select version();"'
# MongoDB
docker compose --env-file .env -f infra/docker-compose.yml exec mongo sh -c 'mongosh -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --quiet --eval "db.runCommand({ping:1})"'
# Object storage: list buckets
docker compose --env-file .env -f infra/docker-compose.yml run --rm --entrypoint aws s3-init s3 ls
```

## Run the backend

Spring Boot 4 modular monolith in `backend/` (Java 21, Maven wrapper — no Maven install needed).
The `local` profile reads the repository's `.env`, so start the local services first.

```bash
cd backend

# Build and run every check: unit tests, integration tests (Testcontainers; Docker must
# be running), module-boundary test, formatting and coverage gate
./mvnw verify                      # Windows cmd/PowerShell: .\mvnw.cmd verify

# Run against the Compose services
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Health (only liveness/readiness are exposed)
curl http://localhost:8080/actuator/health/readiness

# Fix Java formatting
./mvnw spotless:apply
```

| Profile | Use | Configuration |
| --- | --- | --- |
| `local` | Your machine | Repository `.env` (`LOCAL_*` values) |
| `dev` | Zero-cost cloud | Environment variables `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD`, `APP_MONGO_URI` |
| `prod` | Production (later) | Same variables as `dev` |

A missing setting stops start-up with a message naming it. `dev` and `prod` log JSON.

Docker image (same image for every environment; the profile is chosen at run time):

```bash
docker build -t lora-backend backend/
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev \
  -e APP_DB_URL=... -e APP_DB_USERNAME=... -e APP_DB_PASSWORD=... -e APP_MONGO_URI=... lora-backend
```

## Everyday commands

```bash
# Stop (data is kept in Docker volumes)
docker compose --env-file .env -f infra/docker-compose.yml down

# Stop and DELETE all local data
docker compose --env-file .env -f infra/docker-compose.yml down -v

# Follow logs of one service
docker compose --env-file .env -f infra/docker-compose.yml logs -f postgres
```

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `failed to connect to the docker API` | Start Docker Desktop and wait until it says "Engine running". |
| `required variable ... is missing a value` | You skipped step 2: create `.env` from `.env.example`. |
| `port is already allocated` | Another program uses the port. Change the matching `LOCAL_*_PORT` in `.env` (for example a locally installed PostgreSQL on 5432). |
| A service stays `unhealthy` | `docker compose ... logs <service>`; if the data volume is from an old setup, reset with `down -v`. |
| Changed a password in `.env` but login fails | PostgreSQL and MongoDB set credentials only on first start. Reset with `down -v`. |
| Backend: `Missing required settings for profile 'local'` | Create `.env` from `.env.example` in the repository root. |
| Integration tests fail with `Could not find a valid Docker environment` | Start Docker Desktop; Testcontainers needs it. |
| Commit blocked by spotless | Run `./mvnw spotless:apply` in `backend/`, then stage the files again. |
| Commit blocked by gitleaks | The hook found something that looks like a secret. Remove it; never bypass with `--no-verify`. |

## Security

- Never commit `.env`, keys, dumps or real credentials. `.env.example` holds fake values only.
- Every commit is scanned by gitleaks through the pre-commit hook.
- Report a vulnerability privately to the project owner; do not open a public issue.
