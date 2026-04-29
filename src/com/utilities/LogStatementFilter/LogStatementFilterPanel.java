package com.utilities.LogStatementFilter;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class LogStatementFilterPanel extends JPanel {

    private final Launcher launcher;

    private static final Color CARD_ACCENT = new Color(0x00CCFF);
    private static final Color CARD_BG     = new Color(0x0D0E18);
    private static final Font  COURIER_B10 = new Font("Courier New", Font.BOLD,  10);
    private static final Font  COURIER_11  = new Font("Courier New", Font.PLAIN, 11);
    private static final Font  COURIER_13  = new Font("Courier New", Font.PLAIN, 13);

    private JTextField fileField;
    private JTextField keywordField;
    private JTextArea  statusArea;
    private JCheckBox  caseCheckBox;
    private JButton    filterBtn;
    private JLabel     fileMetaLabel;

    private final Map<String, Integer> keywordMatchCount = new LinkedHashMap<>();

    public LogStatementFilterPanel(Launcher launcher) {
        this.launcher = launcher;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_BASE);
        buildUI();
    }

    private void buildUI() {
        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildBody(),      BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0,0,0,20));
                for (int y = 0; y < getHeight(); y+=3) g2.drawLine(0,y,getWidth(),y);
                g2.setColor(Theme.BORDER_DIM);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);

        // Left
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 7));
        left.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel("LOG FILTER");
        title.setFont(Theme.FONT_MONO_LG); title.setForeground(CARD_ACCENT);
        JLabel sub = new JLabel("STATEMENT EXTRACTION ENGINE");
        sub.setFont(COURIER_B10); sub.setForeground(Theme.TEXT_DIM);
        titleBlock.add(title); titleBlock.add(Box.createVerticalStrut(1)); titleBlock.add(sub);

        fileField = new JTextField("no file selected…");
        fileField.setEditable(false);
        fileField.setFont(COURIER_11);
        fileField.setForeground(Theme.TEXT_SECONDARY);
        fileField.setBackground(Theme.BG_DEEP);
        fileField.setPreferredSize(new Dimension(320, 28));
        fileField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER_MID, 1, true),
                BorderFactory.createEmptyBorder(4, 9, 4, 9)));

        fileMetaLabel = new JLabel("");
        fileMetaLabel.setFont(COURIER_B10);
        fileMetaLabel.setForeground(Theme.TEXT_SECONDARY);

        JButton browseBtn = Theme.ghostButton("+ BROWSE");
        browseBtn.addActionListener(this::browseFile);

        left.add(titleBlock);
        left.add(Theme.vDivider());
        left.add(fileField);
        left.add(browseBtn);
        left.add(fileMetaLabel);

        // Right
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 7));
        right.setOpaque(false);

        caseCheckBox = new JCheckBox("CASE SENSITIVE");
        caseCheckBox.setOpaque(false);
        Theme.styleCheckbox(caseCheckBox);
        caseCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { caseCheckBox.setForeground(Theme.TEXT_PRIMARY); }
            public void mouseExited (MouseEvent e) { caseCheckBox.setForeground(Theme.TEXT_SECONDARY); }
        });

        filterBtn = Theme.button("▶  RUN FILTER");
        filterBtn.addActionListener(this::processFile);
        filterBtn.setPreferredSize(new Dimension(130, 32));

        right.add(caseCheckBox); right.add(filterBtn);

        bar.add(vcenter(left),  BorderLayout.WEST);
        bar.add(vcenter(right), BorderLayout.EAST);
        bar.setPreferredSize(new Dimension(0, Theme.NAV_HEIGHT + 12));
        return bar;
    }

    private JPanel vcenter(JPanel p) { JPanel w = new JPanel(new GridBagLayout()); w.setOpaque(false); w.add(p); return w; }

    private JSplitPane buildBody() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildHelpPanel());
        split.setDividerLocation(440); split.setResizeWeight(0.55);
        split.setDividerSize(4); split.setBorder(null);
        split.setBackground(Theme.BORDER_MID);
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                javax.swing.plaf.basic.BasicSplitPaneDivider d = new javax.swing.plaf.basic.BasicSplitPaneDivider(this);
                d.setBackground(Theme.BORDER_MID); d.setBorder(null); return d;
            }
        });
        return split;
    }

    private JPanel buildLeftPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(Theme.BG_BASE);

        // Keyword card
        JPanel kwCard = buildCard("KEYWORDS  —  comma-separated");
        kwCard.setLayout(new BorderLayout());

        keywordField = new JTextField();
        keywordField.setFont(COURIER_13);
        keywordField.setBackground(Theme.BG_DEEP);
        keywordField.setForeground(Theme.TEXT_SECONDARY);
        keywordField.setCaretColor(Theme.ACCENT);
        keywordField.setSelectionColor(Theme.ACCENT_DARK);
        keywordField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0x00CCFF, true).darker(), 1),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        keywordField.setText("e.g.  ERROR, timeout, NullPointer");
        keywordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (keywordField.getForeground().equals(Theme.TEXT_SECONDARY)) {
                    keywordField.setText(""); keywordField.setForeground(Theme.ACCENT_GLOW);
                    keywordField.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(CARD_ACCENT, 1), BorderFactory.createEmptyBorder(7,10,7,10)));
                }
            }
            public void focusLost(FocusEvent e) {
                if (keywordField.getText().trim().isEmpty()) {
                    keywordField.setText("e.g.  ERROR, timeout, NullPointer");
                    keywordField.setForeground(Theme.TEXT_SECONDARY);
                    keywordField.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(new Color(0x004466), 1), BorderFactory.createEmptyBorder(7,10,7,10)));
                }
            }
        });
        keywordField.addActionListener(this::processFile);
        kwCard.add(keywordField, BorderLayout.CENTER);

        // Status card
        JPanel statusCard = buildCard("OUTPUT  &  KEYWORD STATS");
        statusCard.setLayout(new BorderLayout());
        statusArea = new JTextArea();
        statusArea.setFont(COURIER_11);
        statusArea.setBackground(Theme.BG_DEEP);
        statusArea.setForeground(Theme.TEXT_SECONDARY);
        statusArea.setCaretColor(Theme.ACCENT);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true); statusArea.setWrapStyleWord(true);
        statusArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        statusArea.setText("Awaiting input…");
        JScrollPane scroll = new JScrollPane(statusArea);
        Theme.styleScrollPane(scroll, CARD_ACCENT);
        statusCard.add(scroll, BorderLayout.CENTER);

        p.add(kwCard,     BorderLayout.NORTH);
        p.add(statusCard, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildHelpPanel() {
        JPanel card = buildCard("HOW  TO  USE");
        card.setLayout(new BorderLayout());

        JTextPane tp = new JTextPane();
        tp.setEditable(false);
        tp.setBackground(Theme.BG_DEEP);
        tp.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        StyledDocument doc = tp.getStyledDocument();
        Style base = doc.addStyle("base", null);
        StyleConstants.setFontFamily(base, "Courier New"); StyleConstants.setFontSize(base, 11);
        StyleConstants.setForeground(base, Theme.TEXT_SECONDARY);
        Style head = doc.addStyle("head", base);
        StyleConstants.setForeground(head, CARD_ACCENT); StyleConstants.setBold(head, true);
        Style hi   = doc.addStyle("hi",   base); StyleConstants.setForeground(hi, Theme.TEXT_PRIMARY); StyleConstants.setBold(hi, true);
        Style mk   = doc.addStyle("mk",   base); StyleConstants.setForeground(mk, Theme.ACCENT); StyleConstants.setBold(mk, true);
        Style dim  = doc.addStyle("dim",  base); StyleConstants.setForeground(dim, Theme.TEXT_DIM);

        try {
            ins(doc,"KEYWORDS\n",head);
            ins(doc,"  Enter keywords separated by commas.\n  No limit — as many as needed.\n\n",base);
            ins(doc,"  Example:\n",dim); ins(doc,"  ERROR, timeout, NullPointer\n\n",hi);
            ins(doc,"WHAT IS A STATEMENT?\n",head);
            ins(doc,"  Lines belonging to one log entry\n  are treated as a single unit.\n\n",base);
            ins(doc,"  New statement starts when line begins with ",base);
            ins(doc,"[",mk); ins(doc,"  (timestamp bracket).\n\n",base);
            ins(doc,"  Example:\n",dim);
            ins(doc,"  [10:32:01] ERROR db failed\n    at DB.connect():45\n    caused by: timeout\n\n",hi);
            ins(doc,"  If ANY keyword matches, the ENTIRE\n  statement is written to output.\n\n",base);
            ins(doc,"CASE SENSITIVE\n",head);
            ins(doc,"  OFF ",mk); ins(doc,"(default) — matches any casing\n",base);
            ins(doc,"  ON  ",mk); ins(doc,"— exact casing only.\n\n",base);
            ins(doc,"OUTPUT FILE\n",head);
            ins(doc,"  Saved as ",base); ins(doc,"name_filtered.log\n",hi);
            ins(doc,"  in the same folder as your log.\n",base);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> tp.setCaretPosition(0));
        JScrollPane scroll = new JScrollPane(tp);
        Theme.styleScrollPane(scroll, CARD_ACCENT);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        bar.setBackground(Theme.BG_SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0, Theme.BORDER_DIM));
        JLabel lbl = new JLabel("select a .log file, enter keywords, then press RUN FILTER or Enter");
        lbl.setFont(COURIER_B10); lbl.setForeground(Theme.TEXT_DIM);
        bar.add(lbl); return bar;
    }

    private JPanel buildCard(String label) {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        TitledBorder tb = BorderFactory.createTitledBorder(
                new LineBorder(new Color(0x004466), 1), "  " + label + "  ");
        tb.setTitleFont(COURIER_B10); tb.setTitleColor(new Color(0x006688));
        card.setBorder(new CompoundBorder(tb, BorderFactory.createEmptyBorder(6,8,8,8)));
        return card;
    }

    private static void ins(StyledDocument doc, String text, Style style) throws Exception {
        doc.insertString(doc.getLength(), text, style);
    }

    private void browseFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Log files (*.log)", "log"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            fileField.setText(f.getAbsolutePath()); fileField.setForeground(Theme.TEXT_PRIMARY);
            fileMetaLabel.setText(f.getName() + "  (" + humanSize(f.length()) + ")");
            fileMetaLabel.setForeground(CARD_ACCENT);
            statusArea.setForeground(Theme.TEXT_SECONDARY);
            statusArea.setText("File loaded.\nEnter keywords and press RUN FILTER.");
        }
    }

    private void processFile(ActionEvent e) {
        String path = fileField.getText();
        if (path.startsWith("no file") || path.isEmpty()) { warn("Please select a log file first.", "Missing File"); return; }
        List<String> keywords = getKeywords();
        if (keywords.isEmpty()) { warn("Please enter at least one keyword.", "Missing Keywords"); return; }

        boolean cs = caseCheckBox.isSelected();
        keywordMatchCount.clear(); keywords.forEach(k -> keywordMatchCount.put(k, 0));
        filterBtn.setEnabled(false); filterBtn.setText("  PROCESSING…");
        statusArea.setForeground(Theme.TEXT_SECONDARY); statusArea.setText("Running filter…\n");

        File inputFile = new File(path);
        File outputFile = new File(inputFile.getParent(),
                inputFile.getName().replaceFirst("(\\.log)?$", "_filtered.log"));

        new SwingWorker<Void,Void>() {
            int matched = 0;
            @Override protected Void doInBackground() throws Exception {
                try (var br = Files.newBufferedReader(inputFile.toPath());
                     var bw = Files.newBufferedWriter(outputFile.toPath())) {
                    String line; StringBuilder stmt = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("[") && stmt.length() > 0) {
                            if (matchAndCount(stmt.toString(), keywords, cs)) { bw.write(stmt.toString()); matched++; }
                            stmt.setLength(0);
                        }
                        stmt.append(line).append(System.lineSeparator());
                    }
                    if (stmt.length() > 0 && matchAndCount(stmt.toString(), keywords, cs)) { bw.write(stmt.toString()); matched++; }
                }
                return null;
            }
            @Override protected void done() {
                try { get(); updateStatus(matched, outputFile); }
                catch (Exception ex) { statusArea.setForeground(Theme.ERROR); statusArea.setText("ERROR:\n  " + (ex.getCause()!=null?ex.getCause():ex).getMessage()); }
                finally { filterBtn.setEnabled(true); filterBtn.setText("▶  RUN FILTER"); }
            }
        }.execute();
    }

    private boolean matchAndCount(String stmt, List<String> kw, boolean cs) {
        boolean m = false; String src = cs ? stmt : stmt.toLowerCase();
        for (String k : kw) { if (src.contains(cs?k:k.toLowerCase())) { keywordMatchCount.merge(k,1,Integer::sum); m=true; } }
        return m;
    }

    private void updateStatus(int total, File out) {
        StringBuilder sb = new StringBuilder();
        sb.append("OUTPUT FILE\n  ").append(out.getName()).append("\n\nTOTAL MATCHED  ").append(total).append("\n");
        sb.append("─".repeat(30)).append("\nKEYWORD HITS\n");
        keywordMatchCount.forEach((k,v) -> sb.append(String.format("  %-22s %d\n",k,v)));
        statusArea.setForeground(Theme.SUCCESS); statusArea.setText(sb.toString());
    }

    private List<String> getKeywords() {
        List<String> list = new ArrayList<>();
        String raw = keywordField.getText().trim();
        if (raw.isEmpty() || raw.startsWith("e.g.")) return list;
        for (String s : raw.split(",")) { String t = s.trim(); if (!t.isEmpty()) list.add(t); }
        return list;
    }

    private void warn(String msg, String title) { JOptionPane.showMessageDialog(this, msg, title, JOptionPane.WARNING_MESSAGE); }
    private String humanSize(long b) {
        if (b<1024) return b+" B"; if (b<1024*1024) return String.format("%.1f KB",b/1024.0);
        return String.format("%.1f MB",b/(1024.0*1024));
    }
}