# Backend — Java / Spring Boot Coding Rules

> Applies to all `.java` files across: `user-service`, `task-service`, `notification-service`, `api-gateway`

---

## Package Structure (Clean Architecture)

Every microservice must follow this **exact** package structure. Do not deviate.

```
com.smarttask.<service>/
├── config/              # Spring configuration classes only (@Configuration)
├── controller/          # REST controllers only — zero business logic
├── service/             # Business logic interfaces
│   └── impl/            # Service implementations
├── repository/          # Spring Data JPA repository interfaces only
├── domain/              # JPA entity classes
├── dto/                 # Data Transfer Objects
│   ├── request/         # Inbound request bodies
│   └── response/        # Outbound response bodies
├── mapper/              # MapStruct mapper interfaces
├── exception/           # Custom exceptions + global exception handler
├── messaging/           # RabbitMQ producers and consumers
└── util/                # Stateless, side-effect-free utility classes
```

**Why this matters:** Each layer has one job. Controllers talk HTTP. Services talk business rules. Repositories talk database. Mixing these creates unmaintainable code.

---

## Controller Rules

Controllers handle **HTTP concerns only**: routing, input validation triggering, and response wrapping. Nothing else.

**Rules:**
- No business logic in controllers — delegate everything to a Service
- Always return `ResponseEntity<T>` with an **explicit HTTP status code**
- Use `@Valid` on all `@RequestBody` parameters
- Use specific mapping annotations only: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
- Never use `@RequestMapping` at method level

```java
// ✅ CORRECT
@PostMapping
public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
}

// ❌ WRONG — business logic in controller, wrong annotation, missing ResponseEntity
@RequestMapping(method = RequestMethod.POST)
public TaskResponse createTask(@RequestBody CreateTaskRequest request) {
    Task task = new Task();
    task.setTitle(request.title());
    taskRepository.save(task);
    return new TaskResponse(task.getId(), task.getTitle());
}
```

---

## Service Rules

Services contain **all business logic**. They are the heart of the application.

**Rules:**
- Always define a **Service interface** and a **separate implementation class**
- Implementation must be annotated with `@Service` and `@RequiredArgsConstructor`
- Use `@Transactional(readOnly = true)` on all read-only methods
- Use `@Transactional` on all write methods
- Throw **custom exceptions** from the service layer — never return `null`
- Never call one service's repository directly from another service

```java
// ✅ CORRECT — interface
public interface TaskService {
    TaskResponse findById(Long id);
    TaskResponse create(CreateTaskRequest request);
    TaskResponse updateStatus(Long id, TaskStatus newStatus);
    void delete(Long id);
}

// ✅ CORRECT — implementation
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        log.info("Fetching task with id: {}", id);
        return taskRepository.findById(id)
            .map(taskMapper::toResponse)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Task task = taskMapper.toEntity(request);
        Task saved = taskRepository.save(task);
        log.info("Task created with id: {}", saved.getId());
        return taskMapper.toResponse(saved);
    }
}
```

---

## Entity Rules

Entities represent database tables. Keep them clean and focused on persistence only.

**Rules:**
- Entities must live in the `domain` package
- Always use `Long` as the primary key with `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- Avoid bidirectional relationships unless absolutely necessary
- Override `equals()` and `hashCode()` based on **business key**, not `id`
- Use `@Getter @Setter @NoArgsConstructor` from Lombok — **never use `@Data` on entities** (causes JPA issues)
- Use `@CreatedDate` and `@LastModifiedDate` for audit fields — never set them manually

```java
// ✅ CORRECT
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(name = "assignee_id")
    private Long assigneeId;

    private LocalDateTime dueDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

---

## DTO Rules

DTOs are the API contract. They are the only objects that cross service boundaries.

**Rules:**
- Always use **separate Request DTOs and Response DTOs**
- Never expose JPA entity objects via API responses
- Request DTOs must have **Bean Validation** annotations
- Response DTOs must be **immutable** — use Java Records
- Never put business logic in DTOs

```java
// ✅ Request DTO with validation
public record CreateTaskRequest(

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Priority is required")
    Priority priority,

    @Future(message = "Due date must be in the future")
    LocalDateTime dueDate,

    Long assigneeId
) {}

// ✅ Response DTO — immutable record
public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    Priority priority,
    Long assigneeId,
    LocalDateTime dueDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

---

## MapStruct Rules

MapStruct generates mapping code at compile time. Use it for **all** entity-DTO conversions.

**Rules:**
- Always use MapStruct — never write manual mapping code in services or controllers
- Mapper interfaces live in the `mapper` package
- Annotate with `@Mapper(componentModel = "spring")` so Spring can inject them
- Use `@Mapping` to handle fields with different names or types

```java
// ✅ CORRECT
@Mapper(componentModel = "spring")
public interface TaskMapper {

