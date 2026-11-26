#!/usr/bin/env bash
set -euo pipefail
: "${RUNNER_TEMP:?}" "${GITHUB_WORKSPACE:?}"
cd "$GITHUB_WORKSPACE"
repo="$RUNNER_TEMP/codegen-maven"
reports="$RUNNER_TEMP/codegen-pilot-reports"
mkdir "$repo" "$reports"
modules=(candid agent internetidentity management reactnative spring)
for module in "${modules[@]}" codegen; do
  sha="$(git -C "$module" rev-parse HEAD)"
  printf '%s %s\n' "$module" "$sha"
  if [[ "$module" != codegen ]]; then
    var="${module^^}_REV"
    [[ "$sha" == "${!var}" ]]
  fi
done | tee "$reports/sources.txt"

# Retry only dependency rate limiting. Fail normally on compilation/test errors.
g() {
  local module="$1" attempt rc logfile
  shift
  for attempt in 1 2 3; do
    logfile="$reports/$module-$attempt.log"
    if gradle --no-daemon --console=plain -Dmaven.repo.local="$repo" \
      --init-script "$GITHUB_WORKSPACE/agent/ci/local-artifacts.init.gradle" \
      -p "$module" "$@" 2>&1 | tee "$logfile"; then
      return 0
    else
      rc=$?
    fi
    if [[ "$attempt" == 3 ]] || ! grep -Eq 'Received status code 429|HTTP (status )?429' "$logfile"; then
      return "$rc"
    fi
    sleep "$((attempt * 12))"
  done
}

g candid -PrunJavacc=true clean publishToMavenLocal
for module in "${modules[@]:1}"; do
  g "$module" clean publishToMavenLocal
done

(
  cd "$repo"
  for module in "${modules[@]}"; do
    artifact="ic4j-$module"
    sha256sum "org/ic4j/$artifact/0.8.5/$artifact-0.8.5.jar"
  done
) | tee "$reports/inputs.sha256"

# check already compiles Java sources emitted by the generator test suite.
g codegen clean check fatJar
python3 - <<'PY' | tee "$reports/test-counts.txt"
from pathlib import Path
from xml.etree import ElementTree as ET

cases = [c for xml in Path('codegen/build/test-results/test').glob('TEST-*.xml')
         for c in ET.parse(xml).getroot().iter('testcase')]
ran = [c for c in cases if c.find('skipped') is None]
regressions = [c for c in ran if c.attrib['classname'].endswith('.GeneratorRegressionTest')]
assert regressions, 'GeneratorRegressionTest did not run'
compiled = list(Path('codegen/build/classes/java/generatedTest').rglob('*.class'))
assert compiled, 'No generated test sources were compiled'
print(f'{len(ran)} tests executed ({len(regressions)} generator regressions), {len(cases)-len(ran)} skipped; {len(compiled)} generated test classes compiled')
PY
(cd "$repo"; sha256sum -c "$reports/inputs.sha256")

# Check the actual packaged CLI offline, then compile the result with the thin
# same-job Agent/Candid JARs rather than the fat CLI jar.
fixture="$RUNNER_TEMP/codegen-cli-smoke"
mkdir -p "$fixture/generated" "$fixture/classes"
cat >"$fixture/service.did" <<'DID'
service : {
  read : () -> (text) query;
  write : (text) -> (text);
}
DID
jar=codegen/build/libs/ic4j-codegen-with-dependencies-0.8.5.jar
test -f "$jar"
java -jar "$jar" --candid "$fixture/service.did" --package-name ci.generated \
  --output-dir "$fixture/generated" java SmokeClient | tee "$reports/cli.log"
test -f "$fixture/generated/ci/generated/SmokeClient.java"
agent="$repo/org/ic4j/ic4j-agent/0.8.5/ic4j-agent-0.8.5.jar"
candid="$repo/org/ic4j/ic4j-candid/0.8.5/ic4j-candid-0.8.5.jar"
javac --release 8 -cp "$agent:$candid" -d "$fixture/classes" \
  "$fixture/generated/ci/generated/SmokeClient.java"
test -f "$fixture/classes/ci/generated/SmokeClient.class"
cp "$fixture/generated/ci/generated/SmokeClient.java" "$reports/"
printf 'PASS: packaged Codegen CLI generated a Java client compiled with --release 8\n' | tee "$reports/cli-result.txt"
