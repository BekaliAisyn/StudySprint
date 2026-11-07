#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if [ -n "${JAVA_HOME:-}" ]; then PATH="$JAVA_HOME/bin:$PATH"; export PATH; fi
mkdir -p build/classes
find src/main/java -name '*.java' > build/sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
if [ "${1:-}" = test ]; then
  find src/test/java -name '*.java' > build/tests.txt
  javac --release 17 -encoding UTF-8 -cp build/classes -d build/classes @build/tests.txt
  java -cp build/classes studysprint.CoreTests
fi
jar --create --file build/StudySprint.jar --main-class studysprint.Main -C build/classes .
if [ "${1:-}" = run ]; then java -jar build/StudySprint.jar; fi
