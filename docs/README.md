# GoGas Documentation

GoGas is a multi-tenant web application for Italian **GAS** (_gruppi di acquisto solidale_ — solidarity purchasing groups): communities that buy products collectively, place group orders against suppliers, and track per-member accounting and balances.

The project is a Spring Boot monolith with a React single-page application that is built into the backend's static resources and served from the same origin.

## Repository layout

```
gogas-rest-api/
├── pom.xml                  # Root Maven multi-module build
├── gogas-persistence/       # Data layer: JPA entities, repositories, Flyway migrations
├── gogas-service/           # Application layer: REST API, security, services, scheduling
│   └── frontend/            # React + TypeScript single-page application
├── telegram-service/        # Telegram notification microservice (declared module)
└── docs/                    # This documentation
```

## Documentation index

| Document | Description |
|----------|-------------|
| [Backend](./backend.md) | Spring Boot API: architecture, multi-tenancy, security, data model, REST controllers, build & run, testing |
| [Frontend](./frontend.md) | React SPA: stack, structure, routing, authentication, API integration, build & integration with the backend |

## High-level architecture

```
                      ┌─────────────────────────────────────────────┐
   Browser            │                gogas-service                 │
 ┌──────────┐  HTTPS  │  ┌───────────────┐     ┌──────────────────┐  │
 │ React SPA│◄───────►│  │ Spring MVC    │     │ TenantInterceptor│  │
 │ (served  │  /api   │  │ REST API      │────►│ + JWT filter     │  │
 │  as      │         │  │ controllers   │     │ (tenant isolation│  │
 │  static) │         │  └───────┬───────┘     └────────┬─────────┘  │
 └──────────┘         │          │                      │            │
                      │     services / workflow         │            │
                      │          │                      ▼            │
                      │   gogas-persistence     TenantRoutingDataSource
                      │   (JPA + Flyway)                │            │
                      └──────────────────────────────────┼───────────┘
                                                          │
                           ┌──────────────┬───────────────┼──────────────┐
                           ▼              ▼               ▼              ▼
                        master DB     tenant DB "A"   tenant DB "B"   ...
                        (tenants      (gas A data)    (gas B data)
                         registry)
```

Each tenant (one GAS) has its own SQL Server database. A **master database** holds a `tenants` table that maps a tenant id to its database connection. The tenant is resolved from the request host name (the innermost subdomain, e.g. `gasname.aequos.eu → gasname`) on every request, and the datasource is routed accordingly.

## Technology summary

**Backend**
- Java 11, Spring Boot 2.7.2 (Maven multi-module)
- Spring Data JPA + Hibernate, SQL Server, Flyway migrations
- Spring Security with stateless JWT (auth0 `java-jwt`)
- Database-per-tenant multi-tenancy
- OpenFeign (Aequos platform integration), email/push/Telegram notifications
- Apache POI (Excel) and iText (PDF) report generation
- JUnit 5 + Testcontainers (real SQL Server) integration tests

**Frontend**
- React 17 + TypeScript, built with Vite 4
- Material-UI v4, Redux Toolkit, React Router v5
- axios HTTP client, notistack notifications
- UI language: Italian (no i18n layer)
