package com.utilities.InsertCommandFormatter;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class InsertCommandFormatterPanel extends JPanel {

    private final Launcher launcher;
    private JTextArea   inputArea, outputArea;
    private JTextField  searchField;
    private JLabel      matchCountLabel;

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

    private void buildUI() {
        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildSplit(),     BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0,0,0,20));
                for (int y = 0; y < getHeight(); y += 3) g2.drawLine(0, y, getWidth(), y);
                g2.setColor(Theme.BORDER_DIM);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 7));
        left.setOpaque(false);

        JLabel title = new JLabel("INSERT FORMATTER");
        title.setFont(Theme.FONT_MONO_LG);
        title.setForeground(Theme.ACCENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 14));

        JSeparator sep = Theme.vDivider();

        JButton formatBtn    = Theme.button("▶  FORMAT");
        JButton clearBtn     = Theme.ghostButton("CLEAR");
        JButton copyQueryBtn = Theme.button("⎘  COPY QUERY");

        String[] sizes = {"10","11","12","13","14","15","16","18","20","22","24"};
        JComboBox<String> fontCombo = Theme.comboBox(sizes);
        fontCombo.setSelectedItem("13");
        fontCombo.setPreferredSize(new Dimension(58, 26));
        fontCombo.addActionListener(e -> {
            int sz = Integer.parseInt((String) fontCombo.getSelectedItem());
            inputArea.setFont(new Font("Courier New", Font.PLAIN, sz));
            outputArea.setFont(new Font("Courier New", Font.PLAIN, sz));
        });

        left.add(title); left.add(sep);
        left.add(formatBtn); left.add(clearBtn); left.add(copyQueryBtn);
        left.add(Theme.vDivider());
        JLabel szLbl = new JLabel(" size:"); szLbl.setFont(Theme.FONT_LABEL);
        szLbl.setForeground(Theme.TEXT_SECONDARY);
        left.add(szLbl); left.add(fontCombo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 7));
        right.setOpaque(false);

        searchField = Theme.textField(16);
        searchField.setPreferredSize(new Dimension(190, 28));
        searchField.setFont(Theme.FONT_MONO_SM);

        JButton searchBtn = Theme.ghostButton("FIND");
        JButton prevBtn   = Theme.ghostButton("↑");
        JButton nextBtn   = Theme.ghostButton("↓");

        matchCountLabel = new JLabel("");
        matchCountLabel.setFont(Theme.FONT_LABEL);
        matchCountLabel.setForeground(Theme.TEXT_SECONDARY);

        right.add(matchCountLabel); right.add(searchField);
        right.add(searchBtn); right.add(prevBtn); right.add(nextBtn);

        bar.add(vcenter(left),  BorderLayout.WEST);
        bar.add(vcenter(right), BorderLayout.EAST);

        formatBtn.addActionListener(e -> runFormat());
        clearBtn.addActionListener(e -> { inputArea.setText(""); outputArea.setText(""); });
        copyQueryBtn.addActionListener(e -> copyQuery());
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        nextBtn.addActionListener(e -> highlightNext());
        prevBtn.addActionListener(e -> highlightPrev());

        return bar;
    }

    private JPanel vcenter(JPanel p) {
        JPanel w = new JPanel(new GridBagLayout()); w.setOpaque(false); w.add(p); return w;
    }

    private JSplitPane buildSplit() {
        inputArea  = Theme.textArea();
        outputArea = Theme.textArea();
        inputArea.setLineWrap(true); inputArea.setWrapStyleWord(true);
        outputArea.setEditable(true);

        JPanel leftPanel  = paneWithHeader("INPUT QUERY",              inputArea,  Theme.ACCENT);
        JPanel rightPanel = paneWithHeader("FORMATTED COL-VALUE TABLE", outputArea, new Color(0x4488FF));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setResizeWeight(0.45);
        split.setBorder(null);
        split.setDividerSize(4);
        split.setBackground(Theme.BORDER_MID);
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider d = new BasicSplitPaneDivider(this);
                d.setBackground(Theme.BORDER_MID); d.setBorder(null); return d;
            }
        });
        return split;
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

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        bar.setBackground(Theme.BG_SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_DIM));
        JLabel lbl = new JLabel("ready");
        lbl.setFont(Theme.FONT_LABEL); lbl.setForeground(Theme.TEXT_DIM);
        bar.add(lbl); return bar;
    }

    private void runFormat() {
        clearHighlights();
        String raw = inputArea.getText().trim();
        if (raw.isEmpty()) return;
        try {
            ParseResult r = parseInsert(raw);
            lastColumns = r.columns; lastValues = r.values; lastTableName = r.tableName;
            outputArea.setText(buildColValueTable(r.columns, r.values));
        } catch (Exception ex) {
            outputArea.setText("// ERROR:\n// " + ex.getMessage());
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
            JOptionPane.showMessageDialog(this, "Copied to clipboard.", "Done", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { outputArea.setText("// Copy failed: " + ex.getMessage()); }
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
}