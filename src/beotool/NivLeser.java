package beotool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest Nivellement-Rohdaten (Leica GSI-8/GSI-16, Trimble DAT M5) und bildet daraus
 * Standpunkte für die Höhenauswertung (1D).
 *
 * Jeder Rückblick beginnt eine neue Aufstellung mit dem Namen {@code nnnn_Rückblickpunkt}
 * (laufender Zähler, vierstellig). Ein zweiter Rückblick auf denselben Punkt in derselben
 * Aufstellung (Messverfahren RVVR/BFFB) gehört zur laufenden Aufstellung. Vorblicke und
 * Zwischenblicke werden der laufenden Aufstellung zugeordnet. Die Lattenablesung steht im
 * Feld "Höhenunterschied", die Zielweite im Feld "Strecke", die Richtung ist 0.
 */
public final class NivLeser {

    /** Optionen des Einlesens. */
    public static class Optionen {
        public boolean negativeZwischenblickeLoeschen = false;
        public boolean fuehrendeNullenLoeschen = false;
        public int zaehlerBeginn = 1;
    }

    private enum Art {
        RUECK, VOR, ZWISCHEN
    }

    private static final class Ablesung {
        final Art art;
        final String punkt;
        final double wert;
        final double weite;
        final int zeile;

        Ablesung(Art art, String punkt, double wert, double weite, int zeile) {
            this.art = art;
            this.punkt = punkt;
            this.wert = wert;
            this.weite = weite;
            this.zeile = zeile;
        }
    }

    private NivLeser() {
    }

    /**
     * Liest mehrere Dateien nacheinander; der Aufstellungszähler läuft über alle Dateien weiter.
     * GSI- und DAT-Dateien können gemeinsam gelesen werden, das Format wird je Datei an der
     * Endung erkannt (sonst gilt das gewählte Format).
     */
    public static Messdaten lies(List<Path> dateien, Messdaten.Format format, Optionen opt) throws IOException {
        Messdaten daten = new Messdaten(format);
        int zaehler = Math.max(1, opt.zaehlerBeginn);
        for (Path p : dateien) {
            List<String> zeilen = Files.readAllLines(p, StandardCharsets.ISO_8859_1);
            String name = p.getFileName().toString();
            daten.dateien.add(p.toString());
            // Format je Datei nach der Endung, GSI und DAT dürfen gemischt werden
            String klein = name.toLowerCase(java.util.Locale.ROOT);
            Messdaten.Format f = klein.endsWith(".gsi") ? Messdaten.Format.GSI
                    : klein.endsWith(".dat") ? Messdaten.Format.DAT : format;
            List<Ablesung> liste = f == Messdaten.Format.GSI
                    ? gsi(zeilen, name, daten.hinweise)
                    : dat(zeilen, name, daten.hinweise);
            Messdaten.Standpunkt sp = null;
            String rueckPunkt = null;
            boolean hatVorblick = false;
            for (Ablesung a : liste) {
                String punkt = opt.fuehrendeNullenLoeschen ? ohneNullen(a.punkt) : a.punkt;
                if (a.art == Art.ZWISCHEN && opt.negativeZwischenblickeLoeschen && a.wert < 0) {
                    daten.hinweise.add(name + " Zeile " + a.zeile + ": negativer Zwischenblick zu " + punkt
                            + " gelöscht.");
                    continue;
                }
                if (a.art == Art.RUECK) {
                    boolean gleicheAufstellung = sp != null && punkt.equals(rueckPunkt) && hatVorblick;
                    if (!gleicheAufstellung) {
                        sp = new Messdaten.Standpunkt(String.format("%04d", zaehler) + "_" + punkt);
                        sp.instrumentenhoehe = 0.0;
                        sp.datei = name;
                        sp.zeile = a.zeile;
                        daten.standpunkte.add(sp);
                        zaehler++;
                        rueckPunkt = punkt;
                        hatVorblick = false;
                    }
                } else if (sp == null) {
                    daten.hinweise.add(name + " Zeile " + a.zeile + ": Ablesung zu " + punkt
                            + " vor dem ersten Rückblick übersprungen.");
                    continue;
                } else if (a.art == Art.VOR) {
                    hatVorblick = true;
                }
                Messdaten.Beobachtung b = new Messdaten.Beobachtung();
                b.ziel = punkt;
                b.strecke = a.weite;
                b.richtung = 0.0;
                b.reflektorhoehe = 0.0;
                b.ablesung = a.wert;
                b.datei = name;
                b.zeile = a.zeile;
                sp.beobachtungen.add(b);
            }
        }
        return daten;
    }

    static String ohneNullen(String s) {
        int i = 0;
        while (i < s.length() - 1 && s.charAt(i) == '0') {
            i++;
        }
        return s.substring(i);
    }

    // ------------------------------------------------------------------ Leica GSI

