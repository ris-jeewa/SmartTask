# DevOps — Docker / Kubernetes / Helm / GitHub Actions Rules

> Applies to: `Dockerfile`, `k8s/**/*.yaml`, `.github/workflows/**/*.yml`, and Helm chart files.

---

## Docker Rules

### Dockerfile Rules

- Always use **multi-stage builds** to keep final images small
- Always **pin base image versions** — never use the `latest` tag
- Always run as a **non-root user** in the final stage
- Copy only what is needed into the final image
- Add a `HEALTHCHECK` instruction to every Dockerfile

```dockerfile
# ✅ CORRECT — Spring Boot multi-stage Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S smarttask && adduser -S smarttask -G smarttask
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
USER smarttask
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```dockerfile
# ✅ CORRECT — React / Nginx multi-stage Dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.25-alpine AS runtime
RUN addgroup -S smarttask && adduser -S smarttask -G smarttask
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s CMD wget -qO- http://localhost/health || exit 1
```

```dockerfile
# ❌ WRONG — do not do any of this
FROM openjdk:latest          # never use latest
USER root                    # never run as root
COPY . /app                  # never copy everything
```

### docker-compose Rules (local dev only)

- `docker-compose.yml` is for **local development only** — never referenced in production or CI/CD
- Service names must match Kubernetes service names exactly
- Use **named volumes** for persistent data (PostgreSQL, RabbitMQ)
- Always set resource limits on all services

---

## Kubernetes Rules

### General Manifest Rules

Every manifest must follow these non-negotiable rules:

- Always set `resources.requests` and `resources.limits` on every container
- Always define `livenessProbe` and `readinessProbe` on every Deployment
- **Never use `latest`** image tag — always use a specific version or commit SHA
- Set `imagePullPolicy: Always` when using commit SHA tags
- Add meaningful labels to every resource: `app`, `version`, `component`, `part-of`

```yaml
# ✅ CORRECT — Deployment with all required fields
apiVersion: apps/v1
kind: Deployment
metadata:
  name: task-service
  namespace: smarttask
  labels:
    app: task-service
    component: backend
    part-of: smarttask
spec:
  replicas: 2
  selector:
    matchLabels:
      app: task-service
  template:
    metadata:
      labels:
        app: task-service
        version: "1.0.0"
    spec:
      containers:
        - name: task-service
          image: ghcr.io/your-username/smarttask-task-service:sha-abc123
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          env:
            - name: DB_URL
              valueFrom:
                configMapKeyRef:
                  name: task-service-config
                  key: db-url
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: task-service-secrets
                  key: db-password
```

### Namespace Rules

- Use a dedicated namespace for the entire application: `smarttask`
- Always specify `namespace: smarttask` in every manifest
- Never deploy application workloads into `default` or `kube-system`

```yaml
# Create namespace first
apiVersion: v1
kind: Namespace
metadata:
  name: smarttask
  labels:
    part-of: smarttask
```

### Secret Rules

- **Never commit real secret values** to the repository — ever, under any circumstances
- Use Kubernetes Secrets for all credentials (DB passwords, JWT secret, SMTP credentials, Redis password)
- Store secret values as base64 encoded strings
- Reference secrets via `secretKeyRef` in container env — never hardcode values in manifests
- Document all required secrets in `README.md` without revealing values — use a `secrets.example.yaml`

```yaml
# ✅ CORRECT — Secret manifest (commit with placeholder values only)
apiVersion: v1
kind: Secret
metadata:
  name: task-service-secrets
  namespace: smarttask
type: Opaque
data:
  db-password: BASE64_ENCODED_VALUE_HERE   # Never commit real value
  jwt-secret: BASE64_ENCODED_VALUE_HERE
```

### ConfigMap Rules

- Use ConfigMaps for **non-sensitive** configuration only
- One ConfigMap per service
- Reference ConfigMap values via `configMapKeyRef` in container env

```yaml
# ✅ CORRECT — ConfigMap for non-sensitive config
apiVersion: v1
kind: ConfigMap
metadata:
  name: task-service-config
  namespace: smarttask
data:
  db-url: "jdbc:postgresql://your-server.postgres.database.azure.com:5432/smarttask_tasks"
  rabbitmq-host: "rabbitmq-service"
  rabbitmq-port: "5672"
  server-port: "8080"
