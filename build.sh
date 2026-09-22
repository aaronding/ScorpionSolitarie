#!/bin/sh
# Builds Scorpion.jar from src/. Needs a JDK 11 or newer on the PATH.
set -e
cd "$(dirname "$0")"

rm -rf build
mkdir -p build/classes

javac --release 11 -nowarn -encoding UTF-8 -d build/classes $(find src -name '*.java')

# Copy images, help pages and license alongside the classes.
(cd src && find . -type f ! -name '*.java' ! -name '*.form' -exec sh -c \
  'mkdir -p "../build/classes/$(dirname "$1")" && cp "$1" "../build/classes/$1"' _ {} \;)

# The About dialog offers the source for download (GPL).
(cd . && zip -qr build/classes/com/family/about/src.zip src)

jar --create --file Scorpion.jar --main-class com.family.solitaire.MainApp -C build/classes .
echo "Built Scorpion.jar"
