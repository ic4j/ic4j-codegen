#!/bin/zsh

load_maven_env() {
  emulate -L zsh
  set -euo pipefail

  local props_file="${1:-$HOME/.m2/maven-central.properties}"
  local jdk_version="${JDK_VERSION:-21}"

  if [[ ! -f "$props_file" ]]; then
    echo "Credentials properties file not found: $props_file" >&2
    return 1
  fi

  read_prop() {
    local key="$1"
    awk -F= -v target="$key" '
      /^[[:space:]]*#/ {next}
      NF < 2 {next}
      {
        k=$1
        sub(/^[[:space:]]+/, "", k)
        sub(/[[:space:]]+$/, "", k)
        if (k == target) {
          val=substr($0, index($0, "=")+1)
          sub(/^[[:space:]]+/, "", val)
          sub(/[[:space:]]+$/, "", val)
          print val
          exit
        }
      }
    ' "$props_file"
  }

  local ossrh_username ossrh_password gpg_passphrase

  ossrh_username="$(read_prop ossrhUsername || true)"
  ossrh_password="$(read_prop ossrhPassword || true)"
  gpg_passphrase="$(read_prop mavenGpgPassphrase || true)"

  if [[ -z "$ossrh_username" || -z "$ossrh_password" ]]; then
    echo "Missing ossrhUsername/ossrhPassword in $props_file" >&2
    return 1
  fi

  typeset -gx CENTRAL_PORTAL_USERNAME="$ossrh_username"
  typeset -gx CENTRAL_PORTAL_PASSWORD="$ossrh_password"

  if [[ -n "$gpg_passphrase" ]]; then
    typeset -gx SIGNING_PASSWORD="$gpg_passphrase"
  fi

  if [[ -z "${JAVA_HOME:-}" ]]; then
    local detected_home
    detected_home="$(/usr/libexec/java_home -v "$jdk_version" 2>/dev/null || true)"
    if [[ -n "$detected_home" ]]; then
      typeset -gx JAVA_HOME="$detected_home"
      export PATH="$JAVA_HOME/bin:$PATH"
    fi
  fi

  echo "Loaded Maven Central env from $props_file"
  echo "- CENTRAL_PORTAL_USERNAME: set"
  echo "- CENTRAL_PORTAL_PASSWORD: set"
  if [[ -n "${SIGNING_PASSWORD:-}" ]]; then
    echo "- SIGNING_PASSWORD: set"
  else
    echo "- SIGNING_PASSWORD: not set (Gradle will resolve from gradle.properties)"
  fi
  echo "- JAVA_HOME: ${JAVA_HOME:-not set}"
}

load_maven_env "$@"