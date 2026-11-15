package beotool;

import java.util.Comparator;

/**
 * Sortierung von Punktnummern wie Excel "Text als Zahl sortieren":
 * Punktnummern, die als Zahl lesbar sind, kommen zuerst und werden numerisch sortiert,
 * danach alle übrigen alphabetisch.
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

    private static double zahl(String s) {
        if (s == null || !s.matches("[+-]?\\d+(\\.\\d+)?")) {
            return Double.NaN;
        }
        return Double.parseDouble(s);
    }
}
