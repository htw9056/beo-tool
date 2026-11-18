package beotool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

/**
 * Oberfläche von beo-tool.
 */
public final class Gui {

    private final JFrame fenster = new JFrame(Version.titel());
    private final Parameter par = new Parameter();

    private final JComboBox<Messdaten.Format> format = new JComboBox<>(Messdaten.Format.values());
    private final JComboBox<Parameter.Ausgleichungsart> art = new JComboBox<>(Parameter.Ausgleichungsart.values());
    private final DefaultListModel<String> messModell = new DefaultListModel<>();
    private final DefaultListModel<String> festModell = new DefaultListModel<>();
    private final JList<String> messListe = new JList<>(messModell);
    private final JList<String> festListe = new JList<>(festModell);
    private final JTextField ausgabe = new JTextField();

    private final JCheckBox hauptpolygon = new JCheckBox("Prüfung Hauptpolygonzug");
    private final JCheckBox zielachse = new JCheckBox("Zielachsfehler aus Lage I/II anbringen");
    private final JCheckBox zwischenblicke = new JCheckBox("Negative Zwischenblicke löschen");
    private final JCheckBox nullen = new JCheckBox("Führende Nullen der Punktnummern löschen");
    private final JSpinner zaehler = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));

    private final JTextField redR = feld();
    private final JTextField redS = feld();
    private final JTextField redH = feld();
    private final JLabel redHText = new JLabel();
    private final JTextField redZ = feld();
    private final JTextField mitZ = feld();
    private final JTextField mitR = feld();
    private final JTextField mitS = feld();
    private final JTextField mitH = feld();
    private final JTextField neuY = feld();
    private final JTextField neuX = feld();

    private final JTabbedPane reiter = new JTabbedPane();
    private final JTextArea zusammenfassung = textfeld();
    private final JTextArea reduktion = textfeld();
    private final JTextArea vereinigung = textfeld();
    private final JTextArea anzahl = textfeld();
    private final JTextArea beobachtungen = textfeld();
    private final JTextArea punkte = textfeld();
    private final JButton rechnen = new JButton("Berechnen und Dateien schreiben");
    private final JButton nurRechnen = new JButton("Nur berechnen");
    private final JLabel status = new JLabel(" ");

    private Gui() {
    }

    public static void starten() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Standardaussehen
        }
        Locale.setDefault(Locale.GERMANY);
        new Gui().aufbauen();
    }

    private static JTextField feld() {
        JTextField f = new JTextField(8);
        f.setHorizontalAlignment(JTextField.RIGHT);
        return f;
    }

    private static JTextArea textfeld() {
        JTextArea t = new JTextArea();
        t.setEditable(false);
        t.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        return t;
    }

    private void aufbauen() {
        try {
            par.laden(Parameter.standardDatei());
        } catch (IOException e) {
            // Vorgaben verwenden
        }
        symbol();
        fenster.setJMenuBar(menue());
        fenster.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        fenster.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                beenden();
            }
        });

        JScrollPane eingabeScroll = new JScrollPane(eingaben());
        eingabeScroll.getVerticalScrollBar().setUnitIncrement(16);
        eingabeScroll.setBorder(null);
        // Berechnen-Knöpfe immer sichtbar unter den Eingaben
        JPanel knoepfe = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rechnen.setFont(rechnen.getFont().deriveFont(Font.BOLD));
        rechnen.setToolTipText("Berechnen und alle Ausgabedateien schreiben (F5)");
        rechnen.addActionListener(e -> starten(true));
        nurRechnen.setToolTipText("Nur berechnen und anzeigen, keine Dateien schreiben");
        nurRechnen.addActionListener(e -> starten(false));
        knoepfe.add(rechnen);
        knoepfe.add(nurRechnen);
        JPanel linksScroll = new JPanel(new BorderLayout());
        linksScroll.add(eingabeScroll, BorderLayout.CENTER);
        linksScroll.add(knoepfe, BorderLayout.SOUTH);

        reiter.addTab("Ergebnis", new JScrollPane(zusammenfassung));
        reiter.addTab("Reduktion", new JScrollPane(reduktion));
        reiter.addTab("Standpunktvereinigung", new JScrollPane(vereinigung));
        reiter.addTab("Anzahl Anzielungen", new JScrollPane(anzahl));
        reiter.addTab("Beobachtungen für Jag3D", new JScrollPane(beobachtungen));
        reiter.addTab("Punkte", new JScrollPane(punkte));
        zusammenfassung.setText(begruessung());

        JSplitPane teilung = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, linksScroll, reiter);
        teilung.setResizeWeight(0.0);
        teilung.setDividerLocation(500);

        JPanel unten = new JPanel(new BorderLayout());
        unten.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        unten.add(status, BorderLayout.CENTER);
        JLabel urheber = new JLabel("© " + Version.URHEBER);
        urheber.setForeground(Color.GRAY);
        unten.add(urheber, BorderLayout.EAST);

        fenster.getContentPane().setLayout(new BorderLayout());
        fenster.getContentPane().add(teilung, BorderLayout.CENTER);
        fenster.getContentPane().add(unten, BorderLayout.SOUTH);
        uebernehmenInMaske();
        formatGeaendert();
        fenster.setTransferHandler(new DateiAblage());
        fenster.setSize(new Dimension(1400, 860));
        fenster.setLocationRelativeTo(null);
        fenster.setVisible(true);
    }

    private void symbol() {
        List<Image> bilder = new ArrayList<>();
        for (String n : new String[] {"symbol16.png", "symbol32.png", "symbol64.png"}) {
            URL u = Gui.class.getResource("bilder/" + n);
            if (u != null) {
                try {
                    bilder.add(ImageIO.read(u));
                } catch (IOException e) {
                    // ohne Symbol
                }
            }
        }
        if (!bilder.isEmpty()) {
            fenster.setIconImages(bilder);
        }
    }

    private JMenuBar menue() {
        JMenuBar mb = new JMenuBar();
        JMenu datei = new JMenu("Datei");
        JMenuItem mess = new JMenuItem("Messdateien hinzufügen ...");
        mess.addActionListener(e -> messHinzu());
        JMenuItem fest = new JMenuItem("Festpunktdateien hinzufügen ...");
        fest.addActionListener(e -> festHinzu());
        JMenuItem ordner = new JMenuItem("Ausgabeordner öffnen");
        ordner.addActionListener(e -> ordnerOeffnen());
        JMenuItem ende = new JMenuItem("Beenden");
        ende.addActionListener(e -> beenden());
        datei.add(mess);
        datei.add(fest);
        datei.addSeparator();
        datei.add(ordner);
        datei.addSeparator();
        datei.add(ende);
        JMenu berechnen = new JMenu("Berechnen");
        JMenuItem b1 = new JMenuItem("Berechnen und Dateien schreiben");
        b1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        b1.addActionListener(e -> starten(true));
        JMenuItem b2 = new JMenuItem("Nur berechnen");
        b2.addActionListener(e -> starten(false));
        JMenuItem vorgaben = new JMenuItem("Grenzwerte auf Vorgaben zurücksetzen");
        vorgaben.addActionListener(e -> vorgaben());
        berechnen.add(b1);
        berechnen.add(b2);
        berechnen.addSeparator();
        berechnen.add(vorgaben);
        JMenu hilfe = new JMenu("Hilfe");
        JMenuItem hb = new JMenuItem("Handbuch");
        hb.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        hb.addActionListener(e -> handbuch());
        JMenuItem ueber = new JMenuItem("Über beo-tool");
        ueber.addActionListener(e -> ueber());
        hilfe.add(hb);
        hilfe.add(ueber);
        mb.add(datei);
        mb.add(berechnen);
        mb.add(hilfe);
        return mb;
    }

    private JPanel eingaben() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel daten = gruppe("Messdaten");
        zeile(daten, 0, "Format:", format);
        messListe.setVisibleRowCount(5);
        zeileVoll(daten, 1, liste(messListe, this::messHinzu, messModell));
        p.add(daten);

        JPanel fest = gruppe("Festpunkte (*.pkt, Punktnummer Rechtswert Hochwert Höhe)");
        festListe.setVisibleRowCount(3);
        zeileVoll(fest, 0, liste(festListe, this::festHinzu, festModell));
        p.add(fest);

        JPanel aus = gruppe("Ausgabe");
        zeile(aus, 0, "Ausgleichungsart:", art);
        JPanel ord = new JPanel(new BorderLayout(4, 0));
        ord.add(ausgabe, BorderLayout.CENTER);
        JButton w = new JButton("...");
        w.setToolTipText("Ausgabeordner wählen (leer = Ordner der ersten Messdatei)");
        w.addActionListener(e -> ordnerWaehlen());
        ord.add(w, BorderLayout.EAST);
        zeile(aus, 1, "Ausgabeordner:", ord);
        JLabel hinweis = new JLabel("leer = Ordner der ersten Messdatei");
        hinweis.setForeground(Color.GRAY);
        zeile(aus, 2, "", hinweis);
        p.add(aus);

        hauptpolygon.setToolTipText("Nur Beobachtungen zu Punkten verwenden, die selbst Standpunkt sind");
        zielachse.setToolTipText("Zielachsfehler je Ziel aus dem Mittel von Lage I und Lage II bestimmen und anbringen");
        JPanel sonder = gruppe("Sonderfunktionen");
        zeileVoll(sonder, 0, hauptpolygon);
        zeileVoll(sonder, 1, zielachse);
        zeileVoll(sonder, 2, zwischenblicke);
        zeileVoll(sonder, 3, nullen);
        zeile(sonder, 4, "Beginn Zählernummer:", zaehler);
        p.add(sonder);

        JPanel red = gruppe("Reduktion von Messwerten (Grenzwerte)");
        zeile(red, 0, "Richtungsdifferenz [gon]:", redR);
        zeile(red, 1, "Streckendifferenz [m]:", redS);
        zeile(red, 2, redHText, redH);
        zeile(red, 3, "Zenitwinkeldifferenz [gon]:", redZ);
        p.add(red);

        JPanel mit = gruppe("Standpunktvereinigung (Grenzwerte)");
        zeile(mit, 0, "Richtungsdifferenz [gon]:", mitR);
        zeile(mit, 1, "Streckendifferenzen [m]:", mitS);
        zeile(mit, 2, "Höhenunterschiede [m]:", mitH);
        zeile(mit, 3, "Zenitwinkel [gon]:", mitZ);
        p.add(mit);

        JPanel neu = gruppe("Neupunkte bei 1D (leer = größter Wert + 100 m)");
        zeile(neu, 0, "Rechtswert:", neuY);
        zeile(neu, 1, "Hochwert:", neuX);
        p.add(neu);

        p.add(Box.createVerticalGlue());

        format.addActionListener(e -> formatGeaendert());
        art.addActionListener(e -> formatGeaendert());
        redZ.setToolTipText("Nur bei Ausgabe als Originalbeobachtungen (Satzart 20.)");
        mitZ.setToolTipText("Nur bei Ausgabe als Originalbeobachtungen (Satzart 20.)");
        return p;
    }

    private JPanel liste(JList<String> l, Runnable hinzu, DefaultListModel<String> modell) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        JScrollPane s = new JScrollPane(l);
        s.setPreferredSize(new Dimension(340, l.getVisibleRowCount() * 22 + 6));
        p.add(s, BorderLayout.CENTER);
        JPanel k = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton plus = new JButton("Hinzufügen ...");
        plus.addActionListener(e -> hinzu.run());
        JButton minus = new JButton("Entfernen");
        minus.addActionListener(e -> {
            for (String v : l.getSelectedValuesList()) {
                modell.removeElement(v);
            }
        });
        JButton leeren = new JButton("Leeren");
        leeren.addActionListener(e -> modell.clear());
        k.add(plus);
        k.add(minus);
        k.add(leeren);
        p.add(k, BorderLayout.SOUTH);
        l.setTransferHandler(new DateiAblage());
        boolean fest = modell == festModell;
        l.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK), "einfuegen");
        l.getActionMap().put("einfuegen", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                einfuegen(fest);
            }
        });
        l.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "entfernen");
        l.getActionMap().put("entfernen", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                for (String v : l.getSelectedValuesList()) {
                    modell.removeElement(v);
                }
            }
        });
        JButton paste = new JButton("Pfad einfügen");
        paste.setToolTipText("Kopierten Pfad oder kopierte Dateien einfügen (Strg+V in der Liste)");
        paste.addActionListener(e -> einfuegen(fest));
        k.add(paste);
        return p;
    }

    private static JPanel gruppe(String titel) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(titel),
                BorderFactory.createEmptyBorder(2, 4, 4, 4)));
        p.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return p;
    }

    private static void zeile(JPanel p, int y, Object text, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2, 2, 2, 8);
        p.add(text instanceof JComponent ? (JComponent) text : new JLabel(String.valueOf(text)), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = c instanceof JTextField && ((JTextField) c).getColumns() == 8
                ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        p.add(c, g);
    }

    private static void zeileVoll(JPanel p, int y, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2, 2, 2, 2);
        p.add(c, g);
    }

    private void formatGeaendert() {
        Messdaten.Format f = (Messdaten.Format) format.getSelectedItem();
        boolean niv = f != null && f.istNivellement();
        hauptpolygon.setEnabled(!niv);
        zielachse.setEnabled(!niv);
        zwischenblicke.setEnabled(niv);
        nullen.setEnabled(niv);
        zaehler.setEnabled(niv);
        redHText.setText(niv ? "Höhenunterschieddifferenz Niv. [m]:" : "Höhenunterschieddifferenz [m]:");
        redH.setText(Zahl.kurz(niv ? par.redHoeheNiv : par.redHoehe, 6));
        Parameter.Ausgleichungsart a = (Parameter.Ausgleichungsart) art.getSelectedItem();
        boolean og = a != null && a.original() && !niv;
        redZ.setEnabled(og);
        mitZ.setEnabled(og);
    }

    private void uebernehmenInMaske() {
        format.setSelectedItem(par.format);
        art.setSelectedItem(par.art);
        messModell.clear();
        par.messdateien.forEach(messModell::addElement);
        festModell.clear();
        par.festpunktdateien.forEach(festModell::addElement);
        ausgabe.setText(par.ausgabeordner);
        hauptpolygon.setSelected(par.hauptpolygon);
        zielachse.setSelected(par.zielachsfehlerAnbringen);
        zwischenblicke.setSelected(par.negativeZwischenblickeLoeschen);
        nullen.setSelected(par.fuehrendeNullenLoeschen);
        zaehler.setValue(par.zaehlerBeginn);
        redR.setText(Zahl.kurz(par.redRichtung, 6));
        redS.setText(Zahl.kurz(par.redStrecke, 6));
        mitR.setText(Zahl.kurz(par.mitRichtung, 6));
        mitS.setText(Zahl.kurz(par.mitStrecke, 6));
        mitH.setText(Zahl.kurz(par.mitHoehe, 6));
        redZ.setText(Zahl.kurz(par.redZenit, 6));
        mitZ.setText(Zahl.kurz(par.mitZenit, 6));
        neuY.setText(Double.isNaN(par.neupunktY) ? "" : Zahl.kurz(par.neupunktY, 3));
        neuX.setText(Double.isNaN(par.neupunktX) ? "" : Zahl.kurz(par.neupunktX, 3));
        formatGeaendert();
    }

    /** Liest die Maske in die Parameter; liefert eine Fehlermeldung oder null. */
    private String uebernehmenAusMaske() {
        par.format = (Messdaten.Format) format.getSelectedItem();
        par.art = (Parameter.Ausgleichungsart) art.getSelectedItem();
        par.messdateien.clear();
        for (int i = 0; i < messModell.size(); i++) {
            par.messdateien.add(messModell.get(i));
        }
        par.festpunktdateien.clear();
        for (int i = 0; i < festModell.size(); i++) {
            par.festpunktdateien.add(festModell.get(i));
        }
        par.ausgabeordner = ohneAnfuehrung(ausgabe.getText());
        par.hauptpolygon = hauptpolygon.isSelected();
        par.zielachsfehlerAnbringen = zielachse.isSelected();
        par.negativeZwischenblickeLoeschen = zwischenblicke.isSelected();
        par.fuehrendeNullenLoeschen = nullen.isSelected();
        par.zaehlerBeginn = (Integer) zaehler.getValue();
        try {
            par.redRichtung = positiv(redR, "Richtungsdifferenz (Reduktion)");
            par.redStrecke = positiv(redS, "Streckendifferenz (Reduktion)");
            double h = positiv(redH, "Höhenunterschieddifferenz (Reduktion)");
            if (par.format.istNivellement()) {
                par.redHoeheNiv = h;
            } else {
                par.redHoehe = h;
            }
            par.mitRichtung = positiv(mitR, "Richtungsdifferenz (Standpunktvereinigung)");
            par.mitStrecke = positiv(mitS, "Streckendifferenzen (Standpunktvereinigung)");
            par.mitHoehe = positiv(mitH, "Höhenunterschiede (Standpunktvereinigung)");
            par.redZenit = positiv(redZ, "Zenitwinkeldifferenz (Reduktion)");
            par.mitZenit = positiv(mitZ, "Zenitwinkel (Standpunktvereinigung)");
            par.neupunktY = optional(neuY, "Rechtswert Neupunkte");
            par.neupunktX = optional(neuX, "Hochwert Neupunkte");
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        return null;
    }

    private static double positiv(JTextField f, String name) {
        double w = Zahl.lies(f.getText());
        if (Double.isNaN(w) || w <= 0) {
            throw new IllegalArgumentException(name + ": bitte eine Zahl größer 0 eingeben.");
        }
        return w;
    }

    private static double optional(JTextField f, String name) {
        if (f.getText().trim().isEmpty()) {
            return Double.NaN;
        }
        double w = Zahl.lies(f.getText());
        if (Double.isNaN(w)) {
            throw new IllegalArgumentException(name + ": keine gültige Zahl.");
        }
        return w;
    }

    private void vorgaben() {
        Parameter v = new Parameter();
        par.redRichtung = v.redRichtung;
        par.redStrecke = v.redStrecke;
        par.redHoehe = v.redHoehe;
        par.redHoeheNiv = v.redHoeheNiv;
        par.mitRichtung = v.mitRichtung;
        par.mitStrecke = v.mitStrecke;
        par.mitHoehe = v.mitHoehe;
        par.redZenit = v.redZenit;
        par.mitZenit = v.mitZenit;
        redZ.setText(Zahl.kurz(par.redZenit, 6));
        mitZ.setText(Zahl.kurz(par.mitZenit, 6));
        redR.setText(Zahl.kurz(par.redRichtung, 6));
        redS.setText(Zahl.kurz(par.redStrecke, 6));
        mitR.setText(Zahl.kurz(par.mitRichtung, 6));
        mitS.setText(Zahl.kurz(par.mitStrecke, 6));
        mitH.setText(Zahl.kurz(par.mitHoehe, 6));
        formatGeaendert();
    }

    private File startordner() {
        if (!messModell.isEmpty()) {
            return new File(messModell.get(0));
        }
        if (!festModell.isEmpty()) {
            return new File(festModell.get(0));
        }
        return null;
    }

    private void messHinzu() {
        File[] f = Dateiauswahl.oeffnenMehrere(fenster, "Messdateien hinzufügen", startordner(),
                "Messdateien (*.beo, *.gsi, *.dat)", "beo", "gsi", "dat");
        for (File d : f) {
            messDatei(d);
        }
    }

    private void messDatei(File f) {
        String p = f.getAbsolutePath();
        if (!messModell.contains(p)) {
            messModell.addElement(p);
        }
        String n = f.getName().toLowerCase(Locale.ROOT);
        if (n.endsWith(".beo")) {
            format.setSelectedItem(Messdaten.Format.BEO);
        } else if (n.endsWith(".gsi")) {
            format.setSelectedItem(Messdaten.Format.GSI);
            art.setSelectedItem(Parameter.Ausgleichungsart.D1);
        } else if (n.endsWith(".dat")) {
            format.setSelectedItem(Messdaten.Format.DAT);
            art.setSelectedItem(Parameter.Ausgleichungsart.D1);
        }
    }

    private void festHinzu() {
        File[] f = Dateiauswahl.oeffnenMehrere(fenster, "Festpunktdateien hinzufügen", startordner(),
                "Punktdateien (*.pkt, *.txt)", "pkt", "txt");
        for (File d : f) {
            festDatei(d);
        }
    }

    private void festDatei(File f) {
        if (!festModell.contains(f.getAbsolutePath())) {
            festModell.addElement(f.getAbsolutePath());
        }
    }

    /**
     * Einfügen mit Strg+V: kopierte Dateien aus dem Explorer oder kopierte Pfade
     * (auch "Als Pfad kopieren" mit Anführungszeichen, mehrere Zeilen möglich).
     */
    private void einfuegen(boolean fest) {
        List<File> dateien = new ArrayList<>();
        try {
            java.awt.datatransfer.Transferable t = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> l = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                dateien.addAll(l);
            } else if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                for (String z : text.split("[\\r\\n]+")) {
                    String pfad = ohneAnfuehrung(z);
                    if (!pfad.isEmpty()) {
                        dateien.add(new File(pfad));
                    }
                }
            }
        } catch (Exception e) {
            // nichts einzufügen
        }
        List<String> fehlt = new ArrayList<>();
        for (File d : dateien) {
            if (!d.isFile()) {
                fehlt.add(d.getPath());
            } else if (fest) {
                festDatei(d.getAbsoluteFile());
            } else {
                messDatei(d.getAbsoluteFile());
            }
        }
        if (!fehlt.isEmpty()) {
            JOptionPane.showMessageDialog(fenster, "Datei nicht gefunden:\n" + String.join("\n", fehlt),
                    "Einfügen", JOptionPane.WARNING_MESSAGE);
        }
    }

    static String ohneAnfuehrung(String s) {
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    private void ordnerWaehlen() {
        JFileChooser c = new JFileChooser();
        String a = ohneAnfuehrung(ausgabe.getText());
        File start = !a.isEmpty() ? new File(a) : startordner();
        if (start != null) {
            c.setCurrentDirectory(start.isDirectory() ? start : start.getAbsoluteFile().getParentFile());
        }
        c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        c.setDialogTitle("Ausgabeordner wählen (Pfad kann auch eingefügt werden)");
        if (c.showOpenDialog(fenster) == JFileChooser.APPROVE_OPTION) {
            ausgabe.setText(c.getSelectedFile().getAbsolutePath());
        }
    }

    private void ordnerOeffnen() {
        if (uebernehmenAusMaske() != null || par.messdateien.isEmpty()) {
            return;
        }
        try {
            Desktop.getDesktop().open(Auswertung.ordner(par).toFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(fenster, "Ordner kann nicht geöffnet werden:\n" + e.getMessage());
        }
    }

    private void starten(boolean schreiben) {
        String fehler = uebernehmenAusMaske();
        if (fehler != null) {
            JOptionPane.showMessageDialog(fenster, fehler, "Eingabe", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (par.messdateien.isEmpty()) {
            JOptionPane.showMessageDialog(fenster, "Bitte zuerst eine oder mehrere Messdateien hinzufügen.",
                    "Messdaten", JOptionPane.WARNING_MESSAGE);
            return;
        }
        speichernStill();
        rechnen.setEnabled(false);
        nurRechnen.setEnabled(false);
        status.setText("Berechnung läuft ...");
        new SwingWorker<Auswertung.Ergebnis, Void>() {
            @Override
            protected Auswertung.Ergebnis doInBackground() throws Exception {
                return schreiben ? Auswertung.ausfuehren(par) : Auswertung.rechnen(par);
            }

            @Override
            protected void done() {
                rechnen.setEnabled(true);
                nurRechnen.setEnabled(true);
                try {
                    zeigen(get(), schreiben);
                } catch (Exception e) {
                    Throwable t = e.getCause() != null ? e.getCause() : e;
                    status.setText("Fehler");
                    JOptionPane.showMessageDialog(fenster, t.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void zeigen(Auswertung.Ergebnis e, boolean geschrieben) {
        zusammenfassung.setText(e.zusammenfassung());
        String zusatz = par.hauptpolygonZusatz();
        reduktion.setText(e.dateien.get("JAGRED" + zusatz + ".OutR"));
        vereinigung.setText(e.dateien.get("JAGMIT" + zusatz + ".OutM"));
        anzahl.setText(e.dateien.get("JAGNET" + zusatz + ".OutN"));
        String beob = Ausgabe.beobachtungenName(par);
        beobachtungen.setText(beob + "\n\n" + e.dateien.get(beob));
        StringBuilder sb = new StringBuilder();
        sb.append(Ausgabe.festpunkteName(par)).append("\n").append(e.dateien.get(Ausgabe.festpunkteName(par)));
        sb.append("\n").append(Ausgabe.neupunkteName(par)).append("\n").append(e.dateien.get(Ausgabe.neupunkteName(par)));
        sb.append("\n").append(Ausgabe.unbenutztName(par)).append("\n").append(e.dateien.get(Ausgabe.unbenutztName(par)));
        punkte.setText(sb.toString().replace("\r", ""));
        for (JTextArea t : new JTextArea[] {zusammenfassung, reduktion, vereinigung, anzahl, beobachtungen, punkte}) {
            t.setText(t.getText().replace("\r", ""));
            t.setCaretPosition(0);
        }
        reiter.setSelectedIndex(0);
        status.setText(geschrieben ? e.geschrieben.size() + " Dateien geschrieben nach " + e.ordner
                : "Berechnet, keine Dateien geschrieben.");
    }

    private void speichernStill() {
        try {
            par.speichern(Parameter.standardDatei());
        } catch (IOException e) {
            // nicht kritisch
        }
    }

    private void beenden() {
        uebernehmenAusMaske();
        speichernStill();
        fenster.dispose();
        System.exit(0);
    }

    private String begruessung() {
        return Version.titel() + "\n\n"
                + "Reduktion von Messwerten, Standpunktvereinigung und Anzahl der Anzielungen\n"
                + "für NEPTAN-Beobachtungsdateien (*.beo) und Nivellementdaten (GSI, DAT),\n"
                + "Ausgabe für die Netzausgleichung mit Jag3D (1D, 2D, 3D).\n\n"
                + "1. Messdateien hinzufügen (oder in das Fenster ziehen)\n"
                + "2. Festpunktdateien hinzufügen (optional)\n"
                + "3. Ausgleichungsart und Grenzwerte prüfen\n"
                + "4. Berechnen (F5)\n\n"
                + "Handbuch: F1";
    }

    private void handbuch() {
        JEditorPane p = new JEditorPane();
        p.setEditable(false);
        p.setContentType("text/html");
        try (InputStream in = Gui.class.getResourceAsStream("hilfe/hilfe.html")) {
            String html = in == null ? "<p>Handbuch nicht gefunden.</p>"
                    : new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("@VERSION@",
                            Version.NAME + " " + Version.VERSION);
            p.setText(html);
        } catch (IOException e) {
            p.setText("<p>Handbuch nicht lesbar.</p>");
        }
        p.addHyperlinkListener(ev -> {
            if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (ev.getDescription() != null && ev.getDescription().startsWith("#")) {
                    p.scrollToReference(ev.getDescription().substring(1));
                } else if (ev.getURL() != null) {
                    try {
                        Desktop.getDesktop().browse(ev.getURL().toURI());
                    } catch (Exception ex) {
                        // ignorieren
                    }
                }
            }
        });
        p.setCaretPosition(0);
        JFrame f = new JFrame("Handbuch – " + Version.titel());
        f.setIconImages(fenster.getIconImages());
        f.getContentPane().add(new JScrollPane(p));
        f.setSize(900, 800);
        f.setLocationRelativeTo(fenster);
        f.setVisible(true);
    }

    private void ueber() {
        JOptionPane.showMessageDialog(fenster, Version.titel() + "\n\n"
                + "Reduktion von Messwerten, Standpunktvereinigung und Anzahl der Anzielungen\n"
                + "für NEPTAN-Beobachtungsdateien und Nivellementdaten, Ausgabe für Jag3D.\n\n"
                + "MIT-Lizenz\nFragen und Hinweise: https://github.com/htw9056/beo-tool/discussions",
                "Über beo-tool", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Dateien per Ziehen und Ablegen übernehmen. */
    private final class DateiAblage extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport s) {
            return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport s) {
            try {
                @SuppressWarnings("unchecked")
                List<File> dateien = (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                for (File f : dateien) {
                    String n = f.getName().toLowerCase(Locale.ROOT);
                    if (n.endsWith(".pkt") || n.endsWith(".txt")) {
                        festDatei(f);
                    } else if (f.isFile()) {
                        messDatei(f);
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
