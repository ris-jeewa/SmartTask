# SmartTask — Global Coding Rules

> These rules apply to **every file** in the project regardless of language or layer.

---

## Project Overview

SmartTask is a full-stack microservices task management application built with the following stack:

| Layer | Technologies |
|-------|-------------|
| Frontend | React, TypeScript, Vite, Tailwind CSS, shadcn/ui, React Query, Redux |
| Backend | Java 17, Spring Boot 3, Spring Security, Spring Cloud Gateway, MapStruct |
| Database | Azure PostgreSQL Flexible Server |
| Messaging | RabbitMQ |
| Monitoring | Prometheus, Grafana, Spring Actuator |
| DevOps | Docker, Kubernetes (Minikube), Helm, GitHub Actions, SonarCloud |

---

## General Principles

- Follow **SOLID principles** at all times
- Prefer **composition over inheritance**
- Write **self-documenting code** — names should reveal intent without needing a comment
- Keep functions and methods **small and focused** (single responsibility)
- Avoid **magic numbers and magic strings** — use named constants
- Never leave `TODO` comments without a linked GitHub Issue number
  ```
  // TODO #42 — refactor token validation logic
  ```
- Every public method must have a meaningful **Javadoc or JSDoc comment**
- **No dead code** — remove unused imports, variables, and methods immediately
- **Maximum file length: 300 lines.** If exceeded, split into smaller units
- **Maximum function/method length: 30 lines.** If exceeded, extract helper methods

---

## Naming Conventions

| Context | Convention | Example |
|---------|-----------|---------|
| Classes (Java) | PascalCase | `TaskServiceImpl` |
| Interfaces (Java) | PascalCase | `TaskService` |
| Methods & variables (Java) | camelCase | `findTaskById` |
| Constants (Java) | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Components (React) | PascalCase | `TaskCard.tsx` |
| Hooks (React) | camelCase with `use` prefix | `useTasks.ts` |
| Files (React) | Match the component/hook name | `TaskCard.tsx`, `useTasks.ts` |
| Env variables | SCREAMING_SNAKE_CASE | `DB_PASSWORD` |

**Rules:**
- Be explicit — avoid abbreviations unless universally understood (`id`, `url`, `dto`)
- Names must describe **WHAT** the thing does, not **HOW** it does it
- Boolean variables and methods must read as questions: `isActive`, `hasPermission`, `canDelete`

---

## Git Commit Rules

Always use **Conventional Commits** format:

```
<type>: <short description in present tense>
```

| Type | When to use |
|------|-------------|
| `feat:` | Adding a new feature |
| `fix:` | Fixing a bug |
| `chore:` | Build, config, dependency updates |
| `docs:` | Documentation only changes |
| `test:` | Adding or updating tests |
| `refactor:` | Code change that is neither a fix nor a feature |

**Examples:**
```
feat: add JWT refresh token rotation to user service
fix: resolve task status not updating on reassignment
test: add unit tests for TaskServiceImpl
chore: update spring-boot-starter to 3.2.1
```

**Branch Naming:**
```
feature/task-crud
fix/jwt-expiry-bug
chore/update-dependencies
docs/update-readme
```

**Rules:**
- Never commit directly to `main` — always use feature branches and PRs
- Every PR must reference a GitHub Issue
- Squash commits before merging to keep history clean

---

## Code Quality Rules

- **SonarCloud quality gate must pass** before any PR can be merged
- Fix all `BLOCKER` and `CRITICAL` SonarCloud issues before merging
- Minimum test coverage: **70%** on service and controller layers
- No commented-out code in committed files — delete it or open a GitHub Issue

---

## Security Rules

- **Never hardcode** credentials, secrets, or API keys anywhere in code
- Always load secrets from **environment variables** or **Kubernetes Secrets**
- Never log sensitive data: passwords, tokens, or PII (names, emails, phone numbers)
- Always **validate and sanitize** all external input before processing
- Never disable security features without a written comment explaining the reason and the linked GitHub Issue

---

## Documentation Rules

- Every service must have a `README.md` explaining: purpose, how to run locally, environment variables required
- The root `README.md` must include: architecture diagram, tech stack, local setup guide, CI/CD badge, SonarCloud badge, demo video link
- OpenAPI/Swagger docs must be enabled on every backend service
- All environment variables must be documented in a `.env.example` file at the root of each service

---

## File Organization Rules

- Group files by **feature/domain**, not by type
- Shared utilities go in a `util/` or `utils/` folder
- Configuration classes go in a `config/` folder
- Constants go in a `constants/` file or folder — never scatter them across the codebase
