package beotool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Festpunkte aus einer oder mehreren Punktdateien (*.pkt), z. B. NEPTAN BTFEST.PKT.
 * Jede Zeile: Punktnummer Rechtswert Hochwert [Höhe], durch Leerzeichen getrennt.
 * Kopfzeilen und Zeilen ohne Koordinaten werden übergangen, -1000 bedeutet "kein Wert".
 */
public class Festpunkte {

    /** Ein Festpunkt. */
    public static class Punkt {
        public final String name;
        public final double y;
        public final double x;
        public final double z;

        public Punkt(String name, double y, double x, double z) {
            this.name = name;
            this.y = y;
            this.x = x;
            this.z = z;
        }
    }

    public final List<Punkt> punkte = new ArrayList<>();
    public final List<String> hinweise = new ArrayList<>();
    public final List<String> dateien = new ArrayList<>();

    public void lies(Path datei) throws IOException {
        dateien.add(datei.toString());
        List<String> zeilen = Files.readAllLines(datei, StandardCharsets.ISO_8859_1);
        int nr = 0;
        for (String roh : zeilen) {
            nr++;
            String z = roh.trim();
            if (z.isEmpty()) {
                continue;
            }
            if (z.startsWith("#") || z.startsWith("'") || z.startsWith("//")) {
                continue;
            }
            String[] t = z.split("[\\s;]+");
            if (t.length == 2) {
                // nur Höhe: Punktnummer Höhe
                if (Double.isNaN(Zahl.lies(t[1]))) {
                    continue;
                }
                punkte.add(new Punkt(t[0], Double.NaN, Double.NaN, wert(t[1])));
                continue;
            }
            if (t.length < 3) {
                continue;
            }
            double y = wert(t[1]);
            double x = wert(t[2]);
            double h = t.length > 3 ? wert(t[3]) : Double.NaN;
            if (Double.isNaN(Zahl.lies(t[1])) || Double.isNaN(Zahl.lies(t[2]))) {
                continue; // Kopfzeile
            }
            punkte.add(new Punkt(t[0], y, x, h));
        }
    }

    private static double wert(String s) {
        double w = Zahl.lies(s);
        return (!Double.isNaN(w) && w == -1000.0) ? Double.NaN : w;
    }

    /** Sortiert nach Punktnummer (Zahlen numerisch vor Text) und entfernt doppelte Punktnummern. */
    public void ordnen() {
        Map<String, Punkt> m = new LinkedHashMap<>();
        for (Punkt p : punkte) {
            if (m.containsKey(p.name)) {
                hinweise.add("Festpunkt " + p.name + " mehrfach vorhanden, der erste Eintrag wird verwendet.");
                continue;
            }
            m.put(p.name, p);
        }
        punkte.clear();
        punkte.addAll(m.values());
        punkte.sort(Comparator.comparing(p -> p.name, Punktnummer.VERGLEICH));
    }

    public Punkt suche(String name) {
        for (Punkt p : punkte) {
            if (p.name.equals(name)) {
                return p;
            }
        }
        return null;
    }
}
