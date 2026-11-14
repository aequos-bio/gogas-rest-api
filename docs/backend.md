# Backend Documentation

The backend is a Spring Boot monolith that exposes the GoGas REST API and serves the bundled frontend. It is organized as a Maven multi-module project.

- **Framework:** Spring Boot 2.7.2
- **Language:** Java 11
- **Database:** Microsoft SQL Server (one database per tenant)
- **Build tool:** Maven (multi-module)
- **Main entry point:** `eu.aequos.gogas.GoGasApiApplication` (in `gogas-service`)
- **Default port:** `8081` (overridable via the `PORT` environment variable)

---

## 1. Module structure

The root `pom.xml` (`eu.aequos.gogas:gogas-rest-api:1.0-SNAPSHOT`, packaging `pom`) declares three modules.

| Module | Responsibility |
|--------|----------------|
| `gogas-persistence` | Data layer — JPA entities, Spring Data repositories, JPA Specifications, projections, and Flyway SQL migrations. Base package `eu.aequos.gogas.persistence`. |
| `gogas-service` | Application / web layer — the Spring Boot application, REST controllers, services, DTOs, security/JWT, multi-tenancy routing, notifications, scheduling, external integrations, and report generation. Depends on `gogas-persistence`. |
| `telegram-service` | Standalone Telegram notification microservice (declared in the root module list; packaged as `telegram-service-1.0-SNAPSHOT.jar`). |

Dependencies common to all modules (JPA, web, data-rest, validation, SQL Server driver, Lombok, test, Testcontainers, Log4j) are declared once in the root `pom.xml` and inherited by every module.

### Key dependencies

| Area | Dependency | Version |
|------|-----------|---------|
| Core | `spring-boot-starter-parent` | 2.7.2 |
| Web | `spring-boot-starter-web`, `-data-jpa`, `-data-rest`, `-validation` | (BOM) |
| Security | `spring-boot-starter-security` | (BOM) |
| JWT | `com.auth0:java-jwt` | 3.8.2 |
| DB driver | `com.microsoft.sqlserver:mssql-jdbc` | 9.4.1.jre11 |
| Migrations | `flyway-core`, `flyway-sqlserver` | — |
| Integration | `spring-cloud-starter-openfeign` | 3.1.1 |
| Templating | `spring-boot-starter-thymeleaf`, `thymeleaf-layout-dialect` | 3.3.0 |
| Reports | `poi-ooxml` (Excel) / `itext-core` (PDF) | 4.1.0 / 8.0.2 |
| Passwords | `passay` | 1.5.0 |
| Attachments | `tika-core` (type detection) | 1.10 |
| Testing | `junit-jupiter`, `testcontainers` (`mssqlserver`) | 5.5.2 / 1.17.6 |

> Note: the root `pom.xml` excludes then re-pins `log4j-to-slf4j` / `log4j-api` to 2.17.0 (Log4Shell mitigation). Stale 2.15.0 entries are still present and should be cleaned up.

---

## 2. Multi-tenancy

GoGas uses a **database-per-tenant** model with runtime datasource routing. Each GAS is a tenant with its own SQL Server database.

### Master database

A master database holds a `tenants` table that lists every tenant and its connection details:

```sql
CREATE TABLE tenants (
    tenant_id varchar(30)  NOT NULL PRIMARY KEY,
    username  varchar(30)  NOT NULL,
    password  varchar(30)  NOT NULL,
    url       varchar(256) NOT NULL
);
```

The master connection is configured under `spring.datasource.*` and bound to `MasterDatasetConfig` (`@ConfigurationProperties("spring.datasource")`).

### Runtime routing

| Component | Role |
|-----------|------|
| `TenantRegistry` | At startup, reads all rows from `tenants`, builds a `DriverManagerDataSource` per tenant, and (if `spring.flyway.enabled`) runs Flyway `repair()` + `migrate()` on each tenant DB with baseline version `20191210_001`. Also derives the tenant id from the request host (`extractFromHostName`). |
| `TenantContext` | ThreadLocal holding the current request's tenant id. |
| `TenantRoutingDataSource` | Extends `AbstractRoutingDataSource`; `determineCurrentLookupKey()` returns `TenantContext.getTenantId()` (defaults to `localhost`). |
| `DataSourceConfig` | Registers the routing datasource as the primary `DataSource` bean. `GoGasApiApplication` excludes `DataSourceAutoConfiguration` so this custom datasource is used. |
| `TenantInterceptor` | Spring MVC `HandlerInterceptor`; on every request resolves the tenant from the host, rejects unknown tenants with **404**, sets `TenantContext` + per-tenant log file (MDC), and clears everything in `afterCompletion`. |