    TaskResponse toResponse(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "TODO")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toEntity(CreateTaskRequest request);
}
```

---

## Exception Handling Rules

Every error case deserves a specific exception and a consistent response format.

**Rules:**
- Define a custom exception for every distinct error case
- All custom exceptions must extend `RuntimeException`
- One `@RestControllerAdvice` class per service handles all exceptions globally
- Always return a consistent error response with: `timestamp`, `status`, `message`, `path`
- Never expose stack traces or internal details in API responses

```java
// ✅ Custom exception
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Task not found with id: " + id);
    }
}

// ✅ Consistent error response
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String message,
    String path
) {}

// ✅ Global handler
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(
            TaskNotFoundException ex, HttpServletRequest request) {
        log.warn("Task not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                LocalDateTime.now(), 404, ex.getMessage(), request.getRequestURI()
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(LocalDateTime.now(), 400, message, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                LocalDateTime.now(), 500, "An unexpected error occurred", request.getRequestURI()
            ));
    }
}
```

---

## Repository Rules

Repositories are purely data access — no logic.

**Rules:**
- Only query methods in repositories — no business logic whatsoever
- Use Spring Data JPA method naming for simple queries
- Use `@Query` with JPQL for complex queries — avoid native SQL unless necessary
- Always return `Optional<T>` for single-entity lookups
- Always use `Pageable` for queries that can return large result sets

```java
// ✅ CORRECT
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndAssigneeId(Long id, Long assigneeId);

    Page<Task> findAllByStatus(TaskStatus status, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.dueDate < :now AND t.status != :status")
    List<Task> findOverdueTasks(LocalDateTime now, TaskStatus status);
}
```

---

## Lombok Rules

| Use | Annotation | Never use |
|-----|-----------|-----------|
| Dependency injection | `@RequiredArgsConstructor` | `@Autowired` |
| Logging | `@Slf4j` | Manual logger instantiation |
| Entities | `@Getter @Setter @NoArgsConstructor` | `@Data` on entities |
| DTOs | Use Java Records instead | `@Data` |
| Immutable value objects | `@Value` | — |

---

## Logging Rules

Use `@Slf4j` and log at the **correct level**:

| Level | When to use | Example |
|-------|-------------|---------|
| `ERROR` | Unexpected system failures | Database connection lost |
| `WARN` | Recoverable issues | Retry attempt 2 of 3 |
| `INFO` | Significant business events | User registered, task created |
| `DEBUG` | Detailed flow info (off in prod) | Entering method with params |

```java
// ✅ CORRECT
log.info("Task created with id: {}", task.getId());
log.warn("Retry attempt {} for task notification", retryCount);
log.error("Failed to send notification for task id: {}", taskId, exception);

// ❌ WRONG — never log sensitive data
log.info("User logged in with password: {}", password);
log.debug("Token value: {}", jwtToken);
```

---

## Testing Rules

| Test type | Tool | Scope |
|-----------|------|-------|
| Unit tests | JUnit 5 + Mockito | Service layer, utility classes |
| Integration tests | @SpringBootTest + Testcontainers | Controller layer, full flow |

**Rules:**
- Every `ServiceImpl` class must have a corresponding unit test class
- Mock all dependencies with Mockito in unit tests — never use real beans
- Test method naming: `should_[expectedBehavior]_when_[condition]`
- Minimum coverage: **70%** on service and controller layers

```java
// ✅ CORRECT test naming and structure
@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void should_returnTaskResponse_when_taskExists() {
        Task task = new Task();
        TaskResponse expected = new TaskResponse(1L, "Test", null, TaskStatus.TODO, Priority.HIGH, null, null, null, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(expected);

        TaskResponse result = taskService.findById(1L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void should_throwTaskNotFoundException_when_taskDoesNotExist() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.findById(999L));
    }
}
```

---

## Spring Security Rules

- JWT validation happens at the **API Gateway level only**
- Individual microservices trust the gateway and extract user info from forwarded headers
- Always use `BCryptPasswordEncoder` for password hashing — never store plain text passwords
- Never disable CSRF without a comment explaining why and a linked GitHub Issue

---

## RabbitMQ Rules

- Define all queue names, exchange names, and routing keys as **constants** in a dedicated `RabbitMQConstants` class
- Producers live in the `messaging` package
- Consumers live in the `messaging` package and are annotated with `@RabbitListener`
- Always configure a **Dead Letter Queue (DLQ)** for every main queue
- Message payload classes must be serializable — use Records or immutable classes

```java
// ✅ Constants class
public final class RabbitMQConstants {
    private RabbitMQConstants() {}

    public static final String TASK_EXCHANGE = "task.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_DLQ = "notification.queue.dlq";
    public static final String TASK_CREATED_ROUTING_KEY = "task.created";
    public static final String TASK_ASSIGNED_ROUTING_KEY = "task.assigned";
}
```
