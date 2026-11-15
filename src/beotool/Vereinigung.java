package beotool;

import java.util.ArrayList;
import java.util.List;

/**
 * Standpunktvereinigung (Excel: Standpunktvereinigung).
 *
 * Gleichnamige Standpunkte werden über gemeinsame Ziele zusammengefasst:
 * <ol>
 * <li>Orientierungsunterschied OU = Mittel der Richtungsdifferenzen der gemeinsamen Ziele;
 * das Ziel mit der größten Abweichung vom Mittel wird so lange ausgeschlossen, bis alle
 * Abweichungen den Grenzwert einhalten.</li>
 * <li>Gemeinsame Ziele: Richtung, Strecke und Höhenunterschied werden nach der Anzahl der
 * Aufstellungen gewichtet gemittelt, die zu dem jeweiligen Wert schon beigetragen haben; ein
 * fehlender Wert wird nicht mit 0 gemittelt.</li>
 * <li>Übrige Ziele der weiteren Aufstellung werden mit Richtung + OU übernommen.</li>
 * </ol>
 * Ohne gemeinsame Ziele bleiben die Standpunkte getrennt.
 */
public final class Vereinigung {

    /** Ziel eines (vereinigten) Standpunktes. */
    public static class Ziel {
        public int nr;
        public String name;
        /** Kennung der Vereinigungen, z. B. " 2(8)" = mit der 2. Aufstellung über 8 Ziele vereinigt. */
        public String kennung = "";
        public double richtung;
        public double strecke;
        public double hoehe;
        public String vRichtung = "";
        public String dStrecke = "";
        public String dHoehe = "";
        public int nRichtung;
        public int nStrecke;
        public int nHoehe;
        public int nGesamt;
        /** Richtung von der Vereinigung ausgeschlossen (Grenzwert überschritten). */
        public boolean ausgeschlossen;
        /** Anzahl der Aufstellungen, die zu Richtung, Strecke und Höhenunterschied beigetragen haben. */
        int aufstR = 1;
        int aufstS = 1;
        int aufstH = 1;
    }

    /** Standpunkt nach der Vereinigung. */
    public static class Standpunkt {
        public String name;
        public double instrumentenhoehe;
        /** Orientierungsunterschiede der angefügten Aufstellungen, z. B. "OU2=0.00109gon". */
        public String orientierung = "";
        public String hinweis = "";
        public int aufstellungen = 1;
        public final List<Ziel> ziele = new ArrayList<>();
    }

    private Vereinigung() {
    }

