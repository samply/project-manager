# One project-manager instance, one UI

**Status:** Decision (2026-09-25)
**Decision:** A project-manager backend serves exactly one UI. To run another UI (for example a test lens), deploy another project-manager instance.

## Context

In the beta phase there are two lens UIs (production and test) but only one project-manager server. Until the test VM was ready, one option was to let the single backend accept both UIs. We evaluated this and rejected it.

## What "the UI" means to the backend

The backend has one configured UI, `frontend.base-url` (bound in `FrontendConfiguration`). It is used for more than CORS:

| Use | Where | Effect |
|---|---|---|
| CORS allowed origin | `SecurityConfiguration.corsConfigurationSource()` | Only this origin (plus the Explorer) may call the API from a browser with credentials. |
| Links in emails | `EmailKeyValues.addProject()` → `FrontendService.fetchUrl()` | `PROJECT_VIEW_URL` in notification emails points to this UI. |
| Explorer redirect | `FrontendService.fetchExplorerRedirectUri()` | After a feasibility search, the Explorer sends the user back to this UI. |
| Site paths | `frontend.sites` | Routes such as the project view are resolved against this UI. |
| Frontend variables | `frontend.variables`, `fetch-frontend-variables` | All clients get the same UI settings. |

Adding a second origin to CORS lets a second UI call the API. It does not make the rest of this table aware of that UI.

## Why not accept a second UI

### 1. Emails have no request to tell which UI to use

Many emails are sent by scheduled jobs (for example `ExporterJob` and `DataShieldTokenManagerJob`), not in response to a UI request. There is no `Origin` header to look at. Emails would keep linking to one UI, or the backend would need to store which UI each project (or each user) belongs to.

### 2. Storing the UI per project or per user makes the domain model more complex

For correct links from jobs, the UI would have to be stored:

- **per project:** a new column, a migration for existing projects, and rules for projects used from both UIs or moved between them; or
- **per user:** a stored "home UI" and links built per recipient, which changes every email flow.

Both change the data model for a deployment concern.

### 3. Configuration would have to be per UI

`frontend.sites` and `frontend.variables` are global. Two UIs with different routes or settings would need a map of named UIs, and every place that reads the frontend configuration would need to know which one applies.

### 4. It mixes environments

In our case the second UI is a **test** lens. If it shares a production backend, it also shares the production database, users, projects, emails and bridgehead exports. Test activity would reach real people and real bridgeheads. A separate test instance keeps them apart.

### 5. It does not fit the concept

The project-manager has one UI. Other components interact with it as **integrations**, not as additional UIs:

- the **Explorer** (feasibility search) has its own `EXPLORER_URL`, CORS entry and redirect handling;
- bridgeheads, the exporter and DataSHIELD communicate server-to-server.

A new component should follow this pattern: a dedicated, explicitly configured integration, not another UI origin.

## Consequences

- Each UI gets its own project-manager instance (and database). The test lens gets a test project-manager on the test VM.
- `frontend.base-url` stays a single value, and CORS stays limited to that UI plus configured integrations.
- Emails, redirects and site links stay unambiguous, whether triggered by a request or a job.

## When to revisit

Revisit only if one project must really be worked on from several UIs **of the same environment** (for example white-labelled UIs for the same projects). That would need the per-project or per-user UI model described above, and should be designed as a feature, not as a CORS change.
