package beotool;

import java.util.ArrayList;
import java.util.List;

/**
 * Eingelesene, nicht reduzierte Messdaten: Standpunkte mit ihren Beobachtungen.
 */
public class Messdaten {

    /** Herkunft der Daten. */
    public enum Format {
        BEO("NEPTAN-Beobachtungsdatei (*.beo)"),
        GSI("Leica GSI-8/GSI-16 Nivellement (*.gsi)"),
        DAT("Trimble DAT M5 Nivellement (*.dat)");

        public final String text;

        Format(String text) {
            this.text = text;
        }

        public boolean istNivellement() {
            return this != BEO;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** Eine Beobachtung (Zeile Satzart 20 bzw. eine Lattenablesung). */
    public static class Beobachtung {
        public String ziel;
        /** Schrägstrecke [m] (beo) bzw. Zielweite [m] (Nivellement), NaN wenn nicht vorhanden. */
        public double strecke = Double.NaN;
        /** Horizontalrichtung [gon], NaN wenn nicht vorhanden. */
        public double richtung = Double.NaN;
        /** Zenitdistanz [gon], NaN wenn nicht vorhanden. */
        public double zenit = Double.NaN;
        /** Reflektorhöhe [m], NaN wenn nicht vorhanden. */
        public double reflektorhoehe = Double.NaN;
        /** Lattenablesung [m] (nur Nivellement). */
        public double ablesung = Double.NaN;
        public String datei = "";
        public int zeile;
    }

    /** Ein Standpunkt (Satzart 10) mit seinen Beobachtungen in Messreihenfolge. */
    public static class Standpunkt {
        public String name;
        public double instrumentenhoehe = Double.NaN;
        public final List<Beobachtung> beobachtungen = new ArrayList<>();
        public String datei = "";
        public int zeile;

        public Standpunkt(String name) {
            this.name = name;
        }
    }

    public final Format format;
    public final List<Standpunkt> standpunkte = new ArrayList<>();
    public final List<String> hinweise = new ArrayList<>();
    public final List<String> dateien = new ArrayList<>();

    public Messdaten(Format format) {
        this.format = format;
    }

    public int anzahlBeobachtungen() {
        int n = 0;
        for (Standpunkt s : standpunkte) {
            n += s.beobachtungen.size();
        }
        return n;
    }
}
