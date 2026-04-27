# Database — PostgreSQL Rules

> Applies to: `**/db/migration/*.sql` and `**/application*.yml` files across all microservices.

---

## Schema Design Rules

| Rule | Correct | Wrong |
|------|---------|-------|
| Primary key type | `BIGSERIAL` | `INT`, `UUID` without reason |
| String columns | `VARCHAR(255)` with max length | `TEXT` for short strings |
| Enum-like columns | `VARCHAR` + `CHECK` constraint | PostgreSQL `ENUM` type |
| Null handling | Explicit `NOT NULL` + `DEFAULT` | Leaving nullable without reason |
| Audit columns | `created_at`, `updated_at` on every table | Omitting them |
| Index on FK | Always add index on foreign key columns | Unindexed foreign keys |
| Select star | Never in production queries | `SELECT *` |

**Rules:**
- Always add `created_at` and `updated_at` to **every** table
- Use `VARCHAR` with an explicit max length — use `TEXT` only when content is truly unbounded
- Use `VARCHAR` + `CHECK` constraint for enum-like columns (easier to migrate than PostgreSQL `ENUM`)
- Never use `NULL` as a meaningful value — use explicit defaults
- Always add indexes on foreign key columns and columns used in `WHERE` clauses

---

## Query Rules (Spring Data JPA)

- Prefer JPQL over native SQL in Spring Data repositories
- Use `@Query` with JPQL for complex queries
- Break down any query requiring more than 5 joins into smaller service-level operations
- Always use `Pageable` for queries that can return large result sets
- **Never use `SELECT *`** in production queries — Spring Data handles this, but be explicit in `@Query`

```java
// ✅ CORRECT — Spring Data method naming for simple queries
Optional<Task> findByIdAndAssigneeId(Long id, Long assigneeId);
Page<Task> findAllByStatus(TaskStatus status, Pageable pageable);
List<Task> findAllByAssigneeIdAndStatusNot(Long assigneeId, TaskStatus excludedStatus);

// ✅ CORRECT — @Query with JPQL for complex queries
@Query("""
    SELECT t FROM Task t
    WHERE t.dueDate < :now
    AND t.status NOT IN (:excludedStatuses)
    ORDER BY t.priority DESC, t.dueDate ASC
    """)
List<Task> findOverdueTasks(
    @Param("now") LocalDateTime now,
    @Param("excludedStatuses") List<TaskStatus> excludedStatuses
);

// ✅ CORRECT — Pageable for large result sets
@Query("SELECT t FROM Task t WHERE t.assigneeId = :assigneeId")
Page<Task> findAllByAssigneeId(@Param("assigneeId") Long assigneeId, Pageable pageable);
```

---

## Azure PostgreSQL Specific Rules

- Always connect via **SSL** — Azure enforces this by default, never disable it
- Store the full connection string as an **environment variable** — never hardcode it
- Use connection pooling via **HikariCP** (Spring Boot default)
- Maximum pool size per service: **10 connections** (respect Azure free tier connection limits)
- Never modify HikariCP pool settings without benchmarking first

---

## application.yml Configuration Rules

```yaml
# ✅ CORRECT — application.yml for any microservice
spring:
  datasource:
    url: ${DB_URL}                         # Always from env var
    username: ${DB_USERNAME}               # Always from env var
    password: ${DB_PASSWORD}               # Always from env var
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10               # Respect Azure connection limits
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: validate                  # NEVER use create or create-drop
    show-sql: false                       # NEVER true in any committed config
    open-in-view: false                   # Disable OSIV — always
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false


# ❌ WRONG — never do any of these
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop               # DESTROYS DATA on restart
    show-sql: true                        # Leaks queries in production logs
  datasource:
    url: jdbc:postgresql://myserver.postgres.database.azure.com:5432/db  # hardcoded
    password: mypassword123               # hardcoded secret — critical violation
```

---

## Spring Profile Rules

Always use profiles to separate dev and prod configuration:

```
src/main/resources/
├── application.yml           # Shared base config (no secrets, no env-specific values)
├── application-dev.yml       # Dev overrides (local RabbitMQ, verbose logging)
└── application-prod.yml      # Prod overrides (Azure services, minimal logging)
```

```yaml
# application-dev.yml — development overrides
spring:
  rabbitmq:
    host: localhost
    port: 5672
  redis:
    host: localhost
    port: 6379

logging:
  level:
    com.smarttask: DEBUG
    org.springframework.security: DEBUG

# application-prod.yml — production overrides
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: 6380
    ssl: true
    password: ${REDIS_PASSWORD}

logging:
  level:
    com.smarttask: INFO
    org.springframework: WARN
```

---

## Refresh Token Table (User Service)

Store refresh tokens in PostgreSQL with a TTL column for expiry tracking:

```sql
-- V1_2__create_refresh_tokens_table.sql
-- Rollback: DROP TABLE IF EXISTS refresh_tokens;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500)    NOT NULL UNIQUE,
    expires_at  TIMESTAMP       NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token   ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```
