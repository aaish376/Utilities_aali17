package com.utilities.InsertCommandFormatter;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class InsertCommandFormatterPanel extends JPanel {

    private final Launcher launcher;

    // ── Accents for this panel ───────────────────────────────────────────────
    private static final Color PANEL_ACCENT  = Theme.ACCENT;
    private static final Color OUTPUT_ACCENT = Theme.INFO;

    // ── State ─────────────────────────────────────────────────────────────────
    private JTextArea   inputArea, outputArea;
    private JTextField  searchField;
    private JLabel      matchCountLabel;
    private Theme.StatusBar statusBar;

    private final Highlighter.HighlightPainter hlPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(0x00FF8840, true));

    private final List<Integer> matchPositions = new ArrayList<>();
    private int currentMatchIndex = -1;
    private List<String> lastColumns, lastValues;
    private String lastTableName;

    public InsertCommandFormatterPanel(Launcher launcher) {
        this.launcher = launcher;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_BASE);
        buildUI();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI CONSTRUCTION
    // ══════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildSplit(),     BorderLayout.CENTER);
        statusBar = Theme.statusBar();
        add(statusBar.panel, BorderLayout.SOUTH);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JLabel title = new JLabel("Insert Command Formatter");
        title.setFont(Theme.FONT_UI_BOLD);
        title.setForeground(PANEL_ACCENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));

        JButton formatBtn    = Theme.button("▶  Format");
        JButton clearBtn     = Theme.ghostButton("Clear");
        JButton copyQueryBtn = Theme.button("⎘  Copy Query");

        String[] sizes = {"10","11","12","13","14","15","16","18","20","22","24"};
        JComboBox<String> fontCombo = Theme.comboBox(sizes);
        fontCombo.setSelectedItem("13");
        fontCombo.setPreferredSize(new Dimension(58, 26));
        fontCombo.addActionListener(e -> {
            int sz = Integer.parseInt((String) fontCombo.getSelectedItem());
            inputArea.setFont(new Font("Consolas", Font.PLAIN, sz));
            outputArea.setFont(new Font("Consolas", Font.PLAIN, sz));
        });

        JLabel szLbl = new JLabel("Size:");
        szLbl.setFont(Theme.FONT_LABEL);
        szLbl.setForeground(Theme.TEXT_SECONDARY);

        JPanel left = Theme.toolRow(title, Theme.vDivider(),
                formatBtn, clearBtn, copyQueryBtn, Theme.vDivider(), szLbl, fontCombo);
        left.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        searchField = Theme.textField(16);
        searchField.setPreferredSize(new Dimension(190, 28));
        searchField.setFont(Theme.FONT_MONO_SM);

        JButton searchBtn = Theme.ghostButton("Find");
        JButton prevBtn   = Theme.ghostButton("↑");
        JButton nextBtn   = Theme.ghostButton("↓");

        matchCountLabel = new JLabel("");
        matchCountLabel.setFont(Theme.FONT_UI_SM);
        matchCountLabel.setForeground(Theme.TEXT_SECONDARY);

        JButton howToUseBtn = Theme.helpButton("How to Use");
        howToUseBtn.addActionListener(e -> launcher.navigateTo(Launcher.INSERT_HELP));

        JPanel right = Theme.toolRow(matchCountLabel, searchField, searchBtn, prevBtn, nextBtn, Theme.vDivider(), howToUseBtn);

        JPanel bar = Theme.toolbar(left, right);

        formatBtn.addActionListener(e -> runFormat());
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
            statusBar.set("Ready", Theme.TEXT_DIM);
        });
        copyQueryBtn.addActionListener(e -> copyQuery());
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        nextBtn.addActionListener(e -> highlightNext());
        prevBtn.addActionListener(e -> highlightPrev());

        return bar;
    }

    // ── Main split: input | formatted output ────────────────────────────────
    private JSplitPane buildSplit() {
        inputArea  = Theme.textArea();
        outputArea = Theme.textArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        outputArea.setEditable(true);

        JPanel leftPanel  = paneWithHeader("Input Query",               inputArea,  PANEL_ACCENT);
        JPanel rightPanel = paneWithHeader("Formatted Col-Value Table", outputArea, OUTPUT_ACCENT);

        return Theme.split(leftPanel, rightPanel, 500, 0.45);
    }

    private JPanel paneWithHeader(String title, JTextArea area, Color accent) {
        JPanel header = Theme.sectionHeader(title, accent);

        JScrollPane scroll = new JScrollPane(area);
        Theme.styleScrollPane(scroll, accent);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_DEEP);
        p.add(header, BorderLayout.NORTH);
        p.add(scroll,  BorderLayout.CENTER);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FORMATTING / SEARCH LOGIC
    // ══════════════════════════════════════════════════════════════════════════

    private void runFormat() {
        clearHighlights();
        String raw = inputArea.getText().trim();
        if (raw.isEmpty()) return;
        try {
            ParseResult r = parseInsert(raw);
            lastColumns = r.columns; lastValues = r.values; lastTableName = r.tableName;
            outputArea.setText(buildColValueTable(r.columns, r.values));
            statusBar.set("Formatted " + r.columns.size() + " column(s) from table " + r.tableName, Theme.SUCCESS);
        } catch (Exception ex) {
            outputArea.setText("// ERROR:\n// " + ex.getMessage());
            statusBar.set("Error: " + ex.getMessage(), Theme.ERROR);
        }
    }

    private void copyQuery() {
        try {
            List<String> cols = new ArrayList<>(), vals = new ArrayList<>();
            for (String line : outputArea.getText().split("\n")) {
                if (line.contains("|") && !line.startsWith("COLUMN")) {
                    String[] p = line.split("\\|");
                    if (p.length >= 2) { String c = p[0].trim(), v = p[1].trim(); if (!c.isEmpty()) { cols.add(c); vals.add(v); } }
                }
            }
            lastColumns = cols; lastValues = vals;
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(generateInsertSQL()), null);
            statusBar.set("Copied INSERT statement to clipboard", Theme.SUCCESS);
            JOptionPane.showMessageDialog(this, "Copied to clipboard.", "Done", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            outputArea.setText("// Copy failed: " + ex.getMessage());
            statusBar.set("Copy failed: " + ex.getMessage(), Theme.ERROR);
        }
    }

    private void performSearch() {
        clearHighlights(); matchPositions.clear(); currentMatchIndex = -1;
        String q = searchField.getText();
        if (q.isEmpty()) { matchCountLabel.setText(""); return; }
        String text = outputArea.getText();
        int i = text.indexOf(q);
        while (i >= 0) { matchPositions.add(i); i = text.indexOf(q, i+1); }
        if (!matchPositions.isEmpty()) { currentMatchIndex = 0; highlightAtIndex(); }
        updateMatchCount();
    }
    private void highlightNext()  { if (matchPositions.isEmpty()) return; currentMatchIndex = (currentMatchIndex+1)%matchPositions.size(); highlightAtIndex(); updateMatchCount(); }
    private void highlightPrev()  { if (matchPositions.isEmpty()) return; currentMatchIndex = (currentMatchIndex-1+matchPositions.size())%matchPositions.size(); highlightAtIndex(); updateMatchCount(); }
    private void highlightAtIndex() {
        clearHighlights();
        try { int pos = matchPositions.get(currentMatchIndex);
            outputArea.getHighlighter().addHighlight(pos, pos+searchField.getText().length(), hlPainter);
            outputArea.setCaretPosition(pos); } catch (Exception ignored) {}
    }
    private void clearHighlights() { outputArea.getHighlighter().removeAllHighlights(); }
    private void updateMatchCount() {
        matchCountLabel.setText(matchPositions.isEmpty() ? "no matches" : (currentMatchIndex+1)+" / "+matchPositions.size());
    }

    private static class ParseResult { String tableName; List<String> columns; List<String> values; }
    private static ParseResult parseInsert(String sql) {
        sql = sql.trim().replaceAll("\\s+", " "); String upper = sql.toUpperCase();
        if (!upper.startsWith("INSERT INTO")) throw new IllegalArgumentException("Must start with INSERT INTO");
        int fp = sql.indexOf("("), sp = sql.indexOf(")");
        String table = sql.substring("INSERT INTO".length(), fp).trim();
        List<String> cols = new ArrayList<>();
        for (String c : sql.substring(fp+1, sp).split(",")) cols.add(c.trim());
        int vi = upper.indexOf("VALUES"); if (vi < 0) throw new IllegalArgumentException("VALUES keyword missing");
        int ov = sql.indexOf("(", vi), cv = sql.lastIndexOf(")");
        List<String> vals = splitValues(sql.substring(ov+1, cv));
        if (vals.size() != cols.size()) throw new IllegalArgumentException("Column/value count mismatch.");
        ParseResult r = new ParseResult(); r.tableName = table; r.columns = cols; r.values = vals; return r;
    }
    private static List<String> splitValues(String part) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder(); int depth = 0;
        for (char ch : part.toCharArray()) {
            if (ch=='(') depth++; else if (ch==')') depth--;
            if (ch==',' && depth==0) { list.add(sb.toString().trim()); sb.setLength(0); } else sb.append(ch);
        }
        if (sb.length()>0) list.add(sb.toString().trim()); return list;
    }
    private String buildColValueTable(List<String> cols, List<String> vals) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-40s | %s\n", "COLUMN", "VALUE"));
        sb.append("─".repeat(80)).append("\n");
        for (int i = 0; i < cols.size(); i++) sb.append(String.format("%-40s | %s\n", cols.get(i), vals.get(i)));
        return sb.toString();
    }
    private String generateInsertSQL() {
        if (lastColumns == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(lastTableName).append(" (\n");
        for (int i = 0; i < lastColumns.size(); i++) sb.append("    ").append(lastColumns.get(i)).append(i<lastColumns.size()-1?",":"").append("\n");
        sb.append(") VALUES (\n");
        for (int i = 0; i < lastValues.size(); i++) sb.append("    ").append(lastValues.get(i)).append(i<lastValues.size()-1?",":"").append("\n");
        sb.append(");"); return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELP PAGE
    // ══════════════════════════════════════════════════════════════════════════

    public JPanel createHelpPanel() {
        String html = ""
                + "<h2>Insert Command Formatter</h2>"
                + "<p>Turns a raw <code>INSERT INTO ... VALUES (...)</code> statement into a readable "
                + "column/value table, and lets you copy a cleanly reformatted version back out.</p>"
                + "<h3>Steps</h3>"
                + "<ol>"
                + "<li>Paste a full <code>INSERT INTO table (col1, col2, ...) VALUES (val1, val2, ...)</code> "
                + "statement into the left <b>Input Query</b> pane.</li>"
                + "<li>Click <b>Format</b> — the right pane shows each column lined up next to its value.</li>"
                + "<li>Click <b>Copy Query</b> to copy a nicely re-indented <code>INSERT</code> statement "
                + "(built from the current column/value table) to your clipboard.</li>"
                + "<li>Use <b>Clear</b> to reset both panes.</li>"
                + "</ol>"
                + "<h3>Searching the output</h3>"
                + "<p>Type into the search box in the toolbar and press <b>Find</b> (or Enter) to highlight "
                + "matches in the formatted output. Use the <b>↑</b> / <b>↓</b> buttons to step between matches; "
                + "the count next to the search box shows your current position.</p>"
                + "<h3>Font size</h3>"
                + "<p>The <b>Size</b> dropdown changes the font size of both the input and output panes.</p>";
        return Theme.helpPage("Insert Command Formatter — How to Use", PANEL_ACCENT, html,
                () -> launcher.navigateTo(Launcher.INSERT));
    }
}
