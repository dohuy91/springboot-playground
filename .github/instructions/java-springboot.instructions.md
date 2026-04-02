---
applyTo: "**/*.java,**/*.gradle,**/*.properties,**/*.yml,**/*.yaml"
---

# Spring Boot Best Practices

Use these standards when generating or editing Spring Boot code in this repository.

## Project and Structure

- Use Gradle and Spring Boot starters for dependency management.
- Organize packages by feature/domain rather than by layer.
- Keep code aligned with the existing module boundaries (`src`, `libraries/common`, `libraries/message-queue`).

## Dependency Injection and Components

- Prefer constructor injection for required dependencies.
- Keep dependency fields `private final`.
- Use Spring stereotypes appropriately (`@Service`, `@Repository`, `@RestController`, `@ControllerAdvice`).

## Configuration

- Keep configuration externalized in `application.properties` or profile-specific files.
- Use type-safe configuration with `@ConfigurationProperties` when values are grouped.
- Do not hardcode secrets.

## API and Web Layer

- Design clear RESTful endpoints.
- Use DTOs for request/response contracts; do not expose entities directly.
- Use Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.) for input validation.
- Follow centralized error handling with `@ControllerAdvice`.
- Reuse `GlobalExceptionHandler` conventions in `libraries/common/src/main/java/com/springbootplayground/common/web/GlobalExceptionHandler.java`.

## Service Layer

- Keep business logic in stateless `@Service` classes.
- Use `@Transactional` at the appropriate method/class granularity.

## Data Layer

- Use Spring Data repositories for standard CRUD.
- Use custom queries only when needed.
- Use projections/DTO queries for read optimization where useful.

## Logging

- Use SLF4J (`Logger`/`LoggerFactory`).
- Prefer parameterized logs over string concatenation.

## Testing

- Write unit tests with JUnit 5 and Mockito.
- Use test slices (`@WebMvcTest`, `@DataJpaTest`) for focused tests.
- Use `@SpringBootTest` only for integration scenarios requiring full context.

## Security

- Follow secure defaults and validate/sanitize untrusted input.
- Avoid introducing insecure patterns in authentication, authorization, or data access.

