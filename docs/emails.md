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
| `PROJECT_BACK_TO_REVIEW` | `CENTRAL_APPROVAL` -> `REVIEW` | creator |
