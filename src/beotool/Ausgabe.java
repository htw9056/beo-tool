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
     * Spalten: Satzart(6) Exzentrum(10) Punkt(15) Horizontalstrecke(18) Richtung(16)
     * Höhenunterschied(15) Instrumenten-/Reflektorhöhe(14). Bei 2D bleibt der Höhenunterschied leer.
     * Instrumenten- und Reflektorhöhe sind 0, weil der Höhenunterschied von Punkt zu Punkt gilt.
     */
    public static String lagemessung(List<Vereinigung.Standpunkt> liste, Parameter par, int sl) {
        StringBuilder sb = new StringBuilder();
        boolean mitHoehe = par.art == Parameter.Ausgleichungsart.D3;
        for (Vereinigung.Standpunkt s : liste) {
            sb.append(Zahl.rechts("10.", 6)).append(Zahl.rechts("0.0", 10)).append(Zahl.rechts(s.name, 15))
                    .append(" ".repeat(18 + 16 + 15)).append(Zahl.rechts("0.000", 14)).append(NL);
            for (Vereinigung.Ziel z : s.ziele) {
                sb.append(Zahl.rechts("50.", 6)).append(Zahl.rechts("0.0", 10)).append(Zahl.rechts(z.name, 15))
                        .append(Zahl.rechts(Zahl.fmt(z.strecke, sl), 18))
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
                sb.append(Zahl.rechts(s.name, 24)).append(Zahl.rechts(z.name, 24));
                if (niv) {
                    sb.append(Zahl.rechts(Zahl.fmt(0, 5), 28)).append(Zahl.rechts(Zahl.fmt(z.hoehe, sl), 24))
                            .append(Zahl.rechts(Zahl.fmt(0, 5), 24));
                } else {
                    sb.append(Zahl.rechts("0.000", 28)).append(Zahl.rechts("0.000", 24))
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
     * Teilt alle Punkte der Beobachtungen in Fest- und Neupunkte (Excel: Festpunkte_schreiben).
     * 1D: Festpunkt = Höhe vorhanden; Neupunkte erhalten Rechts-/Hochwert des Neupunktes und Höhe 0.
     * 2D/3D: Festpunkt = Rechtswert vorhanden; Neupunkte erhalten 0.
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
        sortiert.sort(Punktnummer.VERGLEICH);
        Punkte e = new Punkte();
        Set<String> benutzt = new LinkedHashSet<>();
        boolean d1 = par.art == Parameter.Ausgleichungsart.D1;
        boolean d2 = par.art == Parameter.Ausgleichungsart.D2;

        double maxY = Double.NaN;
        double maxX = Double.NaN;
        for (String n : sortiert) {
            Festpunkte.Punkt p = fp.suche(n);
            if (p != null) {
                if (!Double.isNaN(p.y)) {
                    maxY = Double.isNaN(maxY) ? p.y : Math.max(maxY, p.y);
                }
                if (!Double.isNaN(p.x)) {
                    maxX = Double.isNaN(maxX) ? p.x : Math.max(maxX, p.x);
                }
            }
        }
        e.neupunktY = !Double.isNaN(par.neupunktY) ? par.neupunktY
                : Math.round((Double.isNaN(maxY) ? 0 : maxY) + 100);
        e.neupunktX = !Double.isNaN(par.neupunktX) ? par.neupunktX
                : Math.round((Double.isNaN(maxX) ? 0 : maxX) + 100);

        for (String n : sortiert) {
            Festpunkte.Punkt p = fp.suche(n);
            boolean istFest;
            if (d1) {
                istFest = p != null && !Double.isNaN(p.z) && p.z != 0.0;
            } else {
                istFest = p != null && !Double.isNaN(p.y) && p.y != 0.0;
            }
            if (p != null) {
                benutzt.add(n);
            }
            if (istFest) {
                if (d1) {
                    e.fest.add(new String[] {n, wert(p.y, e.neupunktY), wert(p.x, e.neupunktX), Zahl.kurz(p.z, 6)});
                } else if (d2) {
                    e.fest.add(new String[] {n, Zahl.kurz(p.y, 6), wert(p.x, 0)});
                } else {
                    e.fest.add(new String[] {n, Zahl.kurz(p.y, 6), wert(p.x, 0), wert(p == null ? Double.NaN : p.z, 0)});
                }
            } else {
                if (d1) {
                    double y = p != null && !Double.isNaN(p.y) ? p.y : e.neupunktY;
                    double x = p != null && !Double.isNaN(p.x) ? p.x : e.neupunktX;
                    e.neu.add(new String[] {n, Zahl.kurz(y, 6), Zahl.kurz(x, 6), "0"});
                } else if (d2) {
                    e.neu.add(new String[] {n, "0", "0"});
                } else {
                    e.neu.add(new String[] {n, "0", "0", "0"});
                }
            }
        }
        for (Festpunkte.Punkt p : fp.punkte) {
            if (!benutzt.contains(p.name)) {
                e.unbenutzt.add(new String[] {p.name, Zahl.kurz(p.y, 6), Zahl.kurz(p.x, 6), Zahl.kurz(p.z, 6)});
            }
        }
        return e;
    }

    private static String wert(double w, double ersatz) {
        return Zahl.kurz(Double.isNaN(w) ? ersatz : w, 6);
    }

    public static String punktdatei(List<String[]> zeilen) {
        int[] breiten = {16, 28, 24, 24};
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

    private static String kopf(String titel, Parameter par, Messdaten daten) {
        StringBuilder sb = new StringBuilder();
        sb.append(Version.titel()).append(NL);
        sb.append(titel).append(NL);
        sb.append("=".repeat(titel.length())).append(NL).append(NL);
        sb.append("Messdatenformat:   ").append(daten.format.text).append(NL);
        for (String d : daten.dateien) {
            sb.append("Messdatei:         ").append(d).append(NL);
        }
        sb.append("Ausgleichungsart:  ").append(par.art.text).append(NL);
        if (daten.format == Messdaten.Format.BEO) {
            sb.append("Hauptpolygonzug:   ").append(par.hauptpolygon
                    ? "geprüft - Beobachtungen zu Punkten, die kein Standpunkt sind, gelöscht"
                    : "nicht geprüft").append(NL);
        }
        return sb.toString();
    }

    /** Protokoll der Reduktion (entspricht JAGRED.OUT). */
    public static String protokollReduktion(List<Reduktion.Standpunkt> red, Parameter par, Messdaten daten) {
        int sl = Reduktion.stellenLaenge(daten.format);
        int sr = Reduktion.stellenRichtung();
        StringBuilder sb = new StringBuilder(kopf("Reduktion von Messwerten", par, daten));
        sb.append(NL);
        sb.append("Richtungsdifferenz:        ").append(Zahl.fmt(par.redRichtung, 5)).append(" gon").append(NL);
        sb.append("Streckendifferenz:         ").append(Zahl.fmt(par.redStrecke, 5)).append(" m").append(NL);
        sb.append("Höhenunterschieddifferenz: ").append(Zahl.fmt(par.redHoeheWirksam(), 5)).append(" m").append(NL);
        if (!daten.format.istNivellement()) {
            sb.append("Zielachsfehler:            ").append(par.zielachsfehlerAnbringen
                    ? "aus Lage I/II je Ziel bestimmt und angebracht" : "nicht angebracht").append(NL);
        }
        sb.append(NL);
        sb.append("*** = eliminiert (Abweichung vom Mittel >= Grenzwert), DIFF = Mittel - Einzelwert").append(NL);
        for (Reduktion.Standpunkt s : red) {
            sb.append(NL).append("STANDPUNKT ").append(s.name).append("   Instrumentenhöhe ")
                    .append(Zahl.fmt(s.instrumentenhoehe, 3)).append(" m").append(NL);
            sb.append("-".repeat(118)).append(NL);
            sb.append(Zahl.rechts("NR", 6)).append(Zahl.rechts("ZPKT", 15)).append(Zahl.rechts("L", 3))
                    .append(Zahl.rechts("RICHTUNG", 14)).append(Zahl.rechts("DIFF", 13))
                    .append(Zahl.rechts("STRECKE", 13)).append(Zahl.rechts("DIFF", 13))
                    .append(Zahl.rechts("HOEHE", 13)).append(Zahl.rechts("DIFF", 13))
                    .append(Zahl.rechts("N", 4)).append("  BEMERKUNG").append(NL);
            for (Reduktion.Ziel z : s.ziele) {
                sb.append(NL);
                String bem = "Anzahl R/S/H " + z.nRichtung + "/" + z.nStrecke + "/" + z.nHoehe + " von " + z.nGesamt;
                if (!Double.isNaN(z.zielachsfehler)) {
                    bem += ", c = " + Zahl.fmt(z.zielachsfehler * 1000, 2) + " mgon angebracht";
                }
                sb.append(Zahl.rechts(Integer.toString(z.nr), 6)).append(Zahl.rechts(z.name, 15)).append("   ")
                        .append(Zahl.rechts(Zahl.fmt(z.richtung, sr), 14)).append(" ".repeat(13))
                        .append(Zahl.rechts(Zahl.fmt(z.strecke, sl), 13)).append(" ".repeat(13))
                        .append(Zahl.rechts(Zahl.fmt(z.hoehe, sl), 13)).append(" ".repeat(13)).append("    ")
                        .append("  ").append(bem).append(NL);
                for (Reduktion.Einzelwert e : z.einzelwerte) {
                    sb.append(" ".repeat(21)).append(Zahl.rechts(e.lage2 ? "II" : "I", 3))
                            .append(Zahl.rechts(Zahl.fmt(e.richtung, sr), 14))
                            .append(Zahl.rechts(diff(e.vRichtung, e.richtungElim, sr), 13))
                            .append(Zahl.rechts(Zahl.fmt(e.strecke, sl), 13))
                            .append(Zahl.rechts(diff(e.vStrecke, e.streckeElim, sl), 13))
                            .append(Zahl.rechts(Zahl.fmt(e.hoehe, sl), 13))
                            .append(Zahl.rechts(diff(e.vHoehe, e.hoeheElim, sl), 13))
                            .append(Zahl.rechts(Integer.toString(e.nr), 4))
                            .append("  ").append(e.datei).append(" Z.").append(e.zeile)
                            .append(e.hinweis.isEmpty() ? "" : "  " + e.hinweis).append(NL);
                }
            }
        }
        return sb.toString();
    }

    private static String diff(double v, boolean elim, int stellen) {
        if (Double.isNaN(v)) {
            return "";
        }
        return (elim ? "*** " : "") + Zahl.fmt(v, stellen);
    }

    /** Protokoll der Standpunktvereinigung (entspricht JAGMIT.OUT). */
    public static String protokollVereinigung(List<Vereinigung.Standpunkt> liste, Parameter par, Messdaten daten) {
        int sl = Reduktion.stellenLaenge(daten.format);
        int sr = Reduktion.stellenRichtung();
        StringBuilder sb = new StringBuilder(kopf("Standpunktvereinigung", par, daten));
        sb.append(NL);
        sb.append("Richtungsdifferenz:   ").append(Zahl.fmt(par.mitRichtung, 5)).append(" gon").append(NL);
        sb.append("Streckendifferenzen:  ").append(Zahl.fmt(par.mitStrecke, 5)).append(" m").append(NL);
        sb.append("Höhenunterschiede:    ").append(Zahl.fmt(par.mitHoehe, 5)).append(" m").append(NL);
        sb.append(NL);
        sb.append("Spalte K:  2(8) = mit der 2. Aufstellung über 8 gemeinsame Ziele vereinigt").append(NL);
        sb.append("           OU2  = Orientierungsunterschied der 2. Aufstellung").append(NL);
        sb.append("DIFF:      Richtung = Verbesserung, Strecke/Höhe = Differenz 1. - weitere Aufstellung").append(NL);
        sb.append("           n*** = Grenzwert überschritten (Richtung: von der Vereinigung ausgeschlossen)")
                .append(NL);
        sb.append("ANZ:       Anzahl verwendeter Messungen Richtung/Strecke/Höhenunterschied").append(NL);
        for (Vereinigung.Standpunkt s : liste) {
            sb.append(NL).append("STANDPUNKT ").append(s.name);
            if (s.aufstellungen > 1) {
                sb.append("   (").append(s.aufstellungen).append(" Aufstellungen)");
            }
            if (!s.orientierung.isEmpty()) {
                sb.append("   ").append(s.orientierung);
            }
            if (!s.hinweis.isEmpty()) {
                sb.append("   ").append(s.hinweis);
            }
            sb.append(NL);
            sb.append("-".repeat(122)).append(NL);
            sb.append(Zahl.rechts("NR", 6)).append(Zahl.rechts("ZPKT", 15)).append(Zahl.rechts("K", 12))
                    .append(Zahl.rechts("RICHTUNG", 14)).append(Zahl.rechts("DIFF", 14))
                    .append(Zahl.rechts("STRECKE", 13)).append(Zahl.rechts("DIFF", 14))
                    .append(Zahl.rechts("HOEHE", 13)).append(Zahl.rechts("DIFF", 14))
                    .append(Zahl.rechts("ANZ", 12)).append(NL);
            for (Vereinigung.Ziel z : s.ziele) {
                sb.append(Zahl.rechts(Integer.toString(z.nr), 6)).append(Zahl.rechts(z.name, 15))
                        .append(Zahl.rechts(z.kennung.trim(), 12))
                        .append(Zahl.rechts(Zahl.fmt(z.richtung, sr), 14)).append(Zahl.rechts(z.vRichtung, 14))
                        .append(Zahl.rechts(Zahl.fmt(z.strecke, sl), 13)).append(Zahl.rechts(z.dStrecke, 14))
                        .append(Zahl.rechts(Zahl.fmt(z.hoehe, sl), 13)).append(Zahl.rechts(z.dHoehe, 14))
                        .append(Zahl.rechts(z.nRichtung + "/" + z.nStrecke + "/" + z.nHoehe, 12)).append(NL);
            }
        }
        return sb.toString();
    }

    /** Anzahl der Anzielungen (entspricht JAGNET.OUT). */
    public static String protokollAnzahl(List<Anzahl.Zeile> zeilen) {
        StringBuilder sb = new StringBuilder();
        sb.append(Version.titel()).append(NL);
        sb.append("Anzahl der Anzielungen").append(NL).append(NL);
        sb.append(Zahl.rechts("Punktnummer", 15)).append(Zahl.rechts("Standpunkte", 15))
                .append(Zahl.rechts("Zielpunkte", 15)).append(Zahl.rechts("Gesamt", 15)).append(NL).append(NL);
        int unkontrolliert = 0;
        for (Anzahl.Zeile z : zeilen) {
            String g = (z.unkontrolliert() ? "*** " : "") + z.gesamt();
            if (z.unkontrolliert()) {
                unkontrolliert++;
            }
            sb.append(Zahl.rechts(z.punkt, 15)).append(Zahl.rechts(Integer.toString(z.standpunkte), 15))
                    .append(Zahl.rechts(Integer.toString(z.zielpunkte), 15)).append(Zahl.rechts(g, 15)).append(NL);
        }
        sb.append(NL).append(zeilen.size()).append(" Punkte, davon ").append(unkontrolliert)
                .append(" nur einmal angezielt (***)").append(NL);
        return sb.toString();
    }
}
