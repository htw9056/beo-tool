package beotool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reduktion von Messwerten je Standpunkt (Excel: Messdaten_reduzieren).
 *
 * Für jedes Ziel eines Standpunktes:
 * <ol>
 * <li>Lage II auf Lage I umrechnen (Richtung ± 200 gon, wenn die Zenitdistanz &gt; 200 gon ist).</li>
 * <li>Horizontalstrecke s·sin(z) und Höhenunterschied s·cos(z) − Reflektorhöhe + Instrumentenhöhe
 * (beim Nivellement: Zielweite und Lattenablesung unverändert).</li>
 * <li>Richtungen über 0/400 gon in einen gemeinsamen Bereich legen.</li>
 * <li>Zielachsfehler aus dem Mittel der Lage I und Lage II bestimmen und an den Einzelrichtungen
 * anbringen (wie beim Satzmittel); das Richtungsmittel ändert sich dadurch nicht.</li>
 * <li>Je Messelement iterativ den Einzelwert mit der größten Abweichung vom Mittel eliminieren,
 * solange die Abweichung den Grenzwert erreicht oder überschreitet.</li>
 * <li>Mittel der verbleibenden Werte.</li>
 * </ol>
 */
public final class Reduktion {

    /** Ein Einzelwert eines Zieles. */
    public static class Einzelwert {
        public int nr;
        public boolean lage2;
        public double richtung = Double.NaN;
        public double strecke = Double.NaN;
        public double hoehe = Double.NaN;
        public boolean richtungElim;
        public boolean streckeElim;
        public boolean hoeheElim;
        public double vRichtung = Double.NaN;
        public double vStrecke = Double.NaN;
        public double vHoehe = Double.NaN;
        public String hinweis = "";
        public String datei = "";
        public int zeile;
    }

    /** Gemitteltes Ziel. */
    public static class Ziel {
        public int nr;
        public String name;
        public double richtung = Double.NaN;
        public double strecke = Double.NaN;
        public double hoehe = Double.NaN;
        public int nRichtung;
        public int nStrecke;
        public int nHoehe;
        public int nGesamt;
        /** Angebrachter Zielachsfehler c [gon], NaN wenn nicht bestimmt. */
        public double zielachsfehler = Double.NaN;
        public final List<Einzelwert> einzelwerte = new ArrayList<>();
    }

    /** Reduzierter Standpunkt. */
    public static class Standpunkt {
        public String name;
        public double instrumentenhoehe;
        public final List<Ziel> ziele = new ArrayList<>();
    }

    /** Nachkommastellen von Richtung, Strecke und Höhenunterschied. */
    public static int stellenRichtung() {
        return 5;
    }

    public static int stellenLaenge(Messdaten.Format f) {
        return f.istNivellement() ? 6 : 4;
    }

    private Reduktion() {
    }

    public static List<Standpunkt> rechnen(Messdaten daten, Parameter par, List<String> hinweise) {
        boolean niv = daten.format.istNivellement();
        int sl = stellenLaenge(daten.format);
        int sr = stellenRichtung();
        double tolR = par.redRichtung;
        double tolS = par.redStrecke;
        double tolH = par.redHoeheWirksam();
        List<Standpunkt> ergebnis = new ArrayList<>();
        int zielNr = 1;
        for (Messdaten.Standpunkt sp : daten.standpunkte) {
            Standpunkt rs = new Standpunkt();
            rs.name = sp.name;
            double ih = Double.isNaN(sp.instrumentenhoehe) ? 0.0 : sp.instrumentenhoehe;
            rs.instrumentenhoehe = ih;
            Map<String, Ziel> ziele = new LinkedHashMap<>();
            for (Messdaten.Beobachtung b : sp.beobachtungen) {
                Ziel z = ziele.get(b.ziel);
                if (z == null) {
                    z = new Ziel();
                    z.name = b.ziel;
                    ziele.put(b.ziel, z);
                }
                Einzelwert e = new Einzelwert();
                e.nr = z.einzelwerte.size() + 1;
                e.datei = b.datei;
                e.zeile = b.zeile;
                if (niv) {
                    e.richtung = 0.0;
                    e.strecke = Zahl.runden(b.strecke, sl);
                    e.hoehe = Zahl.runden(b.ablesung, sl);
                } else {
                    e.lage2 = !Double.isNaN(b.zenit) && b.zenit > 200.0;
                    if (!Double.isNaN(b.richtung)) {
                        double r = e.lage2 ? b.richtung + 200.0 : b.richtung;
                        if (r > 400.0) {
                            r -= 400.0;
                        }
                        e.richtung = Zahl.runden(r, sr);
                    }
                    if (!Double.isNaN(b.strecke)) {
                        if (!Double.isNaN(b.zenit)) {
                            double zr = b.zenit * Math.PI / 200.0;
                            e.strecke = Zahl.runden(Math.abs(b.strecke * Math.sin(zr)), sl);
                            double th = Double.isNaN(b.reflektorhoehe) ? 0.0 : b.reflektorhoehe;
                            e.hoehe = Zahl.runden(b.strecke * Math.cos(zr) - th + ih, sl);
                        } else {
                            e.strecke = Zahl.runden(b.strecke, sl);
                            e.hinweis = "keine Horizontalstrecke!";
                        }
                    }
                }
                z.einzelwerte.add(e);
            }
            for (Ziel z : ziele.values()) {
                z.nr = zielNr++;
                mitteln(z, niv, tolR, tolS, tolH, sr, sl, par.zielachsfehlerAnbringen);
                rs.ziele.add(z);
            }
            ergebnis.add(rs);
        }
        return ergebnis;
    }

