#!/usr/bin/env bash
# Parse-level check of app Kotlin files using the Kotlin compiler bundled with Gradle.
# Android/Compose symbols cannot resolve offline, so only genuine syntax errors are reported.
# Usage: scripts/syntax-check-app.sh path/to/File.kt [more files...]
set -uo pipefail
export JAVA_TOOL_OPTIONS=
GRADLE_HOME="${GRADLE_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v gradle)")")")}"
LIB="$GRADLE_HOME/lib"
jar() { ls "$LIB"/$1 | head -n1; }
CP="$(jar 'kotlin-compiler-embeddable-*.jar'):$(jar 'kotlin-stdlib-2*.jar'):$(jar 'kotlin-script-runtime-*.jar'):$(jar 'kotlin-reflect-*.jar'):$(jar 'kotlin-daemon-embeddable-*.jar'):$(jar 'trove4j*.jar'):$(jar 'annotations-*.jar'):$(jar 'kotlinx-coroutines-core-jvm-*.jar')"
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT
java -cp "$CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -nowarn -cp "$(jar 'kotlin-stdlib-2*.jar')" -d "$OUT/o" "$@" > "$OUT/log" 2>&1
if grep -E "Expecting|Unexpected|Unclosed|Missing|Illegal|Syntax error|Only one|Platform declaration|redeclaration|Conflicting" "$OUT/log"; then
  echo "SYNTAX PROBLEMS FOUND"; exit 1
fi
echo "no parse-level errors in: $*"