### Tenant resolution

The tenant id is the innermost subdomain of the request host (from the `origin` header, falling back to the server name):

```
gasname.aequos.eu  ->  gasname
```

---

## 3. Security & authentication

Stateless JWT authentication built on auth0 `java-jwt`.

### Login flow

1. `POST /authenticate` → `AuthenticationController.createAuthenticationToken` accepts a `CredentialsDTO` (username/password).
2. The tenant is resolved from the request host via `TenantRegistry.extractFromHostName`.
3. Credentials are checked through Spring's `AuthenticationManager` (with a super-admin shortcut via `SuperAdminConfig`).
4. `GoGasUserDetails` is loaded via `AuthorizationService` and stamped with the tenant.
5. `JwtTokenHandler.generateToken` issues a token (HMAC256, issuer `gogas`, validity `jwt.duration.minutes`, default 60).
6. The token is returned in **three** places: the response body (`BasicResponseDTO`), the `Authentication` header, and a `jwt-token` **cookie**.

A `POST /authenticate/legacy` endpoint accepts an old-format JWT (verified with a legacy HMAC key, host-checked) and exchanges it for a new token.

### JWT claims

`subject` = username, plus `tenant`, user `id`, `firstname`, `lastname`, `role`, `enabled`, `manager`.

### Request filter

`JwtAuthenticationTokenFilter` (a `OncePerRequestFilter`):

- Extracts the token from the `jwt-token` cookie **or** `Authorization: Bearer <token>` header.
- Verifies the signature/expiry.
- **Enforces tenant isolation:** the `tenant` claim must equal the tenant derived from the request host. A token issued for one GAS cannot be replayed against another.
- On success, sets a `UsernamePasswordAuthenticationToken` in the security context and puts the username into the SLF4J MDC.
- Skips authentication for `/authenticate`, `/favicon.png`, `/api/user/password/reset`, and prefixes `/login`, `/info`, `/static`, `/assets`.
- Expired/invalid token → **401** for `/api/**` paths, redirect to `/login` otherwise.

### Configuration & roles

`WebSecurityConfig` (extends `WebSecurityConfigurerAdapter`): CSRF disabled, `SessionCreationPolicy.STATELESS`, `@EnableGlobalMethodSecurity(prePostEnabled=true)`, custom `JwtAuthenticationEntryPoint`, filter added before `UsernamePasswordAuthenticationFilter`, OPTIONS ignored. `GrantedAuthorityDefaults("")` removes the `ROLE_` prefix so roles are the raw single characters:

| Role | Meaning |
|------|---------|
| `U` | Utente (member) |
| `S` | Amico (friend of a member) |
| `A` | Amministratore (admin) |

Password hashing uses a custom `ShaPasswordEncoder`; password generation/policy for reset flows uses `RandomPassword` + Passay.

### Method-level authorization

Custom annotations in `security/annotations` delegate to `AuthorizationService` via SpEL `@PreAuthorize`:

`@IsAdmin`, `@IsManager`, `@IsAdminOrCurrentUser`, `@IsCurrentUserFriend`, `@IsOrderManager`, `@IsOrderTypeManager`, `@IsProductManager`, `@IsOrderItemOwner`, `@CanViewBalance`.

> **Security note:** the JWT signing keys (current and legacy) are hard-coded in `JwtTokenHandler`. Consider externalizing them to configuration/secrets.

---

## 4. Data model (persistence layer)

Entities live in `gogas-persistence/.../persistence/entity`. Table and column names are the Italian legacy SQL Server names (mapped via `@Table`/`@Column`); primary keys are SQL Server `uniqueidentifier` values generated with Hibernate `uuid2`.

### Core entities

