package beotool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Anzahl der Anzielungen je Punkt (Excel: Anzahl_Anzielungen) nach der Standpunktvereinigung:
 * wie oft ein Punkt Standpunkt war und von wie vielen Standpunkten er angezielt wurde.
 * Punkte mit insgesamt höchstens einer Anzielung sind nicht kontrolliert und werden markiert.
 */
public final class Anzahl {

    /** Zeile der Auflistung. */
    public static class Zeile {
        public String punkt;
        public int standpunkte;
        public int zielpunkte;

        public int gesamt() {
            return standpunkte + zielpunkte;
        }

        public boolean unkontrolliert() {
            return gesamt() <= 1;
        }
    }

    private Anzahl() {
    }

    public static List<Zeile> zaehlen(List<Vereinigung.Standpunkt> liste) {
        TreeMap<String, Zeile> m = new TreeMap<>(Punktnummer.VERGLEICH);
        for (Vereinigung.Standpunkt s : liste) {
            m.computeIfAbsent(s.name, Anzahl::neu).standpunkte++;
            Set<String> ziele = new LinkedHashSet<>();
            for (Vereinigung.Ziel z : s.ziele) {
                ziele.add(z.name);
            }
            for (String z : ziele) {
                m.computeIfAbsent(z, Anzahl::neu).zielpunkte++;
            }
        }
        return new ArrayList<>(m.values());
    }

    private static Zeile neu(String p) {
        Zeile z = new Zeile();
        z.punkt = p;
        return z;
    }
}
