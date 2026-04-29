package com.utilities.UnloadToInsert;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class UnloadToInsertPanel extends JPanel {

    private final Launcher launcher;

    private static final Color ACCENT_BLUE = new Color(0x4488FF);
    private static final Color ACCENT_PINK = new Color(0xFF4488);

    private final JTextArea selectArea  = Theme.textArea();
    private final JTextArea unloadArea  = Theme.textArea();
    private final JTextArea outputArea  = Theme.textArea();
    private       JLabel    statusLabel;
    private       JTextField delimField;
    private       JCheckBox  delimCustomCb;

    public UnloadToInsertPanel(Launcher launcher) {
        this.launcher = launcher;
        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_BASE);
        buildUI();
    }

    private void buildUI() {
        add(buildToolbar(),  BorderLayout.NORTH);
        add(buildCenter(),   BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_SURFACE); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(new Color(0,0,0,20));
                for (int y=0;y<getHeight();y+=3) g2.drawLine(0,y,getWidth(),y);
                g2.setColor(Theme.BORDER_DIM); g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
        left.setOpaque(false);

        JLabel title = new JLabel("UNLOAD → INSERT");
        title.setFont(Theme.FONT_MONO_LG);
        title.setForeground(ACCENT_PINK);
        title.setBorder(BorderFactory.createEmptyBorder(0,8,0,14));

        JSeparator sep = Theme.vDivider();

        JButton genBtn   = Theme.button("▶  GENERATE");
        JButton copyBtn  = Theme.button("⎘  COPY OUTPUT");
        JButton clearBtn = Theme.ghostButton("✕  CLEAR ALL");

        genBtn.addActionListener(e -> generate());
        copyBtn.addActionListener(e -> copyOutput());
        clearBtn.addActionListener(e -> clearAll());

        // Delimiter
        JLabel delimLbl = new JLabel(" delimiter:");
        delimLbl.setFont(Theme.FONT_LABEL); delimLbl.setForeground(Theme.TEXT_SECONDARY);

        delimField = Theme.textField(3);
        delimField.setText("|");
        delimField.setPreferredSize(new Dimension(42, 28));
        delimField.setEnabled(false);
        delimField.setFont(Theme.FONT_MONO_MD);

        delimCustomCb = new JCheckBox("custom");
        delimCustomCb.setOpaque(false);
        Theme.styleCheckbox(delimCustomCb);
        delimCustomCb.addActionListener(e -> delimField.setEnabled(delimCustomCb.isSelected()));

        left.add(title); left.add(sep);
        left.add(genBtn); left.add(copyBtn); left.add(clearBtn);
        left.add(Theme.vDivider());
        left.add(delimLbl); left.add(delimField); left.add(delimCustomCb);

        bar.add(vcenter(left), BorderLayout.WEST);
        bar.setPreferredSize(new Dimension(0, Theme.NAV_HEIGHT + 8));
        return bar;
    }

    private JPanel vcenter(JPanel p) { JPanel w = new JPanel(new GridBagLayout()); w.setOpaque(false); w.add(p); return w; }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(5, 0, 5, 0);
        g.weightx = 1;

        selectArea.setRows(7); unloadArea.setRows(7); outputArea.setRows(12);

        g.gridy = 0; g.weighty = 0.25;
        p.add(makeSection("SELECT STATEMENT", selectArea, Theme.ACCENT), g);

        g.gridy = 1; g.weighty = 0.25;
        p.add(makeSection("UNLOAD ROWS  —  one per line", unloadArea, ACCENT_BLUE), g);

        g.gridy = 2; g.weighty = 0.5;
        p.add(makeSection("GENERATED INSERT STATEMENTS", outputArea, ACCENT_PINK), g);

        outputArea.setEditable(false);
        return p;
    }

    private JPanel makeSection(String title, JTextArea ta, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel header = Theme.sectionHeader(title, accent);

        JScrollPane scroll = new JScrollPane(ta);
        Theme.styleScrollPane(scroll, accent);

        // Neon border around the whole section
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_ELEVATED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_LG, Theme.RADIUS_LG);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, Theme.RADIUS_LG, Theme.RADIUS_LG);
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.add(header, BorderLayout.NORTH);
        wrap.add(scroll,  BorderLayout.CENTER);

        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG_SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_DIM));

        statusLabel = new JLabel("  paste a SELECT and unload rows, then click GENERATE");
        statusLabel.setFont(Theme.FONT_LABEL);
        statusLabel.setForeground(Theme.TEXT_DIM);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    // ── Core logic (unchanged from original) ─────────────────────────────────
    private void generate() {
        outputArea.setText(""); status("generating…", Theme.TEXT_SECONDARY);
        String sel = selectArea.getText().trim(), unl = unloadArea.getText().trim();
        if (sel.isEmpty())  { status("✗  paste a SELECT statement first.", Theme.ERROR); return; }
        if (unl.isEmpty())  { status("✗  paste at least one unload row.", Theme.ERROR); return; }

        List<String> columns;
        try { columns = parseColumns(sel); } catch (Exception ex) { status("✗  could not parse columns: "+ex.getMessage(), Theme.ERROR); return; }

        String table = detectTable(sel);
        if (table.isEmpty()) { status("✗  could not detect table name from SELECT.", Theme.ERROR); return; }

        String delim = delimCustomCb.isSelected() ? delimField.getText() : "|";
        if (delim.isEmpty()) delim = "|";

        String[] rows = unl.split("\\r?\\n");
        StringBuilder sb = new StringBuilder(); int count = 0;
        for (String raw : rows) {
            raw = raw.trim(); if (raw.isEmpty()) continue;
            if (raw.endsWith(delim)) raw = raw.substring(0, raw.length()-delim.length());
            String[] vals = raw.split(Pattern.quote(delim), -1);
            if (vals.length != columns.size()) {
                sb.append("-- ⚠ row ").append(count+1).append(": expected ").append(columns.size())
                        .append(" values, got ").append(vals.length).append("\n-- raw: ").append(raw).append("\n\n");
                count++; continue;
            }
            sb.append(buildInsert(table, columns, vals)).append("\n"); count++;
        }
        outputArea.setText(sb.toString().trim());
        status("✓  "+count+" INSERT(s) generated  —  table: "+table+"  —  columns: "+columns.size(), Theme.SUCCESS);
    }

    private List<String> parseColumns(String sql) {
        String upper = sql.toUpperCase();
        int selIdx = upper.indexOf("SELECT"), fromIdx = upper.lastIndexOf("\nFROM");
        if (fromIdx < 0) fromIdx = upper.lastIndexOf(" FROM");
        String colPart = (selIdx>=0 && fromIdx>selIdx) ? sql.substring(selIdx+6, fromIdx).trim()
                : (selIdx>=0 ? sql.substring(selIdx+6).trim() : sql);
        List<String> raw = splitTopLevelCommas(colPart), cols = new ArrayList<>();
        for (String t : raw) { t=t.trim(); if (!t.isEmpty()) cols.add(extractAlias(t)); }
        return cols;
    }

    private String extractAlias(String expr) {
        expr = expr.replaceAll("--.*","").trim();
        String[] parts = expr.split("\\s+");
        if (parts.length>=2) return parts[parts.length-1].replaceAll("[\"'`]","");
        String base = expr.replaceAll("::.*","").trim();
        if (base.contains(".")) base = base.substring(base.lastIndexOf('.')+1);
        return base.replaceAll("[\"'`]","");
    }

    private List<String> splitTopLevelCommas(String s) {
        List<String> r = new ArrayList<>(); int depth = 0; StringBuilder cur = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c=='(') { depth++; cur.append(c); } else if (c==')') { depth--; cur.append(c); }
            else if (c==',' && depth==0) { r.add(cur.toString()); cur.setLength(0); } else cur.append(c);
        }
        if (cur.length()>0) r.add(cur.toString()); return r;
    }

    private String detectTable(String sql) {
        Matcher m = Pattern.compile("(?i)\\bfrom\\s+(\\S+)").matcher(sql);
        return m.find() ? m.group(1).replaceAll(";","").trim() : "";
    }

    private String buildInsert(String table, List<String> cols, String[] vals) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append("\n    (");
        for (int i=0;i<cols.size();i++) { if(i>0) sb.append(", "); sb.append(cols.get(i)); }
        sb.append(")\nVALUES\n    (");
        for (int i=0;i<vals.length;i++) { if(i>0) sb.append(", "); sb.append(formatValue(vals[i])); }
        sb.append(");"); return sb.toString();
    }

    private String formatValue(String v) {
        if (v==null||v.trim().isEmpty()) return "NULL"; v=v.trim();
        if (v.matches("-?\\d+(\\.\\d+)?")) return v;
        if (v.matches("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}")) return "DATETIME("+v.replace('T',' ')+") YEAR TO SECOND";
        if (v.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}")) return "TO_DATETIME('"+v+"', '%m/%d/%Y %H:%M:%S')";
        if (v.matches("\\d{2}:\\d{2}:\\d{2}")) return "DATETIME("+v+") HOUR TO SECOND";
        if (v.matches("\\d{4}-\\d{2}-\\d{2}")) return "TO_DATE('"+v+"', '%Y-%m-%d')";
        if (v.matches("\\d{2}/\\d{2}/\\d{4}")) return "TO_DATE('"+v+"', '%m/%d/%Y')";
        return "'"+v.replace("'","''")+"'";
    }

    private void copyOutput() {
        String text = outputArea.getText();
        if (text.isEmpty()) { status("nothing to copy.", Theme.TEXT_SECONDARY); return; }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        status("✓  copied to clipboard.", Theme.SUCCESS);
    }

    private void clearAll() { selectArea.setText(""); unloadArea.setText(""); outputArea.setText(""); status(" ", Theme.TEXT_DIM); }
    private void status(String msg, Color c) { statusLabel.setText("  "+msg); statusLabel.setForeground(c); }
}
//package com.utilities.UnloadToInsert;
//
//import javax.swing.*;
//import javax.swing.border.*;
//import javax.swing.event.*;
//import java.awt.*;
//import java.awt.datatransfer.*;
//import java.awt.event.*;
//import java.util.*;
//import java.util.List;
//import java.util.regex.*;
//
///**
// * UnloadToInsert — GUI tool to generate SQL INSERT statements
// * from a SELECT query + pipe-delimited unload rows.
// *
// * Compile:  javac UnloadToInsert.java
// * Run:      java UnloadToInsert
// * Package:  jar cfe UnloadToInsert.jar UnloadToInsert *.class
// */
//public class UnloadToInsert extends JFrame {
//
//    // ── UI colours ────────────────────────────────────────────────────────────
//    private static final Color BG        = new Color(18, 20, 28);
//    private static final Color PANEL_BG  = new Color(26, 29, 40);
//    private static final Color BORDER_C  = new Color(55, 60, 80);
//    private static final Color ACCENT    = new Color(82, 160, 255);
//    private static final Color ACCENT2   = new Color(120, 220, 160);
//    private static final Color FG        = new Color(220, 225, 240);
//    private static final Color FG_DIM    = new Color(130, 140, 165);
//    private static final Color BTN_BG    = new Color(40, 90, 170);
//    private static final Color BTN_HOV   = new Color(55, 115, 215);
//    private static final Color OUT_BG    = new Color(14, 16, 22);
//
//    private static final Font  MONO      = new Font("Monospaced", Font.PLAIN, 13);
//    private static final Font  LABEL_F   = new Font("SansSerif", Font.BOLD,  12);
//    private static final Font  TITLE_F   = new Font("SansSerif", Font.BOLD,  18);
//
//    // ── Widgets ───────────────────────────────────────────────────────────────
//    private final JTextArea selectArea  = makeTextArea(8);
//    private final JTextArea unloadArea  = makeTextArea(8);
//    private final JTextArea outputArea  = makeTextArea(14);
//    private final JLabel    statusLabel = new JLabel(" ");
//    private final JTextField  delimField   = new JTextField("|", 4);
//    private final JCheckBox   delimCustomCb = new JCheckBox("Custom");
//
//    // ─────────────────────────────────────────────────────────────────────────
//    public UnloadToInsert() {
//        super("Unload → INSERT Generator");
//        setDefaultCloseOperation(EXIT_ON_CLOSE);
//        setPreferredSize(new Dimension(980, 780));
//        setBackground(BG);
//        buildUI();
//        pack();
//        setLocationRelativeTo(null);
//        setVisible(true);
//    }
//
//    // ── Build UI ──────────────────────────────────────────────────────────────
//    private void buildUI() {
//        JPanel root = new JPanel(new BorderLayout(0, 0));
//        root.setBackground(BG);
//        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 12, 18));
//        setContentPane(root);
//
//        root.add(buildHeader(),  BorderLayout.NORTH);
//        root.add(buildCenter(), BorderLayout.CENTER);
//        root.add(buildStatus(), BorderLayout.SOUTH);
//    }
//
//    private JPanel buildHeader() {
//        JPanel p = new JPanel(new BorderLayout());
//        p.setBackground(BG);
//        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
//
//        JLabel title = new JLabel("⬡  Unload → INSERT Generator");
//        title.setFont(TITLE_F);
//        title.setForeground(ACCENT);
//        p.add(title, BorderLayout.WEST);
//
//        JLabel hint = new JLabel("Paste SELECT, delimited unload rows, click Generate");
//        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
//        hint.setForeground(FG_DIM);
//        p.add(hint, BorderLayout.EAST);
//        return p;
//    }
//
//    private JPanel buildCenter() {
//        JPanel p = new JPanel(new GridBagLayout());
//        p.setBackground(BG);
//        GridBagConstraints g = new GridBagConstraints();
//        g.fill = GridBagConstraints.BOTH;
//        g.insets = new Insets(5, 0, 5, 0);
//        g.weightx = 1;
//
//        // ── Delimiter row ─────────────────────────────────────────────────
//        g.gridy = 0; g.weighty = 0;
//        JPanel delimRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
//        delimRow.setBackground(BG);
//        delimRow.add(makeLabel("Delimiter:"));
//        styleTextField(delimField);
//        delimField.setDisabledTextColor(new Color(180, 185, 200));
//        delimField.setEnabled(false);
//        delimCustomCb.setBackground(BG);
//        delimCustomCb.setForeground(FG);
//        delimCustomCb.setFont(LABEL_F);
//        delimCustomCb.setFocusPainted(false);
//        delimCustomCb.addActionListener(e -> delimField.setEnabled(delimCustomCb.isSelected()));
//        delimRow.add(delimField);
//        delimRow.add(delimCustomCb);
//        p.add(delimRow, g);
//
//        // ── SELECT ────────────────────────────────────────────────────────
//        g.gridy = 1; g.weighty = 0.3;
//        p.add(makeSection("SELECT Statement", selectArea, ACCENT), g);
//
//        // ── Unload rows ───────────────────────────────────────────────────
//        g.gridy = 2; g.weighty = 0.3;
//        p.add(makeSection("Unload Rows  (one per line)", unloadArea, ACCENT2), g);
//
//        // ── Buttons row ───────────────────────────────────────────────────
//        g.gridy = 3; g.weighty = 0;
//        p.add(buildButtonRow(), g);
//
//        // ── Output ────────────────────────────────────────────────────────
//        g.gridy = 4; g.weighty = 0.4;
//        p.add(makeSection("Generated INSERT Statements", outputArea, new Color(200,160,90)), g);
//
//        return p;
//    }
//
//    private JPanel buildButtonRow() {
//        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
//        p.setBackground(BG);
//
//        JButton genBtn   = makeButton("▶  Generate INSERTs", BTN_BG, BTN_HOV);
//        JButton copyBtn  = makeButton("⎘  Copy Output",      new Color(45,100,60), new Color(60,130,80));
//        JButton clearBtn = makeButton("✕  Clear All",        new Color(90,35,35),  new Color(120,50,50));
//
//        genBtn.addActionListener(e -> generate());
//        copyBtn.addActionListener(e -> copyOutput());
//        clearBtn.addActionListener(e -> clearAll());
//
//        p.add(genBtn);
//        p.add(copyBtn);
//        p.add(clearBtn);
//        return p;
//    }
//
//    private JPanel buildStatus() {
//        JPanel p = new JPanel(new BorderLayout());
//        p.setBackground(BG);
//        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
//        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
//        statusLabel.setForeground(FG_DIM);
//        p.add(statusLabel, BorderLayout.WEST);
//        return p;
//    }
//
//    // ── Core Logic ────────────────────────────────────────────────────────────
//    private void generate() {
//        outputArea.setText("");
//        status("Generating…", FG_DIM);
//
//        String selectSQL = selectArea.getText().trim();
//        String unloadText = unloadArea.getText().trim();
//
//        if (selectSQL.isEmpty()) { status("✗  Paste a SELECT statement first.", Color.RED); return; }
//        if (unloadText.isEmpty()) { status("✗  Paste at least one unload row.", Color.RED); return; }
//
//        // ── Parse columns from SELECT ─────────────────────────────────────
//        List<String> columns;
//        try {
//            columns = parseColumns(selectSQL);
//        } catch (Exception ex) {
//            status("✗  Could not parse columns: " + ex.getMessage(), Color.RED);
//            return;
//        }
//
//        // ── Detect table name ─────────────────────────────────────────────
//        String table = detectTable(selectSQL);
//        if (table.isEmpty()) {
//            status("✗  Could not detect table name from SELECT.", Color.RED);
//            return;
//        }
//
//        // ── Parse unload rows ─────────────────────────────────────────────
//        String[] rows = unloadText.split("\\r?\\n");
//        StringBuilder sb = new StringBuilder();
//        int count = 0;
//
//        for (String raw : rows) {
//            raw = raw.trim();
//            if (raw.isEmpty()) continue;
//
//            // Strip trailing delimiter then split
//            String delim = delimField.getText();
//            if (delim.isEmpty()) delim = "!";
//            if (raw.endsWith(delim)) raw = raw.substring(0, raw.length() - delim.length());
//            String[] vals = raw.split(Pattern.quote(delim), -1);
//
//            if (vals.length != columns.size()) {
//                sb.append("-- ⚠ Row ").append(count + 1)
//                        .append(": expected ").append(columns.size())
//                        .append(" values, got ").append(vals.length).append("\n");
//                sb.append("-- RAW: ").append(raw).append("\n\n");
//                count++;
//                continue;
//            }
//
//            sb.append(buildInsert(table, columns, vals));
//            sb.append("\n");
//            count++;
//        }
//
//        outputArea.setText(sb.toString().trim());
//        status("✓  " + count + " INSERT(s) generated for table " + table + " and " + columns.size() + " columns detected.", ACCENT2);
//    }
//
//    /**
//     * Parses column names/aliases from a SELECT … FROM query.
//     * Handles:  col,  col alias,  col::cast,  col::cast alias,  expression alias
//     */
//    private List<String> parseColumns(String sql) {
//        // Strip leading SELECT keyword
//        String upper = sql.toUpperCase();
//        int selIdx = upper.indexOf("SELECT");
//        int fromIdx = upper.lastIndexOf("\nFROM");
//        if (fromIdx < 0) fromIdx = upper.lastIndexOf(" FROM");
//        if (fromIdx < 0) fromIdx = upper.lastIndexOf("\nfrom");
//
//        String colPart;
//        if (selIdx >= 0 && fromIdx > selIdx) {
//            colPart = sql.substring(selIdx + 6, fromIdx).trim();
//        } else if (selIdx >= 0) {
//            colPart = sql.substring(selIdx + 6).trim();
//        } else {
//            colPart = sql;
//        }
//
//        // Split on commas (not inside parentheses)
//        List<String> raw = splitTopLevelCommas(colPart);
//        List<String> cols = new ArrayList<>();
//
//        for (String token : raw) {
//            token = token.trim();
//            if (token.isEmpty()) continue;
//            cols.add(extractAlias(token));
//        }
//        return cols;
//    }
//
//    /** Returns alias if present, otherwise the base column name stripped of ::cast. */
//    private String extractAlias(String expr) {
//        // Remove inline comments
//        expr = expr.replaceAll("--.*", "").trim();
//
//        // Split by whitespace to look for alias (last word that isn't a cast keyword)
//        String[] parts = expr.split("\\s+");
//        if (parts.length >= 2) {
//            // Last token is alias
//            String alias = parts[parts.length - 1];
//            // Remove quotes if any
//            alias = alias.replaceAll("[\"'`]", "");
//            return alias;
//        }
//
//        // No alias — strip ::cast and return base name
//        String base = expr.replaceAll("::.*", "").trim();
//        // If it contains a dot (table.col), take the col part
//        if (base.contains(".")) base = base.substring(base.lastIndexOf('.') + 1);
//        base = base.replaceAll("[\"'`]", "");
//        return base;
//    }
//
//    /** Top-level comma split (respects parentheses depth). */
//    private List<String> splitTopLevelCommas(String s) {
//        List<String> result = new ArrayList<>();
//        int depth = 0;
//        StringBuilder cur = new StringBuilder();
//        for (char c : s.toCharArray()) {
//            if      (c == '(') { depth++; cur.append(c); }
//            else if (c == ')') { depth--; cur.append(c); }
//            else if (c == ',' && depth == 0) {
//                result.add(cur.toString());
//                cur.setLength(0);
//            } else {
//                cur.append(c);
//            }
//        }
//        if (cur.length() > 0) result.add(cur.toString());
//        return result;
//    }
//
//    /** Pull table name after FROM keyword. */
//    private String detectTable(String sql) {
//        Matcher m = Pattern.compile("(?i)\\bfrom\\s+(\\S+)").matcher(sql);
//        if (m.find()) {
//            return m.group(1).replaceAll(";", "").trim();
//        }
//        return "";
//    }
//
//    /** Build a single INSERT statement. */
//    private String buildInsert(String table, List<String> cols, String[] vals) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("INSERT INTO ").append(table).append("\n");
//        sb.append("    (");
//        for (int i = 0; i < cols.size(); i++) {
//            if (i > 0) sb.append(", ");
//            sb.append(cols.get(i));
//        }
//        sb.append(")\nVALUES\n    (");
//        for (int i = 0; i < vals.length; i++) {
//            if (i > 0) sb.append(", ");
//            sb.append(formatValue(vals[i]));
//        }
//        sb.append(");");
//        return sb.toString();
//    }
//
//    /**
//     * Formats a value for use in a Teradata INSERT.
//     *
//     * Detection order:
//     *   1. Empty / blank                  -> NULL
//     *   2. Pure numeric                   -> unquoted number
//     *   3. TIMESTAMP  YYYY-MM-DD HH:MM:SS -> TO_TIMESTAMP('...', 'YYYY-MM-DDBHH:MI:SS')
//     *   4. TIMESTAMP  MM/DD/YYYY HH:MM:SS -> TO_TIMESTAMP('...', 'MM/DD/YYYYBHH:MI:SS')
//     *   5. TIME       HH:MM:SS            -> TIME '...'
//     *   6. DATE       YYYY-MM-DD          -> DATE '...'
//     *   7. DATE       MM/DD/YYYY          -> TO_DATE('...', 'MM/DD/YYYY')
//     *   8. Anything else                  -> quoted string
//     */
//    private String formatValue(String v) {
//        if (v == null || v.trim().isEmpty()) return "NULL";
//        v = v.trim();
//
//        // Numeric
//        if (v.matches("-?\\d+(\\.\\d+)?")) return v;
//
//        // TIMESTAMP: YYYY-MM-DD HH:MM:SS  (space or T separator)
//        // -> DATETIME(YYYY-MM-DD HH:MM:SS) YEAR TO SECOND
//        if (v.matches("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}")) {
//            String norm = v.replace('T', ' ');
//            return "DATETIME(" + norm + ") YEAR TO SECOND";
//        }
//
//        // TIMESTAMP: MM/DD/YYYY HH:MM:SS
//        // -> TO_DATETIME('MM/DD/YYYY HH:MM:SS', '%m/%d/%Y %H:%M:%S')
//        if (v.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}")) {
//            return "TO_DATETIME('" + v + "', '%m/%d/%Y %H:%M:%S')";
//        }
//
//        // TIME: HH:MM:SS
//        // -> DATETIME(HH:MM:SS) HOUR TO SECOND
//        if (v.matches("\\d{2}:\\d{2}:\\d{2}")) {
//            return "DATETIME(" + v + ") HOUR TO SECOND";
//        }
//
//        // DATE: YYYY-MM-DD
//        // -> TO_DATE('YYYY-MM-DD', '%Y-%m-%d')
//        if (v.matches("\\d{4}-\\d{2}-\\d{2}")) {
//            return "TO_DATE('" + v + "', '%Y-%m-%d')";
//        }
//
//        // DATE: MM/DD/YYYY
//        // -> TO_DATE('MM/DD/YYYY', '%m/%d/%Y')
//        if (v.matches("\\d{2}/\\d{2}/\\d{4}")) {
//            return "TO_DATE('" + v + "', '%m/%d/%Y')";
//        }
//
//        // Plain string
//        return "'" + v.replace("'", "''") + "'";
//    }
//
//    // ── Actions ───────────────────────────────────────────────────────────────
//    private void copyOutput() {
//        String text = outputArea.getText();
//        if (text.isEmpty()) { status("Nothing to copy.", FG_DIM); return; }
//        Toolkit.getDefaultToolkit().getSystemClipboard()
//                .setContents(new StringSelection(text), null);
//        status("✓  Copied to clipboard.", ACCENT2);
//    }
//
//    private void clearAll() {
//        selectArea.setText("");
//        unloadArea.setText("");
//        outputArea.setText("");
//        status(" ", FG_DIM);
//    }
//
//    private void status(String msg, Color c) {
//        statusLabel.setText(msg);
//        statusLabel.setForeground(c);
//    }
//
//    // ── Widget helpers ────────────────────────────────────────────────────────
//    private JTextArea makeTextArea(int rows) {
//        JTextArea ta = new JTextArea(rows, 60);
//        ta.setFont(MONO);
//        ta.setBackground(OUT_BG);
//        ta.setForeground(FG);
//        ta.setCaretColor(ACCENT);
//        ta.setLineWrap(false);
//        ta.setTabSize(4);
//        ta.setSelectedTextColor(Color.WHITE);
//        ta.setSelectionColor(new Color(60, 100, 180));
//        ta.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
//        return ta;
//    }
//
//    private JScrollPane scrollWrap(JTextArea ta) {
//        JScrollPane sp = new JScrollPane(ta,
//                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
//                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        sp.setBorder(BorderFactory.createLineBorder(BORDER_C));
//        sp.getViewport().setBackground(OUT_BG);
//        return sp;
//    }
//
//    private JPanel makeSection(String title, JTextArea ta, Color accent) {
//        JPanel p = new JPanel(new BorderLayout(0, 4));
//        p.setBackground(BG);
//        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
//
//        JLabel lbl = new JLabel(title);
//        lbl.setFont(LABEL_F);
//        lbl.setForeground(accent);
//        p.add(lbl, BorderLayout.NORTH);
//        p.add(scrollWrap(ta), BorderLayout.CENTER);
//        return p;
//    }
//
//    private JLabel makeLabel(String text) {
//        JLabel l = new JLabel(text);
//        l.setFont(LABEL_F);
//        l.setForeground(FG_DIM);
//        return l;
//    }
//
//    private void styleTextField(JTextField tf) {
//        tf.setFont(MONO);
//        tf.setBackground(OUT_BG);
//        tf.setForeground(Color.WHITE);
//        tf.setCaretColor(ACCENT);
//        tf.setOpaque(true);
//        tf.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createLineBorder(BORDER_C),
//                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
//    }
//
//    private JButton makeButton(String label, Color bg, Color hover) {
//        JButton btn = new JButton(label);
//        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
//        btn.setBackground(bg);
//        btn.setForeground(Color.WHITE);
//        btn.setFocusPainted(false);
//        btn.setBorderPainted(false);
//        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
//        btn.addMouseListener(new MouseAdapter() {
//            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
//            public void mouseExited (MouseEvent e) { btn.setBackground(bg);    }
//        });
//        return btn;
//    }
//
//    // ── Entry point ───────────────────────────────────────────────────────────
//    public static void main(String[] args) {
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception ignored) {}
//        SwingUtilities.invokeLater(UnloadToInsert::new);
//    }
//}