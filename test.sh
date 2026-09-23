#!/bin/sh
# Builds the game, then compiles and runs the tests in test/. Needs a JDK 17 or newer.
set -e
cd "$(dirname "$0")"
./build.sh
. ./deps.sh
fetch_junit

rm -rf build/test-classes
mkdir -p build/test-classes
javac -nowarn -encoding UTF-8 -cp "build/classes:$JUNIT" -d build/test-classes $(find test -name '*.java')
(cd test && find . -type f ! -name '*.java' -exec sh -c \
  'mkdir -p "../build/test-classes/$(dirname "$1")" && cp "$1" "../build/test-classes/$1"' _ {} \;)

java -Djava.awt.headless=true -jar "$JUNIT" execute --disable-banner --details=summary \
    --class-path build/classes:build/test-classes --scan-class-path
