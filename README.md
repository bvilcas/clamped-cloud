# Clamped Cloud

Issue tracking for student teams. Clamped aggregates, assigns, and triages programming
issues through a fixed ticket lifecycle, and guarantees that every fix is confirmed by
somebody other than the person who wrote it.

Part of the [Clamped](https://github.com/bvilcas) monorepo alongside **clamped-local**
(self-hosted SDK + server).

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

## Where this is heading

Clamped started as a general error-monitoring and security-tracking platform, and it grew
the surface area that comes with that: a CVE lookup, a vulnerability view, project
messaging, notifications, and a calendar. The direction now is narrower.

**Refocusing on student teams.** The audience is a capstone or hackathon team, not a
security organisation. Features that only pay off at company scale move out of the main
path, and the language moves with them: tickets and projects rather than vulnerabilities
and fleets.

**The two-person rule moves to the front.** It is the reason to choose Clamped over a
spreadsheet or a Trello board, and it currently sits behind screens that never mention it.
The redesigned issue page states the rule in context and shows the workflow as a five-step
progress trail rather than a status dropdown.

**Fewer screens, denser screens.** Twenty-three views is more than this product needs. The
core experience is five: a landing page, a dashboard, the issue list, the issue detail,
and the report form. Each is meant to fit a laptop screen without scrolling, because the
current layout buries a short list under a long one.

**Interface first.** Those five screens were rebuilt in plain HTML and CSS before any of
it becomes Vue, so the layout, density, and wording could be settled without fighting a
component library. The Vue views follow that prototype rather than the other way round,
and Vuetify comes out with them. The prototype and its notes live in **clamped-redesign**.

**Calendar and messaging stay, quietly.** Deadlines and changing requirements are real
problems for a team that meets twice a week. Both remain, as support for the ticket flow
rather than destinations of their own.

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
