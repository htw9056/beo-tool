# Exportvorlagen für NEPTAN-Beobachtungsdateien (*.beo)

Mit diesen Vorlagen schreiben die Feldrechner die Messungen direkt als NEPTAN-Beobachtungsdatei
(`*.beo`, Satzart 10 Standpunkt / 20 Zielpunkt, nichtreduzierte Messdaten), die beo-tool einliest.

## Trimble Access – `Trimble/Neptan.xsl`

1. `Neptan.xsl` auf den Controller in den Ordner `Trimble Data\System Files` kopieren
   (oder mit dem Trimble Installation Manager bzw. Trimble Sync übertragen).
2. In Trimble Access: **Job → Exportieren**, Dateiformat **Neptan** wählen.
3. Die Abfragen bestätigen: „Kennwert Kopfzeile (Satz 0.)“ = **24** (Dateityp NEPTAN) und
   „Kommentare ausgeben“ (Ja/Nein).

Was die Vorlage schreibt:

- Schrägstrecke in m mit 4 Nachkommastellen, bereits um Prismenkonstante, atmosphärische ppm und
  Stationsmaßstab korrigiert (Maßstab nicht bei Anschlussmessungen)
- Richtung und Zenitdistanz in gon mit 5 Nachkommastellen, unabhängig von der Anzeige im Job
- Instrumenten- bzw. Reflektorhöhe, Punktcode, Prismenkonstante und ppm als Text
- Kommentare: Instrumentennummer, Temperatur, Luftdruck, Messjob
- fehlende Werte als Leerzeichen; gelöschte Punkte und reine Koordinatenpunkte werden nicht ausgegeben

## Leica

folgt
