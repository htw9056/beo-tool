package beotool;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Aufruf ohne Oberfläche (beo-tool-konsole.exe bzw. java -jar beo-tool.jar ...).
 */
public final class Konsole {

    private Konsole() {
    }

    static void hilfe(PrintStream o) {
        o.println(Version.titel());
        o.println();
        o.println("Aufruf: beo-tool-konsole [Optionen] Messdatei [Messdatei ...]");
        o.println();
        o.println("  -f beo|gsi|dat        Messdatenformat (Vorgabe: nach Dateiendung)");
        o.println("  -a 1D|2D|3D|3DO       Ausgleichungsart (Vorgabe: 2D, bei gsi/dat 1D);");
        o.println("                        3DO = gemittelte Originalbeobachtungen (Satzart 20.)");
        o.println("  -p Datei              Festpunktdatei (mehrfach möglich)");
        o.println("  -o Ordner             Ausgabeordner (Vorgabe: Ordner der ersten Messdatei)");
        o.println("  --hauptpolygon        nur Beobachtungen zwischen Standpunkten verwenden (beo)");
        o.println("  --zielachse           Zielachsfehler aus Lage I/II anbringen (beo)");
        o.println("  --zwischenblicke      negative Zwischenblicke löschen (gsi/dat)");
        o.println("  --nullen              führende Nullen der Punktnummern löschen (gsi/dat)");
        o.println("  --zaehler n           Beginn Zählernummer der Aufstellungen (gsi/dat, Vorgabe 1)");
        o.println("  --neupunkt Y,X        Rechts- und Hochwert der Neupunkte bei 1D");
        o.println("  --red R,S,H[,Z]       Grenzwerte Reduktion: Richtung [gon], Strecke [m], Höhe [m], Zenit [gon]");
        o.println("  --mit R,S,H[,Z]       Grenzwerte Standpunktvereinigung: Richtung, Strecke, Höhe, Zenit");
        o.println("  -n                    nur rechnen und anzeigen, keine Dateien schreiben");
        o.println("  -h                    diese Hilfe");
    }

    public static int start(String[] args) {
        PrintStream o = new PrintStream(System.out, true, konsolenZeichensatz());
        Parameter par = new Parameter();
        boolean formatGesetzt = false;
        boolean artGesetzt = false;
        boolean schreiben = true;
        try {
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "-h":
                    case "--hilfe":
                    case "/?":
                        hilfe(o);
                        return 0;
                    case "-f":
                        par.format = format(args[++i]);
                        formatGesetzt = true;
                        break;
                    case "-a":
                        par.art = art(args[++i]);
                        artGesetzt = true;
                        break;
                    case "-p":
                        par.festpunktdateien.add(args[++i]);
                        break;
                    case "-o":
                        par.ausgabeordner = args[++i];
                        break;
                    case "--hauptpolygon":
                        par.hauptpolygon = true;
                        break;
                    case "--zielachse":
                        par.zielachsfehlerAnbringen = true;
                        break;
                    case "--ohne-zielachse":
                        par.zielachsfehlerAnbringen = false;
                        break;
                    case "--zwischenblicke":
                        par.negativeZwischenblickeLoeschen = true;
                        break;
                    case "--nullen":
                        par.fuehrendeNullenLoeschen = true;
                        break;
                    case "--zaehler":
                        par.zaehlerBeginn = Integer.parseInt(args[++i]);
                        break;
                    case "--neupunkt": {
                        String[] t = args[++i].split("[,;]");
                        par.neupunktY = Zahl.lies(t[0]);
                        par.neupunktX = Zahl.lies(t[1]);
                        break;
                    }
                    case "--red": {
                        double[] w = drei(args[++i]);
                        par.redRichtung = w[0];
                        par.redStrecke = w[1];
                        par.redHoehe = w[2];
                        par.redHoeheNiv = w[2];
                        if (w.length > 3) {
                            par.redZenit = w[3];
                        }
                        break;
                    }
                    case "--mit": {
                        double[] w = drei(args[++i]);
                        par.mitRichtung = w[0];
                        par.mitStrecke = w[1];
                        par.mitHoehe = w[2];
                        if (w.length > 3) {
                            par.mitZenit = w[3];
                        }
                        break;
                    }
                    case "-n":
                        schreiben = false;
                        break;
                    default:
                        if (a.startsWith("-") && a.length() > 1) {
                            o.println("Unbekannte Option: " + a);
                            hilfe(o);
                            return 2;
                        }
                        par.messdateien.add(a);
                }
            }
            if (par.messdateien.isEmpty()) {
                hilfe(o);
                return 2;
            }
            if (!formatGesetzt) {
                String n = par.messdateien.get(0).toLowerCase(Locale.ROOT);
                par.format = n.endsWith(".gsi") ? Messdaten.Format.GSI
                        : n.endsWith(".dat") ? Messdaten.Format.DAT : Messdaten.Format.BEO;
            }
            if (!artGesetzt && par.format.istNivellement()) {
                par.art = Parameter.Ausgleichungsart.D1;
            }
            Auswertung.Ergebnis e = schreiben ? Auswertung.ausfuehren(par) : Auswertung.rechnen(par);
            o.print(e.zusammenfassung());
            return 0;
        } catch (ArrayIndexOutOfBoundsException ex) {
            o.println("Zu der letzten Option fehlt ein Wert.");
            return 2;
        } catch (Exception ex) {
            o.println("Fehler: " + ex.getMessage());
            return 1;
        }
    }

    private static Charset konsolenZeichensatz() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            try {
                return Charset.forName("IBM850");
            } catch (RuntimeException e) {
                return Charset.defaultCharset();
            }
        }
        return Charset.defaultCharset();
    }

    private static Messdaten.Format format(String s) {
        switch (s.toLowerCase(Locale.ROOT)) {
            case "beo":
                return Messdaten.Format.BEO;
            case "gsi":
                return Messdaten.Format.GSI;
            case "dat":
                return Messdaten.Format.DAT;
            default:
                throw new IllegalArgumentException("Unbekanntes Format: " + s);
        }
    }

    private static Parameter.Ausgleichungsart art(String s) {
        switch (s.toUpperCase(Locale.ROOT)) {
            case "1D":
                return Parameter.Ausgleichungsart.D1;
            case "2D":
                return Parameter.Ausgleichungsart.D2;
            case "3D":
                return Parameter.Ausgleichungsart.D3;
            case "3DO":
                return Parameter.Ausgleichungsart.D3O;
            default:
                throw new IllegalArgumentException("Unbekannte Ausgleichungsart: " + s);
        }
    }

    private static double[] drei(String s) {
        String[] t = s.split("[,;]");
        if (t.length != 3 && t.length != 4) {
            throw new IllegalArgumentException("Drei oder vier Werte erwartet: " + s);
        }
        double[] w = new double[t.length];
        for (int i = 0; i < t.length; i++) {
            w[i] = Zahl.lies(t[i]);
        }
        return w;
    }
}
