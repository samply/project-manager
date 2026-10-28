# Project manager admins, committee members and site admins

**Status:** Decision (2026-10-27, D12 of the central committee plan)
**Decision:** The project manager admin can add and remove project manager admins, committee members and site admins
by hand in the configuration page. These lists only decide **who gets emails**. Permissions always come from the
identity provider's groups.

## Why the lists exist

The backend only learns who has a role when that person logs in: the role comes from the `groups` claim of the token
(`PM_ADMIN_GROUPS`, `CENTRAL_APPROVAL_COMMITTEE_GROUPS`, `BK_ADMIN_GROUP_PREFIX/SUFFIX`). Emails to "all project
manager admins", "the committee" or "the site admins" are sent by the backend at any time, so it needs their
addresses beforehand. That is why each login stores them (`NewUsersImporter`):

| Role | Table |
|---|---|
| Project manager admin | `project_manager_admin_user` |
| Central approval committee | `central_approval_committee_user` |
| Site admin (per site) | `bridgehead_admin_user` (email + bridgehead) |

Two problems remain with the login alone:

- Someone who has the group but has **not logged in yet** gets no emails.
- Someone who **lost the group** keeps getting emails: nothing removes them.

## Adding and removing by hand

Endpoints, project manager admin only (configuration page, module `USER`):

| List | GET (list), POST (add), DELETE (remove) | Parameters |
|---|---|---|
| Project manager admins | `/project-manager-admins` | `email` |
| Committee members | `/central-approval-committee-members` | `email` |
| Site admins | `/bridgehead-admins` | `email`, `bridgehead` (a site of this deployment) |

The lists show the name from the user table once the person has logged in; before, only the address.

What adding and removing mean:

- **Adding gives emails, not permissions.** Someone added by hand gets the emails at once, also before the first
  login. They can only act in the project manager with the group.
- **Removing stops the emails, not the permissions.** Only that row is deleted: never the user, their projects or
  their notifications. Someone removed who still has the group is **added again at the next login**. To take a role
  away for good, remove the person from the group in the identity provider.
- **The first site admin is the site's contact** (lowest id) when none is configured: `BRIDGEHEAD_ADMIN_*` in the
  emails and the name in the results table. It is computed each time, also for old projects, so removing that admin
  makes the next one the contact.

No other table references these lists, so removing an entry cannot break anything else. The notifications stay
intact: they store the acting person's address as plain text and look the name up in the user table, which a removal
never touches.

## Why no synchronization with the identity provider

Reading the groups from the identity provider (e.g. the Keycloak admin API) would keep the lists exact. We decided
against it on purpose:

- The identity provider changes: Keycloak, then Authentik, then Keycloak again so far, and it may change again.
- Other deployments may not use Keycloak at all (DKTK uses Authentik).
- The VerbIS Keycloak is deliberately closed: there is no console access, and mcauth exists so that nobody needs it.
  Access to its admin API is even less realistic.

The backend only relies on the token's `groups` claim (OIDC standard) and on what the project manager admin maintains
by hand.
