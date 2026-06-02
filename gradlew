#!/bin/sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
GRADLE_VERSION=8.14
DIST_NAME="gradle-${GRADLE_VERSION}-bin"
DIST_URL="https://services.gradle.org/distributions/${DIST_NAME}.zip"
CACHE_DIR="$APP_HOME/.gradle/wrapper/dists/${DIST_NAME}"
INSTALL_DIR="$CACHE_DIR/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$INSTALL_DIR/bin/gradle"

find_java_home() {
	if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
		printf '%s\n' "$JAVA_HOME"
		return 0
	fi

	if command -v java >/dev/null 2>&1; then
		return 0
	fi

	if [ -x /usr/libexec/java_home ]; then
		for version in 17 11 1.8 8; do
			candidate=$(/usr/libexec/java_home -v "$version" 2>/dev/null || true)
			if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ]; then
				printf '%s\n' "$candidate"
				return 0
			fi
		done
	fi

	for pattern in \
		"$HOME/.sdkman/candidates/java/17"* \
		"$HOME/.sdkman/candidates/java/11"* \
		"$HOME/.sdkman/candidates/java/8"* \
		"/Library/Java/JavaVirtualMachines/jdk-17"*.jdk/Contents/Home \
		"/Library/Java/JavaVirtualMachines/jdk-11"*.jdk/Contents/Home \
		"/Library/Java/JavaVirtualMachines/jdk-1.8"*.jdk/Contents/Home \
		"/Library/Java/JavaVirtualMachines/zulu-8"*.jdk/Contents/Home \
		"/opt/homebrew/Cellar/openjdk@17"/*/libexec/openjdk.jdk/Contents/Home; do
		if [ -x "$pattern/bin/java" ]; then
			printf '%s\n' "$pattern"
			return 0
		fi
	done

	return 1
}

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME:-/missing}/bin/java" ]; then
	if JAVA_HOME_FOUND=$(find_java_home); then
		if [ -n "$JAVA_HOME_FOUND" ]; then
			JAVA_HOME=$JAVA_HOME_FOUND
			export JAVA_HOME
			PATH="$JAVA_HOME/bin:$PATH"
			export PATH
		fi
	fi
fi

if ! command -v java >/dev/null 2>&1 && [ ! -x "${JAVA_HOME:-/missing}/bin/java" ]; then
	echo "Unable to find a Java runtime. Install JDK 17, 11, or 8 or set JAVA_HOME." >&2
	exit 1
fi

if [ ! -x "$GRADLE_BIN" ]; then
	mkdir -p "$CACHE_DIR"
	if [ -n "${PYTHON_BIN:-}" ] && command -v "$PYTHON_BIN" >/dev/null 2>&1; then
		:
	elif [ -x "$APP_HOME/.venv/bin/python" ]; then
		PYTHON_BIN="$APP_HOME/.venv/bin/python"
	elif [ -x /usr/bin/python3 ]; then
		PYTHON_BIN=/usr/bin/python3
	elif command -v python3 >/dev/null 2>&1; then
		PYTHON_BIN=$(command -v python3)
	else
		echo "python3 is required to bootstrap Gradle from $DIST_URL" >&2
		exit 1
	fi
	"$PYTHON_BIN" -c 'import pathlib, sys, urllib.request, zipfile; url, zip_path, target = sys.argv[1:4]; pathlib.Path(target).mkdir(parents=True, exist_ok=True); urllib.request.urlretrieve(url, zip_path); zipfile.ZipFile(zip_path).extractall(target)' "$DIST_URL" "$CACHE_DIR/${DIST_NAME}.zip" "$CACHE_DIR"
	chmod +x "$GRADLE_BIN"
fi

exec "$GRADLE_BIN" "$@"