| Entity (table) | Purpose |
|----------------|---------|
| `User` (`utenti`) | Members, friends, admins. Fields include username (`utente`), password (`pwd`), `role` (`ruolo`), name, email, `attivo`, and a self-referencing `friendReferral` (a friend belongs to a referring member). Includes a stored-procedure mapping `SaldoContabile_Get_ByIdUtente` for balances. |
| `Order` (`dateordini`) | A group order instance. Carries status, opening/due/delivery dates, order type, invoice fields, paid flag, external order ids. Domain logic on the entity: `isEditable()`, `isExpired()`, `isNotYetOpened()`, `getStatus()`. |
| `OrderType`, `OrderItem`, `OrderManager`, `SupplierOrderItem` | Ordering domain. |
| `Product`, `ProductCategory`, `Supplier`, `ShippingCost`, `Year` (`anno`) | Catalog & configuration. |
| `AccountingEntry`, `AccountingEntryReason`, `AccountingGasEntry`, `UserBalance`, `UserBalanceEntry` | Accounting & balances. |
| `Configuration`, `Menu`, `MenuByRole` | App configuration, menu, role-menu mapping. |
| `NotificationPreferences`, `PushToken` | Notification settings & push device tokens. |

### Order status

`OrderStatus` enum: Opened / Closed / Accounted / Cancelled (`Aperto` / `Chiuso` / `Contabilizzato` / `Annullato`).

### Repositories & queries

- 22 Spring Data repositories, one per aggregate (e.g. `UserRepo.findByUsername`, `OrderRepo.findByIdWithType`, `OrderManagerRepo.findByUser`).
- Dynamic queries use JPA **Specifications** in `persistence/specification`: `AccountingGasSpecs`, `AccountingSpecs`, `OrderSpecs`, `ProductSpecs`, `UserBalanceSpecs`, plus a generic `SpecificationBuilder`.
- Projections in `persistence/utils` and `entity/derived` (e.g. `UserTotal`, `UserTransactionFull`, `UserCoreInfo`).

### Flyway migrations

Located in `gogas-persistence/src/main/resources/db/migration` (SQL Server dialect):

| Migration | Description |
|-----------|-------------|
| `V20191210_001__schema_creation.sql` | Full legacy schema (also the Flyway **baseline** `20191210_001`). |
| `V20200122_001__supplier_order_product_external_id.sql` | Supplier order product external id. |
| `V20200215_001__accounting_code_order_type.sql` | Accounting code on order type. |
| `V20240115_001__fill_notifications.sql` | Populate notification data. |
| `V20240206_001__unique_constraint_on_orders.sql` | Unique constraint on orders. |

Migrations run per tenant at startup via `TenantRegistry` when `spring.flyway.enabled=true`.

---

## 5. REST API

Controllers live in `eu.aequos.gogas.controllers`. Most are mapped under `api/...`.

| Controller | Base path | Domain |
|-----------|-----------|--------|
| `AuthenticationController` | — | `authenticate`, `authenticate/legacy`, `info`, `info/gas`, `info/logo`, `info/build` |
| `UserController` | `api/user` | User CRUD, list, password reset |
| `FriendController` | `api/friend` | Friends of a member |
| `OrderUserController` | `api/order/user` | Member's own orders |
| `OrderFriendController` | `api/order/friend` | Orders placed on behalf of friends |
| `OrderManagerController` | `api/order/manage` | Order management (managers) |
| `OrderTypeController` | `api/ordertype` | Order types |
| `DeliveryController` | `api/delivery` | Deliveries |
| `ProductController` | `api/products` | Products |
| `ProductCategoryController` | `api/category` | Product categories |
| `SupplierController` | `api/supplier` | Suppliers |
| `AccountingUserController` | `api/accounting/user` | User accounting & balances |
| `AccountingFriendController` | `api/accounting/friend` | Friend accounting |
| `AccountingGasController` | `api/accounting/gas` | GAS-level accounting |
| `AccountingReasonController` | `api/accounting/reason` | Accounting reasons (causali) |
| `ConfigurationController` | `api/configuration` | General configuration |
| `YearController` | `api/year` | Accounting years |
| `HomeController` | `api/home` | Home dashboard data |
| `FrontendController` | — | Serves the SPA / legacy Thymeleaf UI |

Errors are centralized by `RestExceptionHandler` (`@ControllerAdvice`), returning a `RestApiError`. Domain exceptions extend `GoGasException`: `ItemNotFoundException`, `DuplicatedItemException`, `OrderClosedException`, `OrderAlreadyExistsException`, `InvalidOrderActionException`, `UserNotAuthorizedException`, `MissingOrInvalidParameterException`, `ItemNotDeletableException`.

### OpenAPI

The `swagger-maven-plugin` (3.1.8) generates an OpenAPI spec from `eu.aequos.gogas.controllers` at compile time, with a `jwt` apiKey security definition. A `redoc` Maven profile renders it to HTML.

---

## 6. Services & cross-cutting concerns