    private static void mitteln(Ziel z, boolean niv, double tolR, double tolS, double tolH, int sr, int sl,
            boolean zielachse) {
        List<Einzelwert> ew = z.einzelwerte;
        z.nGesamt = ew.size();

        // Richtungen über 0/400 gon in einen gemeinsamen Bereich legen
        double min = Double.NaN;
        double max = Double.NaN;
        for (Einzelwert e : ew) {
            if (!Double.isNaN(e.richtung)) {
                min = Double.isNaN(min) ? e.richtung : Math.min(min, e.richtung);
                max = Double.isNaN(max) ? e.richtung : Math.max(max, e.richtung);
            }
        }
        if (!Double.isNaN(min) && max - min > 200.0) {
            for (Einzelwert e : ew) {
                if (!Double.isNaN(e.richtung) && e.richtung < 200.0) {
                    e.richtung += 400.0;
                }
            }
        }

        // Zielachsfehler aus Lage I und Lage II
        if (zielachse && !niv) {
            double s1 = 0;
            double s2 = 0;
            int n1 = 0;
            int n2 = 0;
            for (Einzelwert e : ew) {
                if (Double.isNaN(e.richtung)) {
                    continue;
                }
                if (e.lage2) {
                    s2 += e.richtung;
                    n2++;
                } else {
                    s1 += e.richtung;
                    n1++;
                }
            }
            if (n1 > 0 && n2 > 0) {
                double c = (s2 / n2 - s1 / n1) / 2.0;
                z.zielachsfehler = c;
                for (Einzelwert e : ew) {
                    if (!Double.isNaN(e.richtung)) {
                        e.richtung = Zahl.runden(e.lage2 ? e.richtung - c : e.richtung + c, sr);
                    }
                }
            }
        }

        // Elimination und Mittel je Messelement
        z.richtung = element(ew, 0, tolR, sr);
        z.strecke = element(ew, 1, tolS, sl);
        z.hoehe = element(ew, 2, tolH, sl);
        if (!Double.isNaN(z.richtung) && z.richtung >= 400.0) {
            z.richtung -= 400.0;
        }
        for (Einzelwert e : ew) {
            if (!Double.isNaN(e.richtung) && e.richtung >= 400.0) {
                e.richtung -= 400.0;
            }
            if (!Double.isNaN(e.richtung) && !e.richtungElim) {
                z.nRichtung++;
            }
            if (!Double.isNaN(e.strecke) && !e.streckeElim) {
                z.nStrecke++;
            }
            if (!Double.isNaN(e.hoehe) && !e.hoeheElim) {
                z.nHoehe++;
            }
        }
    }

    private static double wert(Einzelwert e, int k) {
        return k == 0 ? e.richtung : k == 1 ? e.strecke : e.hoehe;
    }

    private static boolean elim(Einzelwert e, int k) {
        return k == 0 ? e.richtungElim : k == 1 ? e.streckeElim : e.hoeheElim;
    }

    private static void setzeElim(Einzelwert e, int k) {
        if (k == 0) {
            e.richtungElim = true;
        } else if (k == 1) {
            e.streckeElim = true;
        } else {
            e.hoeheElim = true;
        }
    }

    private static void setzeV(Einzelwert e, int k, double v) {
        if (k == 0) {
            e.vRichtung = v;
        } else if (k == 1) {
            e.vStrecke = v;
        } else {
            e.vHoehe = v;
        }
    }

    /** Iterative Elimination und Mittelbildung eines Messelementes; liefert das gerundete Mittel. */
    private static double element(List<Einzelwert> ew, int k, double tol, int stellen) {
        while (tol > 0) {
            double summe = 0;
            int n = 0;
            for (Einzelwert e : ew) {
                double w = wert(e, k);
                if (!Double.isNaN(w) && !elim(e, k)) {
                    summe += w;
                    n++;
                }
            }
            if (n < 2) {
                break;
            }
            double mittel = summe / n;
            Einzelwert groesster = null;
            double maxAbw = 0;
            for (Einzelwert e : ew) {
                double w = wert(e, k);
                if (Double.isNaN(w) || elim(e, k)) {
                    continue;
                }
                double a = Math.abs(mittel - w);
                if (a > maxAbw) {
                    maxAbw = a;
                    groesster = e;
                }
            }
            if (groesster != null && maxAbw >= tol) {
                setzeElim(groesster, k);
            } else {
                break;
            }
        }
        double summe = 0;
        int n = 0;
        for (Einzelwert e : ew) {
            double w = wert(e, k);
            if (!Double.isNaN(w) && !elim(e, k)) {
                summe += w;
                n++;
            }
        }
        if (n == 0) {
            return Double.NaN;
        }
        double mittel = Zahl.runden(summe / n, stellen);
        for (Einzelwert e : ew) {
            double w = wert(e, k);
            if (!Double.isNaN(w)) {
                setzeV(e, k, Zahl.runden(mittel - w, stellen));
            }
        }
        return mittel;
    }
}
