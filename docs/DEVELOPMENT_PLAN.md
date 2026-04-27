# SmartTask — 3-Week Development Plan

> **Goal:** Build and deploy a production-grade full-stack microservices task management application before the career fair.
> **Timeline:** 21 days | **Solo developer**

---

## Tech Stack at a Glance

| Layer | Technologies |
|-------|-------------|
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui, React Query, Redux Toolkit, React Hook Form, Zod |
| **Backend** | Java 17, Spring Boot 3, Spring Security, Spring Cloud Gateway, MapStruct |
| **Database** | Azure Database for PostgreSQL Flexible Server |
| **Messaging** | RabbitMQ |
| **Monitoring** | Spring Actuator, Prometheus, Grafana |
| **DevOps** | Docker, Kubernetes (Minikube), Helm, GitHub Actions, SonarCloud |

---

## Monorepo Structure

```
smarttask/
├── frontend/
├── user-service/
├── task-service/
├── notification-service/
├── api-gateway/
├── k8s/
│   ├── charts/smarttask/       (Helm chart)
│   └── manifests/              (raw YAML for reference)
├── docs/
│   └── rules/                  (coding standard .md files)
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── cd.yml
├── docker-compose.yml          (local dev only)
└── README.md
```

---

## Pre-Development Checklist

Complete these before Day 1:

- [ ] Create GitHub repository (public, monorepo)
- [ ] Set up SonarCloud — connect to GitHub repo, note the project token
- [ ] Create Azure free account and provision **Azure Database for PostgreSQL Flexible Server**
- [ ] Create two databases on the server: `smarttask_users`, `smarttask_tasks`
- [ ] Note Azure DB hostname, username, password
- [ ] Install locally: Java 17, Maven, Node 20, Docker Desktop, Minikube, Helm, kubectl
- [ ] Set up Cursor AI with `.cursor/rules/` files
- [ ] Create GitHub Projects board with columns: `Backlog`, `In Progress`, `In Review`, `Done`
- [ ] Open one GitHub Issue per day listed below and add to backlog

---

## Week 1 — Backend Foundation

> **Goal:** All three microservices running locally with tests passing, connected to Azure PostgreSQL, SonarCloud green.

---

### Day 1 — Project Scaffold + Azure Setup

**GitHub Issue:** `chore: initialize monorepo and configure azure postgresql`

