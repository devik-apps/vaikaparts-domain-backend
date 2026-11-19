# VaikaParts Domain Backend

The Domain Backend is the Spring Boot service that owns VaikaParts business rules and data. It serves the web application, coordinates contact unlocks with Pecunia, and publishes asynchronous events through RabbitMQ.

## What the service does

- Manages researchers, sellers, managers, spare-part demands, offers, statuses, and paginated results.
- Handles contact unlocks through PAPI (MVola, Airtel Money, Orange Money), manual Orange Money review, and the `FREE` provider when `ENABLE_CONTACT_PAYMENT=false`.
- Stores domain data in PostgreSQL and evolves the schema with Flyway migrations.
- Integrates Supabase JWT authentication, S3-compatible object storage, RabbitMQ, SMTP, Befiana SMS, Pecunia, and web push.
- Sends notifications through in-app/WebSocket, email, SMS, and Web Push, subject to server configuration and each user's preferences.

Pecunia owns payment-provider integration. Domain reserves and tracks the contact unlock, calls Pecunia for PAPI payment operations, and releases the contact after a successful payment or the applicable release event.

## Architecture and code map

This is a layered Spring Boot application, not a set of independently deployed domain microservices.

| Area | Location | Responsibility |
| --- | --- | --- |
| REST controllers | `src/main/java/com/devikapps/vaikaparts/endpoint/` | HTTP endpoints, request validation, and response mapping |
| Services | `.../service/` | Business rules and orchestration |
| Repositories and persistence entities | `.../repository/` | Database access and JPA entities |
| Domain models | `.../model/` | Business objects and enums |
| Mappers | `.../mapper/` | Persistence/API/domain conversions |
| Events | `.../event/` and `.../service/outbox/` | RabbitMQ events, outbox publishing, and consumers |
| API contract | `doc/api.yaml` | OpenAPI specification used to generate clients |
| Database migrations | `src/main/resources/db/migration/` | Append-only Flyway schema changes |

PostgreSQL stores the application data. RabbitMQ carries asynchronous work; the outbox makes event publication recoverable. Supabase supplies authentication identities. S3-compatible storage holds uploaded media. Pecunia handles payment-provider operations.

## Naming and change conventions

- Java packages use lowercase names; classes and records use `PascalCase`; methods and fields use `camelCase`.
- Keep responsibilities in the existing layers. REST types generally end in `Request` or `Response`; controllers, services, repositories, and mappers use their corresponding suffixes.
- Persistence entities commonly use a `J` prefix (`JOffer`, `JDemand`); domain models omit it (`Offer`, `Demand`).
- Unit test classes end in `Test`; integration tests end in `IT`.
- Add Flyway migrations using `V1_0_<next-number>__short_description.sql`. Never edit a migration already applied to a shared database.
- Update `doc/api.yaml` when an API contract changes. Generated Java and TypeScript clients are built from this specification; Web App must use a compatible published TypeScript client.
- Keep credentials out of source, examples, logs, and commits. Private GitHub Packages dependencies require an authorized `GITHUB_TOKEN` with package read access.

## Local development

### Prerequisites

- JDK 21 and the checked-in Gradle wrapper.
- Maven on `PATH`: the normal Gradle compile task generates and installs the OpenAPI Java client into Maven Local.
- PostgreSQL, RabbitMQ, and the configured S3-compatible storage endpoint. SMTP settings are also required by the current application configuration. Supabase settings are required for authentication.
- Docker for integration tests, which use Testcontainers for PostgreSQL, RabbitMQ, LocalStack, and GreenMail.
- GitHub Packages access for the generated client artifacts. Without access, client version resolution can fall back to a local snapshot, but the artifact must still be generated and installed locally.
- A GitHub token with package read access is needed when resolving Maven artifacts and building the application image.

### Configure and run

Copy `.env.template` to `.env`, then replace every placeholder with local values. The application does **not** load `.env` automatically when launched with Gradle; export its variables in the shell first:

```bash
cp .env.template .env
# Edit .env, then in Bash or Zsh:
set -a
source .env
set +a
make run
```

`make run` starts `./gradlew bootRun`; the default application port is `8080`. `make help` lists the available targets. For local integration, point the required service variables to running instances. External channels such as PAPI, SMS, email, and Web Push also need their provider credentials and feature flags when used.