    public static List<Standpunkt> rechnen(List<Reduktion.Standpunkt> red, Parameter par, int sl,
            List<String> hinweise) {
        int sr = Reduktion.stellenRichtung();
        List<Standpunkt> liste = new ArrayList<>();
        for (Reduktion.Standpunkt rs : red) {
            Standpunkt s = new Standpunkt();
            s.name = rs.name;
            s.instrumentenhoehe = rs.instrumentenhoehe;
            for (Reduktion.Ziel rz : rs.ziele) {
                Ziel z = new Ziel();
                z.nr = rz.nr;
                z.name = rz.name;
                z.richtung = rz.richtung;
                z.strecke = rz.strecke;
                z.hoehe = rz.hoehe;
                z.nRichtung = rz.nRichtung;
                z.nStrecke = rz.nStrecke;
                z.nHoehe = rz.nHoehe;
                z.nGesamt = rz.nGesamt;
                s.ziele.add(z);
            }
            liste.add(s);
        }

        for (int i = 0; i < liste.size(); i++) {
            Standpunkt st1 = liste.get(i);
            int zaehler = 1;
            for (int j = i + 1; j < liste.size(); j++) {
                Standpunkt st2 = liste.get(j);
                if (!st2.name.equals(st1.name)) {
                    continue;
                }
                zaehler++;
                String marke = zaehler + "*** ";

                // Orientierungsunterschied mit Ausschluss
                List<Ziel[]> paare = new ArrayList<>();
                for (Ziel a : st1.ziele) {
                    if (Double.isNaN(a.richtung) || a.vRichtung.startsWith(marke)) {
                        continue;
                    }
                    for (Ziel b : st2.ziele) {
                        if (b.name.equals(a.name) && !Double.isNaN(b.richtung)) {
                            paare.add(new Ziel[] {a, b});
                        }
                    }
                }
                double ou = Double.NaN;
                while (!paare.isEmpty()) {
                    double summe = 0;
                    for (Ziel[] p : paare) {
                        summe += Zahl.runden(Zahl.norm200(p[0].richtung - p[1].richtung), sr);
                    }
                    ou = Zahl.runden(summe / paare.size(), sr);
                    Ziel[] groesstes = null;
                    double maxAbw = 0;
                    for (Ziel[] p : paare) {
                        double d = Zahl.runden(Zahl.norm200(p[0].richtung - p[1].richtung), sr);
                        double a = Math.abs(Zahl.runden(ou - d, sr));
                        if (a > maxAbw) {
                            maxAbw = a;
                            groesstes = p;
                        }
                    }
                    if (groesstes != null && maxAbw > par.mitRichtung) {
                        double d = Zahl.runden(Zahl.norm200(groesstes[0].richtung - groesstes[1].richtung), sr);
                        String text = marke + Zahl.fmt(Zahl.runden(ou - d, sr - 1), sr - 1);
                        groesstes[0].vRichtung = text;
                        groesstes[0].ausgeschlossen = true;
                        groesstes[1].vRichtung = text;
                        groesstes[1].ausgeschlossen = true;
                        hinweise.add("Standpunktvereinigung " + st1.name + " (" + zaehler + ". Aufstellung): Ziel "
                                + groesstes[0].name + " ausgeschlossen, Abweichung " + Zahl.fmt(ou - d, sr) + " gon.");
                        paare.remove(groesstes);
                        ou = Double.NaN;
                    } else {
                        break;
                    }
                }
                if (paare.isEmpty() || Double.isNaN(ou)) {
                    st2.hinweis = "keine gemeinsamen Zielpunkte gefunden";
                    hinweise.add("Standpunktvereinigung: Standpunkt " + st2.name
                            + " hat keine gemeinsamen Ziele mit der ersten Aufstellung, bleibt getrennt. Prüfung notwendig!");
                    zaehler--;
                    continue;
                }
                int anzahl = paare.size();

                // gemeinsame Ziele mitteln
                List<Ziel> uebrig = new ArrayList<>(st2.ziele);
                for (Ziel a : new ArrayList<>(st1.ziele)) {
                    if (Double.isNaN(a.richtung) || a.vRichtung.contains("***")) {
                        continue;
                    }
                    Ziel b = null;
                    for (Ziel c : uebrig) {
                        if (c.name.equals(a.name)) {
                            b = c;
                            break;
                        }
                    }
                    if (b == null || b.ausgeschlossen && b.vRichtung.startsWith(marke)) {
                        continue;
                    }
                    if (!Double.isNaN(b.richtung)) {
                        int nN = a.aufstR + 1;
                        double d = Zahl.runden(Zahl.norm200(a.richtung - b.richtung), sr);
                        a.vRichtung = Zahl.fmt(Zahl.runden(Zahl.norm200(ou - d), sr - 1), sr - 1);
                        double r2 = Zahl.norm400(b.richtung + ou);
                        double dr = Zahl.norm200(r2 - a.richtung);
                        double r = Zahl.runden(Zahl.norm400(a.richtung + dr / nN), sr);
                        a.richtung = r >= 400.0 ? r - 400.0 : r;
                        a.nRichtung += b.nRichtung;
                        a.aufstR++;
                    }
                    a.kennung = " " + zaehler + "(" + anzahl + ")" + a.kennung;

                    if (Double.isNaN(a.strecke) || Double.isNaN(b.strecke)) {
                        a.dStrecke = "k.A.";
                        if (Double.isNaN(a.strecke)) {
                            a.strecke = b.strecke;
                            a.aufstS = 1;
                        }
                    } else {
                        double diff = Zahl.runden(a.strecke - b.strecke, 4);
                        a.dStrecke = (Math.abs(diff) > par.mitStrecke ? marke : "") + Zahl.fmt(diff, 4);
                        a.strecke = Zahl.runden((a.strecke * a.aufstS + b.strecke) / (a.aufstS + 1), sl);
                        a.aufstS++;
                    }
                    a.nStrecke += b.nStrecke;

                    if (Double.isNaN(a.hoehe) || Double.isNaN(b.hoehe)) {
                        a.dHoehe = "k.A.";
                        if (Double.isNaN(a.hoehe)) {
                            a.hoehe = b.hoehe;
                            a.aufstH = 1;
                        }
                    } else {
                        double diff = Zahl.runden(a.hoehe - b.hoehe, 4);
                        a.dHoehe = (Math.abs(diff) > par.mitHoehe ? marke : "") + Zahl.fmt(diff, 4);
                        a.hoehe = Zahl.runden((a.hoehe * a.aufstH + b.hoehe) / (a.aufstH + 1), sl);
                        a.aufstH++;
                    }
                    a.nHoehe += b.nHoehe;
                    a.nGesamt += b.nGesamt;
                    uebrig.remove(b);
                }

                // übrige Ziele mit Orientierungsunterschied anfügen
                for (Ziel b : uebrig) {
                    if (!Double.isNaN(b.richtung)) {
                        double r = Zahl.runden(Zahl.norm400(b.richtung + ou), sr);
                        b.richtung = r >= 400.0 ? r - 400.0 : r;
                    }
                    st1.ziele.add(b);
                }
                String ouText = "OU" + zaehler + "=" + Zahl.fmt(ou, sr) + "gon";
                st1.orientierung = st1.orientierung.isEmpty() ? ouText : st1.orientierung + " / " + ouText;
                st1.aufstellungen++;
                hinweise.add("Standpunktvereinigung " + st1.name + ": " + zaehler + ". Aufstellung über " + anzahl
                        + " gemeinsame Ziele vereinigt, " + ouText + ".");
                liste.remove(j);
                j--;
            }
        }
        return liste;
    }
}
