# AGENTS.md — Agent Guidelines

> **Read [CLAUDE.md](./CLAUDE.md) in full before making any changes.** Every section below maps directly to a section in CLAUDE.md. When in doubt, CLAUDE.md is the authority.

---

## Start Here

1. Read `CLAUDE.md` fully.
2. Read the source files relevant to your task — never assume structure from memory.
3. Run `./gradlew test` to confirm the baseline passes before making changes.

---

## Project Overview → [CLAUDE.md § Project Overview](./CLAUDE.md#project-overview)

- Spring Boot **4.0.2**, Java **24**, Gradle **8.14**.
- Infrastructure: PostgreSQL 17.4 on port **6432**, Kafka (KRaft) on port **9092**, app on port **8080**.
- All infrastructure is provided via `docker-compose.yml` — start it before running the app.

---

## Module Structure → [CLAUDE.md § Module Structure](./CLAUDE.md#module-structure)

Three modules exist. Put new code in the right one:

| Task | Module |
|------|--------|
| New domain feature (entity, service, REST API) | `src/` |
| New shared exception type | `libraries/common` |
| Change global error response format | `libraries/common` → `GlobalExceptionHandler` |
| New Kafka producer/consumer config | `libraries/message-queue` |
| New message persistence or retry logic | `libraries/message-queue` → `QueueMessageService` |
| New messaging admin endpoint | `libraries/message-queue` → `QueueMessageController` |

Before adding code to a module, trace the existing layer chain for that domain:
`entity/ → repository/ → service/ → web/`

---

## Build & Run Commands → [CLAUDE.md § Build & Run Commands](./CLAUDE.md#build--run-commands)

```bash
docker compose up -d                                              # start Postgres + Kafka
./gradlew bootRun                                                 # run app
./gradlew bootRun --args='--spring.profiles.active=dev'          # run with seed data
./gradlew test                                                    # run all tests
./gradlew build                                                   # build all modules
docker compose down                                               # stop infrastructure
```

Always run `./gradlew test` after making changes.

---

## Package & Naming Conventions → [CLAUDE.md § Package & Naming Conventions](./CLAUDE.md#package--naming-conventions)

Packages are organized by **domain/feature first**, not by technical layer.

New classes go in: `com.springbootplayground.<domain>.<sublayer>`

Examples:
- A new order entity → `com.springbootplayground.order.entity.Order`
- A new order controller → `com.springbootplayground.order.web.OrderController`

Follow the class naming table from CLAUDE.md exactly:

| Type | Pattern | Example |
|------|---------|---------|
| Entity | `<Domain>` | `Product`, `QueueMessage` |
| Repository | `<Domain>Repository` | `ProductRepository` |
| Service | `<Domain>Service` | `ProductService` |
| Controller | `<Domain>Controller` | `ProductController` |
| Config | `<Feature>Config` | `KafkaProducerConfig` |
| Listener | `<Domain>EventListener` | `ProductEventListener` |
| Event/DTO | `<Domain><Action>Event` / `<Domain>Request` | `ProductCreatedEvent`, `ProductRequest` |
| Exception | `<Domain><Reason>Exception` | `ProductNotFoundException` |
| Properties | `<Feature>Properties` | `KafkaProperties` |

---

## Architectural Patterns → [CLAUDE.md § Architectural Patterns](./CLAUDE.md#architectural-patterns)

### Dependency Injection
- **Always** use constructor injection with `@RequiredArgsConstructor`.
- All injected fields must be `private final`.
- **Never** use `@Autowired` on fields.

### Exception Handling
- New domain exceptions must extend `NotFoundException` or `AlreadyExistsException` from `libraries/common`.
- `GlobalExceptionHandler` auto-maps these to HTTP 404/409 — do **not** add `@ResponseStatus` or duplicate handler methods.
- All error payloads use the `ErrorResponse` record.

### Clock & Time
- **Never** call `LocalDateTime.now()` or `Instant.now()` directly.
- Inject `ClockProvider` (from `libraries/common`) and use `clockProvider.getClock()`.
- JPA auditing timestamps (`created_at`, `updated_at`) are managed automatically via `@CreatedDate` / `@LastModifiedDate` + `JpaAuditingConfig` — do not set them manually.

