package com.utilities.UnloadToInsert;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class UnloadToInsertPanel extends JPanel {

    private final Launcher launcher;

    private static final Color ACCENT_BLUE = new Color(0x4488FF);
    private static final Color ACCENT_PINK = new Color(0xFF4488);

    private final JTextArea selectArea  = Theme.textArea();
    private final JTextArea ddlArea     = Theme.textArea();
    private final JTextArea unloadArea  = Theme.textArea();
    private final JTextArea outputArea  = Theme.textArea();
    private       Theme.StatusBar statusBar;
    private       JTextField delimField;
    private       JCheckBox  delimCustomCb;
    private       JComboBox<String> ddlComboBox;
    private       boolean   suppressComboEvents = false;

    private static final String DDL_STORE_DIRNAME = "ddl_store";
    private static final String DDL_PLACEHOLDER   = "— select saved DDL —";

    public UnloadToInsertPanel(Launcher launcher) {
        this.launcher = launcher;
        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_BASE);
        buildUI();
    }

    private void buildUI() {
        add(buildToolbar(),  BorderLayout.NORTH);
        add(buildCenter(),   BorderLayout.CENTER);
        statusBar = Theme.statusBar();
        statusBar.set("Paste a SELECT and unload rows (+ optional DDL for accurate typing), then click Generate", Theme.TEXT_DIM);
        add(statusBar.panel, BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JLabel title = new JLabel("Unload to Insert");
        title.setFont(Theme.FONT_UI_BOLD);
        title.setForeground(ACCENT_PINK);
        title.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));

        JButton genBtn   = Theme.button("▶  Generate");
        JButton copyBtn  = Theme.button("⎘  Copy Output");
        JButton clearBtn = Theme.ghostButton("✕  Clear All");

        genBtn.addActionListener(e -> generate());
        copyBtn.addActionListener(e -> copyOutput());
        clearBtn.addActionListener(e -> clearAll());

        // Delimiter
        JLabel delimLbl = new JLabel("Delimiter:");
        delimLbl.setFont(Theme.FONT_LABEL);
        delimLbl.setForeground(Theme.TEXT_SECONDARY);

        delimField = Theme.textField(3);
        delimField.setText("|");
        delimField.setPreferredSize(new Dimension(42, 28));
        delimField.setEnabled(false);
        delimField.setFont(Theme.FONT_MONO_MD);

        delimCustomCb = new JCheckBox("Custom");
        delimCustomCb.setOpaque(false);
        Theme.styleCheckbox(delimCustomCb);
        delimCustomCb.addActionListener(e -> delimField.setEnabled(delimCustomCb.isSelected()));

        JPanel left = Theme.toolRow(title, Theme.vDivider(), genBtn, copyBtn, clearBtn,
                Theme.vDivider(), delimLbl, delimField, delimCustomCb);
        left.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JButton howToUseBtn = Theme.helpButton("How to Use");
        howToUseBtn.addActionListener(e -> launcher.navigateTo(Launcher.UNLOAD_HELP));

        return Theme.toolbar(left, howToUseBtn);
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(5, 0, 5, 0);
        g.weightx = 1;

        selectArea.setRows(7); ddlArea.setRows(7); unloadArea.setRows(7); outputArea.setRows(12);
        outputArea.setEditable(false);

        g.gridy = 0; g.weighty = 0.28;
        p.add(buildTopRow(), g);

        g.gridy = 1; g.weighty = 0.22;
        p.add(makeSection("Unload Rows — One per Line", unloadArea, ACCENT_BLUE), g);

        g.gridy = 2; g.weighty = 0.5;
        p.add(buildOutputSection(), g);

        return p;
    }

    private JPanel buildTopRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setBackground(Theme.BG_BASE);
        row.add(makeSection("Select Statement", selectArea, Theme.ACCENT));
        row.add(buildDdlSection());
        return row;
    }

    /** Read-only generated output — styled as a log/output panel per the shared theme. */
    private JPanel buildOutputSection() {
        JPanel panel = Theme.logPanel("Generated Insert Statements", ACCENT_PINK, outputArea, () -> outputArea.setText(""));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return panel;
    }

    private JPanel makeSection(String title, JTextArea ta, Color accent) {
        JPanel header = Theme.sectionHeader(title, accent);
        JScrollPane scroll = new JScrollPane(ta);
        Theme.styleScrollPane(scroll, accent);
        return wrapSection(header, scroll, accent);
    }

    private JPanel buildDdlSection() {
        JPanel header = Theme.sectionHeader("Table DDL (optional — enables accurate typing)", Theme.WARN);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        controls.setOpaque(false);

        ddlComboBox = Theme.comboBox(new String[]{DDL_PLACEHOLDER});
        ddlComboBox.setPreferredSize(new Dimension(150, 24));
        ddlComboBox.addActionListener(e -> { if (!suppressComboEvents) loadSelectedDdl(); });

        JButton saveBtn   = Theme.ghostButton("⬇  Save");
        JButton updateBtn = Theme.ghostButton("↻  Update");
        JButton deleteBtn = Theme.ghostButton("✕  Delete");

        saveBtn.addActionListener(e -> saveDdlAs());
        updateBtn.addActionListener(e -> updateSelectedDdl());
        deleteBtn.addActionListener(e -> deleteSelectedDdl());

        controls.add(ddlComboBox);
        controls.add(saveBtn);
        controls.add(updateBtn);
        controls.add(deleteBtn);
        header.add(controls, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(ddlArea);
        Theme.styleScrollPane(scroll, Theme.WARN);

        refreshDdlComboBox(null);
        return wrapSection(header, scroll, Theme.WARN);
    }

    /** Wraps a header + scroll pane in the shared rounded, soft-accented section chrome. */
    private JPanel wrapSection(JPanel header, JScrollPane scroll, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

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

        Map<String, ColumnType> ddlTypes = parseDdlColumnTypes(ddlArea.getText().trim());
        int typedCols = 0;
        for (String c : columns) if (ddlTypes.containsKey(c.toUpperCase())) typedCols++;

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
            sb.append(buildInsert(table, columns, vals, ddlTypes)).append("\n"); count++;
        }
        outputArea.setText(sb.toString().trim());
        String typingNote = ddlTypes.isEmpty() ? "no DDL — auto-detected types"
                : typedCols+"/"+columns.size()+" columns typed from DDL";
        status("✓  "+count+" INSERT(s) generated  —  table: "+table+"  —  columns: "+columns.size()+"  —  "+typingNote, Theme.SUCCESS);
    }

    // ── Saved DDL management ─────────────────────────────────────────────────
    /** Directory containing the running jar (or classes dir, when run from an IDE). */
    private File getAppDir() {
        try {
            File loc = new File(UnloadToInsertPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return loc.isFile() ? loc.getParentFile() : loc;
        } catch (URISyntaxException | NullPointerException ex) {
            return new File(".");
        }
    }

    private File getDdlStoreDir() {
        File dir = new File(getAppDir(), DDL_STORE_DIRNAME);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File ddlFile(String name) {
        String safe = name.trim().replaceAll("[^a-zA-Z0-9_.-]", "_");
        return new File(getDdlStoreDir(), safe + ".ddl");
    }

    private List<String> listSavedDdlNames() {
        File[] files = getDdlStoreDir().listFiles((d, n) -> n.toLowerCase().endsWith(".ddl"));
        List<String> names = new ArrayList<>();
        if (files != null) for (File f : files) names.add(f.getName().replaceAll("(?i)\\.ddl$", ""));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private void refreshDdlComboBox(String selectName) {
        suppressComboEvents = true;
        ddlComboBox.removeAllItems();
        ddlComboBox.addItem(DDL_PLACEHOLDER);
        for (String n : listSavedDdlNames()) ddlComboBox.addItem(n);
        ddlComboBox.setSelectedItem(selectName != null ? selectName : DDL_PLACEHOLDER);
        suppressComboEvents = false;
    }

    private String detectDdlTableName(String ddl) {
        Matcher m = Pattern.compile("(?i)CREATE\\s+TABLE\\s+([\\w.\"]+)").matcher(ddl);
        return m.find() ? m.group(1).replaceAll("\"", "") : "";
    }

    private void saveDdlAs() {
        String ddl = ddlArea.getText().trim();
        if (ddl.isEmpty()) { status("✗  nothing to save — paste a DDL first.", Theme.ERROR); return; }

        String suggested = detectDdlTableName(ddl);
        String name = JOptionPane.showInputDialog(this, "Save DDL as:", suggested.isEmpty() ? "my_table" : suggested);
        if (name == null || name.trim().isEmpty()) return;
        name = name.trim();

        File f = ddlFile(name);
        if (f.exists()) {
            int c = JOptionPane.showConfirmDialog(this, "\""+name+"\" already exists. Overwrite?",
                    "Confirm overwrite", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
        }
        try {
            Files.write(f.toPath(), ddl.getBytes(StandardCharsets.UTF_8));
            refreshDdlComboBox(name);
            status("✓  DDL saved as \""+name+"\".", Theme.SUCCESS);
        } catch (IOException ex) {
            status("✗  could not save DDL: "+ex.getMessage(), Theme.ERROR);
        }
    }

    private void updateSelectedDdl() {
        Object sel = ddlComboBox.getSelectedItem();
        if (sel == null || ddlComboBox.getSelectedIndex() <= 0) {
            status("✗  select a saved DDL from the dropdown first.", Theme.ERROR); return;
        }
        String ddl = ddlArea.getText().trim();
        if (ddl.isEmpty()) { status("✗  nothing to save — paste a DDL first.", Theme.ERROR); return; }
        try {
            Files.write(ddlFile((String) sel).toPath(), ddl.getBytes(StandardCharsets.UTF_8));
            status("✓  DDL \""+sel+"\" updated.", Theme.SUCCESS);
        } catch (IOException ex) {
            status("✗  could not update DDL: "+ex.getMessage(), Theme.ERROR);
        }
    }

    private void deleteSelectedDdl() {
        Object sel = ddlComboBox.getSelectedItem();
        if (sel == null || ddlComboBox.getSelectedIndex() <= 0) {
            status("✗  select a saved DDL from the dropdown first.", Theme.ERROR); return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Delete saved DDL \""+sel+"\"?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        File f = ddlFile((String) sel);
        if (f.exists() && f.delete()) {
            refreshDdlComboBox(null);
            ddlArea.setText("");
            status("✓  DDL \""+sel+"\" deleted.", Theme.SUCCESS);
        } else {
            status("✗  could not delete DDL file.", Theme.ERROR);
        }
    }

    private void loadSelectedDdl() {
        Object sel = ddlComboBox.getSelectedItem();
        if (sel == null || ddlComboBox.getSelectedIndex() <= 0) return;
        try {
            byte[] bytes = Files.readAllBytes(ddlFile((String) sel).toPath());
            ddlArea.setText(new String(bytes, StandardCharsets.UTF_8));
            status("✓  loaded DDL \""+sel+"\".", Theme.SUCCESS);
        } catch (IOException ex) {
            status("✗  could not load DDL: "+ex.getMessage(), Theme.ERROR);
        }
    }

    // ── DDL-aware typing ──────────────────────────────────────────────────────
    private static class ColumnType {
        String category;   // STRING, NUMBER, DATE, DATETIME, UNKNOWN
        String qualifier;   // DATETIME qualifier text, e.g. "YEAR TO SECOND" / "HOUR TO SECOND"
    }

    /** Parses a CREATE TABLE DDL into a map of UPPERCASE column name -> ColumnType. */
    private Map<String, ColumnType> parseDdlColumnTypes(String ddl) {
        Map<String, ColumnType> map = new LinkedHashMap<>();
        if (ddl == null || ddl.isEmpty()) return map;

        String block = extractParenBlock(ddl);
        if (block.isEmpty()) return map;

        for (String defRaw : splitTopLevelCommas(block)) {
            String def = defRaw.replaceAll("--.*", "").trim();
            if (def.isEmpty()) continue;

            String upperDef = def.toUpperCase();
            if (upperDef.startsWith("PRIMARY") || upperDef.startsWith("FOREIGN")
                    || upperDef.startsWith("CONSTRAINT") || upperDef.startsWith("UNIQUE")
                    || upperDef.startsWith("CHECK")   || upperDef.startsWith("INDEX")
                    || upperDef.startsWith("KEY"))    continue;

            String[] tokens = def.split("\\s+");
            if (tokens.length < 2) continue;

            String colName  = tokens[0].replaceAll("[\"'`]", "").toUpperCase();
            String baseType = tokens[1].replaceAll("\\(.*", "").toUpperCase();

            ColumnType ct = new ColumnType();
            if (baseType.contains("CHAR") || baseType.equals("TEXT") || baseType.contains("VARCHAR")) {
                ct.category = "STRING";
            } else if (baseType.equals("DATETIME")) {
                ct.category = "DATETIME";
                StringBuilder qual = new StringBuilder();
                for (int i = 2; i < tokens.length; i++) {
                    String t = tokens[i].toUpperCase().replaceAll("[,;]", "");
                    if (t.isEmpty()) continue;
                    if (t.equals("NOT") || t.equals("NULL") || t.equals("DEFAULT")
                            || t.equals("PRIMARY") || t.equals("UNIQUE")) break;
                    qual.append(qual.length() > 0 ? " " : "").append(t);
                }
                ct.qualifier = qual.length() > 0 ? qual.toString() : null;
            } else if (baseType.equals("DATE")) {
                ct.category = "DATE";
            } else if (baseType.matches("INT(EGER)?8?|SMALLINT|BIGINT|SERIAL8?|DECIMAL|NUMERIC|FLOAT|DOUBLE|MONEY|REAL")) {
                ct.category = "NUMBER";
            } else {
                ct.category = "UNKNOWN";
            }
            map.put(colName, ct);
        }
        return map;
    }

    /** Returns the text inside the first top-level parenthesis block, e.g. the column list of a CREATE TABLE. */
    private String extractParenBlock(String ddl) {
        int start = ddl.indexOf('(');
        if (start < 0) return "";
        int depth = 0;
        for (int i = start; i < ddl.length(); i++) {
            char c = ddl.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { depth--; if (depth == 0) return ddl.substring(start+1, i); }
        }
        return ddl.substring(start+1);
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

    private String buildInsert(String table, List<String> cols, String[] vals, Map<String, ColumnType> ddlTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append("\n    (");
        for (int i=0;i<cols.size();i++) { if(i>0) sb.append(", "); sb.append(cols.get(i)); }
        sb.append(")\nVALUES\n    (");
        for (int i=0;i<vals.length;i++) {
            if(i>0) sb.append(", ");
            ColumnType ct = ddlTypes.get(cols.get(i).toUpperCase());
            sb.append(formatValue(vals[i], ct));
        }
        sb.append(");"); return sb.toString();
    }

    /** Formats a value using its known DDL column type when available, otherwise falls back to shape-based auto-detection. */
    private String formatValue(String v, ColumnType type) {
        if (v==null||v.trim().isEmpty()) return "NULL"; v=v.trim();
        if (type == null || type.category == null || type.category.equals("UNKNOWN")) return formatValue(v);

        switch (type.category) {
            case "STRING":
                return "'"+v.replace("'","''")+"'";
            case "NUMBER":
                return v.matches("-?\\d+(\\.\\d+)?") ? v : "'"+v.replace("'","''")+"'";
            case "DATE":
                if (v.matches("\\d{4}-\\d{2}-\\d{2}")) return "TO_DATE('"+v+"', '%Y-%m-%d')";
                if (v.matches("\\d{2}/\\d{2}/\\d{4}")) return "TO_DATE('"+v+"', '%m/%d/%Y')";
                return formatValue(v);
            case "DATETIME": {
                if (v.matches("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}")) {
                    String qual = type.qualifier != null ? type.qualifier : "YEAR TO SECOND";
                    return "DATETIME("+v.replace('T',' ')+") "+qual;
                }
                if (v.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}")) {
                    return "TO_DATETIME('"+v+"', '%m/%d/%Y %H:%M:%S')";
                }
                if (v.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    String qual = type.qualifier != null ? type.qualifier : "HOUR TO SECOND";
                    return "DATETIME("+v+") "+qual;
                }
                return formatValue(v);
            }
            default:
                return formatValue(v);
        }
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

    private void clearAll() {
        selectArea.setText(""); ddlArea.setText(""); unloadArea.setText(""); outputArea.setText("");
        suppressComboEvents = true; ddlComboBox.setSelectedItem(DDL_PLACEHOLDER); suppressComboEvents = false;
        status(" ", Theme.TEXT_DIM);
    }
    private void status(String msg, Color c) { statusBar.set(msg, c); }

    // ══════════════════════════════════════════════════════════════════════════
    // HELP PAGE
    // ══════════════════════════════════════════════════════════════════════════

    public JPanel createHelpPanel() {
        String html = ""
                + "<h2>Unload to Insert</h2>"
                + "<p>Converts raw UNLOAD/export rows into ready-to-run <code>INSERT</code> statements, "
                + "using a SELECT statement to know the column order and (optionally) a table DDL to help "
                + "with type-aware value formatting.</p>"
                + "<h3>Steps</h3>"
                + "<ol>"
                + "<li>Paste your <b>Select Statement</b> — its column list defines the column order used "
                + "for every generated <code>INSERT</code>.</li>"
                + "<li>Paste your <b>Unload Rows</b> — one row per line, fields separated by the delimiter "
                + "(<code>|</code> by default).</li>"
                + "<li>Optionally paste a <b>Table DDL</b> — this lets values be formatted more precisely "
                + "(dates, numbers, etc. instead of plain quoted strings).</li>"
                + "<li>Click <b>Generate</b> to build the <code>INSERT</code> statements in the output panel.</li>"
                + "</ol>"
                + "<h3>Delimiter</h3>"
                + "<p>Defaults to <code>|</code>. Tick <b>Custom</b> to type a different delimiter for your "
                + "unload rows.</p>"
                + "<h3>Saving a DDL for reuse</h3>"
                + "<p>Paste a DDL, then use <b>Save</b> to store it under a name for later reuse from the "
                + "dropdown. <b>Update</b> overwrites the currently selected saved DDL with what's in the box; "
                + "<b>Delete</b> removes it.</p>"
                + "<h3>Output</h3>"
                + "<p><b>Copy Output</b> copies the generated statements to your clipboard. <b>Clear All</b> "
                + "resets every field, including the DDL selection.</p>";
        return Theme.helpPage("Unload to Insert — How to Use", ACCENT_PINK, html,
                () -> launcher.navigateTo(Launcher.UNLOAD));
    }
}
