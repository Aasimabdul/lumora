#!/usr/bin/env bash
# Offline compile + test of the pure-Kotlin `core` module using the Kotlin compiler,
# kotlinx-coroutines and JUnit jars that ship inside a Gradle distribution.
# This is a fallback for environments that cannot reach Maven repositories; the normal
# path is `./gradlew :core:test` (see docs/BUILDING.md).
set -euo pipefail
export JAVA_TOOL_OPTIONS=

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GRADLE_HOME="${GRADLE_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v gradle)")")")}"
LIB="$GRADLE_HOME/lib"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

jar() { ls "$LIB"/$1 | head -n1; }
STDLIB="$(jar 'kotlin-stdlib-2*.jar')"
COROUTINES="$(jar 'kotlinx-coroutines-core-jvm-*.jar')"
JUNIT="$(jar 'junit-4*.jar')"
HAMCREST="$(jar 'hamcrest-core-*.jar')"

COMPILER_CP="$(jar 'kotlin-compiler-embeddable-*.jar'):$STDLIB:$(jar 'kotlin-script-runtime-*.jar'):$(jar 'kotlin-reflect-*.jar'):$(jar 'kotlin-daemon-embeddable-*.jar')"
for extra in trove4j annotations- kotlinx-coroutines-core-jvm; do
  f="$(ls "$LIB"/"$extra"*.jar 2>/dev/null | head -n1 || true)"; [ -n "$f" ] && COMPILER_CP="$COMPILER_CP:$f"
done

kotlinc() { java -cp "$COMPILER_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -nowarn "$@"; }

echo "== compile main =="
kotlinc -cp "$STDLIB:$COROUTINES" -d "$OUT/main" $(find "$ROOT/core/src/main" -name '*.kt')
echo "== compile tests =="
kotlinc -cp "$STDLIB:$COROUTINES:$JUNIT:$OUT/main" -d "$OUT/test" $(find "$ROOT/core/src/test" -name '*.kt')

echo "== run tests =="
CLASSES="$(cd "$OUT/test" && find . -name '*Test.class' | sed 's#^\./##; s#\.class$##; s#/#.#g' | sort)"
java -cp "$STDLIB:$COROUTINES:$JUNIT:$HAMCREST:$OUT/main:$OUT/test" org.junit.runner.JUnitCore $CLASSES