**Tasks:**
- [ ] Initialize monorepo folder structure
- [ ] Generate all 4 Spring Boot projects via [start.spring.io](https://start.spring.io):
  - `user-service` — dependencies: Spring Web, Spring Data JPA, Spring Security, PostgreSQL Driver, Lombok, MapStruct, Actuator, Validation
  - `task-service` — same as above
  - `notification-service` — same + Spring AMQP (RabbitMQ)
  - `api-gateway` — Spring Cloud Gateway, Spring Security
- [ ] Configure `application.yml` for each service with Azure PostgreSQL connection
- [ ] Verify all services start and connect to Azure DB
- [ ] Connect SonarCloud to the GitHub repo
- [ ] Add `.gitignore`, `README.md` stub, and `.env.example` to repo root
- [ ] First commit: `chore: initialize smarttask monorepo`

**Definition of Done:** All 4 Spring Boot apps start without errors. Azure DB migrations run successfully.

---

### Day 2 — User Service

**GitHub Issue:** `feat: implement user service with jwt authentication`

**Tasks:**
- [ ] Implement `User` entity in `domain/` package
- [ ] Implement `UserRepository` with `findByEmail` and `findByUsername`
- [ ] Implement `UserService` interface + `UserServiceImpl`
- [ ] Implement `RegisterRequest`, `LoginRequest`, `AuthResponse` DTOs (Java Records)
- [ ] Implement `UserMapper` with MapStruct
- [ ] Implement `JwtService` — generate and validate access tokens (15 min) and refresh tokens (7 days)
- [ ] Implement `AuthController` with `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh` endpoints
- [ ] Configure Spring Security — permit auth endpoints, protect all others
- [ ] Implement `GlobalExceptionHandler` with `@RestControllerAdvice`
- [ ] Enable Swagger/OpenAPI docs
- [ ] Write unit tests for `UserServiceImpl` (min 5 test cases)
- [ ] Test all endpoints with Postman

**Definition of Done:** Register, login, and refresh token endpoints work. JWT is issued and validated. Tests pass. SonarCloud shows no BLOCKER issues.

---

### Day 3 — Task Service

**GitHub Issue:** `feat: implement task service with full crud`

**Tasks:**
- [ ] Implement `Task` entity with fields: `title`, `description`, `status`, `priority`, `assigneeId`, `dueDate`, `createdAt`, `updatedAt`
- [ ] Implement `TaskStatus` and `Priority` enums
- [ ] Implement `TaskRepository` with queries for filter by status, assignee, and overdue tasks
- [ ] Implement `TaskService` interface + `TaskServiceImpl`
- [ ] Implement `CreateTaskRequest`, `UpdateTaskRequest`, `TaskResponse` DTOs
- [ ] Implement `TaskMapper` with MapStruct
- [ ] Implement `TaskController` with:
  - `GET /api/tasks` — paginated, filterable by status and priority
  - `GET /api/tasks/{id}` — single task
  - `POST /api/tasks` — create
  - `PUT /api/tasks/{id}` — full update
  - `PATCH /api/tasks/{id}/status` — status transition only
  - `DELETE /api/tasks/{id}` — soft delete (set status to CANCELLED)
- [ ] Add `@Valid` validation on all request bodies
- [ ] Implement `GlobalExceptionHandler`
- [ ] Enable Swagger/OpenAPI docs
- [ ] Write unit tests for `TaskServiceImpl` (min 8 test cases)
- [ ] Test all endpoints with Postman

**Definition of Done:** All 6 CRUD endpoints work with correct HTTP status codes. Pagination works. Tests pass.

---

### Day 4 — Notification Service + RabbitMQ

**GitHub Issue:** `feat: implement notification service with rabbitmq integration`

**Tasks:**
- [ ] Add RabbitMQ to `docker-compose.yml` (image: `rabbitmq:3-management`)
- [ ] Run RabbitMQ locally via Docker: `docker-compose up rabbitmq -d`
- [ ] Create `RabbitMQConstants.java` class in task-service with queue names, exchange names, routing keys
- [ ] Configure RabbitMQ in task-service: declare exchange, queues, DLQ, bindings in `@Configuration`
- [ ] Implement `TaskEventPublisher` in task-service messaging package
  - Publish `TaskCreatedEvent` on task creation
  - Publish `TaskAssignedEvent` on task assignment
- [ ] In notification-service:
  - Implement `RabbitMQConsumer` with `@RabbitListener` for both events
  - Implement `EmailNotificationService` using JavaMailSender (Gmail SMTP)
  - Implement `NotificationController` — `GET /api/notifications` for user's notification history
- [ ] Configure Gmail SMTP in `application.yml` (use Gmail App Password)
- [ ] Write unit tests for consumer logic (min 4 test cases)
- [ ] End-to-end test: create a task → verify email is received

**Definition of Done:** Creating a task triggers a RabbitMQ event. Notification service consumes it and sends an email. Failed messages go to DLQ.

---

### Day 5 — API Gateway + Cross-cutting Concerns

**GitHub Issue:** `feat: implement api gateway with jwt filter and routing`

**Tasks:**
- [ ] Configure Spring Cloud Gateway routes:
  - `/api/auth/**` → user-service (no auth required)
  - `/api/users/**` → user-service (auth required)
  - `/api/tasks/**` → task-service (auth required)
  - `/api/notifications/**` → notification-service (auth required)
- [ ] Implement `JwtAuthenticationFilter` at gateway level — validate JWT, forward user info via headers
- [ ] Configure CORS at gateway level to allow React frontend origin
- [ ] Configure rate limiting on the gateway (Spring Cloud Gateway RequestRateLimiter)
- [ ] Test all routes via Postman through the gateway
- [ ] Verify unauthenticated requests to protected routes return 401
- [ ] Verify CORS headers are returned correctly

**Definition of Done:** All services are accessible only through the gateway. JWT validation at gateway blocks invalid tokens. CORS works for `localhost:5173`.

---

### Day 6 — Monitoring Setup

**GitHub Issue:** `chore: add prometheus metrics and grafana dashboard`

**Tasks:**
- [ ] Add `micrometer-registry-prometheus` dependency to all services
- [ ] Configure `/actuator/prometheus` endpoint on all services
- [ ] Configure liveness (`/actuator/health/liveness`) and readiness (`/actuator/health/readiness`) endpoints
- [ ] Add Prometheus and Grafana to `docker-compose.yml`
- [ ] Write `prometheus.yml` scrape config targeting all 3 service actuator endpoints
- [ ] Start Prometheus and Grafana: `docker-compose up prometheus grafana -d`
- [ ] Import JVM dashboard (Grafana dashboard ID: 4701) into Grafana
- [ ] Build custom SmartTask dashboard with panels:
  - HTTP request rate per service
  - HTTP error rate (4xx, 5xx) per service
  - JVM heap memory usage per service
  - Task creation rate (custom Micrometer counter)
- [ ] Add a custom `Counter` metric in task-service: `smarttask.tasks.created`
- [ ] Verify all metrics appear in Prometheus and Grafana

**Definition of Done:** All 3 services emit Prometheus metrics. Grafana dashboard shows live data. Custom task creation counter works.

---

### Day 7 — Backend Buffer + Integration Testing

**GitHub Issue:** `test: add integration tests and increase coverage to 70%`

**Tasks:**
- [ ] Fix any bugs discovered during the week
- [ ] Add Testcontainers to all services for integration tests
- [ ] Write integration tests for `AuthController` (register + login flow)
- [ ] Write integration tests for `TaskController` (full CRUD flow)
- [ ] Run SonarCloud scan — fix all BLOCKER and CRITICAL issues
- [ ] Verify test coverage is at or above **70%** on service and controller layers
- [ ] Review all code against SOLID principles
- [ ] Make sure all Swagger docs are complete and accurate
- [ ] Write `.env.example` for each service documenting required environment variables
- [ ] Push everything to GitHub

**Definition of Done:** All tests pass. SonarCloud quality gate passes. Coverage ≥ 70%. No BLOCKER issues.

---

## Week 2 — Frontend

> **Goal:** Fully functional React application connected to the backend through the API Gateway.

---

### Day 8 — Frontend Project Setup

**GitHub Issue:** `chore: initialize react frontend with full toolchain`

**Tasks:**
- [ ] Initialize Vite + React + TypeScript project: `npm create vite@latest frontend -- --template react-ts`
- [ ] Install dependencies:
  ```
  npm install tailwindcss @tailwindcss/vite
  npm install @tanstack/react-query axios
  npm install @reduxjs/toolkit react-redux
  npm install react-hook-form @hookform/resolvers zod
  npm install react-router-dom
  npm install recharts
  npm install lucide-react
  ```
- [ ] Install and initialize shadcn/ui: `npx shadcn@latest init`
- [ ] Add shadcn components: `button input form card table dialog toast badge skeleton dropdown-menu`
- [ ] Configure Tailwind CSS with dark mode (`class` strategy)
- [ ] Set up folder structure: `api/`, `components/`, `hooks/`, `pages/`, `store/`, `types/`, `schemas/`, `utils/`, `constants/`
- [ ] Create `src/constants/queryKeys.ts` and `src/constants/routes.ts`
- [ ] Create `src/api/axiosInstance.ts` with request interceptor (JWT attach) and response interceptor (401 handling)
- [ ] Set up Redux store with `authSlice` and `themeSlice`
- [ ] Set up React Router with `ProtectedRoute` component
- [ ] Set up React Query `QueryClientProvider` with global error handling
- [ ] Set up `App.tsx` with router, store provider, and query client provider
- [ ] Configure `.env.example` with `VITE_API_BASE_URL`

**Definition of Done:** `npm run dev` starts without errors. Redux DevTools shows store. React Query DevTools shows up.

---

### Day 9 — Auth Pages + Token Management

**GitHub Issue:** `feat: implement login and register pages with jwt auth`

**Tasks:**
- [ ] Define TypeScript interfaces in `src/types/auth.types.ts`: `UserResponse`, `LoginRequest`, `RegisterRequest`, `AuthResponse`
- [ ] Write Zod schemas in `src/schemas/auth.schema.ts`: `loginSchema`, `registerSchema`
- [ ] Create `src/api/endpoints/auth.api.ts` with `login`, `register`, `refreshToken` functions
- [ ] Implement `LoginPage.tsx`:
  - React Hook Form + Zod validation
  - Show field-level errors
  - Disable button while submitting
  - Show loading spinner on button
  - Redirect to dashboard on success
  - shadcn/ui `Input`, `Button`, `Card` components
- [ ] Implement `RegisterPage.tsx`:
  - Same form discipline as login
  - Password + confirm password with match validation
- [ ] Implement `useAuth` hook: `login`, `register`, `logout` mutations
- [ ] Store access token in Redux, refresh token in `HttpOnly` cookie (via backend)
- [ ] Implement silent token refresh: intercept 401 → call `/api/auth/refresh` → retry original request
- [ ] Implement `ProtectedRoute` that reads from Redux auth state
- [ ] Implement dark/light mode toggle button in the layout header
- [ ] Test: login → dashboard redirect. Logout → login redirect. Invalid credentials → error toast.

**Definition of Done:** Full auth flow works end-to-end. Token refresh works silently. Protected routes redirect unauthenticated users.

---

### Day 10 — Layout + Dashboard Page

**GitHub Issue:** `feat: implement app layout and dashboard page`

**Tasks:**
- [ ] Implement `AppLayout.tsx`:
  - Sidebar with navigation links (Dashboard, Tasks, Notifications)
  - Top navbar with user avatar dropdown (profile, logout)
  - Dark/light mode toggle
  - Responsive: collapsible sidebar on mobile
- [ ] Define TypeScript interfaces in `src/types/task.types.ts`
- [ ] Create `src/api/endpoints/tasks.api.ts`
- [ ] Implement `useDashboardStats` hook using React Query
- [ ] Implement `DashboardPage.tsx`:
  - Summary cards: Total Tasks, Due Today, Overdue, Completed (with trend icons)
  - Task status breakdown bar chart (Recharts `BarChart`)
  - Priority distribution pie chart (Recharts `PieChart`)
  - Recent tasks list (last 5 created)
- [ ] Implement skeleton loading states for all cards and charts
- [ ] Handle error state with user-friendly message

**Definition of Done:** Dashboard loads real data from the API. Charts render correctly. Skeletons show during loading.

---

### Day 11 — Task List Page

**GitHub Issue:** `feat: implement task list page with filtering and pagination`

**Tasks:**
- [ ] Implement `useTasks` hook with React Query — supports filter params
- [ ] Implement `TaskListPage.tsx`:
  - shadcn/ui `Table` with columns: title, status badge, priority badge, assignee, due date, actions
  - Filter bar: status dropdown, priority dropdown, search by title
  - URL-synced filters (update query params on filter change, restore from URL on load)
  - Pagination with page size selector
  - Row actions: Edit, Change Status, Delete (with confirmation dialog)
  - Empty state illustration when no tasks found
- [ ] Implement `TaskStatusBadge` reusable component with color-coded status
- [ ] Implement `PriorityBadge` reusable component with color-coded priority
- [ ] Implement `useDeleteTask` mutation with optimistic update and toast notification
- [ ] Test: filter by status, filter by priority, paginate, delete task

**Definition of Done:** Task list renders with real data. Filters work and persist in URL. Pagination works. Delete with confirmation works.

---

### Day 12 — Create / Edit Task

**GitHub Issue:** `feat: implement create and edit task modal`

**Tasks:**
- [ ] Write Zod schemas in `src/schemas/task.schema.ts`: `createTaskSchema`, `updateTaskSchema`
- [ ] Implement `CreateTaskModal.tsx`:
  - shadcn/ui `Dialog`
  - Fields: title, description, priority (select), due date (date picker), assignee (user search dropdown)
  - React Hook Form + Zod validation
  - Disable submit while submitting
  - Close on success, stay open on error
- [ ] Implement `EditTaskModal.tsx`:
  - Pre-populate form with existing task values
  - Same validation as create
- [ ] Implement `useCreateTask` mutation — invalidates task list query on success
- [ ] Implement `useUpdateTask` mutation — invalidates task detail and list queries on success
- [ ] Implement `TaskDetailPage.tsx`:
  - Full task info display
  - Status transition buttons (TODO → IN PROGRESS → DONE)
  - Edit button opens `EditTaskModal`
  - Activity log placeholder
- [ ] Test: create task → appears in list. Edit task → changes persist. Status change → badge updates.

**Definition of Done:** Create and edit flows work end-to-end. Cache is invalidated after mutations. Status transitions work.

---

### Day 13 — Notifications Panel + UI Polish

**GitHub Issue:** `feat: implement notifications panel and polish ui`

**Tasks:**
- [ ] Define `NotificationResponse` TypeScript interface
- [ ] Create `src/api/endpoints/notifications.api.ts`
- [ ] Implement `useNotifications` hook
- [ ] Implement `NotificationBell` component in navbar:
  - Unread count badge (red dot)
  - Click to open slide-in `Sheet` (shadcn/ui)
  - Notification list with timestamp, message, read/unread state
  - "Mark all as read" button
- [ ] Implement `useMarkNotificationsRead` mutation
- [ ] Polish UI across all pages:
  - Consistent spacing and typography
  - Hover states on all interactive elements
  - Smooth page transitions with CSS
  - Loading skeletons everywhere (no raw spinners)
  - Ensure all pages are responsive on 375px mobile width
  - Verify dark mode on all pages
- [ ] Add 404 page for unknown routes
- [ ] Add global error boundary component

**Definition of Done:** Notifications load and display correctly. UI is consistent and polished. Dark mode works everywhere. Mobile layout is usable.

---

### Day 14 — Frontend Buffer + Code Review

**GitHub Issue:** `chore: frontend code review and bug fixes`

**Tasks:**
- [ ] Fix all UI bugs discovered during the week
- [ ] Audit all API error handling — every mutation must have an `onError` handler with a toast
- [ ] Audit all loading states — every async operation must show a skeleton or spinner
- [ ] Verify all forms have proper validation with field-level error messages
- [ ] Run `npm run lint` — fix all ESLint warnings and errors
- [ ] Run `npm run type-check` — fix all TypeScript errors
- [ ] Run Lighthouse audit in Chrome DevTools — fix any obvious performance issues
- [ ] Review component sizes — any component over 150 lines must be split
- [ ] Verify `ProtectedRoute` works correctly in all edge cases
- [ ] Push all frontend code to GitHub

**Definition of Done:** Zero ESLint errors. Zero TypeScript errors. Lighthouse performance score ≥ 70. All forms validate correctly.

---

## Week 3 — DevOps + Polish + Launch

> **Goal:** Everything containerized, deployed to Kubernetes via Helm, CI/CD pipeline green, README complete, demo recorded.

---

### Day 15 — Dockerize All Services

**GitHub Issue:** `chore: dockerize all services with multi-stage builds`

**Tasks:**
- [ ] Write multi-stage `Dockerfile` for `user-service`
- [ ] Write multi-stage `Dockerfile` for `task-service`
- [ ] Write multi-stage `Dockerfile` for `notification-service`
- [ ] Write multi-stage `Dockerfile` for `api-gateway`
- [ ] Write multi-stage `Dockerfile` for `frontend` (Vite build + Nginx serve)
- [ ] Write `nginx.conf` for the React app (SPA routing — redirect all 404s to `index.html`)
- [ ] Update `docker-compose.yml` to build and run the full stack including all services
- [ ] Test full stack with Docker Compose: `docker-compose up --build`
- [ ] Verify the React app communicates with the backend through the gateway
- [ ] Verify health check endpoints respond in all containers
- [ ] Push all images to GitHub Container Registry (GHCR): `docker push ghcr.io/your-username/smarttask-*`

**Definition of Done:** `docker-compose up --build` starts the entire stack. Full auth and task CRUD flow works in Docker. All images pushed to GHCR.

---

### Day 16 — Kubernetes Manifests

**GitHub Issue:** `chore: write kubernetes manifests for all services`

**Tasks:**
- [ ] Start Minikube: `minikube start --memory=4096 --cpus=2`
- [ ] Enable Ingress addon: `minikube addons enable ingress`
- [ ] Create `smarttask` namespace
- [ ] Write raw YAML manifests under `k8s/manifests/` (these become the basis for Helm templates):
  - `Deployment` + `Service` for each of the 5 components (frontend, gateway, user, task, notification)
  - `Deployment` + `Service` for RabbitMQ
  - `ConfigMap` per service (non-sensitive config)
  - `Secret` per service (DB credentials, JWT secret, SMTP password) — with placeholder values
  - `Ingress` routing `/api/*` to gateway and `/` to frontend
  - `HorizontalPodAutoscaler` for task-service
- [ ] Apply all manifests: `kubectl apply -f k8s/manifests/ -n smarttask`
- [ ] Verify all pods reach `Running` state: `kubectl get pods -n smarttask`
- [ ] Verify Ingress routes correctly via `minikube ip`
- [ ] Test full flow through Minikube Ingress

**Definition of Done:** All 7 deployments are Running in Minikube. Ingress routes traffic correctly. Full app flow works in K8s.

---

### Day 17 — Helm Chart

**GitHub Issue:** `chore: wrap kubernetes manifests in helm chart`

**Tasks:**
- [ ] Create Helm chart structure: `k8s/charts/smarttask/`
- [ ] Write `Chart.yaml` with name, version, description
- [ ] Write `values.yaml` with all parameterized values:
  - Image registry, repository, tag per service
  - Replica counts per service
  - Resource limits per service
  - Ingress hostname
  - ConfigMap values
- [ ] Write `values.dev.yaml` overrides (lower replicas, smaller resource limits)
- [ ] Convert all raw manifests to Helm templates using `{{ .Values.* }}` references
- [ ] Write `_helpers.tpl` with reusable label helpers
- [ ] Run `helm lint k8s/charts/smarttask` — fix all warnings
- [ ] Preview rendered output: `helm template smarttask k8s/charts/smarttask --values k8s/charts/smarttask/values.dev.yaml`
- [ ] Deploy with Helm: `helm upgrade --install smarttask k8s/charts/smarttask --namespace smarttask --values k8s/charts/smarttask/values.dev.yaml`
- [ ] Verify all pods are Running after Helm install
- [ ] Test `helm upgrade` with a new image tag — verify rolling update

**Definition of Done:** `helm lint` passes with zero errors. `helm upgrade --install` deploys cleanly. Rolling update works.

---

### Day 18 — GitHub Actions CI Pipeline

**GitHub Issue:** `chore: implement github actions ci pipeline`

**Tasks:**
- [ ] Create `.github/workflows/ci.yml`
- [ ] Configure triggers: `push` to any branch + `pull_request` targeting `main`
- [ ] Add `backend-test` job:
  - Cache Maven dependencies
  - Run `mvn verify` for all 3 services
  - Publish test results
- [ ] Add `frontend-build` job:
  - Cache npm modules
  - Run `npm ci`, `npm run lint`, `npm run type-check`, `npm run build`
- [ ] Add `sonar-scan` job (depends on `backend-test`):
  - Run SonarCloud scan with `SONAR_TOKEN` secret
  - Fail if quality gate fails
- [ ] Add `docker-build` job (depends on all above):
  - Build all 5 Docker images (no push on CI — push only on CD)
  - Verify images build successfully
- [ ] Add build status badge to README
- [ ] Push and verify pipeline runs green on GitHub Actions

**Definition of Done:** CI pipeline runs on every push. All 4 jobs pass. SonarCloud quality gate passes. Badge shows green in README.

---

### Day 19 — GitHub Actions CD Pipeline

**GitHub Issue:** `chore: implement github actions cd pipeline`

**Tasks:**
- [ ] Create `.github/workflows/cd.yml`
- [ ] Configure trigger: `push` to `main` only
- [ ] Add `build-and-push` job:
  - Login to GHCR with `GITHUB_TOKEN`
  - Build and push all 5 images tagged with `${{ github.sha }}` and `latest`
- [ ] Add `deploy` job (depends on `build-and-push`):
  - Set up kubectl with `KUBECONFIG` secret (base64 encoded Minikube kubeconfig)
  - Run `helm upgrade --install` with the new image SHA tag
- [ ] Add all required secrets to GitHub repository settings:
  - `KUBECONFIG` — base64 Minikube kubeconfig
  - `DB_PASSWORD` — Azure PostgreSQL password
  - `JWT_SECRET` — random 256-bit string
  - `RABBITMQ_PASSWORD`
  - `SMTP_PASSWORD`
  - `SONAR_TOKEN`
- [ ] Add deployment status badge to README
- [ ] Merge a test PR and verify CD pipeline deploys successfully

**Definition of Done:** CD pipeline triggers on merge to main. New Docker images are pushed to GHCR. Helm deploys the new version to Minikube.

---

### Day 20 — Monitoring in Kubernetes

**GitHub Issue:** `chore: deploy prometheus and grafana to kubernetes`

**Tasks:**
- [ ] Add Prometheus community Helm repo: `helm repo add prometheus-community https://prometheus-community.github.io/helm-charts`
- [ ] Deploy `kube-prometheus-stack` via Helm into `monitoring` namespace
- [ ] Configure Prometheus scrape jobs to target all Spring Boot Actuator endpoints in `smarttask` namespace
- [ ] Verify Prometheus scrapes all services: check `Status > Targets` in Prometheus UI
- [ ] Import SmartTask custom Grafana dashboard (exported from local setup on Day 6)
- [ ] Import JVM Micrometer Grafana dashboard (ID: 4701)
- [ ] Configure one Grafana alert rule: HTTP error rate > 5% for 5 minutes → alert
- [ ] Take screenshots of working Grafana dashboards for the README
- [ ] Expose Grafana via port-forward or Ingress path for demo purposes

**Definition of Done:** Prometheus scrapes all services in K8s. Grafana dashboard shows live data. Alert rule is configured.

---

### Day 21 — README + Demo + Launch

**GitHub Issue:** `docs: write readme and record demo video`

**Tasks:**
- [ ] Design architecture diagram in [Excalidraw](https://excalidraw.com) — export as PNG
  - Show: React → API Gateway → 3 microservices → Azure PostgreSQL, RabbitMQ
  - Include Prometheus + Grafana + K8s layer
- [ ] Write comprehensive `README.md`:
  - Project name + one-line description
  - Architecture diagram (embed the Excalidraw PNG)
  - Tech stack table
  - Key features list
  - Local setup guide (step-by-step from clone to running)
  - Environment variables reference (link to `.env.example` files)
  - How to run tests
  - CI/CD pipeline explanation
  - Kubernetes deployment instructions
  - Demo video link (top of README, bold)
  - Badges: CI status, SonarCloud quality gate, SonarCloud coverage
- [ ] Record 2-minute demo video with [Loom](https://loom.com) (free):
  - Show login → dashboard → create task → task appears in list → email notification received → Grafana metrics → Kubernetes pods running
- [ ] Add Loom video link at the very top of README
- [ ] Screenshot the GitHub Actions pipeline green runs — add to README
- [ ] Screenshot the Grafana dashboard — add to README
- [ ] Screenshot the Kubernetes pods running — add to README
- [ ] Final end-to-end smoke test of the full deployed stack
- [ ] Pin the repository on your GitHub profile
- [ ] Add the project to your CV

**Definition of Done:** README is complete with architecture diagram, setup guide, and demo video. Repo is pinned on GitHub profile. You are ready for the career fair.

---

## Daily Habits (Every Day)

| Habit | Why |
|-------|-----|
| Commit at least once using Conventional Commits | Clean git history |
| Open a GitHub Issue before starting work | Shows workflow discipline |
| Close the Issue with a PR (not a direct push) | Shows PR-based workflow |
| Move GitHub Projects cards as you work | Demonstrates project management |
| Run linting and tests before pushing | Keeps CI green |

---

## CV Line (Copy This After Day 21)

> **SmartTask** | React, TypeScript, Java, Spring Boot 3, Spring Cloud Gateway, RabbitMQ, Azure PostgreSQL, Docker, Kubernetes, Helm, GitHub Actions, SonarCloud, Prometheus, Grafana
>
> Built a full-stack microservices task management application with JWT authentication, async email notifications via RabbitMQ, and a React dashboard. Implemented CI/CD with GitHub Actions, containerized all services with Docker multi-stage builds, and deployed to Kubernetes via Helm with Horizontal Pod Autoscaling and Prometheus/Grafana monitoring.

---

## Risk Log

| Risk | Mitigation |
|------|-----------|
| Azure PostgreSQL connection issues | Test connection on Day 1. Keep local PostgreSQL as fallback. |
| RabbitMQ setup complexity | Use Docker image `rabbitmq:3-management` — management UI included. |
| K8s resource limits too low on Minikube | Start Minikube with `--memory=4096 --cpus=2`. |
| Running out of time on Week 3 | Week 3 Day 21 items are polish — core functionality is complete by Day 14. |
| SonarCloud quality gate blocking | Check SonarCloud dashboard daily from Day 2 onward — don't let issues pile up. |
