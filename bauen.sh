#!/bin/sh
# Baut beo-tool.jar (Java 17+)
set -e
cd "$(dirname "$0")"
rm -rf build && mkdir -p build dist
javac --release 17 -encoding UTF-8 -d build src/beotool/*.java
mkdir -p build/beotool/bilder build/beotool/hilfe
cp src/beotool/bilder/*.png build/beotool/bilder/
cp src/beotool/hilfe/* build/beotool/hilfe/
jar cfm dist/beo-tool.jar MANIFEST.MF -C build .
echo "Fertig: dist/beo-tool.jar"
