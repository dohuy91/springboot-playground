# CLAUDE.md — Spring Boot Playground

This file is the primary reference for AI agents and contributors working in this repository. It describes the project structure, conventions, build commands, and architectural decisions that must be followed when generating or modifying code.

---

## Project Overview

A Spring Boot monorepo demonstrating a product management REST API with Kafka event-driven messaging, message persistence, and batch retry. Intended as a learning playground with production-quality patterns.

- **Spring Boot**: 4.0.2
- **Java**: 24 (Gradle toolchain)
- **Build**: Gradle 8.14
- **Database**: PostgreSQL 17.4 (Flyway migrations, port 6432)
- **Messaging**: Apache Kafka (KRaft, port 9092)
- **API port**: 8080

---

## Module Structure

```
springboot-playground/
├── src/                          # Main application — product domain
├── libraries/
│   ├── common/                   # Shared exception hierarchy, clock abstraction, global error handler
│   └── message-queue/            # Kafka producer/consumer configs, message persistence, batch retry
├── docker-compose.yml            # PostgreSQL + Kafka
├── requests.http                 # HTTP client test requests
└── .github/instructions/         # Copilot/Cursor instruction files
```

### Module Responsibilities

| Module | Purpose |
|--------|---------|
| `src/` (root app) | REST CRUD for products, Kafka producer on create, Kafka consumer listener |
| `libraries:common` | `BusinessException` hierarchy, `GlobalExceptionHandler`, `ClockProvider` interface, JPA auditing config |
| `libraries:message-queue` | `QueueMessage` entity/repo, `QueueMessageService`, Kafka producer/consumer config, Spring Batch retry job, admin REST endpoint |

---

## Build & Run Commands

```bash
# Start infrastructure (Postgres + Kafka)
docker compose up -d

# Run application (no dev seed data)
./gradlew bootRun

# Run with dev profile (Flyway seeds test products)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run tests
./gradlew test

# Build all modules
./gradlew build

# Stop infrastructure
docker compose down
```

---

## Package & Naming Conventions

### Package Structure — Feature/Domain First

Packages are organized by **domain/feature**, not by technical layer.

```
com.springbootplayground.
├── common/
│   ├── clock/          # ClockProvider interface, SystemClockProvider
│   ├── config/         # ClockConfig, JpaAuditingConfig
│   ├── exception/      # BusinessException, NotFoundException, AlreadyExistsException
│   └── web/            # GlobalExceptionHandler, ErrorResponse
├── product/
│   ├── entity/         # Product (JPA entity)
│   ├── repository/     # ProductRepository
│   ├── service/        # ProductService
│   ├── event/          # KafkaProducerService, ProductEventListener, ProductCreatedEvent
│   ├── exception/      # ProductNotFoundException
│   └── web/            # ProductController, ProductRequest (record)
└── messaging/          # (in libraries/message-queue)
    ├── entity/         # QueueMessage, MessageStatus, SourceType, StorageMode
    ├── repository/     # QueueMessageRepository
    ├── service/        # QueueMessageService
    ├── config/         # KafkaProducerConfig, KafkaConsumerConfig, KafkaProperties
    ├── batch/          # KafkaRetryJobConfig, KafkaRetryItemProcessor
    └── web/            # QueueMessageController
```

### Class Naming

| Type | Pattern | Example |
|------|---------|---------|
| Entity | `<Domain>` | `Product`, `QueueMessage` |
| Repository | `<Domain>Repository` | `ProductRepository` |
| Service | `<Domain>Service` | `ProductService`, `QueueMessageService` |
| Controller | `<Domain>Controller` | `ProductController` |
| Config | `<Feature>Config` | `KafkaProducerConfig`, `ClockConfig` |
| Listener | `<Domain>EventListener` | `ProductEventListener` |
| Event/DTO | `<Domain><Action>Event` / `<Domain>Request` | `ProductCreatedEvent`, `ProductRequest` |
| Exception | `<Domain><Reason>Exception` | `ProductNotFoundException` |
| Properties | `<Feature>Properties` | `KafkaProperties` |

---

## Architectural Patterns

