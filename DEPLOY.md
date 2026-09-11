# Deploy

## Prerequisites

- Use JDK 21 for publishing tasks.
- Publishing is configured in the tracked `build.gradle`.
- Signing can come from `gradle.properties` or environment variables.
- Central credentials can come from either:
  - `centralUsername` and `centralPassword`
  - `ossrhToken` and `ossrhTokenPassword`
  - `CENTRAL_PORTAL_USERNAME` and `CENTRAL_PORTAL_PASSWORD`

## Local Env Scripts

Helper scripts live under `scripts/`.
Credentials are read from `~/.m2/maven-central.properties` using `ossrhUsername` and `ossrhPassword`.

| Script | Purpose |
|---|---|
| `load-maven-env.sh` | Reads `~/.m2/maven-central.properties`, exports `CENTRAL_PORTAL_USERNAME`, `CENTRAL_PORTAL_PASSWORD`, `SIGNING_PASSWORD`, sets `JAVA_HOME` |
| `release-preflight.sh` | Checks env vars, Maven settings, and verifies the Central Portal token |
| `central-auth-check.py` | Verifies credentials against the Central Portal API |

Load the environment before any publish step:

```bash
source scripts/load-maven-env.sh
```

Run preflight checks before a real release:

```bash
source scripts/load-maven-env.sh
scripts/release-preflight.sh
```

## Dry Runs

Run dry runs first to validate task wiring and credentials without uploading artifacts:

```bash
source scripts/load-maven-env.sh
./gradlew --no-daemon publishToMavenLocal --dry-run --console=plain
```

## Test Matrix

### JDK 8 compile and test

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean test --console=plain
```

### JDK 11 test

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean test --console=plain
```

### JDK 21 test

```bash
source scripts/load-maven-env.sh
./gradlew cleanTest test --console=plain
```

## Full Local Validation

```bash
source scripts/load-maven-env.sh
./gradlew cleanTest test
```

## Local Maven

```bash
source scripts/load-maven-env.sh
./gradlew --no-daemon publishToMavenLocal --console=plain
```

Artifacts are published under:

```bash
~/.m2/repository/org/ic4j/ic4j-codegen/0.8.5
```

## Central Portal Release

```bash
source scripts/load-maven-env.sh
scripts/release-preflight.sh
./gradlew --no-daemon \
  -PreleaseRepositoryUrl="<Maven-compatible-release-repository>" \
  -PreleaseRepositoryUsername="$CENTRAL_PORTAL_USERNAME" \
  -PreleaseRepositoryPassword="$CENTRAL_PORTAL_PASSWORD" \
  publishMavenJavaPublicationToReleaseRepository --console=plain
```

## Central Snapshots

```bash
source scripts/load-maven-env.sh
./gradlew --no-daemon \
  -PreleaseRepositoryUrl="<Maven-compatible-snapshot-repository>" \
  -PreleaseRepositoryUsername="$CENTRAL_PORTAL_USERNAME" \
  -PreleaseRepositoryPassword="$CENTRAL_PORTAL_PASSWORD" \
  publishMavenJavaPublicationToReleaseRepository --console=plain
```

## Useful Checks

```bash
./gradlew tasks --console=plain
./gradlew publishToMavenLocal --info
```

Release checklist for 0.8.0 and later:

1. Run `./gradlew clean test` on JDK 8.
2. Run `./gradlew clean test` on JDK 11.
3. Run `source scripts/load-maven-env.sh && ./gradlew cleanTest test` on JDK 21.
4. Run `source scripts/load-maven-env.sh && ./gradlew publishToMavenLocal` on JDK 21.
5. Run `source scripts/load-maven-env.sh && scripts/release-preflight.sh`.
6. Publish only after the JDK 8, 11, and 21 validation steps all pass.