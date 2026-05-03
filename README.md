# Clamped Cloud

Team issue tracking and error monitoring SaaS. Multi-user, multi-project platform where teams report, triage, and resolve security issues and runtime errors together.

Part of the [Clamped](https://github.com/bvilcas) monorepo alongside **clamped-local** (self-hosted SDK + server).

---

## Tech Stack

**Backend**: Java 21, Spring Boot 3.5.3, Spring Security (JWT), Spring Session (JDBC), JPA/Hibernate, PostgreSQL

**Frontend**: Vue 3, Vuetify 3, Pinia, Vue Router, Vite

---

## Features

- **Projects**: Create and manage projects, invite members, assign roles (Lead / Programmer / Tester)
- **Issues**: Report, assign, and track issues through a full lifecycle: `REPORTED → IN_PROGRESS → PATCHED → UNDER_REVIEW → VERIFIED`
- **Events**: Ingest runtime errors and exceptions via API key; group by fingerprint with deduplication; view stacktraces, severity, and source location
- **Assignments**: Self-assign or manager-assign issues per project
- **Messages**: Per-project team messaging
- **Notifications**: In-app notification feed with read/clear controls
- **Calendar**: Due-date view for open issues
- **CVE Lookup**: Search CVE database when reporting issues
- **Contact**: Contact form routed to admin email
- **Auth**: JWT-based login/register with session persistence and refresh tokens

---

## Project Structure

```
clamped-cloud/
├── backend/                        # Spring Boot application
│   └── src/main/java/io/clamped/cloud/
│       ├── authentication/         # Login, register, JWT filter
│       ├── backendconfig/          # Global exception handler, data initializer
│       ├── calendar/               # Issue due-date calendar endpoint
│       ├── contact/                # Contact form → admin email
│       ├── cve/                    # CVE lookup proxy
│       ├── event/                  # Event ingestion, grouping, deduplication
│       ├── issue/                  # Issue CRUD, status transitions, reporting
│       ├── jwtconfig/              # JWT service, security filter chain
│       ├── message/                # Project messages
│       ├── notification/           # Notifications + email service
│       ├── project/                # Project management
│       ├── sessionconfig/          # Spring Session configuration
│       ├── user/                   # User profile, password, search
│       ├── userissue/              # Issue assignments (user ↔ issue)
│       ├── userproject/            # Project membership and roles
│       └── webconfig/              # CORS configuration
└── frontend/                       # Vue 3 SPA
    └── src/
        ├── components/             # TopBar, SideBar, CodeSnippetEditor
        ├── dto/                    # TypeScript interfaces matching backend DTOs
        ├── router/                 # Vue Router with auth guards
        ├── stores/                 # Pinia auth store
        ├── utils/                  # fetchWithAuth, authService, settingsStorage
        └── views/                  # One file per page (23 views)
```

---

## Prerequisites

- Java 21
- Maven
- Node.js 20+
- PostgreSQL (shared with clamped-local via Docker)

---

## Setup

### 1. Start PostgreSQL

From the `clamped-local` directory (shared container):

```bash
docker-compose up -d
```

### 2. Create the database (first time only)

```bash
docker exec -it clamped-postgres psql -U postgres -c "CREATE DATABASE clamped;"
```

### 3. Configure email (optional)

Edit `backend/src/main/resources/application.properties` and fill in:

```properties
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=YOUR_16_CHAR_APP_PASSWORD
app.admin-email=YOUR_GMAIL@gmail.com
```

### 4. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Starts on **port 8081**.

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Starts on **port 3000**. All `/api/*` requests are proxied to `localhost:8081`.

---

## Event Ingestion API

External SDKs can push events to the cloud via:

```
POST /api/v1/events/ingest
X-Api-Key: <project-api-key>
Content-Type: application/json

{
  "message": "NullPointerException in PaymentService",
  "source": "BACKEND",
  "environment": "PRODUCTION",
  "projectId": 1,
  "severity": "HIGH",
  "exceptionClass": "java.lang.NullPointerException",
  "stacktrace": "...",
  "sourceFile": "PaymentService.java",
  "sourceLine": 42,
  "sourceMethod": "processPayment"
}
```

Events with the same `fingerprint` (SHA-256 of message + source + environment) are deduplicated and their occurrence count is incremented.

---

## Ports

| Service                 | Port |
|-------------------------|------|
| clamped-cloud backend   | 8081 |
| clamped-cloud frontend  | 3000 |
| clamped-local backend   | 8080 |
| clamped-local frontend  | 5173 |
| PostgreSQL              | 5432 |
