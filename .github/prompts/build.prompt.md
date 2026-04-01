---
name: "Build Project"
description: "Build this Spring Boot Gradle project with the Windows Gradle wrapper"
agent: "agent"
---

Build the current workspace using the Gradle wrapper on Windows.

Use this command from the repository root:

```powershell
.\gradlew.bat clean build
```

Requirements:
- Treat this as a build-only task unless I explicitly ask for code changes.
- Report whether compilation, tests, and jar packaging succeeded.
- If the build fails, summarize the first actionable error and the failing Gradle task.
- If the failure looks environment-related, mention the likely cause.

Project-specific notes:
- Prefer the Gradle wrapper over a system Gradle install.
- The Java toolchain is defined in [build.gradle](./build.gradle) and currently requires Java 24.
- Local runtime steps are described in [README.md](./README.md), but `/build` should only run the build command unless I ask for app startup.