package beotool;

import java.util.Comparator;

/**
 * Sortierung von Punktnummern wie im Excel-Werkzeug.
 * {@link #VERGLEICH}: "Text als Zahl sortieren" mit deutschen Ländereinstellungen. Punktnummern,
 * die als Zahl lesbar sind (Ziffern, Punkt als Tausendertrennzeichen wie 107.144 = 107144,
 * Komma als Dezimalzeichen), kommen zuerst und werden numerisch sortiert, danach alle übrigen
 * als Text ohne Unterscheidung der Groß-/Kleinschreibung.
 * {@link #TEXT}: reine Textsortierung (Punktnummern als Text formatiert).
 */
public final class Punktnummer {

    private Punktnummer() {
    }

    public static final Comparator<String> VERGLEICH = (a, b) -> {
        double x = zahl(a);
        double y = zahl(b);
        boolean zx = !Double.isNaN(x);
        boolean zy = !Double.isNaN(y);
        if (zx && zy) {
            int c = Double.compare(x, y);
            return c != 0 ? c : a.compareTo(b);
        }
        if (zx) {
            return -1;
        }
        if (zy) {
            return 1;
        }
        int c = a.compareToIgnoreCase(b);
        return c != 0 ? c : a.compareTo(b);
    };

    public static final Comparator<String> TEXT = (a, b) -> {
        int c = a.compareToIgnoreCase(b);
        return c != 0 ? c : a.compareTo(b);
    };

    private static double zahl(String s) {
        if (s == null) {
            return Double.NaN;
        }
        if (s.matches("[+-]?\\d+(\\.\\d{3})*(,\\d+)?")) {
            return Double.parseDouble(s.replace(".", "").replace(',', '.'));
        }
        return Double.NaN;
    }
}
