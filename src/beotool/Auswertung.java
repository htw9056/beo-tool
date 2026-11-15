package beotool;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gesamter Ablauf wie im Excel-Werkzeug: Einlesen, Prüfung Hauptpolygonzug, Festpunkte,
 * Reduktion, Standpunktvereinigung, Anzahl der Anzielungen und Ausgabe der Dateien.
 */
public final class Auswertung {

    /** Zeichensatz der Ausgabedateien (Windows/ANSI wie bisher aus Excel). */
    public static final Charset ANSI = Charset.forName("windows-1252");

    /** Ergebnis einer Auswertung. */
    public static class Ergebnis {
        public Messdaten daten;
        public Festpunkte festpunkte;
        public List<Reduktion.Standpunkt> reduktion;
        public List<Vereinigung.Standpunkt> vereinigung;
        public List<Anzahl.Zeile> anzahl;
        public Ausgabe.Punkte punkte;
        public final List<String> hinweise = new ArrayList<>();
        /** Dateiname -> Inhalt, in Ausgabereihenfolge. */
        public final Map<String, String> dateien = new LinkedHashMap<>();
        public Path ordner;
        public final List<Path> geschrieben = new ArrayList<>();

        public String zusammenfassung() {
            StringBuilder sb = new StringBuilder();
            int beob = daten.anzahlBeobachtungen();
            int aufst = daten.standpunkte.size();
            int ziele = 0;
            for (Reduktion.Standpunkt s : reduktion) {
                ziele += s.ziele.size();
            }
            sb.append(Version.titel()).append("\n\n");
            sb.append("Messdaten:          ").append(daten.format.text).append("\n");
            sb.append("Aufstellungen:      ").append(aufst).append(" mit ").append(beob).append(" Messungen\n");
            sb.append("Reduziert:          ").append(ziele).append(" gemittelte Ziele\n");
            sb.append("Nach Vereinigung:   ").append(vereinigung.size()).append(" Standpunkte\n");
            int unk = 0;
            for (Anzahl.Zeile z : anzahl) {
                if (z.unkontrolliert()) {
                    unk++;
                }
            }
            sb.append("Punkte:             ").append(anzahl.size()).append(" (").append(unk)
                    .append(" nur einmal angezielt)\n");
            if (punkte != null) {
                sb.append("Festpunkte:         ").append(punkte.fest.size()).append(" verwendet, ")
                        .append(punkte.unbenutzt.size()).append(" nicht verwendet\n");
                sb.append("Neupunkte:          ").append(punkte.neu.size()).append("\n");
            }
            if (ordner != null) {
                sb.append("\nAusgabeordner: ").append(ordner).append("\n");
                for (Path p : geschrieben) {
                    sb.append("  ").append(p.getFileName()).append("\n");
                }
            }
            if (!hinweise.isEmpty()) {
                sb.append("\nHinweise:\n");
                for (String h : hinweise) {
                    sb.append("  - ").append(h).append("\n");
                }
            }
            return sb.toString();
        }
    }

    private Auswertung() {
    }

