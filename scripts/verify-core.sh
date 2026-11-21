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
# The serialization compiler plugin comes from Maven, so offline we compile against a stub of the
# @Serializable annotation and skip the JSON codec (and its tests); CI compiles and tests those.
mkdir -p "$OUT/stub"
printf 'package kotlinx.serialization\nannotation class Serializable\n' > "$OUT/stub/Serializable.kt"
skip() { grep -L 'kotlinx.serialization.json\|CustomThemeCodec' $(find "$1" -name '*.kt'); }
kotlinc -cp "$STDLIB:$COROUTINES" -d "$OUT/main" "$OUT/stub/Serializable.kt" $(skip "$ROOT/core/src/main")
echo "== compile tests =="
kotlinc -cp "$STDLIB:$COROUTINES:$JUNIT:$OUT/main" -d "$OUT/test" $(skip "$ROOT/core/src/test")

echo "== run tests =="
CLASSES="$(cd "$OUT/test" && find . -name '*Test.class' | sed 's#^\./##; s#\.class$##; s#/#.#g' | sort)"
java -cp "$STDLIB:$COROUTINES:$JUNIT:$HAMCREST:$OUT/main:$OUT/test" org.junit.runner.JUnitCore $CLASSES
