# CI: tests and images

`.github/workflows/build.yml` runs the tests (`mvn -B package`) and builds the image `samply/project-manager`
(linux/amd64 and linux/arm64). Whether the image is pushed to Docker Hub, and with which tags, depends on the event:

| Event | Tests | Image | Docker tags |
|---|---|---|---|
| Push to `develop` (a merged pull request is a push too) | yes | built and pushed | `develop` |
| Push to `main` | yes | built and pushed | `main`, `latest` |
| Git tag `vX.Y.Z` | yes | built and pushed | `X.Y.Z`, `X.Y` |
| Every day at 10:00 UTC (schedule; runs on the default branch `main`) | yes | built and pushed | `main`, `latest` |
| Pull request into `develop` or `main` | yes | built, **not pushed** | none |

- A pull request builds the image only to catch a broken build before the merge; nothing reaches Docker Hub. The image
  comes with the merge, because the merge is a push to the target branch.
- A commit pushed directly to `develop` (only possible for whoever may bypass the branch protection) is a push as
  well: it publishes `develop` again.
- The daily run rebuilds only `main`. The backend's `develop` image changes only when something is pushed to
  `develop`. The config repositories (dktk-project-manager, cce-project-manager) rebuild their `develop` image every
  night on top of the backend's `develop`.
- After each push the Trivy scanner checks the pushed image (by its branch or version tag) and uploads the result to
  the repository's *Security* tab.
- No `pr-N` or `sha-…` tags any more (2026-11-02): they filled Docker Hub with one tag per build and nothing used them.
  Old ones are deleted by hand.
