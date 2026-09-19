package com.utilities.LogStatementFilter;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class LogStatementFilterPanel extends JPanel {

    private final Launcher launcher;

    // ── Accent for this panel ─────────────────────────────────────────────────
    private static final Color PANEL_ACCENT = new Color(0x00CCFF);   // cyan

    // ── State ─────────────────────────────────────────────────────────────────
    private JTextField fileField;
    private JTextField keywordField;
    private JTextArea  logArea;
    private JCheckBox  caseCheckBox;
    private JButton    filterBtn;
    private JLabel     fileMetaLabel;
    private Theme.StatusBar statusBar;

    private final Map<String, Integer> keywordMatchCount = new LinkedHashMap<>();

    public LogStatementFilterPanel(Launcher launcher) {
        this.launcher = launcher;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_BASE);
        buildUI();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI CONSTRUCTION
    // ══════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        add(buildToolbar(),        BorderLayout.NORTH);
        add(buildContentPanel(),   BorderLayout.CENTER);
        statusBar = Theme.statusBar();
        add(statusBar.panel, BorderLayout.SOUTH);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JLabel title = new JLabel("Log Statement Filter");
        title.setFont(Theme.FONT_UI_BOLD);
        title.setForeground(PANEL_ACCENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));

        filterBtn = Theme.button("▶  Run Filter");
        filterBtn.addActionListener(this::processFile);

        caseCheckBox = new JCheckBox("Case Sensitive");
        caseCheckBox.setOpaque(false);
        Theme.styleCheckbox(caseCheckBox);

        JPanel left = Theme.toolRow(title, Theme.vDivider(), filterBtn, caseCheckBox);
        left.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JButton howToUseBtn = Theme.helpButton("How to Use");
        howToUseBtn.addActionListener(e -> launcher.navigateTo(Launcher.LOG_HELP));

        return Theme.toolbar(left, howToUseBtn);
    }

    // ── Content: file/keyword config + output ─────────────────────────────────
    private JPanel buildContentPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        top.add(Theme.pathCard(
                "Log file  ·  required",
                "The .log file to scan for matching statements",
                PANEL_ACCENT,
                field -> fileField = field,
                this::browseFile
        ));

        fileMetaLabel = new JLabel(" ");
        fileMetaLabel.setFont(Theme.FONT_UI_SM);
        fileMetaLabel.setForeground(Theme.TEXT_SECONDARY);
        fileMetaLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
        fileMetaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(fileMetaLabel);
        top.add(Box.createVerticalStrut(14));

        keywordField = Theme.textField(20);
        keywordField.addActionListener(this::processFile);
        top.add(Theme.formCard(
                "Keywords",
                "Comma-separated — e.g. ERROR, timeout, NullPointer",
                PANEL_ACCENT,
                keywordField
        ));

        p.add(top, BorderLayout.NORTH);
        p.add(buildOutputPanel(), BorderLayout.CENTER);
        return p;
    }

    private void browseFile(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Log files (*.log)", "log"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            Theme.setPathValue(field, f.getAbsolutePath());
            fileMetaLabel.setText(f.getName() + "  ·  " + humanSize(f.length()));
            logArea.setForeground(Theme.TEXT_SECONDARY);
            logArea.setText("File loaded.\nEnter keywords and press Run Filter.");
            statusBar.set("File loaded — enter keywords and press Run Filter.", Theme.TEXT_SECONDARY);
        }
    }

    // ── Output / keyword-stats panel ──────────────────────────────────────────
    private JPanel buildOutputPanel() {
        logArea = Theme.textArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setText("Awaiting input…");
        return Theme.logPanel("Output & Keyword Stats", PANEL_ACCENT, logArea, () -> logArea.setText(""));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELP PAGE
    // ══════════════════════════════════════════════════════════════════════════

    public JPanel createHelpPanel() {
        String html = ""
                + "<h2>Log Statement Filter</h2>"
                + "<p>Scans a log file for statements containing any of your keywords, and writes the "
                + "matching statements to a new file next to the original.</p>"
                + "<h3>Keywords</h3>"
                + "<p>Enter keywords separated by commas — no limit, as many as needed.<br>"
                + "Example: <code>ERROR, timeout, NullPointer</code></p>"
                + "<h3>What is a \"statement\"?</h3>"
                + "<p>Lines belonging to one log entry are treated as a single unit. A new statement starts "
                + "when a line begins with <code>[</code> (a timestamp bracket) — for example:</p>"
                + "<p><code>[10:32:01] ERROR db failed<br>&nbsp;&nbsp;at DB.connect():45<br>"
                + "&nbsp;&nbsp;caused by: timeout</code></p>"
                + "<p>If <b>any</b> keyword matches, the <b>entire</b> statement is written to the output.</p>"
                + "<h3>Case Sensitive</h3>"
                + "<p><b>Off</b> (default) — matches any casing.<br><b>On</b> — exact casing only.</p>"
                + "<h3>Output file</h3>"
                + "<p>Saved as <code>name_filtered.log</code> in the same folder as your original log file. "
                + "The output panel also shows a per-keyword hit count once filtering finishes.</p>";
        return Theme.helpPage("Log Statement Filter — How to Use", PANEL_ACCENT, html,
                () -> launcher.navigateTo(Launcher.LOG));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILTER LOGIC
    // ══════════════════════════════════════════════════════════════════════════

    private void processFile(ActionEvent e) {
        String path = fileField.getText();
        if (path.isEmpty() || path.equals("(not selected)")) { warn("Please select a log file first.", "Missing File"); return; }
        List<String> keywords = getKeywords();
        if (keywords.isEmpty()) { warn("Please enter at least one keyword.", "Missing Keywords"); return; }

        boolean cs = caseCheckBox.isSelected();
        keywordMatchCount.clear(); keywords.forEach(k -> keywordMatchCount.put(k, 0));
        filterBtn.setEnabled(false); filterBtn.setText("  Processing…");
        statusBar.set("Filtering…", Theme.TEXT_SECONDARY);
        logArea.setForeground(Theme.TEXT_SECONDARY); logArea.setText("Running filter…\n");

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
                try {
                    get();
                    updateStatus(matched, outputFile);
                    statusBar.set("✓  Done — " + matched + " statement(s) matched.", Theme.SUCCESS);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    logArea.setForeground(Theme.ERROR); logArea.setText("ERROR:\n  " + cause.getMessage());
                    statusBar.set("✗  Error — check output panel", Theme.ERROR);
                }
                finally { filterBtn.setEnabled(true); filterBtn.setText("▶  Run Filter"); }
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
        sb.append("Output file\n  ").append(out.getName()).append("\n\nTotal matched  ").append(total).append("\n");
        sb.append("─".repeat(30)).append("\nKeyword hits\n");
        keywordMatchCount.forEach((k,v) -> sb.append(String.format("  %-22s %d\n",k,v)));
        logArea.setForeground(Theme.SUCCESS); logArea.setText(sb.toString());
    }

    private List<String> getKeywords() {
        List<String> list = new ArrayList<>();
        String raw = keywordField.getText().trim();
        if (raw.isEmpty()) return list;
        for (String s : raw.split(",")) { String t = s.trim(); if (!t.isEmpty()) list.add(t); }
        return list;
    }

    private void warn(String msg, String title) { JOptionPane.showMessageDialog(this, msg, title, JOptionPane.WARNING_MESSAGE); }
    private String humanSize(long b) {
        if (b<1024) return b+" B"; if (b<1024*1024) return String.format("%.1f KB",b/1024.0);
        return String.format("%.1f MB",b/(1024.0*1024));
    }
}
