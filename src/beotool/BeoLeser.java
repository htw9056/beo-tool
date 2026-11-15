package beotool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Liest NEPTAN-Beobachtungsdateien (*.beo) mit nichtreduzierten Messdaten.
 *
 * Feste Spalten (Handbuch NEPTAN, Anhang A 6.1.1):
 * 1 Abschaltkennzeichen, 2-5 Satzart, 6-15 Lageexzentrum, 16-30 Punktnummer,
 * 31-50 Schrägstrecke, 51-65 Richtung, 66-80 Zenitdistanz, 81-95 Instrumenten-/Reflektorhöhe.
 * Ausgewertet werden die Satzarten 10 (Standpunkt) und 20 (Zielpunkt). Zeilen mit
 * Abschaltkennzeichen oder anderer Satzart werden übersprungen. -1000. bedeutet "kein Wert".
 */
public final class BeoLeser {

    private BeoLeser() {
    }

    public static void lies(Path datei, Messdaten daten) throws IOException {
        List<String> zeilen = Files.readAllLines(datei, StandardCharsets.ISO_8859_1);
        String name = datei.getFileName().toString();
        daten.dateien.add(datei.toString());
        Messdaten.Standpunkt sp = null;
        int nr = 0;
        int uebersprungen = 0;
        for (String roh : zeilen) {
            nr++;
            String z = roh.replace("\t", " ");
            if (z.trim().isEmpty()) {
                continue;
            }
            String satz = teil(z, 0, 5).trim();
            if (satz.equals("0.") || satz.equals("0")) {
                continue; // Dateikopf
            }
            if (satz.equals("10.")) {
                sp = new Messdaten.Standpunkt(teil(z, 15, 30).trim());
                sp.instrumentenhoehe = wert(teil(z, 80, 95));
                sp.datei = name;
                sp.zeile = nr;
                if (sp.name.isEmpty()) {
                    daten.hinweise.add(name + " Zeile " + nr + ": Standpunkt ohne Punktnummer übersprungen.");
                    sp = null;
                    continue;
                }
                daten.standpunkte.add(sp);
            } else if (satz.equals("20.")) {
                if (sp == null) {
                    daten.hinweise.add(name + " Zeile " + nr + ": Zielpunkt vor dem ersten Standpunkt übersprungen.");
                    continue;
                }
                Messdaten.Beobachtung b = new Messdaten.Beobachtung();
                b.ziel = teil(z, 15, 30).trim();
                b.strecke = wert(teil(z, 30, 50));
                b.richtung = wert(teil(z, 50, 65));
                b.zenit = wert(teil(z, 65, 80));
                b.reflektorhoehe = wert(teil(z, 80, 95));
                b.datei = name;
                b.zeile = nr;
                if (b.ziel.isEmpty()) {
                    daten.hinweise.add(name + " Zeile " + nr + ": Zielpunkt ohne Punktnummer übersprungen.");
                    continue;
                }
                sp.beobachtungen.add(b);
            } else {
                uebersprungen++;
            }
        }
        if (uebersprungen > 0) {
            daten.hinweise.add(name + ": " + uebersprungen
                    + " Zeile(n) mit anderer Satzart oder Abschaltkennzeichen nicht verwendet.");
        }
    }

    static String teil(String z, int von, int bis) {
        if (von >= z.length()) {
            return "";
        }
        return z.substring(von, Math.min(bis, z.length()));
    }

    /** Erster Zahlenwert im Feld, NaN wenn leer oder -1000. */
    static double wert(String feld) {
        String t = feld.trim();
        if (t.isEmpty()) {
            return Double.NaN;
        }
        String erstes = t.split("\\s+")[0];
        double w = Zahl.lies(erstes);
        if (!Double.isNaN(w) && w == -1000.0) {
            return Double.NaN;
        }
        return w;
    }
}
