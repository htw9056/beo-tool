package beotool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Erzeugt die Ausgabedateien als Text. Spaltenbreiten wie im Excel-Werkzeug
 * "Konvertierung für Jag3D" (Speichern als formatierter Text, rechtsbündig).
 */
public final class Ausgabe {

    static final String NL = "\r\n";

    private Ausgabe() {
    }

    // ------------------------------------------------------------- Beobachtungen für Jag3D

    /**
     * Reduzierte Lagebeobachtungen (Satzart 50) für Jag3D bzw. NEPTAN-Leser.
     * Spalten: Satzart(5) Exzentrum(10) Punkt(15) Horizontalstrecke(19) Richtung(16)
     * Höhenunterschied(15) Instrumenten-/Reflektorhöhe(14). Bei 2D bleibt der Höhenunterschied leer.
     * Instrumenten- und Reflektorhöhe sind 0, weil der Höhenunterschied von Punkt zu Punkt gilt.
     */
    public static String lagemessung(List<Vereinigung.Standpunkt> liste, Parameter par, int sl) {
        StringBuilder sb = new StringBuilder();
        boolean mitHoehe = par.art == Parameter.Ausgleichungsart.D3;
        for (Vereinigung.Standpunkt s : liste) {
            sb.append(Zahl.rechts("10.", 5)).append(Zahl.rechts("0.0", 10)).append(Zahl.rechts(s.name, 15))
                    .append(" ".repeat(19)).append(Zahl.links(s.ouExcel, 16)).append(" ".repeat(15))
                    .append(Zahl.rechts("0", 14)).append(NL);
            for (Vereinigung.Ziel z : s.ziele) {
                sb.append(Zahl.rechts("50.", 5)).append(Zahl.rechts("0.0", 10)).append(Zahl.rechts(z.name, 15))
                        .append(Zahl.rechts(Zahl.fmt(z.strecke, sl), 19))
                        .append(Zahl.rechts(Zahl.fmt(z.richtung, Reduktion.stellenRichtung()), 16))
                        .append(Zahl.rechts(mitHoehe ? Zahl.fmt(z.hoehe, sl) : "", 15))
                        .append(Zahl.rechts("0", 14)).append(NL);
            }
        }
        return sb.toString();
    }

    public static String lagemessungName(Parameter par) {
        return "Lagemessung_reduzierte_Werte_" + par.art.text + "_für_Jag3D" + par.hauptpolygonZusatz() + ".BEO";
    }

    /**
     * Höhenbeobachtungen für Jag3D (Textdatei: Standpunkt Ziel ih th Messwert Strecke).
     * Nivellement: virtueller Standpunkt in Höhe des Horizontes, Lattenablesung als
     * Zieltafelhöhe und Messwert 0 (H_Ziel = Horizont − Ablesung).
     * Tachymetrie (beo): Höhenunterschied von Punkt zu Punkt als Messwert.
     */
    public static String hoehenmessung(List<Vereinigung.Standpunkt> liste, Messdaten.Format format, int sl) {
        StringBuilder sb = new StringBuilder();
        boolean niv = format.istNivellement();
        for (Vereinigung.Standpunkt s : liste) {
            for (Vereinigung.Ziel z : s.ziele) {
                if (Double.isNaN(z.hoehe)) {
                    continue;
                }
                sb.append(Zahl.rechts(s.name, 23)).append(Zahl.rechts(z.name, 24));
                if (niv) {
                    sb.append(Zahl.rechts(Zahl.fmt(0, 5), 29)).append(Zahl.rechts(Zahl.fmt(z.hoehe, sl), 24))
                            .append(Zahl.rechts(Zahl.fmt(0, 5), 24));
                } else {
                    sb.append(Zahl.rechts("0.000", 29)).append(Zahl.rechts("0.000", 24))
                            .append(Zahl.rechts(Zahl.fmt(z.hoehe, sl), 24));
                }
                sb.append(Zahl.rechts(Zahl.fmt(z.strecke, sl), 24)).append(NL);
            }
        }
        return sb.toString();
    }

