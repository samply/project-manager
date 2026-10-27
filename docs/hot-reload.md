# Hot reload during local development

The backend uses `spring-boot-devtools`: when the backend runs from the IDE, a change in a watched directory restarts
the application context in the running JVM (a few seconds instead of a new start). It only works locally: the
`spring-boot-maven-plugin` leaves devtools out of the packaged jar, and devtools switches itself off in a packaged
application, so the Docker images and the servers are not affected.

## What restarts the backend

- Compiled classes of the backend (IntelliJ: *Build Project*, or *Build project automatically*).
- The configuration files of the deployment, when the config repository's `development/development.env` sets the
  watched directory:

```
SPRING_DEVTOOLS_RESTART_ADDITIONALPATHS="${ROOT_DIRECTORY}\production"
SPRING_DEVTOOLS_RESTART_POLLINTERVAL=1000ms
SPRING_DEVTOOLS_RESTART_QUIETPERIOD=400ms
SPRING_DEVTOOLS_ADDPROPERTIES=false
SPRING_DEVTOOLS_LIVERELOAD_ENABLED=false
```

Spring maps environment variables to properties without the dashes (`additional-paths` -> `ADDITIONALPATHS`).
Only `production/` is watched: `documents/` (uploaded documents) is in the same repository, and an upload must not
restart the backend. `ADDPROPERTIES=false` keeps the devtools defaults (debug web logging, no caches) out, so the local
backend behaves like the deployed one apart from the restarts; the LiveReload server is not needed (the UI is a
separate application).

## What does not

- **`development.env` and `development-secret.env`**: the IDE passes them to the JVM as environment variables when
  it starts it; a devtools restart keeps the same JVM and therefore the old values. After changing them: *Stop* and
  *Run* the backend.
- `pom.xml` (new libraries): reload Maven and start again.