    /** Rechnet, ohne Dateien zu schreiben. */
    public static Ergebnis rechnen(Parameter par) throws IOException {
        if (par.messdateien.isEmpty()) {
            throw new IOException("Keine Messdatei gewählt.");
        }
        Ergebnis e = new Ergebnis();
        List<Path> pfade = new ArrayList<>();
        for (String s : par.messdateien) {
            Path p = Paths.get(s);
            if (!Files.isRegularFile(p)) {
                throw new IOException("Messdatei nicht gefunden: " + s);
            }
            pfade.add(p);
        }
        if (par.format == Messdaten.Format.BEO) {
            e.daten = new Messdaten(Messdaten.Format.BEO);
            for (Path p : pfade) {
                BeoLeser.lies(p, e.daten);
            }
        } else {
            NivLeser.Optionen opt = new NivLeser.Optionen();
            opt.negativeZwischenblickeLoeschen = par.negativeZwischenblickeLoeschen;
            opt.fuehrendeNullenLoeschen = par.fuehrendeNullenLoeschen;
            opt.zaehlerBeginn = par.zaehlerBeginn;
            e.daten = NivLeser.lies(pfade, par.format, opt);
        }
        if (e.daten.standpunkte.isEmpty()) {
            throw new IOException("In den Messdateien wurden keine Standpunkte gefunden.\n"
                    + "Stimmt das Messdatenformat (" + par.format.text + ")?");
        }
        if (par.hauptpolygon && par.format == Messdaten.Format.BEO) {
            Hauptpolygon.anwenden(e.daten);
        }
        e.hinweise.addAll(e.daten.hinweise);

        e.festpunkte = new Festpunkte();
        for (String s : par.festpunktdateien) {
            Path p = Paths.get(s);
            if (!Files.isRegularFile(p)) {
                throw new IOException("Festpunktdatei nicht gefunden: " + s);
            }
            e.festpunkte.lies(p);
        }
        e.festpunkte.ordnen();
        e.hinweise.addAll(e.festpunkte.hinweise);

        int sl = Reduktion.stellenLaenge(e.daten.format);
        e.reduktion = Reduktion.rechnen(e.daten, par, e.hinweise);
        e.vereinigung = Vereinigung.rechnen(e.reduktion, par, sl, e.hinweise);
        e.anzahl = Anzahl.zaehlen(e.vereinigung);
        e.punkte = Ausgabe.punkte(e.vereinigung, e.festpunkte, par);
        if (par.art == Parameter.Ausgleichungsart.D2 && e.daten.format.istNivellement()) {
            e.hinweise.add("Nivellementdaten mit Ausgleichungsart 2D: Für Höhen ist 1D_(Höhe) vorgesehen.");
        }

        String zusatz = par.hauptpolygonZusatz();
        e.dateien.put("JAGRED" + zusatz + ".OutR", Ausgabe.protokollReduktion(e.reduktion, par, e.daten));
        e.dateien.put("JAGMIT" + zusatz + ".OutM", Ausgabe.protokollVereinigung(e.vereinigung, par, e.daten));
        if (par.art == Parameter.Ausgleichungsart.D1) {
            e.dateien.put(Ausgabe.hoehenmessungName(), Ausgabe.hoehenmessung(e.vereinigung, e.daten.format, sl));
        } else {
            e.dateien.put(Ausgabe.lagemessungName(par), Ausgabe.lagemessung(e.vereinigung, par, sl));
        }
        e.dateien.put(Ausgabe.neupunkteName(par), Ausgabe.punktdatei(e.punkte.neu));
        e.dateien.put(Ausgabe.festpunkteName(par), Ausgabe.punktdatei(e.punkte.fest));
        e.dateien.put(Ausgabe.unbenutztName(par), Ausgabe.punktdatei(e.punkte.unbenutzt));
        e.dateien.put("JAGNET" + zusatz + ".OutN", Ausgabe.protokollAnzahl(e.anzahl));
        return e;
    }

    /** Ausgabeordner: gewählter Ordner oder Ordner der ersten Messdatei. */
    public static Path ordner(Parameter par) {
        if (par.ausgabeordner != null && !par.ausgabeordner.trim().isEmpty()) {
            return Paths.get(par.ausgabeordner.trim());
        }
        Path p = Paths.get(par.messdateien.get(0)).toAbsolutePath().getParent();
        return p != null ? p : Paths.get(".");
    }

    /** Rechnet und schreibt alle Ausgabedateien. */
    public static Ergebnis ausfuehren(Parameter par) throws IOException {
        Ergebnis e = rechnen(par);
        e.ordner = ordner(par);
        Files.createDirectories(e.ordner);
        for (Map.Entry<String, String> d : e.dateien.entrySet()) {
            Path p = e.ordner.resolve(d.getKey());
            Files.write(p, d.getValue().getBytes(ANSI));
            e.geschrieben.add(p);
        }
        return e;
    }
}
