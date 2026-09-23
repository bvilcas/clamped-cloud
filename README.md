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

Title, description, severity (`LOW` through `CRITICAL`), status, due date, and the
repository and commit the fix landed in. A ticket belongs to exactly one project.

```
title:        Login button unresponsive on mobile Safari
projectName:  cs3744-capstone
description:  Submit button does nothing on tap; works on desktop. Repro on iOS 18.
severity:     MEDIUM
status:       REPORTED

- event info -   (optional, if the reporter wants to reference the stack trace)
eventApp:     capstone-frontend
eventHost:    dev-localhost
eventType:    CLICK
eventExtra:   {"component": "LoginButton", "browser": "Mobile Safari", "handlerFired": false}
```

The four event fields are optional and nothing fills them in for you. They are there for
the case where somebody already has the crash in front of them, in a console or a log,
and wants to paste the useful parts onto the ticket instead of describing them in prose.
A ticket without them reads perfectly well; a ticket with them saves the next person from
asking which browser, which host, and what the payload looked like.

That small idea is where [clamped-local](https://github.com/bvilcas/clamped-local/tree/main)
came from. Catching an exception and getting it somewhere useful turned out to be a
problem worth solving properly, so it grew into a self-hosted SDK and server of its own
rather than staying four text boxes on a form.

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

## The redesign

The interface is settled in a separate repository as five static pages, and the port is
in progress here. That repository holds the markup, the class names, the copy, and a
porting document that lists every change this one needs, with the open questions marked.
Read it before changing the views.

The short version: twenty-three views become twelve, Vuetify comes out, invitations are
new backend work, project messaging re-scopes to per-issue notes, and whether event
groups keep a page of their own is still open.

---

## Tech Stack

**Backend**: Java 25, Spring Boot 3.5.3, Spring Security (JWT), Spring Session (JDBC),
JPA/Hibernate, PostgreSQL

**Frontend**: Vue 3, Pinia, Vue Router, Vite

Vuetify is being removed. The interface is a plain stylesheet with a custom-property
block at the top, and Vuetify's typography reset fights the 15px/1.5 scale the layout
depends on. Only two things still need real component behaviour: the notification panel
behind the bell, and the confirm dialog on deletes.

Running it locally: `start-dev.ps1` brings up the database, the API, and the dev server.

---

## Project Structure

```
clamped-cloud/
├── backend/                        # Spring Boot application
│   └── src/main/java/io/clamped/cloud/
│       ├── authentication/         # Login, register, JWT filter
│       ├── backendconfig/          # Global exception handler, data initializer
│       ├── calendar/               # Due dates, folding into the dashboard and list
│       ├── contact/                # Contact form. No page in the new design
│       ├── event/                  # Event groups behind a ticket
│       ├── issue/                  # Issue CRUD, status transitions, reporting
│       ├── jwtconfig/              # JWT service, security filter chain
│       ├── message/                # Project messages, re-scoping to issue notes
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
        └── views/                  # Twelve pages after the port, from 23
            ├── Home.vue                # Landing page, with the sign-up card
            ├── Login.vue               # Sign in
            ├── Dashboard.vue           # Where you left off
            ├── Issues.vue              # The issue list, and a project's issues
            ├── IssueDetail.vue         # One issue: actions, roles, progress
            ├── EditIssue.vue           # Title, description, severity, due date
            ├── Report.vue              # File an issue
            ├── Projects.vue            # Projects you belong to, and creating one
            ├── ProjectPage.vue         # One project: roster, settings, issues
            ├── Settings.vue            # Profile, password, notifications
            ├── AcceptInvite.vue        # Reachable while signed out
            └── NotFound.vue
```