### Tests and quality checks

```bash
make test-unit
make test-integration
make test
make format
make ci-format
```

Integration tests require Docker. `make format-check` invokes the formatter before comparing the working tree, so it can change files; inspect the resulting diff and use it from a clean working tree. Do not stage unrelated files as part of formatting.

## API and generated clients

The OpenAPI source is [`doc/api.yaml`](doc/api.yaml). Swagger UI is exposed by the running service at `/doc`; the API routes use the `/v1` prefix.

Useful Gradle tasks:

```bash
./gradlew resolveClientVersion
./gradlew generateJavaClient
./gradlew generateTypeScriptClient
```

Generated clients are published through the repository's GitHub Actions workflows. When changing the API, update the specification, publish the generated TypeScript client, then update the Web App dependency and deploy the Web App that consumes it.

## Configuration reference

`.env.template` lists the application variables. The main groups are:

- `SPRING_DATASOURCE_*`, `SPRING_RABBITMQ_*`, and `APP_RABBITMQ_SSL` for PostgreSQL and RabbitMQ.
- `SUPABASE_URL`, `SUPABASE_JWT_SECRET`, `SUPABASE_WEBHOOK_SECRET`, and `SUPABASE_DISCOVERY_URL` for authentication and the auth webhook.
- `CLOUD_STORAGE_*` for the S3-compatible bucket; `SPRING_MAIL_*` for SMTP.
- `PECUNIA_BASE_URL` and `PECUNIA_API_KEY` for PAPI operations; `CONTACT_UNLOCK_PRICE_MGA` and `CONTACT_UNLOCK_ORANGE_MONEY_NUMBER` for contact unlock configuration.
- `NOTIFICATIONS_EMAIL_ENABLED`, `NOTIFICATIONS_SMS_ENABLED`, and `NOTIFICATIONS_WEB_PUSH_ENABLED` for external delivery channels.
- `NOTIFICATIONS_WEB_PUSH_DISPATCH_ENABLED`, `PUSH_DISPATCH_URL`, and `PUSH_DISPATCH_SECRET` for dispatch through the Next.js route. The secret must match the Web App value and remain server-side.
- `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, and `VAPID_SUBJECT` for Java's native Web Push sender when that mode is selected.

`ENABLE_CONTACT_PAYMENT` is the Domain-side payment feature flag. The Web App has its own build-time flag, `NEXT_PUBLIC_ENABLE_CONTACT_PAYMENT`; configure both services consistently.

### Web Push dispatch note

The browser registers its subscription through the Domain API. The Next.js dispatch route currently reads subscription rows from its Supabase database. The Web App also contains a Supabase upsert helper, but the current registration action calls Domain instead. Confirm that the two stores are synchronized before enabling dispatch mode; the native Java sender is a separate path.

## Build and deployment

The Docker image is multi-stage: Gradle/JDK 21 builds the JAR, then Eclipse Temurin JRE 21 runs it as a non-root user. The build needs access to the private Maven packages. `make docker-build` and `make docker-run` are available for local image work; `make docker-run` reads `.env`.

The production deployment workflow is `.github/workflows/cd-deploy.yml`. It watches pushes to the branch named `prod`, builds and publishes an image to GHCR, then deploys it to the configured VPS. The current repository also has `production` and `preproduction` branches; a merge to `production` alone does not imply that this workflow runs. Confirm the branch promotion process before deploying.

The workflow expects GitHub secrets named `ENV_VARS`, `GHCR_PAT`, `VPS_SSH_KEY`, `SSH_KEY_PASSPHRASE`, `SSH_DOOR`, `VPS_ADDR`, and `VPS_DEPLOYER`. Store their values in GitHub Actions; do not copy them into the repository.

There is a port discrepancy to keep in mind: Spring defaults to `8080`, while the Dockerfile exposes and health-checks `9090`; `PORT` controls the application port. The OpenAPI local server URL also lists `9090`. Align the environment's `PORT` and container mapping before relying on the Docker health check.

The deployment workflow reads production environment values and host credentials from GitHub secrets. Never copy those values into this README or `.env.template`.
