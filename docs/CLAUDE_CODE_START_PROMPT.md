# Claude Code — start prompt (Lora Fashions)

Paste the block below into Claude Code, opened in the `lora-fashion-saas` folder.
Use it again whenever you start a new Claude Code session; it resumes from CLAUDE.md §19.

```text
You are developing Lora Fashions, a multi-tenant fashion customization SaaS.
CLAUDE.md is the approved, confirmed source of truth; follow it strictly.
The full list of development parts is in docs/DEVELOPMENT_PARTS.md.

Before doing anything else:
1. Read CLAUDE.md and docs/DEVELOPMENT_PARTS.md completely.
2. Read CLAUDE.md §19 (Current project status and part log) to find the current part.
3. Check my environment and report versions or anything missing. Do not install
   anything without asking:
   java -version (need 21), docker --version, docker compose version (Docker must be
   running), node -v (need current LTS), npm -v, git --version, and whether
   https://github.com/IshanMihiranga/lora-fashion-saas.git is reachable and empty.
4. Then post the brief for the current part (Part 0.1 — Repository foundation, if
   nothing is done yet) with every section required by CLAUDE.md §16: objective,
   requirements, scope, architecture, design patterns, database changes, backend,
   frontend, security, validation and error handling, tests, acceptance criteria and
   the commit message. Wait for my approval.

Fixed project values (already in CLAUDE.md):
- Brand: Lora Fashions. Base package: lk.lorafashions. Local folder: lora-fashion-saas.
- Git remote origin: https://github.com/IshanMihiranga/lora-fashion-saas.git, branch main.
- Plans: Starter, Growth, Pro monthly + Growth Yearly, prices and limits in CLAUDE.md §3.

Rules for the whole project:
- Write no code for a part until I approve its brief.
- Implement only the approved part. Never start, or partly build, a later part.
- After implementing: run all relevant tests, then report what was done, the files
  created or changed, key decisions, test results (counts and coverage), known
  limitations and the commit message. Then STOP and wait.
- Only when I say "Next" or "Continue to the next part": commit (one commit per part,
  Conventional Commits with the part number, e.g. "feat(identity): add registration
  and email verification [P1.1]"), push to origin main, update the status and part log
  in CLAUDE.md §19 (included in that part's commit), then post the brief for the next
  part and wait for my approval again.
- Modular monolith only. No microservices, no Redis, no technology outside CLAUDE.md §4
  without my approval.
- Security in every part: never trust client-supplied tenant IDs, roles, prices, totals
  or payment status; never commit secrets; add tenant-isolation tests for every new
  seller or customer endpoint.
- If anything is unclear or the plan seems to need a change, ask me first. Do not assume.
```