### Dependency Injection
- Always use **constructor injection** with `@RequiredArgsConstructor` (Lombok).
- Fields must be `private final`.
- Never use field injection (`@Autowired` on fields).

### Exception Handling
- Domain exceptions extend `NotFoundException` or `AlreadyExistsException` from `libraries/common`.
- `GlobalExceptionHandler` maps these to HTTP 404/409 automatically — do not add `@ResponseStatus` or duplicate mappings.
- Use `ErrorResponse` record for all error payloads.

### Clock & Time
- Never call `LocalDateTime.now()` or `Instant.now()` directly.
- Inject `ClockProvider` and call `clockProvider.getClock()` for testable time.
- JPA auditing uses `@CreatedDate` / `@LastModifiedDate` — wired through `JpaAuditingConfig`.

### Kafka Messaging
- Producer publishes via `KafkaProducerService`; records outcomes to `QueueMessage` via `QueueMessageService`.
- Consumer listens with `@KafkaListener`; records success/failure via `QueueMessageService`.
- `storageMode` (`app.kafka.storage-mode`) controls whether all messages or only failures are persisted.
- Failed messages are retried by the Spring Batch job (`kafkaRetryJob`) — triggered manually via `POST /api/admin/kafka/retry`.
- Use `Propagation.REQUIRES_NEW` for message recording to isolate the audit transaction.

### DTOs
- Use Java `record` types for immutable request/response DTOs.
- Apply Bean Validation directly on record components (`@NotBlank`, `@Size`, `@DecimalMin`, etc.).
- Never expose JPA entities from controller methods.

### Configuration
- Externalize all configuration to `application.properties` (or profile-specific variants).
- Group related properties under a prefix and bind with `@ConfigurationProperties`.
- No hardcoded secrets, URLs, or environment-specific values in source code.

---

## Data Layer

- Use Spring Data repositories for standard CRUD.
- Add custom `@Query` methods only when derived query names become unwieldy.
- Database schema managed exclusively by **Flyway** migrations in `src/main/resources/db/migration/`.
- Never set `spring.jpa.hibernate.ddl-auto` to anything other than `none`.
- Dev-only seed data goes in `src/main/resources/db/dev/` and activates under the `dev` profile.

### JPA Entities
- All entities use `BIGSERIAL` surrogate PKs.
- Auditing fields (`created_at`, `updated_at`) managed by `@CreatedDate` / `@LastModifiedDate`.
- Enable `@EntityListeners(AuditingEntityListener.class)` on every audited entity.

---

## Testing

- **Unit tests**: JUnit 5 + Mockito. Use `@ExtendWith(MockitoExtension.class)`.
- **Slice tests**: Use `@WebMvcTest` for controllers, `@DataJpaTest` for repositories.
- **Integration tests**: Use `@SpringBootTest` only when full context is required.
- Test classes mirror the source package structure under `src/test/java/`.
- Use `SystemClockProvider` with a fixed `Clock` for deterministic time in tests.

---

## Logging

- Use SLF4J via Lombok `@Slf4j`.
- Always use parameterized log statements: `log.info("Created product: {}", product.getId())`.
- Never concatenate strings in log calls.

---

## Git Hooks

This repo uses automated `.githooks`:
- **prepare-commit-msg**: Auto-generates commit messages from staged diff if the message is empty.
- **pre-push**: Validates that all commits have non-empty messages before pushing.

Hooks run automatically when using `git commit` / `git push` after setup. Do not bypass with `--no-verify`.

---

## Key Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.kafka.storage-mode` | `FAILED_ONLY` | `ALL` stores every message; `FAILED_ONLY` stores only failures |
| `app.kafka.retry.max-retries` | `3` | Max retry attempts before marking `EXHAUSTED` |
| `app.kafka.retry.backoff-seconds` | `30` | Seconds between retry attempts |
| `spring.batch.job.enabled` | `false` | Retry job runs only on manual trigger |
| `spring.jpa.open-in-view` | `false` | Disabled — never rely on OSIV |

---

## Infrastructure Ports

| Service | Port |
|---------|------|
| Application | 8080 |
| PostgreSQL | 6432 |
| Kafka | 9092 |