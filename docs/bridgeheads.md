# Bridgeheads

A bridgehead is registered by its variables `BRIDGEHEADS_CONFIG_<ID>_*`. `<ID>` is the bridgehead id in upper case,
with `MINUS` for a hyphen (`dresden-test` → `BRIDGEHEADS_CONFIG_DRESDENMINUSTEST_*`). Any variable of an id
registers it. The config repositories generate them (`production/generate_bridgeheads.sh`).

| Variable | Meaning |
|---|---|
| `HUMANREADABLE`, `AFFILIATION` | Name shown in the UI and in emails |
| `EXPLORERID` | Collection id the Explorer sends for this site when it creates a request |
| `FOCUSBEAMID`, `FILEDISPATCHERBEAMID`, `TOKENMANAGERID` | Beam ids of the site's focus, file dispatcher and token manager (DataSHIELD) |
| `GROUP` | Site group of the identity provider; empty = the bridgehead id |
| `CONTACTS_<n>_*`, `APPROVALCONTACTS_<n>_*` | Contacts and local approval contacts (name and description per language, email address) |

## Unique ids

The explorer, focus and token manager ids must be unique: two bridgeheads with the same id stop the start
(`Bridgeheads with the same explorer id: ...`). Before, the last one silently won and the other was unreachable.

## Site groups

The bridgehead roles come from the groups of the token: `BK_USER_GROUP_PREFIX` + site group +
`BK_USER_GROUP_SUFFIX` (researcher of the site) and `BK_ADMIN_GROUP_PREFIX` + site group + `BK_ADMIN_GROUP_SUFFIX`
(bridgehead admin). The site group is `GROUP`, or the bridgehead id without it. A group gives its role on **every**
registered bridgehead with that site group, so one group can serve several bridgeheads of a site:

```
BRIDGEHEADS_CONFIG_BERLIN_EXPLORERID=lens-berlin              # site group: berlin (the id)
BRIDGEHEADS_CONFIG_BERLINMINUSTEST_EXPLORERID=lens-berlin-test
BRIDGEHEADS_CONFIG_BERLINMINUSTEST_GROUP=berlin               # site group: berlin
```

With `BK_USER_GROUP_PREFIX` = `BK_ADMIN_GROUP_PREFIX` = `/projects/dktk/bridgeheads/`, `BK_USER_GROUP_SUFFIX=/user`
and `BK_ADMIN_GROUP_SUFFIX=/admin`, the group `/projects/dktk/bridgeheads/berlin/admin` makes the user bridgehead admin
of `berlin` and `berlin-test`, `/projects/dktk/bridgeheads/berlin/user` researcher of both. A site group
without a registered bridgehead on this server gives no role. Groups are compared case-insensitively.