### Kafka Messaging
- Producers publish via `KafkaProducerService` and record outcomes via `QueueMessageService`.
- Consumers use `@KafkaListener` and record success/failure via `QueueMessageService`.
- Message storage behavior is controlled by `app.kafka.storage-mode` (`ALL` or `FAILED_ONLY`).
- Failed messages are retried via the Spring Batch job `kafkaRetryJob` — triggered manually at `POST /api/admin/kafka/retry`.
- Message recording transactions must use `Propagation.REQUIRES_NEW` to stay isolated from the business transaction.

### DTOs
- Request/response objects are Java **records** with Bean Validation annotations on components (`@NotBlank`, `@Size`, `@DecimalMin`, `@Min`, `@NotNull`).
- **Never** expose JPA entities from controller return types or method parameters.

### Configuration
- All configuration goes in `application.properties` or profile-specific files (`application-dev.properties`).
- Grouped properties are bound with `@ConfigurationProperties`.
- **No** hardcoded secrets, URLs, or environment-specific values in source code.

---

## Data Layer → [CLAUDE.md § Data Layer](./CLAUDE.md#data-layer)

- Use Spring Data repositories for standard CRUD. Add custom `@Query` methods only when needed.
- Schema is managed **exclusively** by Flyway migrations in `src/main/resources/db/migration/`.
- **Never** modify an existing migration file — always create a new versioned one.
- `spring.jpa.hibernate.ddl-auto` must remain `none`.
- Dev-only seed data belongs in `src/main/resources/db/dev/` and is activated by the `dev` profile.

### JPA Entities
- Use `BIGSERIAL` surrogate PKs.
- Every audited entity must have `@EntityListeners(AuditingEntityListener.class)`.
- Auditing fields (`created_at`, `updated_at`) use `@CreatedDate` / `@LastModifiedDate`.

---

## Testing → [CLAUDE.md § Testing](./CLAUDE.md#testing)

- **Unit tests**: `@ExtendWith(MockitoExtension.class)` + `@Mock` for dependencies.
- **Controller tests**: `@WebMvcTest` slice.
- **Repository tests**: `@DataJpaTest` slice.
- **Integration tests**: `@SpringBootTest` only when a full context is required.
- Test classes mirror source package structure under `src/test/java/`.
- For time-sensitive logic, inject a fixed `Clock` via `SystemClockProvider` — never depend on wall-clock time.
- Add a unit test for every new service method.

---

## Logging → [CLAUDE.md § Logging](./CLAUDE.md#logging)

- Use SLF4J via `@Slf4j` (Lombok).
- Always use parameterized statements: `log.info("Created product: {}", id)`.
- **Never** concatenate strings in log calls.

---

## Git Hooks → [CLAUDE.md § Git Hooks](./CLAUDE.md#git-hooks)

- `prepare-commit-msg`: auto-generates commit messages from staged diff when the message is empty.
- `pre-push`: blocks push if any commit lacks a message.
- **Never** bypass hooks with `--no-verify`.
- **Do not** commit to `main` or create PRs without explicit user instruction.

---

## Key Configuration Properties → [CLAUDE.md § Key Configuration Properties](./CLAUDE.md#key-configuration-properties)

| Property | Default | Description |
|----------|---------|-------------|
| `app.kafka.storage-mode` | `FAILED_ONLY` | `ALL` or `FAILED_ONLY` |
| `app.kafka.retry.max-retries` | `3` | Retries before `EXHAUSTED` |
| `app.kafka.retry.backoff-seconds` | `30` | Seconds between retries |
| `spring.batch.job.enabled` | `false` | Job is manual-only |
| `spring.jpa.open-in-view` | `false` | OSIV disabled — do not rely on it |

---

## Infrastructure Ports → [CLAUDE.md § Infrastructure Ports](./CLAUDE.md#infrastructure-ports)

| Service | Port |
|---------|------|
| Application | 8080 |
| PostgreSQL | 6432 |
| Kafka | 9092 |

---

## Hard Rules — What Agents Must Never Do

- Do not modify existing Flyway migration files.
- Do not set `spring.jpa.hibernate.ddl-auto` to anything other than `none`.
- Do not expose JPA entities from controllers.
- Do not use `@Autowired` field injection.
- Do not call `LocalDateTime.now()` or `Instant.now()` in production code.
- Do not add `@ResponseStatus` or duplicate exception mappings already covered by `GlobalExceptionHandler`.
- Do not hardcode secrets, ports, or environment-specific values in source files.
- Do not rely on OSIV (`spring.jpa.open-in-view=false`).
- Do not bypass git hooks with `--no-verify`.
- Do not push to `main` or open PRs without explicit user instruction.
- Do not add Spring Security unless explicitly requested.