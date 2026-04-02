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

Follow these patterns (also enforced by SonarQube rules `java:S100`, `java:S101`, `java:S116`, `java:S117`, `java:S119`, `java:S120`):

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

- Class names: UpperCamelCase (`java:S101`)
- Method names: lowerCamelCase (`java:S100`)
- Field / variable / parameter names: lowerCamelCase (`java:S116`, `java:S117`)
- Package names: all lowercase, no underscores (`java:S120`)
- Type parameters: single uppercase letter, e.g. `T`, `E` (`java:S119`)
- Do **not** use restricted identifiers (`record`, `sealed`, `permits`, `var`, `yield`) as any identifier (`java:S6213`)

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
- Test class names must end with `Test` (or `Tests`, `IT`) (`java:S3577`).
- Every test method must contain at least one assertion (`java:S2699`).
- Use `assertThrows` (JUnit 5) instead of `@Test(expected = …)` (`java:S5785`).
- Pass `expected` first and `actual` second in `assertEquals` (`java:S2698`).
- Use `SystemClockProvider` with a fixed `Clock` for deterministic time in tests.

---

## Logging

- Use SLF4J via Lombok `@Slf4j`. Never use `System.out` / `System.err` (`java:S106`).
- Always use parameterized log statements: `log.info("Created product: {}", product.getId())`. (`java:S1643`)
- Never concatenate strings in log calls.
- Never call `Throwable.printStackTrace()` — log with SLF4J instead (`java:S4925`).

---

## Code Quality (SonarQube)

All generated and modified Java code must comply with SonarQube rules. The full rule list is maintained in `.github/instructions/java-springboot.instructions.md`. Key rules by category are summarised below.

### Bugs & Reliability
| Rule | Summary |
|------|---------|
| `java:S2259` | Do not dereference potentially-null values without a null check |
| `java:S1764` | Do not use identical expressions on both sides of a binary operator |
| `java:S2142` | Do not silently swallow `InterruptedException`; re-interrupt or rethrow |
| `java:S2093` | Use try-with-resources to close `AutoCloseable` resources |
| `java:S1854` | Remove unused assignments; every assigned value must be read before being overwritten |
| `java:S1871` | Two branches in the same conditional must not have identical implementations |

### Code Smells & Maintainability
| Rule | Summary |
|------|---------|
| `java:S112` | Never throw generic exceptions (`Exception`, `RuntimeException`, `Throwable`) |
| `java:S1168` | Return empty collections/arrays instead of `null` |
| `java:S1192` | Extract duplicated string literals to constants |
| `java:S3740` | Never use raw generic types; always supply type parameters |
| `java:S4144` | Methods must not have identical implementations — extract shared logic |
| `java:S1128` | Remove unused import statements |
| `java:S1481` | Remove unused local variables |
| `java:S1068` | Remove unused private fields |
| `java:S1155` | Use `Collection.isEmpty()` instead of `size() == 0` |
| `java:S2293` | Use the diamond operator `<>` instead of repeating explicit type arguments |
| `java:S1602` | Single-statement lambda bodies must not use a surrounding `{}` block |
| `java:S5361` | Use `String.replace` (not `replaceAll`) when the first argument is a plain literal |

### Security & Vulnerabilities
| Rule | Summary |
|------|---------|
| `java:S6437` | Never hardcode credentials or API keys in source code |
| `java:S2755` | Disable external entity processing in XML parsers (XXE) |
| `java:S4790` | Do not use MD5 or SHA-1 for security-sensitive hashing |
| `java:S5542` | Use a secure cipher mode and padding (e.g. AES/GCM/NoPadding); avoid ECB |
| `java:S5344` | Never store passwords in plaintext; use bcrypt / Argon2 |

### Performance
| Rule | Summary |
|------|---------|
| `java:S1643` | Use `StringBuilder` instead of `+` inside loops |
| `java:S3824` | Prefer `Map.computeIfAbsent` over `containsKey` + `put` |
| `java:S4838` | Use `EnumSet` / `EnumMap` when keys are enum values |

> For the complete rule set, see `.github/instructions/java-springboot.instructions.md § SonarQube Java Rules`.

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