    public static String hoehenmessungName() {
        return "Höhenmessung.NIV";
    }

    // ------------------------------------------------------------- Punktdateien

    /** Ergebnis der Punktaufteilung. */
    public static class Punkte {
        public final List<String[]> neu = new ArrayList<>();
        public final List<String[]> fest = new ArrayList<>();
        public final List<String[]> unbenutzt = new ArrayList<>();
        public double neupunktY = Double.NaN;
        public double neupunktX = Double.NaN;
    }

    /**
     * Teilt alle Punkte der Beobachtungen in Fest- und Neupunkte (Excel: Festpunkte_schreiben),
     * mit derselben Reihenfolge wie im Excel-Werkzeug:
     * Punktnummern als Text sortiert, Koordinaten aus den Festpunkten übernommen, dann nach
     * Rechtswert aufsteigend, bei 1D zuletzt nach Hochwert absteigend (Punkte ohne Koordinaten
     * jeweils am Ende).
     * 1D: Festpunkt = Höhe ungleich 0, Neupunkt = Höhe 0 oder leer; fehlende Rechts-/Hochwerte
     * erhalten die Neupunktkoordinate (Vorgabe: größter Wert + 100 m).
     * 2D/3D: Festpunkt = Rechtswert ungleich 0, Neupunkt = Rechtswert 0 oder leer; leere Werte = 0.
     */
    public static Punkte punkte(List<Vereinigung.Standpunkt> liste, Festpunkte fp, Parameter par) {
        Set<String> namen = new LinkedHashSet<>();
        for (Vereinigung.Standpunkt s : liste) {
            namen.add(s.name);
            for (Vereinigung.Ziel z : s.ziele) {
                namen.add(z.name);
            }
        }
        List<String> sortiert = new ArrayList<>(namen);
        sortiert.sort(Punktnummer.TEXT);
        Punkte e = new Punkte();
        boolean d1 = par.art == Parameter.Ausgleichungsart.D1;
        boolean d2 = par.art == Parameter.Ausgleichungsart.D2;

        List<double[]> zeilen = new ArrayList<>();
        List<String> zn = new ArrayList<>();
        Set<String> benutzt = new LinkedHashSet<>();
        for (String n : sortiert) {
            Festpunkte.Punkt p = fp.suche(n);
            if (p != null) {
                benutzt.add(n);
            }
            zeilen.add(new double[] {p == null ? Double.NaN : p.y, p == null ? Double.NaN : p.x,
                p == null ? Double.NaN : p.z});
            zn.add(n);
        }
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < zeilen.size(); i++) {
            idx.add(i);
        }
        sortiere(idx, zeilen, 0, true);
        if (d1) {
            double ny;
            double nx;
            sortiere(idx, zeilen, 1, true);
            sortiere(idx, zeilen, 0, false);
            ny = zeilen.get(idx.get(0))[0];
            sortiere(idx, zeilen, 1, false);
            nx = zeilen.get(idx.get(0))[1];
            e.neupunktY = !Double.isNaN(par.neupunktY) ? par.neupunktY
                    : Zahl.runden((Double.isNaN(ny) ? 0 : ny) + 100, 0);
            e.neupunktX = !Double.isNaN(par.neupunktX) ? par.neupunktX
                    : Zahl.runden((Double.isNaN(nx) ? 0 : nx) + 100, 0);
        }
        for (int i : idx) {
            double[] w = zeilen.get(i);
            String n = zn.get(i);
            double y = Double.isNaN(w[0]) ? (d1 ? e.neupunktY : 0) : w[0];
            double x = Double.isNaN(w[1]) ? (d1 ? e.neupunktX : 0) : w[1];
            double z = Double.isNaN(w[2]) ? 0 : w[2];
            double pruef = d1 ? z : y;
            String[] zeile = d2 ? new String[] {n, Zahl.fmt(y, 4), Zahl.fmt(x, 4)}
                    : new String[] {n, Zahl.fmt(y, 4), Zahl.fmt(x, 4), Zahl.fmt(z, 4)};
            if (pruef <= 0) {
                e.neu.add(zeile);
            }
            if (pruef != 0) {
                e.fest.add(zeile);
            }
        }
        for (Festpunkte.Punkt p : fp.punkte) {
            if (!benutzt.contains(p.name)) {
                e.unbenutzt.add(new String[] {p.name, Zahl.fmt(p.y, 4), Zahl.fmt(p.x, 4), Zahl.fmt(p.z, 4)});
            }
        }
        return e;
    }

    /** Stabile Sortierung nach einer Spalte; leere Werte bleiben in beiden Richtungen am Ende (wie Excel). */
    private static void sortiere(List<Integer> idx, List<double[]> zeilen, int spalte, boolean aufsteigend) {
        idx.sort((a, b) -> {
            double x = zeilen.get(a)[spalte];
            double y = zeilen.get(b)[spalte];
            boolean lx = Double.isNaN(x);
            boolean ly = Double.isNaN(y);
            if (lx || ly) {
                return lx == ly ? 0 : lx ? 1 : -1;
            }
            return aufsteigend ? Double.compare(x, y) : Double.compare(y, x);
        });
    }

    public static String punktdatei(List<String[]> zeilen) {
        int[] breiten = {15, 29, 24, 24};
        StringBuilder sb = new StringBuilder();
        for (String[] z : zeilen) {
            for (int i = 0; i < z.length; i++) {
                sb.append(Zahl.rechts(z[i], breiten[i]));
            }
            sb.append(NL);
        }
        return sb.toString();
    }

    public static String neupunkteName(Parameter par) {
        return "Neupunkte_" + par.art.text + "_fuer_Jag3D" + par.hauptpolygonZusatz() + ".NeuPkt";
    }

    public static String festpunkteName(Parameter par) {
        return "Festpunkte_" + par.art.text + "_fuer_Jag3D" + par.hauptpolygonZusatz() + ".AltPkt";
    }

    public static String unbenutztName(Parameter par) {
        return "nicht_verwendete_Festpunkte_" + par.art.text + par.hauptpolygonZusatz() + ".DelPkt";
    }

    // ------------------------------------------------------------- Protokolle

    // ------------------------------------------------------------- Protokolle (Aufbau wie Excel)

    /** Eine Textzeile mit festen Spalten wie beim Speichern als formatierter Text aus Excel. */
    static final class Zeile {
        private final StringBuilder sb = new StringBuilder();

        /** Zahl rechtsbündig, endet an Spalte bis. */
        Zeile zahl(String s, int bis) {
            return setzen(s, bis);
        }

        /** Text rechtsbündig, endet eine Stelle vor bis; zu lang = ab von. */
        Zeile text(String s, int von, int bis) {
            if (s == null || s.isEmpty()) {
                return this;
            }
            if (s.length() > bis - von - 1) {
                auffuellen(von);
                sb.append(s);
                return this;
            }
            return setzen(s, bis - 1);
        }

        /** Text linksbündig ab Spalte von. */
        Zeile links(String s, int von) {
            if (s == null || s.isEmpty()) {
                return this;
            }
            auffuellen(von);
            sb.append(s);
            return this;
        }

        private Zeile setzen(String s, int bis) {
            if (s == null || s.isEmpty()) {
                return this;
            }
            int von = bis - s.length();
            if (von < sb.length()) {
                von = sb.length() + (sb.length() > 0 ? 1 : 0);
            }
            auffuellen(von);
            sb.append(s);
            return this;
        }

        private void auffuellen(int bis) {
            while (sb.length() < bis) {
                sb.append(' ');
            }
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    private static String komma(double v, int stellen) {
        return Zahl.formatKomma(Zahl.runden(v, stellen));
    }

    /** Protokoll der Reduktion (JAGRED.OUT, Aufbau wie im Excel-Werkzeug). */
    public static String protokollReduktion(List<Reduktion.Standpunkt> red, Parameter par, Messdaten daten) {
        int sl = Reduktion.stellenLaenge(daten.format);
        int sr = Reduktion.stellenRichtung();
        StringBuilder sb = new StringBuilder();
        sb.append("Reduktion von Messwerten").append(NL).append(NL);
        sb.append(new Zeile().links("Richtungsdifferenz:", 0).zahl(Zahl.fmt(par.redRichtung, 4), 48).text("gon", 48, 62))
                .append(NL);
        sb.append(new Zeile().links("Streckendifferenz:", 0).zahl(Zahl.fmt(par.redStrecke, 4), 48).text("m", 48, 62))
                .append(NL);
        sb.append(new Zeile().links("Höhenunterschieddifferenz:", 0).zahl(Zahl.fmt(par.redHoeheWirksam(), 4), 48)
                .text("m", 48, 62)).append(NL);
        sb.append(new Zeile().links("Additionskonstante:", 0).zahl("0", 48).text("m", 48, 62)).append(NL);
        sb.append(new Zeile().links("Kippachsunterschied:", 0).zahl("0", 48).text("m", 48, 62)).append(NL);
        sb.append(new Zeile().links("Refraktionskoeffizient:", 0).zahl("0.13", 48)).append(NL);
        sb.append(new Zeile().links("Berechnungshorizont:", 0).zahl("0", 48).text("m", 48, 62)).append(NL);
        for (Reduktion.Standpunkt s : red) {
            sb.append(NL).append(NL);
            sb.append(new Zeile().text("STANDPUNKT", 0, 14).text(s.name, 14, 34)).append(NL);
            sb.append("---------------------------------").append(NL).append(NL);
            sb.append(new Zeile().text("NR", 0, 14).text("ZPKT", 14, 34).text("K", 34, 48).text("RICHTUNG", 48, 62)
                    .text("DIFF", 62, 76).text("STRECKE", 76, 90).text("DIFF", 90, 104).text("HOEHE", 104, 118)
                    .text("DIFF", 118, 132).text("NR", 132, 146)).append(NL);
            for (Reduktion.Ziel z : s.ziele) {
                sb.append(NL);
                Zeile m = new Zeile().zahl(Integer.toString(z.nr), 14).text(z.name, 14, 34)
                        .zahl(Zahl.fmt(z.richtung, sr), 62).zahl(Zahl.fmt(z.strecke, sl), 90)
                        .zahl(Zahl.fmt(z.hoehe, sl), 118);
                if (!Double.isNaN(z.zielachsfehler)) {
                    m.links("c = " + Zahl.fmt(z.zielachsfehler * 1000, 2).replace('.', ',') + " mgon angebracht", 148);
                }
                sb.append(m).append(NL);
                int n = 0;
                for (Reduktion.Einzelwert e : z.einzelwerte) {
                    n++;
                    Zeile r = new Zeile();
                    r.zahl(Zahl.fmt(e.richtung, sr), 62);
                    einzelDiff(r, e.richtung, e.vRichtung, e.richtungElim, "*** ", 4, sr - 1, 62, 76);
                    r.zahl(Zahl.fmt(e.strecke, sl), 90);
                    einzelDiff(r, e.strecke, e.vStrecke, e.streckeElim, "** ", sl, sl, 90, 104);
                    r.zahl(Zahl.fmt(e.hoehe, sl), 118);
                    einzelDiff(r, e.hoehe, e.vHoehe, e.hoeheElim, "* ", sl, sl, 118, 132);
                    r.zahl(Integer.toString(n), 146);
                    if (!e.hinweis.isEmpty()) {
                        r.links(e.hinweis, 148);
                    }
                    sb.append(r).append(NL);
                }
            }
        }
        return sb.toString();
    }

    private static void einzelDiff(Zeile r, double wert, double v, boolean elim, String marke, int stellenAnzeige,
            int stellenElim, int von, int bis) {
        if (Double.isNaN(wert)) {
            r.text("k.A.", von, bis);
        } else if (elim) {
            r.text(marke + komma(v, stellenElim), von, bis);
        } else {
            r.zahl(Zahl.fmt(v, stellenAnzeige), bis);
        }
    }

    /** Protokoll der Standpunktvereinigung (JAGMIT.OUT, Aufbau wie im Excel-Werkzeug). */
    public static String protokollVereinigung(List<Vereinigung.Standpunkt> liste, Parameter par, Messdaten daten) {
        int sl = Reduktion.stellenLaenge(daten.format);
        int sr = Reduktion.stellenRichtung();
        StringBuilder sb = new StringBuilder();
        sb.append("Standpunktvereinigung").append(NL).append(NL);
        sb.append(new Zeile().links("Richtungsdifferenz:", 0).zahl(Zahl.fmt(par.mitRichtung, 4), 40).text("gon", 40, 50)
                .links("Erklärung der Spalte 'K'", 63)).append(NL);
        sb.append(new Zeile().links("Querabweichungen:", 0).zahl("100.1500", 40).text("m", 40, 50)
                .links("2(8) - zweiter Standpunkt mit (8) gemeinsamen Zielen vereinigt", 63)).append(NL);
        sb.append(new Zeile().links("Streckendifferenzen:", 0).zahl(Zahl.fmt(par.mitStrecke, 4), 40).text("m", 40, 50)
                .links("3(5) - dritter Standpunkt mit (5) gemeinsamen Zielen vereinigt", 63)).append(NL);
        sb.append(new Zeile().links("Höhenunterschiede:", 0).zahl(Zahl.fmt(par.mitHoehe, 4), 40).text("m", 40, 50)
                .links(" OU2 - Orientierungsunbekannte des zweiten Standpunktes", 63)).append(NL);
        for (Vereinigung.Standpunkt s : liste) {
            sb.append(NL).append(NL);
            Zeile k = new Zeile().links("STANDPUNKT", 0).text(s.name, 10, 20);
            String zusatz = !s.ouExcel.isEmpty() ? s.ouExcel : s.hinweis;
            if (!zusatz.isEmpty()) {
                k.links(zusatz, 40);
            }
            sb.append(k).append(NL);
            sb.append("---------------------------------").append(NL).append(NL);
            sb.append(new Zeile().text("NR", 0, 10).text("ZPKT", 10, 20).text("K", 20, 40).text("RICHTUNG", 40, 50)
                    .text("DIFF", 50, 70).text("STRECKE", 70, 80).text("DIFF", 80, 100).text("HOEHE", 100, 110)
                    .text("DIFF", 110, 130).text("NR", 130, 140)).append(NL).append(NL);
            for (Vereinigung.Ziel z : s.ziele) {
                sb.append(new Zeile().zahl(Integer.toString(z.nr), 10).text(z.name, 10, 20)
                        .text(z.kennung, 20, 40).zahl(Zahl.fmt(z.richtung, sr), 50).text(z.vRichtung, 50, 70)
                        .zahl(Zahl.fmt(z.strecke, sl), 80).text(z.dStrecke, 80, 100)
                        .zahl(Zahl.fmt(z.hoehe, sl), 110).text(z.dHoehe, 110, 130)).append(NL);
            }
        }
        return sb.toString();
    }

    /** Anzahl der Anzielungen (JAGNET.OUT, Aufbau wie im Excel-Werkzeug). */
    public static String protokollAnzahl(List<Anzahl.Zeile> zeilen) {
        StringBuilder sb = new StringBuilder();
        sb.append("Anzahl der Anzielungen").append(NL).append(NL);
        sb.append(new Zeile().zahl("Punktnummer", 14).text("Standpunkte", 14, 30).text("Zielpunkte", 30, 45)
                .text("Gesamt", 45, 60)).append(NL).append(NL);
        for (Anzahl.Zeile z : zeilen) {
            Zeile r = new Zeile().zahl(z.punkt, 14).zahl(Integer.toString(z.standpunkte), 30)
                    .zahl(Integer.toString(z.zielpunkte), 45);
            if (z.unkontrolliert()) {
                r.text("*** " + z.gesamt(), 45, 60);
            } else {
                r.zahl(Integer.toString(z.gesamt()), 60);
            }
            sb.append(r).append(NL);
        }
        return sb.toString();
    }
}
