package beotool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Runden und Formatieren von Zahlen (Dezimalpunkt, kaufmännisch gerundet wie Excel RUNDEN).
 */
public final class Zahl {

    private Zahl() {
    }

    /**
     * Rundet kaufmännisch (halbe Einheiten von null weg) auf n Nachkommastellen; NaN bleibt NaN.
     * Wie Excel wird zuvor auf 15 signifikante Stellen gerundet, damit z. B. 2.01004999999999997
     * (Rechenrest von 2.01005) auf 2.0101 und nicht auf 2.0100 gerundet wird.
     */
    public static double runden(double x, int n) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            return x;
        }
        return dezimal(x).setScale(n, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal dezimal(double x) {
        return new BigDecimal(x).round(new MathContext(15, RoundingMode.HALF_EVEN));
    }

    /** Feste Nachkommastellen mit Dezimalpunkt, leer bei NaN. */
    public static String fmt(double x, int n) {
        if (Double.isNaN(x)) {
            return "";
        }
        BigDecimal b = dezimal(x).setScale(n, RoundingMode.HALF_UP);
        if (b.signum() == 0) {
            b = b.abs();
        }
        return b.toPlainString();
    }

    /** Wie Excel "Standard": höchstens n Nachkommastellen, ohne abschließende Nullen. */
    public static String kurz(double x, int n) {
        if (Double.isNaN(x)) {
            return "";
        }
        BigDecimal b = dezimal(x).setScale(n, RoundingMode.HALF_UP).stripTrailingZeros();
        if (b.signum() == 0) {
            return "0";
        }
        return b.toPlainString();
    }

    /**
     * Wie VBA Format(x, "0.0000#") mit deutschem Dezimalkomma: mindestens 4, höchstens 5
     * Nachkommastellen (so stehen Differenzen als Text in den Excel-Protokollen).
     */
    public static String formatKomma(double x) {
        if (Double.isNaN(x)) {
            return "";
        }
        String s = fmt(x, 5);
        if (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.replace('.', ',');
    }

    /** Liest eine Zahl mit Punkt oder Komma, NaN wenn leer oder ungültig. */
    public static double lies(String s) {
        if (s == null) {
            return Double.NaN;
        }
        String t = s.trim().replace(',', '.');
        if (t.isEmpty()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Normiert einen Winkel auf [0, 400) gon. */
    public static double norm400(double g) {
        double r = g - 400.0 * Math.floor(g / 400.0);
        return r >= 400.0 ? r - 400.0 : r;
    }

    /** Normiert eine Winkeldifferenz auf [-200, 200) gon. */
    public static double norm200(double d) {
        return d - 400.0 * Math.floor((d + 200.0) / 400.0);
    }

    /** Text rechtsbündig auf Breite, mindestens ein Leerzeichen davor, falls zu lang. */
    public static String rechts(String s, int breite) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= breite) {
            return s.isEmpty() ? s : " " + s;
        }
        return " ".repeat(breite - s.length()) + s;
    }

    /** Text linksbündig auf Breite. */
    public static String links(String s, int breite) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= breite) {
            return s + " ";
        }
        return s + " ".repeat(breite - s.length());
    }
}
