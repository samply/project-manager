# Common issues

Problems that look like bugs but come from configuration or request data, with how to recognize and fix them.

## The feasibility table is missing from a request

**Symptom:** feasibility is enabled (`ENABLE_FEASIBILITY=true`), and most requests show the feasibility table under "Selected Sites". One request shows only the site chips instead.

**Cause:** the table needs more than `/feasibility/enabled`. The UI shows it only when all of these are true for the current user and request:

| Condition | Where it's defined |
|---|---|
| `ENABLE_FEASIBILITY=true` | `IS_FEASIBILITY_ENABLED` (`/feasibility/enabled`) |
| The user has the organisation role `RESEARCHER` or `PROJECT_MANAGER_ADMIN` | `@RoleConstraints` on `IS_FEASIBILITY_ENABLED` and `FETCH_FEASIBILITY` |
| The request is in `DRAFT` or `REVIEW` | `@StateConstraints` on `FETCH_FEASIBILITY` |
| **The request's query format is `AST_DATA`** | `@ProjectConstraints(queryFormats = {QueryFormat.AST_DATA})` on `FETCH_FEASIBILITY` |

If one of them fails, the UI treats feasibility as disabled for that request and shows the site chips, without a message.

The usual case is the query format. Requests created with the dashboard's **Create request** button have no Explorer URL, so the creator sets the query and its format in the request form. If the format is changed to something else (for example `FHIR_SEARCH`), the table disappears for that request.

The "Selected Sites" field (and the table in it) is shown only in the **Query** and **Summary** steps of the draft dialog, and in the request view outside the dialog.

**How to check:**
- In the UI, the Query step shows the request's "Query Format".
- In the database:
  ```sql
  select p.code, p.state, q.label, q.query_format
  from samply.project p
  left join samply.query q on q.id = p.query_id
  order by p.modified_at desc;
  ```
- In the browser DevTools → Network, the page's list of actions contains `FETCH_FEASIBILITY` under `PROJECT_BRIDGEHEAD_MODULE` only when every condition above is true.

**Fix:** set the request's Query Format back to `AST_DATA`. The UI decides once when the page loads, so reload the request page afterwards.
