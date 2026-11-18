#!/bin/sh
# Baut das Windows-Paket (ohne installiertes Java lauffähig) unter Linux.
#
# Voraussetzungen: Go, Python 3, zip, ein Linux-JDK und ein Windows-JDK gleicher Version
# (z. B. Eclipse Temurin 21), entpackt.
#
# Aufruf: windows/paket_bauen.sh <Linux-JDK-Ordner> <Windows-JDK-Ordner>
set -e
LINUX_JDK="$1"
WIN_JDK="$2"
if [ -z "$LINUX_JDK" ] || [ -z "$WIN_JDK" ]; then
    echo "Aufruf: $0 <Linux-JDK-Ordner> <Windows-JDK-Ordner>"
    exit 1
fi
cd "$(dirname "$0")/.."
VERSION=$(sed -n 's/.*VERSION = "V\([0-9.]*\)".*/\1/p' src/beotool/Version.java)
DATEIVERSION="$(echo "$VERSION" | cut -d. -f1).$(echo "$VERSION" | cut -d. -f2 | sed "s/^0*//; s/^$/0/").0.0"
NAME="beo-tool_V${VERSION}_Windows"
ZIEL="dist/$NAME"

./bauen.sh
rm -rf "$ZIEL" && mkdir -p "$ZIEL/app"
cp dist/beo-tool.jar "$ZIEL/app/"

echo "Java-Laufzeit erzeugen ..."
"$LINUX_JDK/bin/jlink" --module-path "$WIN_JDK/jmods" \
    --add-modules java.base,java.desktop,java.prefs,jdk.charsets,jdk.localedata --include-locales=de,en \
    --strip-debug --no-header-files --no-man-pages --compress=zip-9 --output "$ZIEL/runtime"

echo "Starter bauen ..."
cd windows/starter
python3 ../ressourcen.py ../beo-tool.ico rsrc_windows_amd64.syso "$DATEIVERSION" \
    "beo-tool - Reduktion und Standpunktvereinigung" "beo-tool.exe"
GOOS=windows GOARCH=amd64 go build -trimpath -ldflags "-s -w -H windowsgui" -o "../../$ZIEL/beo-tool.exe" .
python3 ../ressourcen.py ../beo-tool.ico rsrc_windows_amd64.syso "$DATEIVERSION" \
    "beo-tool - Konsole" "beo-tool-konsole.exe"
GOOS=windows GOARCH=amd64 go build -tags konsole -trimpath -ldflags "-s -w" -o "../../$ZIEL/beo-tool-konsole.exe" .
rm -f rsrc_windows_amd64.syso
cd ../..

cp -r beispiele "$ZIEL/"
cp -r vorlagen "$ZIEL/"
cp LICENSE "$ZIEL/LIZENZ.txt"
cp windows/LIESMICH.txt "$ZIEL/"
cp windows/DRITTANBIETER.txt "$ZIEL/"
sed "s/@VERSION@/beo-tool V$VERSION/g" src/beotool/hilfe/hilfe.html > "$ZIEL/Handbuch.html"
sed -i "s/@VERSION@/V$VERSION/" "$ZIEL/LIESMICH.txt"
# Textdateien mit Windows-Zeilenenden
for f in "$ZIEL/LIESMICH.txt" "$ZIEL/LIZENZ.txt" "$ZIEL/DRITTANBIETER.txt"; do
    sed -i 's/\r*$/\r/' "$f"
done
(cd dist && rm -f "$NAME.zip" && zip -qr "$NAME.zip" "$NAME")
echo "Fertig: dist/$NAME.zip"
