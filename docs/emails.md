# Emails

## When an email is sent

An email needs two things:

1. **Its definition in the code**: `@EmailSender(templateType, recipients)` on an endpoint (or a job that sends it),
   which says *when* and *to whom*.
2. **Its template in the configuration**: `EMAIL_TEMPLATES_CONFIG_PATH` (`email-templates.json`) and the file in
   `EMAIL_TEMPLATES_DIRECTORY`, which say *what*.

Without a template for the type and the recipient's role, the email is **not sent - silently** (no error, no
notification). A deployment chooses its emails by configuring their templates. Sending itself is the optional module
`EMAILS` ([optional-modules.md](optional-modules.md)).

## email-templates.json

```json
{
  "templates": {
    "LOCAL_APPROVAL_STARTED": {
      "subject": "Local approval of <project-code/>",
      "attachment_files": ["FORM"],
      "files": {
        "BRIDGEHEAD_APPROVAL_CONTACT": "local-approval-started-contact.html",
        "CREATOR": "local-approval-started-creator.html",
        "DEFAULT": "local-approval-started.html"
      },
      "bridgeheads": {
        "jurassic-park": {
          "subject": "Local approval at Jurassic Park: <project-code/>",
          "files": { "BRIDGEHEAD_APPROVAL_CONTACT": "local-approval-started-contact-jurassic-park.html" }
        }
      }
    }
  }
}
```

- `files`: template per role of the recipient (`ProjectRole`); `DEFAULT` for every other role.
- `bridgeheads` (optional): templates of one site (key: bridgehead id), e.g. for a site's own local approval process.
  `subject` there is optional (default: the general one); `attachment_files` are always the general ones.
- Lookup: bridgehead + role, bridgehead + `DEFAULT`, role, `DEFAULT`; nothing found: no email.

## Recipients

`EmailRecipientType` resolves the recipients when the endpoint succeeded. Besides the users of the project manager
(creator, bridgehead admins, project manager admins, ...):

- `BRIDGEHEAD_APPROVAL_CONTACTS`: the approval contacts of the bridgehead (or of all bridgeheads of the request),
  configured with `BRIDGEHEADS_CONFIG_<BH>_APPROVALCONTACTS_<n>_NAME_<LANG>`, `_DESCRIPTION_<LANG>`,
  `_EMAILADDRESS`. They are not users: their template role is `BRIDGEHEAD_APPROVAL_CONTACT` (only for emails, never
  for permissions) and `<email-to-name/>` is the configured name in the default language.

### One person, several roles

A user can be a recipient several times in one action (e.g. project manager admin and committee member, or creator
and site admin). Each role can have its own template, so one action can mean different texts for the same person:
those are all sent. Only emails that would be identical are merged: same address, same type, same template file,
same subject and same site. Of those, one is sent (with the first role in time order, `ProjectRolesUtils`). This
applies across all `@EmailSender` annotations of the endpoint.

## Emails by project state

`@EmailSender(projectStates = {...})` sends the email only when the request is in one of these states after the call.
The upload of a local approval uses it for two texts: `NEW_LOCAL_APPROVAL` (in `LOCAL_APPROVAL`) and
`NEW_LOCAL_APPROVAL_FINAL` (in `FINAL`: the site can export now).

## Emails of the local approval

| Type | When | Recipients |
|---|---|---|
| `LOCAL_APPROVAL_STARTED` | `CENTRAL_APPROVAL` -> `LOCAL_APPROVAL` | approval contacts (with the form: `attachment_files: ["FORM"]`), creator, bridgehead admins |
| `NEW_LOCAL_APPROVAL` | local approval uploaded in `LOCAL_APPROVAL` | creator, bridgehead admin of the site, project manager admins |
| `NEW_LOCAL_APPROVAL_FINAL` | local approval uploaded in `FINAL` | creator, bridgehead admin of the site, project manager admins |

## Emails of the central approval

| Type | When | Recipients |
|---|---|---|
| `NEW_PROJECT_ACCEPTED` | technical approval: `REVIEW` (or `ARCHIVED`) -> `CENTRAL_APPROVAL` | creator, bridgehead admins, central approval committee |
| `PROJECT_BACK_TO_REVIEW` | request changes (with message) in `REVIEW` (stays) or `CENTRAL_APPROVAL` (-> `REVIEW`) | creator, project manager admins, central approval committee |

The committee's addresses come from the table `central_approval_committee_user`, filled when a member logs in (like
the project manager admins); its template role is `CENTRAL_APPROVAL_COMMITTEE`. A member who has never logged in gets
no email.

## Messages

The message of an action (`@Message`: reject, request changes, ...) is put into the email (`<message/>`) and also
stored as a notification of the request (`OperationType.MESSAGE`, `details` = the message, `email` = its author), also
when emails are disabled.
