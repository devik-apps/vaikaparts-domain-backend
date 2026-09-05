# VaikaParts backend

This repository holds the domain/core backend of the VaikaParts application.

> © 2026 - Devik'Apps

# Table of Contents

1. [Detailed Architecture](#1-detailed-architecture)
2. [Testing Strategy](#2-testing-strategy)
3. [Environment Management](#3-environment-management)
4. [API Documentation](#4-api-documentation)
5. [Development Workflow with Makefile](#5-development-workflow-with-makefile)
6. [Deployment](#6-deployment)
7. [CI/CD Workflows](#7-cicd-workflows)

---

# 1. Detailed Architecture

## 1.1 Configuration Layer

**Location**: [`config/`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/config/)

The configuration layer establishes infrastructure integrations and cross-cutting concerns:

- **[`BucketConf`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/config/BucketConf.java)**: Configures S3-compatible storage client
  with credential management, region settings, and endpoint configuration. Provides bean definitions for S3 client
  instances and transfer managers used throughout the application.

- **[`EmailConf`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/config/EmailConf.java)**: Establishes Spring Mail sender
  configuration with SMTP settings, authentication, and TLS configuration. Integrates email health indicators for
  operational monitoring.

- **[`RabbitConf`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/config/RabbitConf.java)**: Configures RabbitMQ messaging
  infrastructure including connection factories, message templates, exchange declarations, queue definitions, and
  binding configurations. Establishes message converter strategies and retry policies.

- **[`SecurityConf`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/config/EmailConf.java)**: Provides baseline Spring Security
  configuration serving as a foundation for project-specific authentication and authorization strategies. Teams extend
  this configuration to implement JWT validation, OAuth2 flows, or custom role-based access control policies.

- **[`SwaggerDocConf`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/config/SwaggerDocConf.java)**: Integrates SpringDoc OpenAPI 3.1
  specification rendering. Configures automatic API documentation generation and ensures developer documentation is
  accessible at `/doc` through redirect mechanisms.

- **[`JacksonConf`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/config/SwaggerDocConf.java)**: Centralizes JSON
  serialization and deserialization policies. Configures secure Jackson settings including fail-on-unknown-properties
  behavior, timestamp formatting, and module registration for modern Java types.

- **[
  `MultipartConfigurationInitializer`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/config/MultipartConfInitializer.java)
  **: Validates multipart upload limits at application startup using [
  `MultipartPropertiesValidator`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/file/MultipartFileConverter.java).
  Enforces fail-fast behavior if upload configurations are missing or exceed security thresholds, preventing runtime
  vulnerabilities.

## 1.2 Data Structures

**Location**: [`datastructure/`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/datastructure/)

Algorithmic utilities supporting infrastructure operations without coupling to domain or persistence layers. These
components provide reusable data manipulation patterns that maintain independence from business logic.

**Example**: `ListGrouper` partitions large collections into bounded batches, enabling efficient message publishing or
bulk processing operations while respecting broker message size limits and transaction boundaries.

## 1.3 Event Layer

**Location**: [`event/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/event)

The event layer implements asynchronous messaging patterns:

- **[`consumer/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/event/consumer)**: [
  `EventConsumer`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/consumer/EventConsumer.java) receives messages from
  configured queues while [`EventDispatcher`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/consumer/EventDispatcher.java)
  routes messages to appropriate handlers. This architecture provides a structured consumer pipeline with error
  handling, retry logic, and dead-letter queue integration.

- **[`model/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/event/model)**: Defines event abstractions including [
  `InfraEvent`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/model/InfraEvent.java) base interface, [
  `EventProducer`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/model/EventProducer.java) for message publication, [
  `EventConf`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/model/EventConf.java) for routing configuration, and [
  `DummyEvent`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/model/DummyEvent.java) demonstrating domain event implementation
  patterns.

#### Models:
- [DemandPublishedNotificationRequested](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/model/DemandPublishedNotificationRequested.java): This is the event that caries the demand publish notification.

This event is consumed by [DemandPublishedNotificationRequestedService](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/service/event/DemandPublishedNotificationRequestedService.java).
Which means whenever the DemandPublishedNotificationRequested event is published to the message broker, this is the class that consumes it.

- [DemandPublishedRequested](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/model/DemandPublishedRequested.java): This particular event caries the demand published situations

The corresponding consumer of this event is [DemandPublishedRequestedService](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/service/event/DemandPublishedRequestedService.java)

Note that the consumer is automatically detected by the ar-infra by **name convention**.
One can identify this convention by consulting the following class: [InfraEventTypeRegistrar](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/event/config/InfraEventTypeRegistrar.java).

## 1.4 Exception Layer

**Location**: [`exception/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/exception)

Centralized exception definitions for infrastructure operations:

- Bucket operation failures including upload, download, and presign errors
- Email transmission and health check failures
- File conversion and validation errors
- Directory upload processing exceptions
- Missing authorization and authentication errors
- Multipart handling and validation failures

REST-specific exception handling is implemented in the endpoint layer through [
`ApiExceptionHandler`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/ApiExceptionHandler.java), which
translates infrastructure exceptions into consistent HTTP responses with standardized error payloads.

### Exceptions:
- [bucket exceptions](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/exception/bucket): This particual package holds the custom exceptions related to bucket operations. This include `BucketDeleteException`, `BucketDirectoryUploadException`, `BucketDownloadException`, `BucketHealthCheckException`, `BucketOperationException`, `BucketUploadException`, `DirectoryUploadException`.
- [DemandPublishedNotificationRequestedException](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/exception/DemandPublishedNotificationRequestedException.java): This is the custom exception that handles the runtime exception of the `DemandPublishedNotificationRequested` event.
- [DemandPublishedRequestedException](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/exception/DemandPublishedRequestedException.java): corresponds to the custom exception handling runtime exception on the `DemandPublishedRequested` event.
- [EmailSendException](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/exception/EmailSendException.java): corresponds to a runtime exception related to an email Seding. This may be caused by a malformed email or any other exception related to the email sending.
- [JwtClaimExtractionException](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/exception/JwtClaimExtractionException.java): custom exception on extraction of JWT claims.

## 1.5 Endpoint Layer

**Location**: [`endpoint/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/)

The endpoint layer exposes HTTP interfaces and manages request/response transformation:

- **[`rest/controller/health/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/health)**: Infrastructure
  health
  verification endpoints:
    - `/ping`: Liveness probe for container orchestration
    - `/health/bucket`: Storage health validation through upload, download, and presigned URL generation
    - `/health/email`: Email system health verification through test message transmission
    - `/health/message`: Message broker health through event production and optional consumption verification
    - `/health/db`: Database connectivity and query performance through paginated entity retrieval

- **[
  `rest/controller/ApiExceptionHandler`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/ApiExceptionHandler.java)
  **: Global exception handler implementing `@ControllerAdvice` pattern. Transforms infrastructure exceptions into
  standardized `ErrorResponse` payloads with consistent status codes, error messages, and traceability information.

- **[
  `rest/controller/model/ErrorResponse`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/ErrorResponse.java)
  **: Canonical error response structure containing timestamp, HTTP status, error classification, detailed message,
  request path, and application-specific error codes for client-side error handling.

#### Controllers:
- [DemandController](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/exchange/DemandController.java): holds the endpoints related to the demands exchange operations.
- [OfferController](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/exchange/OfferController.java): contains the endpoint definitions related to the offer exchange operations.
- [ManagerController](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/user/ManagerController.java): holds the endpoints related to manager
- [ResearcherController](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/user/ResearcherController.java): contains the endpoints definitions related to the Researcher users.
- [SellerController](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/user/SellerController.java): contains the endpoints definitions related to the Seller user.
- [SupabaseAuthWebhookController](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/user/SupabaseAuthWebhookController.java): This contains the webhook endpoint to communicate with the Supabase on user creations.
- [UserController](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/user/UserController.java): this contains a general endpoints shared by all type of users.
- [NotificationController](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/NotificationController.java): this holds the endpoints related to in-app notifications.

## 1.6 File Utilities

**Location**: [`file/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/file)

Secure file handling and storage abstractions:

- **[`BucketComponent`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/file/BucketComponent.java)**: High-level abstraction over
  bucket operations providing upload (single files and directory trees), download, and presigned URL generation with
  automatic retry logic and error translation.

- **[`FilenameSanitizer`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/file/FilenameSanitizer.java)**: Sanitizes user-provided
  filenames to prevent path traversal attacks, remove dangerous characters, and enforce length limits.

- **[`MultipartFileConverter`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/file/MultipartFileConverter.java)**: Safe conversion of
  multipart uploads to filesystem representations with validation and temporary storage management.

- **[`TempFileManager`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/file/TempFileManager.java)**: Manages temporary
  file lifecycle with automatic cleanup, secure permissions, and isolation from shared temporary directories.

- **[`TempFileCleaner`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/file/TempFileCleaner.java)**: Scheduled cleanup of orphaned
  temporary files with configurable retention policies.

- **[`FileHash`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/file/FileHash.java)**: Cryptographic hash computation for file
  integrity verification supporting multiple algorithms (SHA-256, SHA-512).

## 1.7 Repository Layer

**Location**: [`repository/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/repository)

Data persistence abstractions:

- **[`repository/model/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/repository/model)**: JPA entity definitions mapped to
  database tables with appropriate constraints, indexes, and relationship configurations.

- **[`repository/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/repository/)**: Spring Data JPA repository interfaces extending
  `JpaRepository` and `JpaSpecificationExecutor` for declarative query methods and type-safe criteria queries.

#### Repositories:
- [UserRepository](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/repository/UserRepository.java)
- [PartRepository](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/repository/PartRepository.java)
- [OfferRepository](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/repository/OfferRepository.java)
- [NotificationRequestedRepository](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/repository/NotificationRequestedRepository.java)
- [DemandRepository](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/repository/DemandRepository.java)
- [DemandPublishedRequestedRepository](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/repository/DemandPublishedRequestedRepository.java)
- [DemandPublishedNotificationRepository](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/repository/DemandPublishedNotificationRepository.java)
## 1.8 Mail Layer

**Location**: [`mail/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/mail)

Email delivery abstractions:

- **[`Email`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/mail/Email.java)**: Immutable value object capturing sender address,
  recipient list, subject line, and message body (plain text or HTML).

- **[`Mailer`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/mail/Mailer.java)**: Email transmission service integrating with Spring
  Mail infrastructure. Provides synchronous and asynchronous sending with comprehensive error handling. Integration
  tests use GreenMail for SMTP simulation without external dependencies.

## 1.9 Service Layer

**Location**: [`service/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/service)

Business logic orchestration implementing domain operations across repository, event, file, and mail subsystems.
Services coordinate transactional boundaries, manage domain invariants, and implement business rules. Health services
aggregate infrastructure checks for operational monitoring.

## 1.10 Core Models

**Location**: [`model/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/model)

Domain models representing business concepts independent of transport protocols and persistence mechanisms. These models
form the core domain language and remain isolated from framework-specific annotations or infrastructure concerns.
### Core models:
#### [User](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/model/user)
- [User](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/user/User.java): This is the base class of the System User model.
- [Seller](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/user/Seller.java): This is the model that represents the Seller type of user.
- [Researcher](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/user/Researcher.java): User of type Researcher
- [Manager](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/user/Manager.java): User of type MANAGER.
- [classifier/](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/model/classifier): This package holds the enum type used for all the models.
- [Exchange](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/exchange/Exchange.java): This is the base class of the two type of exchange such as Demand or Offer.
- [Demand](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/exchange/Demand.java): this is the demand system-core-model.
- [Offer](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/exchange/Offer.java): thisis the Offer system-core-model.
- [Part](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/exchange/Part.java): this model represent the car parts.
- [PartInfo](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/exchange/PartInfo.java)
- [DemandPublishedNotification](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/main/java/com/devikapps/vaikaparts/model/notification/DemandPublishedNotification.java)

## 1.11 Mapper Layer

**Location**: [`mapper/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/mapper)

Bidirectional mapping infrastructure using MapStruct for compile-time code generation. Mappers bridge endpoint DTOs,
core domain models, and repository entities, enforcing the Diamond Model architecture to preserve boundaries between
REST, domain, and persistence layers. Custom mapping methods handle complex transformations while maintaining type
safety.

## 1.12 Validator Layer

**Location**: [`validator/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/validator)

Centralized validation and security verification:

**Base Interface**: `Validator<T>` defines `validate(T)` and `getValidatedType()` with JSR-380 constraint annotations.
Validation failures produce `IllegalArgumentException` for invalid input or `SecurityException` for security violations,
providing explicit failure semantics.

**File Validators** ([`validator/file/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/validator/file)):

- **[`MultipartPropertiesValidator`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/validator/file/MultipartPropertiesValidator.java)
  **: Validates Spring `MultipartProperties` at application startup. Requires explicit max file and request sizes,
  rejects values exceeding absolute maximums, and warns when configurations exceed OWASP recommendations. Integrated
  through `MultipartConfigurationInitializer` for fail-fast behavior.

- **[`FileValidator`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/validator/file/FileValidator.java)**: Validates secure file and
  directory operations using NIO.2 APIs (`toRealPath`, symbolic link detection, readable file verification). Prevents
  TOCTOU vulnerabilities and path traversal attacks through canonical path resolution.

- **[`ZipEntryValidator`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/java/com/devikapps/vaikaparts/validator/file/ZipEntryValidator.java)**: Comprehensive ZIP
  archive validation protecting against CWE-409 zip bombs and malicious archives. Validates entry names for path
  traversal, verifies compression ratios, enforces size limits, detects symbolic links, prevents duplicate entries, and
  monitors total decompressed size. Provides validation methods for both `java.util.zip.ZipEntry` and Apache Commons
  `ZipArchiveEntry`, plus runtime extracted size verification.

**Integration Strategy**: Configuration initializers invoke validators at application startup to enforce fail-fast
behavior. Services and file utilities invoke validators during runtime operations to maintain ongoing security and
correctness guarantees.

## 1.13 Database Migrations

**Location**: [`resources/db/migration/`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/resources/db/migration)

Versioned Flyway SQL scripts implementing incremental schema evolution:

- **[`V0_0_1__Create_dummy_tables.sql`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/resources/db/migration/V0_0_1__Create_dummy_tables.sql)**: Initial
  schema definition creating tables, indexes, constraints, and sequences for demonstration entities.

- **[
  `V0_0_2__Insert_dummy_values_in_dummy_tables.sql`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/src/main/resources/db/migration/V0_0_2__Insert_dummy_values_in_dummy_tables.sql)
  **: Reference data seeding for development and testing environments.

Migrations execute automatically at application startup through Flyway's versioned migration strategy, ensuring
reproducible schema evolution across all environments (development, testing, staging, production). Each migration is
transactional and idempotent.

---

# 2. Testing Strategy

The testing strategy emphasizes validation of actual infrastructure behavior over mock-based verification:

**Unit Tests**: Verify isolated business logic components using mocks where external dependencies are required. Unit
tests focus on algorithm correctness, edge case handling, and domain rule enforcement without infrastructure setup
overhead.

**Integration Tests**: Extend [`FacadeIT`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/test/java/com/devikapps/vaikaparts/conf/FacadeIT.java) base class, which
provisions real infrastructure components through Testcontainers including PostgreSQL, RabbitMQ, S3-compatible storage (
LocalStack), and SMTP servers (GreenMail). [`FacadeIT`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/test/java/com/devikapps/vaikaparts/conf/FacadeIT.java) configures
dynamic properties through Spring's `DynamicPropertyRegistry`, ensures container reuse per JVM to minimize startup
overhead, and provides controlled lifecycle management with automatic cleanup.

This approach validates actual I/O paths, message flows, storage operations, and database queries rather than testing
interface contracts alone. Integration tests reflect production behavior and catch configuration errors, network issues,
and serialization problems that mock-based tests cannot detect.

---

# 3. Environment Management

Environment-specific configuration is managed through externalized properties:

**[`.env.template`](.env.template)**: Canonical environment variable specification documenting all required and optional
configuration parameters including RabbitMQ connection details, PostgreSQL datasource configuration, Backblaze/B2 S3
credentials and endpoints, SMTP mail service settings, and application port bindings. Teams copy this template to `.env`
and populate values appropriate for each environment (local development, continuous integration, staging, production).

**[`EnvConf`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/test/java/com/devikapps/vaikaparts/conf/EnvConf.java)**: Test-specific configuration class injecting
environment-like properties dynamically during integration test execution. Supports API keys, feature flags, JWT signing
keys, external service endpoints, and application-specific thresholds. [
`FacadeIT`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/src/test/java/com/devikapps/vaikaparts/conf/FacadeIT.java) discovers and applies `EnvConf` implementations
through reflection, allowing test-specific overrides without modifying application configuration files.

---

# 4. API Documentation

OpenAPI 3.1 specification located at [`doc/api.yaml`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/doc/api.yaml) provides comprehensive API documentation. The
application automatically redirects root path (`/`) and `/doc` to Swagger UI, rendering the specification immediately
accessible without additional configuration.

Health endpoints are documented as reference examples demonstrating request/response patterns, status codes, and error
scenarios. Development teams extend [`doc/api.yaml`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/doc/api.yaml) to describe new API endpoints, request schemas,
response models, and authentication requirements.

This contract-first approach drives consumer clarity, supports automated client generation, enables API versioning
strategies, and maintains documentation synchronization with implementation through validation in CI/CD pipelines.

---

# 5. Development Workflow with Makefile

The template includes a comprehensive Makefile that standardizes development workflows and enables local execution of
CI/CD pipelines. The Makefile detects the operating system (Linux, macOS, Windows) and adapts commands accordingly,
ensuring consistent behavior across development environments.

## 5.1 Makefile Structure

The Makefile is organized into logical sections:

- **Development**: Build, compilation, and local execution targets
- **Testing**: Unit, integration, and coverage test execution
- **Code Quality**: Formatting, linting, and static analysis
- **Docker**: Container image management and runtime operations
- **CI/CD**: Local replication of continuous integration pipelines
- **Cleanup**: Artifact and resource cleanup operations
- **Setup**: Development tool installation and verification

## 5.2 Common Development Tasks

**Viewing Available Targets**:

```bash
make help
```

This command displays all available Makefile targets with descriptions, organized by category.

**Code Formatting Before Commit**:

```bash
make format
git add --all
make ci-format
```

The `make format` command applies Google Java Format to all Java source files and formats YAML files using yamlfmt.
After staging changes with `git add --all`, `make ci-format` verifies that formatting is correct and matches CI
requirements. This workflow prevents formatting-related CI failures.

**Running Tests Locally**:

```bash
make test              # All tests (unit + integration)
make test-unit         # Unit tests only (*Test.java)
make test-integration  # Integration tests only (*IT.java)
```

Tests execute with the same Testcontainers configuration used in CI, ensuring local test results match continuous
integration behavior.

**Building the Application**:

```bash
make build
```

This target performs format verification, compiles the application, and generates the JAR artifact. The build process
mirrors CI build steps.

**Running the Application**:

```bash
make run    # Standard execution
make dev    # Development mode with active profile
```

## 5.3 Docker Operations

**Building and Running Containers**:

```bash
make docker-build                    # Build image
make docker-run                      # Start container
make docker-logs                     # View container logs
make health-check                    # Verify application health
make docker-stop                     # Stop and remove container
```

**Custom Configuration**:

```bash
make docker-build IMAGE_TAG=v1.2.3
make docker-run PORT=9090
```

## 5.4 Security Scanning

**Semgrep Analysis**:

```bash
make semgrep
```

The `semgrep` target automatically installs pipx (if not present) and Semgrep, then executes security analysis with
SARIF output generation. The Makefile handles cross-platform pipx installation (apt-get for Debian/Ubuntu, dnf for
Fedora, brew for macOS, pip for Windows) and verifies installation success before running analysis.

**Qodana Analysis**:

```bash
make qodana
```

Executes JetBrains Qodana static analysis in a Docker container, generating comprehensive code quality reports.

## 5.5 Local CI/CD Validation

Before pushing code, developers can replicate complete CI/CD pipelines locally:

```bash
make ci-format    # Format check (ci-format.yml)
make ci-test      # Test execution (ci-test.yml)
make ci-build     # Docker build (ci-build.yml)
make ci-qodana    # Qodana analysis (ci-qodana.yml)
make ci-semgrep   # Semgrep analysis (ci-semgrep.yml)
```

These targets execute identical commands to GitHub Actions workflows, enabling early detection of CI failures and
reducing feedback cycle time.

## 5.6 Cleanup Operations

```bash
make clean         # Remove build artifacts
make clean-docker  # Remove Docker images and containers
make clean-all     # Complete cleanup
```

## 5.7 Development Environment Setup

**Tool Installation**:

```bash
make install    # Install Google Java Format and configure permissions
make verify     # Verify Java, Gradle, and Docker availability
```

The `make install` target downloads required tooling (Google Java Format JAR) and configures executable permissions for
scripts. The `make verify` target validates that all required tools are installed and accessible.

## 5.8 Best Practices

1. **Pre-commit validation**: Always execute `make format` before committing code changes.
2. **Pre-push validation**: Run `make ci-format ci-test ci-build` before pushing to remote branches to catch CI failures
   early.
3. **Incremental testing**: Use `make test-unit` during development for faster feedback, reserving `make test` for
   pre-push validation.
4. **Container cleanup**: Periodically execute `make clean-docker` to reclaim disk space from unused images.
2. **Tool verification**: Run `make verify` after system updates to ensure development environment remains consistent.

---

# 6. Deployment

The template includes a multi-stage Dockerfile optimized for production deployment:

**Build Stage**: Uses Gradle 8.10 on JDK 21 Alpine to resolve dependencies and build the Spring Boot JAR. Dependencies
are cached separately from source code to optimize layer caching and reduce rebuild time.

**Runtime Stage**: Uses Eclipse Temurin JRE 21 Alpine as the base image. Creates a non-root `spring` user and group,
copies the application JAR and documentation, and configures execution permissions. The container runs as the `spring`
user to minimize attack surface.

**Health Check Integration**: Dockerfile includes a `HEALTHCHECK` instruction that queries the actuator health
endpoint (`/actuator/health`) at 30-second intervals. Container orchestration platforms (Kubernetes, Docker Swarm, ECS)
use this health check for readiness probes and automatic container restart.

**Entry Point**: [`docker-start.sh`](https://github.com/devik-apps/vaikaparts-domain-backend/blob/preprod/docker-start.sh) serves as the container entry point, executing `java -jar app.jar`
with support for command-line arguments passed from container runtime.

This configuration produces a lean, secure container image suitable for production deployment with standard health
probing and non-root execution.

---

# 7. CI/CD Workflows

Continuous integration and deployment workflows are defined in `.github/workflows/`:

**[`ci-build.yml`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/.github/workflows/ci-build.yml)**: Builds the Docker image using BuildKit for layer caching, verifies
image creation, validates JAR file presence, and confirms Java version. This workflow ensures that the containerized
application is properly assembled and executable. The workflow invokes `make ci-build` to execute the build pipeline
locally within the CI environment.

**[`ci-test.yml`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/.github/workflows/ci-test.yml)**: Executes unit and integration test suites with Gradle test caching
and Testcontainers integration. Tests run with the same configuration as local development, ensuring consistency. The
workflow sets required environment variables for Testcontainers and invokes `make ci-test` to execute the test pipeline.

**[`ci-format.yml`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/.github/workflows/ci-format.yml)**: Enforces Google Java Format standards and YAML formatting
requirements. The pipeline fails if any file does not match formatting conventions, ensuring code consistency across all
contributions. Developers run `./format.sh` or `make format` locally to apply formatting before pushing. The workflow
invokes `make ci-format` to verify formatting compliance.

**[`ci-codeql.yml`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/.github/workflows/ci-codeql.yml)**: Performs security analysis using GitHub CodeQL, identifying
potential security vulnerabilities, code quality issues, and common programming errors through semantic code analysis.

**[`ci-qodana.yml`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/.github/workflows/ci-qodana.yml)**: Executes JetBrains Qodana static analysis with PR comment
integration, annotations, and detailed issue reporting. Results are printed to workflow logs for immediate visibility.

**[`ci-semgrep.yml`](https://github.com/devik-apps/vaikaparts-domain-backend/tree/preprod/.github/workflows/ci-semgrep.yml)**: Runs Semgrep security linting with automatic rule updates and
SARIF output for GitHub Security tab integration. The workflow installs Semgrep via pipx and invokes `make ci-semgrep`
to execute the security scan.

Together, these workflows enforce formatting standards, verify functional correctness, and establish security baselines
on every push and pull request across all branches. The Makefile integration ensures that developers can replicate these
exact checks locally before committing code.

> The [devik-app/vaikaparts-domain-backend](https://github.com/devik-apps/vaikaparts-domain-backend) is based on the [ar-infra architecture](https://github.com/Abega1642/ar-infra-template).
