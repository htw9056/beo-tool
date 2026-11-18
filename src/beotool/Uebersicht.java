package beotool;

import java.util.ArrayList;
import java.util.List;

/**
 * Größte Verbesserungen und Differenzen für die Übersicht (Reiter „Ergebnis“ und Konsole):
 * je Element der betragsgrößte Wert der verwendeten Einzelmessungen (Reduktion) bzw. der
 * Differenzen gemeinsamer Ziele (Standpunktvereinigung), mit Standpunkt und Ziel.
 */
public final class Uebersicht {

    private Uebersicht() {
    }

    /** Ein Element (Richtung, Strecke, ...) mit Index für Reduktion und Vereinigung. */
    private static final class Element {
        final String text;
        final String einheit;
        final int k;
        final int stellen;
        final double grenzRed;
        final double grenzMit;

        Element(String text, String einheit, int k, int stellen, double grenzRed, double grenzMit) {
            this.text = text;
            this.einheit = einheit;
            this.k = k;
            this.stellen = stellen;
            this.grenzRed = grenzRed;
            this.grenzMit = grenzMit;
        }
    }

    private static List<Element> elemente(Parameter par, Messdaten daten) {
        int sl = Reduktion.stellenLaenge(daten.format);
        List<Element> l = new ArrayList<>();
        if (daten.format.istNivellement()) {
            l.add(new Element("Höhenunterschied", "m", 2, sl, par.redHoeheWirksam(), par.mitHoehe));
            l.add(new Element("Strecke", "m", 1, 4, par.redStrecke, par.mitStrecke));
            return l;
        }
        l.add(new Element("Richtung", "gon", 0, 4, par.redRichtung, par.mitRichtung));
        switch (par.art) {
            case D1:
                l.add(new Element("Höhenunterschied", "m", 2, sl, par.redHoeheWirksam(), par.mitHoehe));
                break;
            case D2:
                l.add(new Element("Horizontalstrecke", "m", 1, sl, par.redStrecke, par.mitStrecke));
                break;
            case D3:
                l.add(new Element("Horizontalstrecke", "m", 1, sl, par.redStrecke, par.mitStrecke));
                l.add(new Element("Höhenunterschied", "m", 2, sl, par.redHoeheWirksam(), par.mitHoehe));
                break;
            default:
                l.add(new Element("Schrägstrecke", "m", 4, sl, par.redStrecke, par.mitStrecke));
                l.add(new Element("Zenitwinkel", "gon", 3, 4, par.redZenit, par.mitZenit));
        }
        return l;
    }

    private static double v(Reduktion.Einzelwert e, int k) {
        switch (k) {
            case 0:
                return Double.isNaN(e.richtung) ? Double.NaN : e.vRichtung;
            case 1:
                return Double.isNaN(e.strecke) ? Double.NaN : e.vStrecke;
            case 2:
                return Double.isNaN(e.hoehe) ? Double.NaN : e.vHoehe;
            case 3:
                return Double.isNaN(e.zenit) ? Double.NaN : e.vZenit;
            default:
                return Double.isNaN(e.schraeg) ? Double.NaN : e.vSchraeg;
        }
    }

    private static boolean wertDa(Reduktion.Einzelwert e, int k) {
        switch (k) {
            case 0:
                return !Double.isNaN(e.richtung);
            case 1:
                return !Double.isNaN(e.strecke);
            case 2:
                return !Double.isNaN(e.hoehe);
            case 3:
                return !Double.isNaN(e.zenit);
            default:
                return !Double.isNaN(e.schraeg);
        }
    }

    private static boolean elim(Reduktion.Einzelwert e, int k) {
        switch (k) {
            case 0:
                return e.richtungElim;
            case 1:
                return e.streckeElim;
            case 2:
                return e.hoeheElim;
            case 3:
                return e.zenitElim;
            default:
                return e.schraegElim;
        }
    }

    private static String diffText(Vereinigung.Ziel z, int k) {
        switch (k) {
            case 0:
                return z.vRichtung;
            case 1:
                return z.dStrecke;
            case 2:
                return z.dHoehe;
            case 3:
                return z.dZenit;
            default:
                return z.dSchraeg;
        }
    }

    /** Zahl am Ende eines Differenztextes wie „2*** -0,0012“; NaN bei „k.A.“ oder leer. */
    private static double diffWert(String s) {
        if (s == null || s.trim().isEmpty()) {
            return Double.NaN;
        }
        String[] t = s.trim().split("\\s+");
        try {
            return Double.parseDouble(t[t.length - 1].replace(',', '.'));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    public static String verbesserungen(Auswertung.Ergebnis erg) {
        Parameter par = erg.par;
        List<Element> el = elemente(par, erg.daten);
        StringBuilder sb = new StringBuilder();

        sb.append("\nReduktion: größte Verbesserung der verwendeten Einzelwerte:\n");
        sb.append(String.format("  %-18s %12s  %-26s %10s  %s%n", "Element", "max |v|", "Standpunkt / Ziel",
                "Grenzwert", "eliminiert"));
        for (Element e : el) {
            double max = -1;
            String wo = "";
            int n = 0;
            int nElim = 0;
            for (Reduktion.Standpunkt s : erg.reduktion) {
                for (Reduktion.Ziel z : s.ziele) {
                    for (Reduktion.Einzelwert w : z.einzelwerte) {
                        if (!wertDa(w, e.k)) {
                            continue;
                        }
                        n++;
                        if (elim(w, e.k)) {
                            nElim++;
                            continue;
                        }
                        double v = Math.abs(v(w, e.k));
                        if (!Double.isNaN(v) && v > max) {
                            max = v;
                            wo = s.name + " / " + z.name;
                        }
                    }
                }
            }
            if (n == 0) {
                continue;
            }
            sb.append(String.format("  %-18s %12s  %-26s %10s  %d von %d%n", e.text,
                    max < 0 ? "-" : Zahl.fmt(max, e.stellen) + " " + e.einheit, wo,
                    Zahl.kurz(e.grenzRed, 6), nElim, n));
        }

        boolean vereinigt = false;
        for (Vereinigung.Standpunkt s : erg.vereinigung) {
            for (Vereinigung.Ziel z : s.ziele) {
                if (!z.vRichtung.isEmpty() || !z.dStrecke.isEmpty() || !z.dHoehe.isEmpty()) {
                    vereinigt = true;
                }
            }
        }
        if (vereinigt) {
            sb.append("\nStandpunktvereinigung: größte Differenz gemeinsamer Ziele:\n");
            sb.append(String.format("  %-18s %12s  %-26s %10s  %s%n", "Element", "max |d|", "Standpunkt / Ziel",
                    "Grenzwert", "überschritten"));
            for (Element e : el) {
                double max = -1;
                String wo = "";
                int n = 0;
                int nUeber = 0;
                for (Vereinigung.Standpunkt s : erg.vereinigung) {
                    for (Vereinigung.Ziel z : s.ziele) {
                        String t = diffText(z, e.k);
                        double d = diffWert(t);
                        if (Double.isNaN(d)) {
                            continue;
                        }
                        n++;
                        if (t.contains("***")) {
                            nUeber++;
                        }
                        if (Math.abs(d) > max) {
                            max = Math.abs(d);
                            wo = s.name + " / " + z.name;
                        }
                    }
                }
                if (n == 0) {
                    continue;
                }
                sb.append(String.format("  %-18s %12s  %-26s %10s  %d von %d%n", e.text,
                        Zahl.fmt(max, e.stellen) + " " + e.einheit, wo, Zahl.kurz(e.grenzMit, 6), nUeber, n));
            }
        }
        return sb.toString();
    }
}
