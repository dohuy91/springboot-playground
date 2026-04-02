# Repository Copilot Instructions

Use Spring Boot best practices for all backend work in this repository.

- Apply the detailed rules in `.github/instructions/java-springboot.instructions.md` when generating or editing Java/Spring code.
- Keep API error handling consistent with `libraries/common/src/main/java/com/springbootplayground/common/web/GlobalExceptionHandler.java`.
- Prefer extending the existing `BusinessException` hierarchy in `libraries/common/src/main/java/com/springbootplayground/common/exception` instead of introducing ad-hoc exception responses.
- Keep package structure feature-oriented (for example `product`, `common`, `message-queue`).

