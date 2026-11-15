package beotool;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Prüfung Hauptpolygonzug: Es bleiben nur Beobachtungen zwischen Standpunkten erhalten,
 * also Beobachtungen, deren Zielpunkt selbst als Standpunkt vorkommt. Standpunkte ohne
 * verbleibende Beobachtung entfallen.
 */
public final class Hauptpolygon {

    private Hauptpolygon() {
    }

    /** Wendet die Prüfung an und liefert die Anzahl gelöschter Beobachtungen. */
    public static int anwenden(Messdaten daten) {
        Set<String> stationen = new HashSet<>();
        for (Messdaten.Standpunkt s : daten.standpunkte) {
            stationen.add(s.name);
        }
        int geloescht = 0;
        Iterator<Messdaten.Standpunkt> it = daten.standpunkte.iterator();
        while (it.hasNext()) {
            Messdaten.Standpunkt s = it.next();
            int vorher = s.beobachtungen.size();
            s.beobachtungen.removeIf(b -> !stationen.contains(b.ziel));
            geloescht += vorher - s.beobachtungen.size();
            if (s.beobachtungen.isEmpty()) {
                daten.hinweise.add("Hauptpolygon: Standpunkt " + s.name + " ohne Beobachtung zu einem Standpunkt entfällt.");
                it.remove();
            }
        }
        daten.hinweise.add("Hauptpolygon: " + geloescht + " Beobachtung(en) zu Punkten, die kein Standpunkt sind, gelöscht.");
        return geloescht;
    }
}
