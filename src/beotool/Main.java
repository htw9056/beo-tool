package beotool;

import javax.swing.SwingUtilities;

/**
 * Programmstart: mit Argumenten als Konsolenprogramm, sonst mit Oberfläche.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            System.exit(Konsole.start(args));
        }
        SwingUtilities.invokeLater(Gui::starten);
    }
}