    /**
     * Wortindizes: 11 Punktnummer, 32 Zielweite, 331/335 Rückblick, 332/336 Vorblick,
     * 333/334 Zwischenblick. Codeblöcke (41) und Höhen (83) werden übergangen.
     */
    private static List<Ablesung> gsi(List<String> zeilen, String datei, List<String> hinweise) {
        List<Ablesung> liste = new ArrayList<>();
        int nr = 0;
        for (String roh : zeilen) {
            nr++;
            String z = roh.trim();
            if (z.isEmpty()) {
                continue;
            }
            if (z.startsWith("*")) {
                z = z.substring(1);
            }
            String punkt = null;
            double weite = Double.NaN;
            List<Object[]> werte = new ArrayList<>();
            boolean codeblock = false;
            for (String wort : z.split("\\s+")) {
                if (wort.length() < 8) {
                    continue;
                }
                String wi = wortindex(wort);
                char einheit = wort.charAt(5);
                char vz = wort.charAt(6);
                String daten = wort.substring(7);
                try {
                    switch (wi) {
                        case "11":
                            punkt = gsiPunkt(daten);
                            break;
                        case "41":
                            codeblock = true;
                            break;
                        case "32":
                            weite = gsiZahl(daten, vz, einheit, datei, nr, hinweise);
                            break;
                        case "331":
                        case "335":
                            werte.add(new Object[] {Art.RUECK, gsiZahl(daten, vz, einheit, datei, nr, hinweise)});
                            break;
                        case "332":
                        case "336":
                            werte.add(new Object[] {Art.VOR, gsiZahl(daten, vz, einheit, datei, nr, hinweise)});
                            break;
                        case "333":
                        case "334":
                            werte.add(new Object[] {Art.ZWISCHEN,
                                gsiZahl(daten, vz, einheit, datei, nr, hinweise)});
                            break;
                        default:
                            break;
                    }
                } catch (NumberFormatException e) {
                    hinweise.add(datei + " Zeile " + nr + ": ungültiger Wert in Wort " + wort + " übersprungen.");
                }
            }
            if (codeblock || punkt == null) {
                continue;
            }
            for (Object[] w : werte) {
                liste.add(new Ablesung((Art) w[0], punkt, (Double) w[1], weite, nr));
            }
        }
        return liste;
    }

    static String wortindex(String wort) {
        String zwei = wort.substring(0, 2);
        if (zwei.equals("11") || zwei.equals("41")) {
            return zwei;
        }
        char c = wort.charAt(2);
        if (Character.isDigit(c)) {
            return wort.substring(0, 3);
        }
        return zwei;
    }

    /** Punktnummer aus WI 11: die Füllnullen des GSI-Wortes werden entfernt. */
    static String gsiPunkt(String daten) {
        return ohneNullen(daten.trim());
    }

    static double gsiZahl(String daten, char vorzeichen, char einheit, String datei, int zeile,
            List<String> hinweise) {
        double faktor;
        switch (einheit) {
            case '0':
            case '.':
                faktor = 0.001;
                break;
            case '6':
                faktor = 0.0001;
                break;
            case '8':
                faktor = 0.00001;
                break;
            default:
                hinweise.add(datei + " Zeile " + zeile + ": unbekannte Einheit '" + einheit
                        + "', es wird mm angenommen.");
                faktor = 0.001;
        }
        long n = Long.parseLong(daten.trim());
        double w = n * faktor;
        return vorzeichen == '-' ? -w : w;
    }

    // ------------------------------------------------------------------ Trimble DAT (M5)

    /**
     * Felder: Typ/Punkt "KD1 &lt;Punkt&gt;", Werte "Lr/Rb" Rückblick, "Lv/Rf" Vorblick,
     * "Lz/Rz" Zwischenblick, "HD/E" Zielweite.
     */
    private static List<Ablesung> dat(List<String> zeilen, String datei, List<String> hinweise) {
        List<Ablesung> liste = new ArrayList<>();
        int nr = 0;
        for (String roh : zeilen) {
            nr++;
            if (!roh.contains("|")) {
                continue;
            }
            String[] f = roh.split("\\|", -1);
            if (f.length < 4) {
                continue;
            }
            String typ = f[2].trim();
            if (!(typ.startsWith("KD1") || typ.startsWith("KD2"))) {
                continue;
            }
            String[] t = typ.substring(3).trim().split("\\s+");
            if (t.length == 0 || t[0].isEmpty()) {
                continue;
            }
            String punkt = t[0];
            Art art = null;
            double wert = Double.NaN;
            double hd = Double.NaN;
            for (int i = 3; i < f.length; i++) {
                String feld = f[i].trim();
                if (feld.isEmpty()) {
                    continue;
                }
                String[] w = feld.split("\\s+");
                if (w.length < 2) {
                    continue;
                }
                if (w[1].contains("#")) {
                    hinweise.add(datei + " Zeile " + nr + ": ungültiger Messwert (" + feld + ") übersprungen.");
                    continue;
                }
                double x = Zahl.lies(w[1]);
                if (Double.isNaN(x)) {
                    continue;
                }
                switch (w[0]) {
                    case "Rb":
                    case "Lr":
                        art = Art.RUECK;
                        wert = x;
                        break;
                    case "Rf":
                    case "Lv":
                        art = Art.VOR;
                        wert = x;
                        break;
                    case "Rz":
                    case "Lz":
                        art = Art.ZWISCHEN;
                        wert = x;
                        break;
                    case "HD":
                    case "E":
                        hd = x;
                        break;
                    default:
                        break;
                }
            }
            if (art != null) {
                liste.add(new Ablesung(art, punkt, wert, hd, nr));
            }
        }
        return liste;
    }
}
