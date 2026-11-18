package beotool;

/**
 * Programmname, Version und Urheber.
 */
public final class Version {

    public static final String NAME = "beo-tool";
    public static final String VERSION = "V0.02";
    public static final String URHEBER = "Dipl.-Ing. (FH) Stefan Klein";

    private Version() {
    }

    public static String titel() {
        return NAME + " " + VERSION + " © " + URHEBER;
    }
}
