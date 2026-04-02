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

## SonarQube Java Rules

Apply the rules below when generating or editing Java code. Each rule references the corresponding SonarQube rule ID.

### Naming & Identifiers
- **`java:S6213`** — Do not use restricted identifiers (`record`, `sealed`, `permits`, `var`, `yield`) as class, method, field, or variable names.
- **`java:S100`** — Method names must follow lowerCamelCase convention.
- **`java:S101`** — Class names must follow UpperCamelCase convention.
- **`java:S116`** — Field names must follow lowerCamelCase convention.
- **`java:S117`** — Local variable and parameter names must follow lowerCamelCase convention.
- **`java:S119`** — Type parameter names should be a single uppercase letter (e.g. `T`, `E`, `K`, `V`).
- **`java:S120`** — Package names must be all lowercase with no underscores.
- **`java:S3008`** — Static non-final field names must follow lowerCamelCase (not UPPER_SNAKE_CASE).
- **`java:S1700`** — A field name should not duplicate the name of the enclosing class.

### Bugs & Reliability
- **`java:S1764`** — Do not use identical expressions on both sides of a binary operator (`a == a`, `x && x`).
- **`java:S2259`** — Do not dereference values that may be `null` without a null check.
- **`java:S2583`** — Conditions that always evaluate to the same boolean value should be removed.
- **`java:S2589`** — Boolean expressions should not be gratuitous (e.g., `if (b == true)`).
- **`java:S1862`** — Related `if/else if` chains must not repeat the same condition.
- **`java:S3923`** — All branches of a conditional structure must not have exactly the same implementation.
- **`java:S1854`** — Remove unused assignments; every assigned value must be read before being overwritten or discarded.
- **`java:S2119`** — Reuse `Random` / `SecureRandom` instances rather than creating a new one per call.
- **`java:S2142`** — Do not silently ignore `InterruptedException`; either re-interrupt or rethrow.
- **`java:S2093`** — Use try-with-resources instead of manual `finally` blocks to close `AutoCloseable` resources.
- **`java:S1751`** — Loops with at most one iteration should be refactored or replaced.
- **`java:S2637`** — Fields or variables annotated `@NonNull` must not be assigned `null`.
- **`java:S3626`** — Redundant jump statements (`break`, `continue`, `return`) should be removed.
- **`java:S1871`** — Two branches in the same conditional structure must not have exactly the same implementation.
- **`java:S2175`** — Calls to `Collection` methods with incompatible types (e.g., `contains(int)` on `List<String>`) must be avoided.

### Code Smells & Maintainability
- **`java:S106`** — Do not use `System.out` or `System.err` for logging; use SLF4J.
- **`java:S112`** — Do not throw generic exceptions (`Exception`, `RuntimeException`, `Throwable`, `Error`); use a specific type.
- **`java:S1066`** — Collapse nested `if` statements into a single condition where possible.
- **`java:S1125`** — Remove unnecessary boolean literals in expressions (e.g., `foo == true` → `foo`).
- **`java:S1128`** — Remove unused import statements.
- **`java:S1155`** — Use `Collection.isEmpty()` instead of `collection.size() == 0`.
- **`java:S1168`** — Return empty arrays or collections instead of `null`.
- **`java:S1172`** — Remove unused method parameters (or document intentional non-use).
- **`java:S1481`** — Remove unused local variables.
- **`java:S1068`** — Remove unused private fields.
- **`java:S1192`** — Do not duplicate string literals; extract repeated strings to a constant.
- **`java:S1643`** — Do not concatenate `String` with `+` inside a loop; use `StringBuilder`.
- **`java:S1166`** — Exception handlers must preserve the original exception (pass it as cause or rethrow).
- **`java:S2293`** — Use the diamond operator `<>` instead of repeating explicit generic type arguments.
- **`java:S3740`** — Do not use raw generic types; always provide type parameters.
- **`java:S4925`** — Do not call `Throwable.printStackTrace()`; log with SLF4J instead.
- **`java:S4144`** — Methods must not have identical implementations; extract duplicated logic.
- **`java:S1452`** — Do not use wildcard types (`?`) in method return types.
- **`java:S1602`** — Lambda bodies containing only a single statement must not wrap it in a block `{}`.
- **`java:S1659`** — Declare each variable on its own line; do not declare multiple variables in one statement.
- **`java:S5361`** — Do not use `String.replaceAll` when the first argument is a plain literal, not a regex; use `String.replace` instead.
- **`java:S6541`** — Methods must not be too complex (cyclomatic complexity); break large methods into smaller ones.
- **`java:S1133`** — Deprecated API elements must have a migration path and should be removed in a timely manner.
- **`java:S1123`** — Deprecated elements must have both the `@Deprecated` annotation and the `@deprecated` Javadoc tag.
- **`java:S1149`** — Do not use synchronized legacy collections (`Vector`, `Hashtable`, `Stack`, `StringBuffer`); use modern alternatives.
- **`java:S1450`** — Private fields used only within a single method should be converted to local variables.

### Security & Vulnerabilities
- **`java:S6437`** — Do not hardcode credentials (passwords, tokens, API keys) in source code.
- **`java:S2076`** — OS commands must not be built from unsanitized user input (command injection).
- **`java:S2083`** — File paths derived from user input must be sanitized to prevent path traversal.
- **`java:S2755`** — XML parsers must disable external entity processing to prevent XXE attacks.
- **`java:S4790`** — Do not use weak hashing algorithms (MD5, SHA-1) for security-sensitive purposes; use SHA-256 or stronger.
- **`java:S5344`** — Passwords must not be stored in plaintext; use a strong adaptive hashing function (bcrypt, Argon2).
- **`java:S5542`** — Encryption algorithms must use a secure mode and padding (e.g., AES/GCM/NoPadding); avoid ECB mode.
- **`java:S5547`** — Cipher algorithms must be robust; do not use DES, RC2, RC4, or Blowfish.
- **`java:S3752`** — HTTP endpoints that perform state-changing operations must be protected against CSRF.
- **`java:S2278`** — Do not use DES or 3DES; use AES.

### Performance
- **`java:S1643`** *(see Maintainability)* — Use `StringBuilder` instead of `+` in loops.
- **`java:S2119`** *(see Reliability)* — Reuse `Random` instances.
- **`java:S3824`** — Use `Map.computeIfAbsent` instead of a `containsKey` + `put` pattern.
- **`java:S4838`** — Use `EnumSet` / `EnumMap` instead of general `Set` / `Map` when keys are enum values.

### Testing
- **`java:S2698`** — In JUnit assertions, pass the expected value first and the actual value second: `assertEquals(expected, actual)`.
- **`java:S5785`** — Use `assertThrows` (JUnit 5) rather than `@Test(expected = ...)` or manual try/catch in tests.
- **`java:S2699`** — Test methods must contain at least one assertion.
- **`java:S3577`** — Test class names must end with `Test` (or `Tests`, `IT`, `ITCase`).

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
- See the **SonarQube Java Rules → Security & Vulnerabilities** section above for specific rules that must be followed.