- **Service layer** (`service/`) mirrors the controllers (`UserService`, `OrderUserService`, `OrderManagerService`, `AccountingService`, `ProductService`, `SupplierService`, …). A generic abstract `CrudService<Model, ID>` provides `getRequired` / `create` / `createOrUpdate` with `ItemNotFoundException` / `ItemNotDeletableException` handling.
- **Order workflow** (`workflow/`): a state machine over order status via `OrderWorkflowHandler` and `OrderStatusAction` implementations — `CloseAction`, `CancelAction`, `ReopenAction`, `UndoCancelAction`, `AccountAction`, `UndoAccountAction`, with `ActionValidity`.
- **Aequos integration** (`integration/`): `AequosIntegrationService` with an OpenFeign client `AequosApiClient` (`@EnableFeignClients`) to sync order types, price lists, available orders, send orders, and update weights with the external Aequos platform.
- **Notifications** (`notification/`): `NotificationSender` over multiple channels — email (`spring-boot-starter-mail`), push (OneSignal-style, appid/apikey in properties), and Telegram. `schedule/OrderNotificationTask` runs on cron (`@EnableScheduling`).
- **Reports**: Excel via Apache POI (`ExcelGenerationService`), PDF via iText (`PdfReportService`).

---

## 7. Configuration

Runtime config lives in `gogas-service/src/main/resources/application.properties`. Secrets are supplied via environment variables.

| Setting | Env var / value |
|---------|-----------------|
| Master DB | `DB_HOST`, `DB_NAME`, `DB_USR`, `DB_PWD` (SQL Server `...\SQLEXPRESS:1433`) |
| Attachments folder | `ATTACHMENT_FOLDER` |
| Server port | `PORT` (default `8081`), context-path `/` |
| Hibernate | SQLServer2012Dialect, globally-quoted identifiers |
| Mail (SMTPS) | `MAIL_USR`, `MAIL_PWD` (Aruba) |
| Push | OneSignal appid / apikey |
| Notification cron | `0 0/15 9-23 * * ?` |
| Flyway | `spring.flyway.enabled=true` |
| External services | `telegram.service.url` (`localhost:8080`), `excel.service.url` (`localhost:5000`) |

A `dev` profile (`application-dev.properties`) overrides the context-path to `/gogas`.

---

## 8. Build & run

### Prerequisites

- Java 11, Maven
- A SQL Server instance with a master database and `tenants` table (see §2)
- Node.js (to build the frontend, which is bundled into the backend)

### Build

The frontend must be built first (its output is placed in `gogas-service/src/main/resources`):

```bash
cd gogas-service/frontend
npm install
npm run build
```

Then build the Maven project from the repo root:

```bash
mvn clean install
```

### Run

Set the required environment variables, then start the `gogas-service` application:

```bash
export DB_HOST=... DB_NAME=... DB_USR=... DB_PWD=... ATTACHMENT_FOLDER=...
java -jar gogas-service/target/gogas-service-1.0-SNAPSHOT.jar
```

The app listens on port `8081` by default and serves both the REST API and the SPA from the same origin.

### CI/CD

`.circleci/config.yml` runs `npm install` / `npm run build`, then `mvn clean install`, stores Surefire reports and the service/telegram jars, and SCP-deploys per branch.

---

## 9. Testing

- **Framework:** JUnit 5 (`junit-jupiter`).
- **Approach:** mostly integration tests via `@SpringBootTest` + `@AutoConfigureMockMvc`, backed by **Testcontainers** that spin up a real SQL Server (`GoGasDatabaseContainer`).
- **Base class:** `BaseGoGasIntegrationTest` (`@TestInstance(PER_CLASS)`) injects the container's JDBC URL/credentials via `@DynamicPropertySource`, `@MockBean`s external clients (`AttachmentRepo`, `PushNotificationClient`, `TelegramNotificationClient`, `ExcelServiceClient`), and seeds data through `Mock*Data` lifecycle helpers (`MockUsersData`, `MockOrdersData`, `MockAccountingData`, `MockConfigurationData`).
- **Multi-tenancy in tests:** a `@WithTenant` annotation + `TenantAspect`; requests are driven via a `MockMvcGoGas` helper.
- **Coverage:** accounting (user/friend), orders (create/close/cancel/account, by product, by user, delivery, notifications, order types), products, users, configuration, and the full Aequos integration surface. A few pure unit tests exist (`UserNotificationsCacheUnitTest`, `OrderNotificationTaskUnitTest`).

Run the suite with:

```bash
mvn test
```
