# springboot-playground

## Prerequisites
- Java 21
- Docker (for local Postgres, Kafka, and Keycloak)

## Step-by-step: run locally
1) Start Postgres with Docker Compose:
```powershell
docker compose up -d
```

2) Run schema migration only (no test data):
```powershell
./gradlew bootRun
```

3) Run schema migration + test data:
```powershell
./gradlew bootRun --args='--spring.profiles.active=dev'
```

4) Get an access token from Keycloak:
```bash
curl --request POST "http://localhost:8081/realms/playground/protocol/openid-connect/token" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=springboot-playground-api" \
  --data-urlencode "username=playground-user" \
  --data-urlencode "password=playground"
```

5) Verify the API is up (use Bearer token from step 4):
- Open `requests.http` in your IDE (IntelliJ/VS Code HTTP client) and run the requests.
- Or call `http://localhost:8080/api/products` with `Authorization: Bearer <access_token>`.

## Migration behavior
- Flyway runs automatically when the application starts.
- Default profile uses only `src/main/resources/db/migration`:
	- `V1__create_products.sql` (schema)
- `dev` profile uses both `src/main/resources/db/migration` and `src/main/resources/db/dev`:
	- `V1__create_products.sql` (schema)
	- `V2__seed_products.sql` (test data)

If you need to re-run migrations from scratch, drop/recreate the database (or clear Flyway history) and start the app again.

## Stop services
- Stop the app (Ctrl+C)
- Stop Postgres:
```powershell
docker compose down
```

## Notes
- Database connection settings live in `src/main/resources/application.properties`.
- Profile-specific Flyway settings are in `src/main/resources/application-dev.properties`.
- Local Keycloak realm bootstrap is in `keycloak-realm-playground.json`.
