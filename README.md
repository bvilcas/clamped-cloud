# Clamped Cloud

Issue tracking for student teams. Clamped aggregates, assigns, and triages programming
issues through a fixed ticket lifecycle, and guarantees that every fix is confirmed by
somebody other than the person who wrote it.

---

## Who it is for

Hackathon teams, capstone groups, and clubs. These teams share three problems that
general-purpose trackers handle badly:

- **They turn over every term.** A new member has to be useful in an afternoon, so the
  tool cannot start with a configuration exercise.
- **Nobody owns the process.** There is rarely a project manager, so quality has to come
  from the tool rather than from someone enforcing it.
- **Work gets marked done by the person who did it.** On a student team, "it works on my
  machine" often ends the conversation.

Clamped is deliberately small in response. Roles are fixed rather than configurable, the
workflow has five states and no editor, and the one rule it does enforce is the one teams
skip on their own.

---

## The two-person rule

**Whoever works on an issue cannot be the one who verifies it.**

Two roles exist on any given issue: the `ASSIGNEE` who patches it and the `VERIFIER` who
confirms the fix. A user cannot hold both on the same issue, and the server enforces this
rather than the interface suggesting it.

Role history is what makes it hold. When somebody steps off an issue their role is
revoked, but the record that they once held it survives, so a member cannot take the
assignee role, patch the bug, drop the role, and come back as their own verifier. The
history is what the check reads. It lives in `userissue/`.

---

## How an issue moves

```
REPORTED → IN_PROGRESS → PATCHED → UNDER_REVIEW → VERIFIED
```

Five states, in order, with no branching and no custom columns. Anybody on the project can
move an issue along; only the verifier closes it out at the end.

Three roles attach to an issue, separate from project roles:

| Role on an issue | What it means                               |
|------------------|---------------------------------------------|
| `REPORTER`       | Filed it                                     |
| `ASSIGNEE`       | Working to resolve it                        |
| `VERIFIER`       | Confirms the fix, and cannot be the assignee |

---

## What a ticket holds

Title, description, type (`SECURITY`, `RELIABILITY`, `PERFORMANCE`, `UX`, `OTHER`),
severity (`LOW` through `CRITICAL`), status, due date, and the repository and commit the
fix landed in.

A ticket also carries optional event fields, meaning app, host, event type, and a JSON
payload. These are filled in automatically when a ticket is promoted from a captured
error, and left blank when somebody files one by hand.

---

## Errors become tickets

Running applications push events to the cloud with a project API key:

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

Events with the same fingerprint (SHA-256 of message, source, and environment) are
deduplicated and counted rather than filed twice, so three hundred identical crashes
become one event group that says 300, not three hundred tickets nobody reads. A group
worth fixing gets promoted into an issue, carrying its app, host, and payload with it, and
from there it follows the same lifecycle as anything filed by hand.

This half of the product is where [clamped-local](https://github.com/bvilcas/clamped-local/tree/main)
came from. Catching an exception and shipping it somewhere useful turned out to be a
problem worth solving on its own, so it grew into a self-hosted SDK and server that a
team can run without an account anywhere.

---

## Roles and permissions

| Capability                            | Member | Lead |
|---------------------------------------|:------:|:----:|
| Report an issue                       |   ✓    |  ✓   |
| Self-assign as assignee or verifier   |   ✓    |  ✓   |
| Remove themselves from an issue       |   ✓    |  ✓   |
| Move an issue through the workflow    |   ✓    |  ✓   |
| Update title, description, severity   |   ✓    |  ✓   |
| Assign or remove *other* members      |        |  ✓   |
| Invite, promote, or remove members    |        |  ✓   |
| Delete an issue                       |        |  ✓   |

Two project roles, `LEAD` and `MEMBER`, and that is the whole permission model. Whoever
creates a project is its lead.

---

## Tech Stack

**Backend**: Java 25, Spring Boot 3.5.3, Spring Security (JWT), Spring Session (JDBC),
JPA/Hibernate, PostgreSQL

**Frontend**: Vue 3, Vuetify 3, Pinia, Vue Router, Vite

Running it locally: `start-dev.ps1` brings up the database, the API, and the dev server.

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
│       ├── event/                  # Event ingestion, grouping, deduplication
│       ├── issue/                  # Issue CRUD, status transitions, reporting
│       ├── jwtconfig/              # JWT service, security filter chain
│       ├── message/                # Project messages
│       ├── notification/           # Notifications + email service
│       ├── project/                # Project management
│       ├── sessionconfig/          # Spring Session configuration
│       ├── user/                   # User profile, password, search
│       ├── userissue/              # Issue assignments, and the two-person rule
│       ├── userproject/            # Project membership and roles
│       └── webconfig/              # CORS configuration
└── frontend/                       # Vue 3 SPA
    └── src/
        ├── components/             # TopBar, SideBar, CodeSnippetEditor
        ├── dto/                    # TypeScript interfaces matching backend DTOs
        ├── router/                 # Vue Router with auth guards
        ├── stores/                 # Pinia auth store
        ├── utils/                  # fetchWithAuth, authService, settingsStorage
        └── views/                  # One file per page
            ├── Home.vue                # Landing page, signed out
            ├── Dashboard.vue           # Where you left off
            ├── Issues.vue              # The issue list
            ├── IssueDetail.vue         # One issue: actions, roles, progress
            ├── Report.vue              # File an issue
            │
            ├── Projects.vue            # Projects you belong to
            ├── ProjectPage.vue         # One project
            ├── CreateProject.vue       # New project
            ├── UpdateProjectPage.vue   # Project settings
            ├── Team.vue                # Members: invite, change role, remove
            │
            ├── Events.vue              # Event groups waiting to be triaged
            ├── Calendar.vue            # Due dates for open issues
            ├── Messages.vue            # Per-project messaging
            ├── Notifications.vue       # The feed behind the bell
            │
            ├── Login.vue  Register.vue  Logout.vue
            ├── Profile.vue  Settings.vue
            └── Help.vue  Contact.vue  About.vue
```
