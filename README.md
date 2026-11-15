# beo-tool

**Reduktion von Messwerten, Standpunktvereinigung und Anzahl der Anzielungen** für
NEPTAN-Beobachtungsdateien (`*.beo`) und Nivellementdaten (Leica GSI, Trimble DAT) –
mit Ausgabe für die Netzausgleichung mit **Jag3D** (1D, 2D, 3D).

© Dipl.-Ing. (FH) Stefan Klein · MIT-Lizenz

## Download (Windows)

Unter [Releases](https://github.com/htw9056/beo-tool/releases) die Datei
`beo-tool_Vx.xx_Windows.zip` herunterladen, entpacken und `beo-tool.exe` starten.
Eine Java-Installation ist nicht nötig, die Java-Laufzeit ist im Paket enthalten.

## Funktionen

- **Einlesen:** NEPTAN-Beobachtungsdateien (Satzart 10/20, nichtreduzierte Messdaten),
  Leica GSI-8/GSI-16 und Trimble DAT (M5) vom Digitalnivellier, Festpunktdateien (`*.pkt`)
- **Prüfung Hauptpolygonzug:** nur Beobachtungen zwischen Standpunkten verwenden
- **Reduktion je Standpunkt:** Lage II auf Lage I, Horizontalstrecke und Höhenunterschied,
  Zielachsfehler aus Lage I/II, iterative Elimination von Ausreißern, Mittelbildung
- **Standpunktvereinigung:** gleichnamige Standpunkte über gemeinsame Ziele mit
  Orientierungsunterschied zusammenfassen, gewichtet nach Anzahl der Aufstellungen
- **Anzahl der Anzielungen:** unkontrollierte (nur einmal angezielte) Punkte erkennen
- **Ausgabe:** reduzierte Beobachtungen für Jag3D (`*.BEO` bzw. `Höhenmessung.NIV`),
  Fest-, Neu- und nicht verwendete Festpunkte, Protokolle
- Oberfläche und Aufruf in der Eingabeaufforderung (`beo-tool-konsole.exe`)

Ausführliche Beschreibung: [Handbuch](src/beotool/hilfe/hilfe.html) (im Programm mit F1).

## Aufruf in der Eingabeaufforderung

```
beo-tool-konsole.exe -a 3D -p BTFEST.PKT 20240603_FPF.beo
beo-tool-konsole.exe -p Festpunkte_Hoehe.pkt Beispiel_Schleife.dat
beo-tool-konsole.exe -h
```

## Selbst bauen

Java 17 oder neuer:

```
./bauen.sh        (Linux)
bauen.bat         (Windows)
java -jar dist/beo-tool.jar
```

Das Windows-Paket mit eigener Java-Laufzeit und `beo-tool.exe` entsteht mit
`windows/paket_bauen.sh` (Linux-JDK, Windows-JDK, Go, Python 3) und wird bei jeder Änderung
auf `main` automatisch von GitHub Actions gebaut und als Release veröffentlicht.

## Kontakt

Fragen, Fehlermeldungen und Wünsche ausschließlich über das
[Forum (Discussions)](https://github.com/htw9056/beo-tool/discussions).

## Hinweis

beo-tool ist ein eigenständiges, unabhängig entwickeltes Programm. NEPTAN und Jag3D sind
Produkte ihrer jeweiligen Hersteller; die Namen werden nur verwendet, um die Dateiformate zu
beschreiben.
