# Minimal configuration

What a new deployment must set for the backend to start with all optional modules disabled (no `ENABLE_*` set:
modules are opt-in), and what each module adds. Derived from the code (2026-10-08): every setting without a default that a bean outside the modules reads, plus
`application.yaml`. Not yet proven by a start-up test - that test is planned (`plans/2026-09-30-plan-unit-and-integration-tests.md`,
phase 2, with exactly this set as its configuration).

Examples: `master-project-manager/production/entrypoint.sh` and `production/frontend/frontend-environment.sh`
(values for the master deployment) and its `production/` folder (the files).

## 1. Infrastructure

| Variable | Default | What |
|---|---|---|
| `PROJECT_MANAGER_DB_URL` | `jdbc:postgresql://localhost:5432/project_manager` | PostgreSQL; the tables are created at the first start (Flyway) |
| `PROJECT_MANAGER_DB_USER` / `PROJECT_MANAGER_DB_PASSWORD` | `project_manager` | |
| `OIDC_URL` | - | Issuer of the login server |
| `OIDC_CLIENT_ID` | - | Client of the project manager at the login server |
| `OIDC_CLIENT_SECRET` | empty | Only for a confidential client |

## 2. Core settings without default

Files and folders (the content comes from the deployment repository's `production/` folder):

| Variable | Example (master) | What |
|---|---|---|
| `FORM_FIELDS_DIRECTORY` | `/app/form/form-fields` | The request forms (`*.json`) |
| `FORM_RESOURCES_DIRECTORY` | `/app/form/templates` | Templates of the request PDF |
| `FORM_TEMPLATE_METADATA_DIRECTORY` | `/app/form/templates/metadata` | Metadata of the PDF templates (`request.json`, ...) |
| `FRONTEND_PROJECT_CONFIG_PATH` | `/app/frontend/frontend-project-configs.json` | The request configurations the users choose from; may only offer request types whose modules are enabled |
| `ACTION_MESSAGES_CONFIG_PATH` | `/app/action-messages.json` | Texts of the actions |
| `EMAIL_TEMPLATES_CONFIG_PATH` | `/app/email/email-templates.json` | Which email template for which event and role - needed even with emails disabled: the templates are also rendered in the UI |
| `EMAIL_TEMPLATES_DIRECTORY` | `/app/email/templates` | The email templates |
| `ASSETS_DIRECTORY` | `/app/frontend/assets` | Logos, favicon and other files the frontend loads from the backend |
| `PROJECT_DOCUMENTS_DIRECTORY` | `/app/documents` | Where uploaded documents are stored (writable, persistent) |

Roles, security, explorer:

| Variable | Example (master) | What |
|---|---|---|
| `PM_ADMIN_GROUPS` | `admin-ccp-office` | Login groups of the project manager admins |
| `BK_ADMIN_GROUP_PREFIX` / `BK_ADMIN_GROUP_SUFFIX` | `DKTK_CCP_` | Login group of a site's admins: prefix + site + suffix |
| `BK_USER_GROUP_PREFIX` / `BK_USER_GROUP_SUFFIX` | `DKTK_CCP_` / empty | Login group of a site's users |
| `DB_ENCRYPTION_PRIVATE_KEY_IN_BASE64` | - | Key for the encrypted database columns; generate with `GenerateAESKey.ps1`; never change it afterwards |
| `EXPLORER_URL` | `https://data.dktk.dkfz.de` | The explorer the requests come from (also allowed as origin) |
| `EXPLORER_REDIRECT_URI_PARAMETER` | `redirect_uri` | Parameter name for the way back to the explorer |

## 3. Needed in practice (the start does not fail without them)

- **Sites**: `BRIDGEHEADS_CONFIG_<SITE>_...` (human-readable name, affiliation, contacts, the Beam IDs for the modules
  that talk to the site). Without sites no request can be made.
- **Frontend**: `FRONTEND_BASEURL` and the `FRONTEND_SITES_*` paths (links in emails and notifications);
  `FRONTEND_VARIABLES_*` configure the UI.
- **Email texts**: `EMAIL_CONTEXT_*` (signature, support information), used by the templates.

## 4. Optional modules

Not set (= `false`): nothing more is needed. Each module enabled with `ENABLE_X=true` adds its settings; in `test` mode (where it exists) they
are not needed. Variables and values: `docs/optional-modules.md`.

| Module (`true`) | Adds |
|---|---|
| `EXPORTER` | Beam (`BEAM_URL`, `BEAM_API_KEY`, `BEAM_PROJECT_MANAGER_ID`), `EXPORT_TEMPLATES`, `SAMPLES_TEMPLATES`, `DATASHIELD_TEMPLATES`, `RESEARCH_ENVIRONMENT_TEMPLATES`, `EXPORTER_QUERY_LABEL_TEMPLATE`; the sites' focus Beam IDs |
| `FEASIBILITY` | Beam (as above), `FEASIBILITY_MAPPING`, `FOCUS_LENS_PROJECT` |
| `EMAILS` | SMTP (`SPRING_MAIL_PRIMARY_*`), `PROJECT_MANAGER_EMAIL_FROM` |
| `RESEARCH_ENVIRONMENT` | Coder (`CODER_*`, `coder/*.json`), app register (`APP_REGISTER_*`); needs `EXPORTER`. A working configuration: master-project-manager, commit 17579fd (branch feat/optional-modules, 2026-10-08, new variable names; with the old names ENABLE_CODER/ENABLE_TOKEN_MANAGER also on main, d738236) |
| `DATASHIELD` | `TOKEN_MANAGER_URL`, `MANAGE_TOKENS_CRON_EXPRESSION`; needs `RESEARCH_ENVIRONMENT`. Configuration: as above |
| `EXTERNAL_EXECUTION` | nothing |

Request types follow their modules: with `EXPORTER` disabled, no request type of the current deployments can be
offered (EXPORT, SAMPLES and SEQUENCING need it), so a deployment without the exporter needs a configuration in
`frontend-project-configs.json` without these types - or the exporter in `test` mode for a trial.
