# springboot-playground

## Prerequisites
- Java 21
- Docker (for local Postgres)

## Step-by-step: run locally
1) Start Postgres with Docker Compose:
```powershell
docker compose up -d
```

2) Run the Spring Boot app:
```powershell
./gradlew bootRun
```

3) Verify the API is up:
- Open `requests.http` in your IDE (IntelliJ/VS Code HTTP client) and run the requests.
- Or hit `http://localhost:8080/api/products` in your browser or with curl.

## Stop services
- Stop the app (Ctrl+C)
- Stop Postgres:
```powershell
docker compose down
```

## Notes
- Database connection settings live in `src/main/resources/application.properties`.
- Migrations and seed data are in `src/main/resources/db/migration`.
