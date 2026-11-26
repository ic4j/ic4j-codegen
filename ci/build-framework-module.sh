#!/usr/bin/env bash
set -euo pipefail
: "${RUNNER_TEMP:?}" "${CANDID_PATH:?}" "${JAVA_PACKAGE:?}" "${CANISTER_ID:?}" "${ICP_URL:?}"
target="${1:?spring or reactnative}"
[[ "$target" == spring || "$target" == reactnative ]]
root="$(cd "$(dirname "$0")/.." && pwd -P)"
input="$RUNNER_TEMP/codegen-framework-inputs"
out="$RUNNER_TEMP/codegen-${target}-module"
mkdir "$out"
mkdir -p "$out/src/main/java" "$out/consumer" "$out/maven-repository/org/ic4j"
repo="$out/maven-repository"
cp "$root/ci/framework-module/"*.gradle "$out/"
cp "$input/sources.txt" "$root/LICENSE" "$out/"
(cd "$input"; sha256sum -c inputs.sha256)
read -r did_hash conf_hash < <(python3 - "$root" "$out" <<'PY'
import base64, binascii, hashlib, os, re, subprocess, sys, zlib
from pathlib import Path
from urllib.parse import urlsplit
root, out = map(Path, sys.argv[1:])
rel = Path(os.environ['CANDID_PATH'])
source = (root / rel).resolve(strict=True)
if rel.is_absolute() or rel.suffix != '.did' or not source.is_relative_to(root) or source != root / rel:
    raise SystemExit('Choose a regular tracked .did path within the Codegen repository.')
subprocess.run(['git', '-C', str(root), 'ls-files', '--error-unmatch', '--', str(rel)],
               check=True, stdout=subprocess.DEVNULL)
package, canister, endpoint = (os.environ[key] for key in ('JAVA_PACKAGE','CANISTER_ID','ICP_URL'))
if not re.fullmatch(r'[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+', package):
    raise SystemExit('Choose a dotted, lowercase Java package name.')
if not re.fullmatch(r'[a-z2-7]+(?:-[a-z2-7]+)*', canister):
    raise SystemExit('Choose a valid canister principal.')
encoded = canister.replace('-', '')
if '-'.join(encoded[i:i + 5] for i in range(0, len(encoded), 5)) != canister:
    raise SystemExit('Choose a valid canister principal.')
try:
    principal = base64.b32decode(encoded.upper() + '=' * (-len(encoded) % 8))
except (binascii.Error, ValueError):
    raise SystemExit('Choose a valid canister principal.')
if (len(principal) < 4 or len(principal) > 33
        or base64.b32encode(principal).decode('ascii').rstrip('=').lower() != encoded
        or principal[:4] != zlib.crc32(principal[4:]).to_bytes(4, 'big')):
    raise SystemExit('Choose a valid canister principal.')
url = urlsplit(endpoint)
if any(ch.isspace() for ch in endpoint) or (url.scheme != 'https' and (url.scheme, url.hostname) != ('http', '127.0.0.1')) or not url.hostname or url.username or url.password or url.query or url.fragment:
    raise SystemExit('Use an HTTPS endpoint or HTTP on 127.0.0.1, with no credentials, query, or fragment.')
did = source.read_bytes()
(out / 'service.did').write_bytes(did)
print(hashlib.sha256(did).hexdigest(),
      hashlib.sha256(f'{package}\n{canister}\n{endpoint}'.encode()).hexdigest())
PY
)
codegen="$(git -C "$root" rev-parse HEAD)"
test "$(awk '$1 == "codegen" {print $2}' "$out/sources.txt")" = "$codegen"
version="0.8.5-${codegen:0:12}-${did_hash:0:12}-${conf_hash:0:12}"
name="ic4j-generated-$target"
modules=(agent candid "$target")
if [[ "$target" == spring ]]; then
  modules+=(internetidentity management)
fi
for module in "${modules[@]}"; do
  cp -R "$input/maven-repository/org/ic4j/ic4j-$module" "$repo/org/ic4j/"
done

args=(--candid "$out/service.did" --package-name "$JAVA_PACKAGE" --annotate
      --canister-id "$CANISTER_ID" --network "$ICP_URL" --output-dir "$out/src/main/java")
artifact=jar
if [[ "$target" == spring ]]; then
  java -jar "$input/codegen.jar" "${args[@]}" spring GeneratedService GeneratedClient
  cat >"$out/consumer/Consumer.java" <<JAVA
import $JAVA_PACKAGE.GeneratedService;
import $JAVA_PACKAGE.GeneratedClient;
public final class Consumer {
    public static GeneratedClient use(GeneratedService service) { return service; }
}
JAVA
  usage="Add the dependency to your Spring 5 project and include $JAVA_PACKAGE in component scanning."
else
  artifact=aar
  java -jar "$input/codegen.jar" "${args[@]}" reactnative GeneratedModule
  printf '<?xml version="1.0" encoding="utf-8"?>\n<manifest package="%s"/>\n' "$JAVA_PACKAGE" >"$out/AndroidManifest.xml"
  cat >"$out/consumer/Consumer.java" <<JAVA
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.NativeModule;
import $JAVA_PACKAGE.GeneratedModule;
public final class Consumer {
    public static NativeModule create(ReactApplicationContext ctx) throws Exception {
        return new GeneratedModule(ctx);
    }
}
JAVA
  usage="In a React Native 0.71.11 Android app, add the dependency to the app module and register $JAVA_PACKAGE.GeneratedModule from a ReactPackage. It exposes NativeModules.GeneratedModule. The host supplies React Android; this library does not bundle React, an APK, or a JavaScript package."
fi
gradle --no-daemon --console=plain -p "$out" \
  "-PmoduleName=$name" "-PmoduleTarget=$target" "-PmoduleVersion=$version" \
  "-PandroidApiJar=$root/libs/android-31.jar" "-PreactAndroidJar=$root/libs/react-android-0.71.11.jar" \
  verifyConsumer

dir="$repo/org/ic4j/generated/$name/$version"
test -f "$dir/$name-$version.$artifact"
test -f "$dir/$name-$version.pom"
test -f "$out/build/consumer-classes/Consumer.class"
cp "$dir/$name-$version.$artifact" "$out/"
cp "$dir/$name-$version-sources.jar" "$out/"
cat >"$out/README.md" <<DOC
# $name
Built from \`$CANDID_PATH\` at Codegen commit \`$codegen\`.
Compile-only consumer check passed, using the published bundle binary.
Endpoint: \`$ICP_URL\`, canister: \`$CANISTER_ID\` (anonymous identity).
The defaults are placeholders, not a production deployment.

Add \`maven-repository/\` as a Maven repository in your consuming build, before Maven Central.
Use \`implementation("org.ic4j.generated:$name:$version")\`.
The bundled repository supplies pinned IC4J dependencies; resolve other dependencies normally.
$usage

The included Gradle sources were built with Gradle 8.14 and Java 21 targeting Java 8 bytecode.
To rebuild, run the command below; for React Native add paths to your Android API 31 and React Android 0.71.11 compile JARs.

    gradle -PmoduleName=$name -PmoduleTarget=$target -PmoduleVersion=$version verifyConsumer

No ICP calls or app/device tests were run.
DOC
rm -rf "$out/build" "$out/.gradle"
(cd "$out"; find . -type f -print0 | sort -z | xargs -0 sha256sum) >"$RUNNER_TEMP/$name.sha256"
mv "$RUNNER_TEMP/$name.sha256" "$out/SHA256SUMS"
printf 'PASS: %s consumer compiled against %s\n' "$target" "$name-$version.$artifact"
