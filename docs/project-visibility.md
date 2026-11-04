# Who sees which request

**Status:** Decision (2026-10-28)
**Decision:** Project manager admins and the central approval committee see every request in their lists except the
drafts of others. Hiding a draft only applies to lists: both roles can still open any request, a draft included, by
its project code. Site admins see the drafts that include their site, because researchers often ask them first.

## The lists

Every list of requests is built by one rule, `ProjectService.buildUserVisibilitySpecification()`. A user sees a
request if at least one of these is true:

| Criterion | Who |
|---|---|
| The user created it | everybody |
| It has one of the user's sites (bridgeheads), and the user is site admin there or was assigned to that site of the request | site admins, researchers |
| It is no draft | project manager admins, central approval committee |

So project manager admins and the committee see the same requests: all requests that are no draft, and their own
drafts. A draft belongs to its creator until it is sent; the central roles do not need it in their lists.

Site admins are different: the site criterion does not look at the state, so a site admin sees every request that has
their site, drafts included (a draft from the Explorer already has its sites). This is intended, see below.

The same rule feeds everything that is derived from "the user's requests":

- the dashboard (`fetchUserVisibleProjects`, with its state, archive, creator and site filters on top),
- the options of the dashboard filters (`fetchAllUserVisibleProjects`, `fetchVisibleProjectStates`): a filter only
  offers what the list contains, so DRAFT is offered only to someone who has a draft,
- the notifications and sites of all requests (when no request is given).

The state filter is only a filter. It does not decide which drafts someone sees: that is the visibility rule alone.

## Opening a single request

Opening a request by its code is not decided by the list rule but by the endpoint constraints (`@RoleConstraints`,
`@StateConstraints`). `PROJECT_MANAGER_ADMIN` and `CENTRAL_APPROVAL_COMMITTEE` are allowed in every state, so both can
open a draft of someone else if they know its code.

This is intended. A researcher who is stuck with a draft can ask the project manager admin for help, and the admin
can open it. Normally, drafts stay hidden because they do not appear in any list. The project manager admin gets a lot
of flexibility on purpose: they know the tool best and solve the problems of the others.

## Site admins see the drafts of their site

Many researchers contact their site admin before the project manager admin: they work at the same place, and often
they know each other. The site admin has experience with data requests and knows the site's own systems and
databases, so they are a good first help while a request is still being written. That is why a site admin sees the
drafts that include their site in their lists, while project manager admins and the committee, who only act once a
request has been sent, do not.

## History

Until 2026-10-28 the project manager admin had no visibility restriction at all, and the drafts were hidden in two
other places: the state filter excluded DRAFT for admins when no state was requested, and the list of states for the
filter dropdown dropped DRAFT for admins. The effect was the opposite of the intention:

- the drafts of others were still reachable through the API (`projectState=DRAFT` returned all of them), and through
  the notifications and site lists of the dashboard,
- the admin's own drafts never appeared in the dashboard.

When the central approval committee was added, it got the intended rule as a criterion of the visibility rule. The
admin then got the same criterion, and the two special cases were removed.
