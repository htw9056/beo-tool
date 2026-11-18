package beotool;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Alle Einstellungen einer Auswertung. Werden programmweit in einer Datei im Benutzerordner
 * gespeichert und beim nächsten Start wieder geladen.
 */
public class Parameter {

    /** Ausgleichungsart für die Ausgabe. */
    public enum Ausgleichungsart {
        D1("1D_(Höhe)"), D2("2D_(Lage)"), D3("3D_(Lage&Höhe)"), D3O("3D_(Originalbeobachtungen)");

        public final String text;

        Ausgleichungsart(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return this == D3O ? "3D_(Originalbeobachtungen, Satzart 20.)" : text;
        }

        /** Ausgabe als gemittelte Originalbeobachtungen (Schrägstrecke, Richtung, Zenitwinkel). */
        public boolean original() {
            return this == D3O;
        }
    }

    public Messdaten.Format format = Messdaten.Format.BEO;
    public Ausgleichungsart art = Ausgleichungsart.D2;
    public final List<String> messdateien = new ArrayList<>();
    public final List<String> festpunktdateien = new ArrayList<>();
    /** Ausgabeordner, leer = Ordner der ersten Messdatei. */
    public String ausgabeordner = "";

    // Sonderfunktionen
    public boolean hauptpolygon = false;
    public boolean negativeZwischenblickeLoeschen = false;
    public boolean fuehrendeNullenLoeschen = false;
    public int zaehlerBeginn = 1;

    // Reduktion von Messwerten (Grenzwerte innerhalb eines Standpunktes)
    public double redRichtung = 0.0015;
    public double redStrecke = 0.0007;
    public double redHoehe = 0.100;
    public double redHoeheNiv = 0.00005;
    public boolean zielachsfehlerAnbringen = false;
    /** Grenzwert Zenitwinkel (nur Ausgabe Originalbeobachtungen). */
    public double redZenit = 0.0050;

    // Standpunktvereinigung
    public double mitRichtung = 0.0035;
    public double mitStrecke = 0.003;
    public double mitHoehe = 0.100;
    /** Grenzwert Zenitwinkel bei der Standpunktvereinigung (nur Originalbeobachtungen). */
    public double mitZenit = 0.0050;
    /** Höchstzulässiger Unterschied der Instrumentenhöhen für die Vereinigung von Originalbeobachtungen. */
    public static final double IH_TOLERANZ = 0.0005;

    // Neupunktkoordinaten bei 1D (leer = automatisch größter Rechts-/Hochwert + 100 m)
    public double neupunktY = Double.NaN;
    public double neupunktX = Double.NaN;

    /** Wirksamer Grenzwert für Höhenunterschiede je nach Messdatenformat. */
    public double redHoeheWirksam() {
        return format.istNivellement() ? redHoeheNiv : redHoehe;
    }

    public static Path standardDatei() {
        String appdata = System.getenv("APPDATA");
        if (appdata != null && !appdata.isEmpty()) {
            return Paths.get(appdata, "beo-tool", "einstellungen.properties");
        }
        return Paths.get(System.getProperty("user.home"), ".beo-tool", "einstellungen.properties");
    }

