#!/bin/sh
# Builds Scorpion.jar from src/. Needs a JDK 11 or newer on the PATH.
set -e
cd "$(dirname "$0")"

# Version comes from the latest git tag, e.g. v1.2 -> 1.2 (1.2-3-gabc123 between tags)
VERSION=$(git describe --tags --always 2>/dev/null | sed 's/^v//')

rm -rf build
mkdir -p build/classes

javac --release 11 -nowarn -encoding UTF-8 -d build/classes $(find src -name '*.java')

# Copy images, help pages and license alongside the classes.
(cd src && find . -type f ! -name '*.java' ! -name '*.form' -exec sh -c \
  'mkdir -p "../build/classes/$(dirname "$1")" && cp "$1" "../build/classes/$1"' _ {} \;)

printf 'Implementation-Title: Scorpion Solitaire\nImplementation-Version: %s\n' "$VERSION" > build/manifest.txt
jar --create --file Scorpion.jar --main-class com.family.solitaire.MainApp \
  --manifest build/manifest.txt -C build/classes .
echo "Built Scorpion.jar $VERSION"
