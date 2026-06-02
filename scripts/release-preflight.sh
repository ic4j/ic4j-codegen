#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Checking Central Portal env vars..."
[[ -n "${CENTRAL_PORTAL_USERNAME:-}" ]] || { echo "CENTRAL_PORTAL_USERNAME is not set" >&2; exit 1; }
[[ -n "${CENTRAL_PORTAL_PASSWORD:-}" ]] || { echo "CENTRAL_PORTAL_PASSWORD is not set" >&2; exit 1; }

echo "Checking signing config..."
if [[ -z "${SIGNING_PASSWORD:-}" ]]; then
  echo "- SIGNING_PASSWORD not set in shell; Gradle will resolve from gradle.properties"
else
  echo "- SIGNING_PASSWORD: set"
fi

echo "Checking Maven settings server ids..."
grep -q ossrh "$HOME/.m2/settings.xml" && echo "- ossrh: found" || echo "- ossrh: missing from ~/.m2/settings.xml"
grep -q central "$HOME/.m2/settings.xml" && echo "- central: found" || echo "- central: missing from ~/.m2/settings.xml"

echo "Checking Central Portal credentials..."
python3 "$ROOT_DIR/scripts/central-auth-check.py"

echo "Preflight checks passed."