    public void speichern(Path datei) throws IOException {
        Properties p = new Properties();
        p.setProperty("format", format.name());
        p.setProperty("art", art.name());
        p.setProperty("messdateien", String.join("\n", messdateien));
        p.setProperty("festpunktdateien", String.join("\n", festpunktdateien));
        p.setProperty("ausgabeordner", ausgabeordner);
        p.setProperty("hauptpolygon", Boolean.toString(hauptpolygon));
        p.setProperty("negativeZwischenblickeLoeschen", Boolean.toString(negativeZwischenblickeLoeschen));
        p.setProperty("fuehrendeNullenLoeschen", Boolean.toString(fuehrendeNullenLoeschen));
        p.setProperty("zaehlerBeginn", Integer.toString(zaehlerBeginn));
        p.setProperty("redRichtung", Double.toString(redRichtung));
        p.setProperty("redStrecke", Double.toString(redStrecke));
        p.setProperty("redHoehe", Double.toString(redHoehe));
        p.setProperty("redHoeheNiv", Double.toString(redHoeheNiv));
        p.setProperty("zielachsfehlerAnbringen", Boolean.toString(zielachsfehlerAnbringen));
        p.setProperty("redZenit", Double.toString(redZenit));
        p.setProperty("mitZenit", Double.toString(mitZenit));
        p.setProperty("mitRichtung", Double.toString(mitRichtung));
        p.setProperty("mitStrecke", Double.toString(mitStrecke));
        p.setProperty("mitHoehe", Double.toString(mitHoehe));
        p.setProperty("neupunktY", Double.isNaN(neupunktY) ? "" : Double.toString(neupunktY));
        p.setProperty("neupunktX", Double.isNaN(neupunktX) ? "" : Double.toString(neupunktX));
        Files.createDirectories(datei.getParent());
        try (Writer w = Files.newBufferedWriter(datei, StandardCharsets.UTF_8)) {
            p.store(w, Version.titel());
        }
    }

    public void laden(Path datei) throws IOException {
        if (!Files.exists(datei)) {
            return;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(datei, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        try {
            format = Messdaten.Format.valueOf(p.getProperty("format", format.name()));
        } catch (IllegalArgumentException e) {
            // Vorgabe behalten
        }
        try {
            art = Ausgleichungsart.valueOf(p.getProperty("art", art.name()));
        } catch (IllegalArgumentException e) {
            // Vorgabe behalten
        }
        liste(p.getProperty("messdateien", ""), messdateien);
        liste(p.getProperty("festpunktdateien", ""), festpunktdateien);
        ausgabeordner = p.getProperty("ausgabeordner", ausgabeordner);
        hauptpolygon = Boolean.parseBoolean(p.getProperty("hauptpolygon", "false"));
        negativeZwischenblickeLoeschen = Boolean.parseBoolean(p.getProperty("negativeZwischenblickeLoeschen", "false"));
        fuehrendeNullenLoeschen = Boolean.parseBoolean(p.getProperty("fuehrendeNullenLoeschen", "false"));
        zaehlerBeginn = ganz(p.getProperty("zaehlerBeginn"), zaehlerBeginn);
        redRichtung = zahl(p.getProperty("redRichtung"), redRichtung);
        redStrecke = zahl(p.getProperty("redStrecke"), redStrecke);
        redHoehe = zahl(p.getProperty("redHoehe"), redHoehe);
        redHoeheNiv = zahl(p.getProperty("redHoeheNiv"), redHoeheNiv);
        zielachsfehlerAnbringen = Boolean.parseBoolean(p.getProperty("zielachsfehlerAnbringen", "false"));
        redZenit = zahl(p.getProperty("redZenit"), redZenit);
        mitZenit = zahl(p.getProperty("mitZenit"), mitZenit);
        mitRichtung = zahl(p.getProperty("mitRichtung"), mitRichtung);
        mitStrecke = zahl(p.getProperty("mitStrecke"), mitStrecke);
        mitHoehe = zahl(p.getProperty("mitHoehe"), mitHoehe);
        neupunktY = Zahl.lies(p.getProperty("neupunktY", ""));
        neupunktX = Zahl.lies(p.getProperty("neupunktX", ""));
    }

    private static void liste(String s, List<String> ziel) {
        ziel.clear();
        for (String t : s.split("\n")) {
            if (!t.trim().isEmpty()) {
                ziel.add(t.trim());
            }
        }
    }

    private static double zahl(String s, double vorgabe) {
        double w = Zahl.lies(s);
        return Double.isNaN(w) ? vorgabe : w;
    }

    private static int ganz(String s, int vorgabe) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return vorgabe;
        }
    }

    /** Endung der Dateinamen bei Prüfung Hauptpolygonzug (wie im Excel-Werkzeug). */
    public String hauptpolygonZusatz() {
        return hauptpolygon && format == Messdaten.Format.BEO ? "_Pruefung_Hauptpolygon" : "";
    }
}