```

### Ingress Rules

- Use **one Ingress resource** to route all external traffic
- Route `/api/*` to the API Gateway service
- Route `/` to the React/Nginx service

```yaml
# ✅ CORRECT — Ingress routing
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: smarttask-ingress
  namespace: smarttask
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
    - host: smarttask.local
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: api-gateway-service
                port:
                  number: 8080
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-service
                port:
                  number: 80
```

### HorizontalPodAutoscaler Rules

- Add HPA to the **Task Service** (highest traffic service)
- Scale between 2 and 5 replicas based on CPU utilization

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: task-service-hpa
  namespace: smarttask
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: task-service
  minReplicas: 2
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

---

## Helm Rules

All Kubernetes manifests must be wrapped in a Helm chart.

**Rules:**
- Helm chart lives at `k8s/charts/smarttask/`
- Parameterize: image repository, image tag, replica count, resource limits, ingress host
- Never hardcode environment-specific values inside templates — always use `values.yaml`
- Provide `values.dev.yaml` and `values.prod.yaml` for environment overrides
- Always run `helm lint` before committing chart changes
- Use `helm template` to preview rendered output before applying

```
k8s/charts/smarttask/
├── Chart.yaml
├── values.yaml           # default/base values
├── values.dev.yaml       # dev overrides (lower replicas, smaller limits)
├── values.prod.yaml      # prod overrides (higher replicas, larger limits)
└── templates/
    ├── _helpers.tpl
    ├── namespace.yaml
    ├── deployments/
    │   ├── frontend.yaml
    │   ├── user-service.yaml
    │   ├── task-service.yaml
    │   ├── notification-service.yaml
    │   └── api-gateway.yaml
    ├── services/
    ├── configmaps/
    ├── secrets/
    ├── ingress.yaml
    └── hpa.yaml
```

```yaml
# values.yaml — parameterized defaults
image:
  registry: ghcr.io
  username: your-username
  tag: latest

taskService:
  replicas: 2
  resources:
    requests:
      memory: "256Mi"
      cpu: "250m"
    limits:
      memory: "512Mi"
      cpu: "500m"

ingress:
  host: smarttask.local
```

---

## GitHub Actions Rules

### General Workflow Rules

- **Never hardcode secrets** in workflow files — always use `${{ secrets.SECRET_NAME }}`
- Pin all GitHub Actions to a **specific commit SHA** (not a tag) to prevent supply chain attacks
- Always set `permissions` explicitly on each workflow (principle of least privilege)
- Add `timeout-minutes` to every job to prevent hung pipelines

### CI Workflow (`ci.yml`)

**Triggers:** push to any branch + pull request targeting `main`

**Job order:** `backend-test` → `frontend-build` → `sonar-scan` → `docker-build`

```yaml
# ✅ .github/workflows/ci.yml
name: CI

on:
  push:
    branches: ["**"]
  pull_request:
    branches: [main]

permissions:
  contents: read

jobs:
  backend-test:
    name: Backend Tests
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-maven-

      - name: Run backend tests
        run: |
          cd user-service && mvn verify --batch-mode
          cd ../task-service && mvn verify --batch-mode
          cd ../notification-service && mvn verify --batch-mode

  frontend-build:
    name: Frontend Build & Lint
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci
        working-directory: frontend

      - name: Lint
        run: npm run lint
        working-directory: frontend

      - name: Type check
        run: npm run type-check
        working-directory: frontend

      - name: Build
        run: npm run build
        working-directory: frontend
```

### CD Workflow (`cd.yml`)

**Trigger:** push to `main` only (after PR is merged)

**Job order:** `build-and-push-images` → `deploy`

```yaml
# ✅ .github/workflows/cd.yml
name: CD

on:
  push:
    branches: [main]

permissions:
  contents: read
  packages: write

jobs:
  build-and-push:
    name: Build & Push Docker Images
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v4

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push task-service
        uses: docker/build-push-action@v5
        with:
          context: ./task-service
          push: true
          tags: |
            ghcr.io/${{ github.repository }}/task-service:${{ github.sha }}
            ghcr.io/${{ github.repository }}/task-service:latest

  deploy:
    name: Deploy to Kubernetes
    needs: build-and-push
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v3

      - name: Configure kubeconfig
        run: echo "${{ secrets.KUBECONFIG }}" | base64 -d > kubeconfig.yaml

      - name: Deploy with Helm
        run: |
          helm upgrade --install smarttask k8s/charts/smarttask \
            --namespace smarttask \
            --create-namespace \
            --set image.tag=${{ github.sha }} \
            --values k8s/charts/smarttask/values.dev.yaml \
            --kubeconfig kubeconfig.yaml
```

### SonarCloud Rules

- SonarCloud scan runs on every CI pipeline as part of the `sonar-scan` job
- **Quality Gate must pass** before a PR can be merged (enforce via GitHub branch protection rules)
- Minimum coverage threshold: **70%**
- Fix all `BLOCKER` and `CRITICAL` issues before merging — no exceptions

---

## Environment Variable Naming

Always use `SCREAMING_SNAKE_CASE` for all environment variables:

| Category | Variable names |
|----------|---------------|
| Database | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| JWT | `JWT_SECRET`, `JWT_ACCESS_EXPIRY_MS`, `JWT_REFRESH_EXPIRY_MS` |
| RabbitMQ | `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` |
| Email | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` |
| App | `SERVER_PORT`, `APP_ENV`, `FRONTEND_URL` |

---

## Required GitHub Secrets Checklist

Document these in your README without values. Set them in GitHub repository Settings > Secrets:

```
KUBECONFIG          — base64 encoded kubeconfig for Minikube
DB_PASSWORD         — Azure PostgreSQL password
JWT_SECRET          — random 256-bit string
RABBITMQ_PASSWORD   — RabbitMQ admin password
REDIS_PASSWORD      — Azure Cache for Redis access key
SMTP_PASSWORD       — Gmail app password for notifications
SONAR_TOKEN         — SonarCloud project token
```
