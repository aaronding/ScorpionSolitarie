#!/bin/sh
# Builds Scorpion.jar from src/. Needs a JDK 11 or newer on the PATH.
set -e
cd "$(dirname "$0")"
. ./deps.sh

# Version comes from the latest git tag, e.g. v1.2 -> 1.2 (1.2-3-gabc123 between tags)
VERSION=$(git describe --tags --always 2>/dev/null | sed 's/^v//')

rm -rf build
mkdir -p build/classes/META-INF/licenses

# Libraries go inside the jar so it runs on its own; their licenses go with them.
for jar in $APP_LIBS; do
    (cd build/classes && unzip -qo "../../$jar" -x 'META-INF/MANIFEST.MF' 'META-INF/LICENSE' \
        'module-info.class')
    unzip -p "$jar" META-INF/LICENSE > "build/classes/META-INF/licenses/$(basename "$jar" .jar).txt"
done

javac --release 11 -nowarn -encoding UTF-8 -cp "$APP_CLASSPATH" -d build/classes $(find src -name '*.java')

# Copy card art, help pages and license alongside the classes.
(cd src && find . -type f ! -name '*.java' -exec sh -c \
  'mkdir -p "../build/classes/$(dirname "$1")" && cp "$1" "../build/classes/$1"' _ {} \;)

printf 'Implementation-Title: Scorpion Solitaire\nImplementation-Version: %s\nMulti-Release: true\n' \
    "$VERSION" > build/manifest.txt
jar --create --file Scorpion.jar --main-class com.family.solitaire.MainApp \
  --manifest build/manifest.txt -C build/classes .
echo "Built Scorpion.jar $VERSION"
