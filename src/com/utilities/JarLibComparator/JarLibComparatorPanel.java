package com.utilities.JarLibComparator;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

public class JarLibComparatorPanel extends JPanel {

    private final Launcher launcher;

    // ── Accent for this panel ─────────────────────────────────────────────────
    private static final Color PANEL_ACCENT = new Color(0x9B8CFF);   // soft violet

    // ── State ─────────────────────────────────────────────────────────────────
    private JTextField oldLibField;
    private JTextField newLibField;
    private JTextField outputField;
    private JTextField sheetNameField;
    private JTextArea  logArea;
    private JButton    runBtn;
    private JButton    openReportBtn;
    private JButton    showInExplorerBtn;
    private Theme.StatusBar statusBar;

    private File lastOutputFile = null;

    public JarLibComparatorPanel(Launcher launcher) {
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
        add(buildMainSplit(), BorderLayout.CENTER);
        statusBar = Theme.statusBar();
        add(statusBar.panel, BorderLayout.SOUTH);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JLabel title = new JLabel("Jar Lib Comparator");
        title.setFont(Theme.FONT_UI_BOLD);
        title.setForeground(PANEL_ACCENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));

        runBtn = Theme.button("▶  Run Comparison");
        runBtn.addActionListener(e -> startCompare());

        openReportBtn = Theme.ghostButton("Open Report");
        openReportBtn.setEnabled(false);
        openReportBtn.addActionListener(e -> openReport());

        showInExplorerBtn = Theme.ghostButton("Show in Explorer");
        showInExplorerBtn.setEnabled(false);
        showInExplorerBtn.addActionListener(e -> showInExplorer());

        JPanel left = Theme.toolRow(title, Theme.vDivider(), runBtn, openReportBtn, showInExplorerBtn);
        left.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JButton howToUseBtn = Theme.helpButton("How to Use");
        howToUseBtn.addActionListener(e -> launcher.navigateTo(Launcher.JARCOMPARE_HELP));

        return Theme.toolbar(left, howToUseBtn);
    }

    // ── Main split: left config | right log ───────────────────────────────────
    private JSplitPane buildMainSplit() {
        return Theme.split(buildConfigPanel(), buildLogPanel(), 480, 0.45);
    }

    // ── Left: config panel ────────────────────────────────────────────────────
    private JPanel buildConfigPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 10));

        p.add(Theme.pathCard(
                "Old / prod lib path  ·  required",
                "Directory containing the current jar files",
                PANEL_ACCENT,
                field -> oldLibField = field,
                this::browseDirectory
        ));
        p.add(Box.createVerticalStrut(14));

        p.add(Theme.pathCard(
                "New lib path  ·  required",
                "Directory containing the candidate/new jar files",
                Theme.WARN,
                field -> newLibField = field,
                this::browseDirectory
        ));
        p.add(Box.createVerticalStrut(14));

        p.add(Theme.pathCard(
                "Output report (.xlsx)  ·  required",
                "Where the comparison workbook will be written",
                PANEL_ACCENT,
                field -> outputField = field,
                this::browseOutput
        ));
        p.add(Box.createVerticalStrut(14));

        p.add(buildSheetCard());

        return p;
    }

    private void browseDirectory(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Theme.setPathValue(field, chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseOutput(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Choose output workbook");
        chooser.setSelectedFile(new File("Jar_Comparison_Report.xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".xlsx")) {
                f = new File(f.getParentFile(), f.getName() + ".xlsx");
            }
            Theme.setPathValue(field, f.getAbsolutePath());
        }
    }

    // ── Sheet name card ───────────────────────────────────────────────────────
    private JPanel buildSheetCard() {
        sheetNameField = Theme.textField(20);
        sheetNameField.setText("Comparison");
        sheetNameField.setToolTipText("Press Enter or click Run Comparison");
        sheetNameField.addActionListener(e -> startCompare());

        return Theme.formCard(
                "Sheet name",
                "Name of the worksheet tab inside the generated workbook",
                PANEL_ACCENT,
                sheetNameField
        );
    }

    // ── Right: log panel ──────────────────────────────────────────────────────
    private JPanel buildLogPanel() {
        logArea = Theme.textArea();
        logArea.setEditable(false);
        logArea.setText("Ready — select old-lib, new-lib and output path, then click Run Comparison.\n");
        return Theme.logPanel("Comparison Log", PANEL_ACCENT, logArea, () -> logArea.setText(""));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMPARISON LOGIC
    // ══════════════════════════════════════════════════════════════════════════

    private void startCompare() {
        String oldLib = oldLibField.getText().trim();
        String newLib = newLibField.getText().trim();
        String output = outputField.getText().trim();
        String sheetName = sheetNameField.getText().trim();

        if (oldLib.isEmpty() || oldLib.equals("(not selected)")) {
            showWarn("Please select the old/prod lib directory.", "No Old Lib Path"); return;
        }
        if (newLib.isEmpty() || newLib.equals("(not selected)")) {
            showWarn("Please select the new lib directory.", "No New Lib Path"); return;
        }
        if (output.isEmpty() || output.equals("(not selected)")) {
            showWarn("Please choose an output .xlsx path.", "No Output Path"); return;
        }
        if (sheetName.isEmpty()) {
            sheetName = "Comparison";
        }

        File oldDir = new File(oldLib);
        if (!oldDir.isDirectory()) { showWarn("Old lib directory does not exist:\n" + oldLib, "Invalid Path"); return; }
        File newDir = new File(newLib);
        if (!newDir.isDirectory()) { showWarn("New lib directory does not exist:\n" + newLib, "Invalid Path"); return; }

        runBtn.setEnabled(false);
        runBtn.setText("  Comparing…");
        statusBar.set("Comparing…", Theme.TEXT_SECONDARY);
        openReportBtn.setEnabled(false);
        showInExplorerBtn.setEnabled(false);
        lastOutputFile = null;
        logArea.setText("");

        final String finalSheetName = sheetName;
        final File outputFile = new File(output);

        SwingWorker<JarLibComparator.Result, String> worker = new SwingWorker<>() {

            @Override
            protected JarLibComparator.Result doInBackground() throws Exception {
                File parent = outputFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                return JarLibComparator.run(oldLib, newLib, output, finalSheetName, msg -> publish(msg));
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) appendLog(msg + "\n");
            }

            @Override
            protected void done() {
                try {
                    JarLibComparator.Result result = get();
                    lastOutputFile = outputFile;

                    appendLog("\n");
                    appendLog("Old jars: " + result.oldCount() + "   New jars: " + result.newCount() + "\n");
                    for (Map.Entry<String, Integer> e : result.statusCounts().entrySet()) {
                        appendLog("  " + e.getKey() + ": " + e.getValue() + "\n");
                    }
                    appendLog("Report saved to: " + outputFile.getAbsolutePath() + "\n");

                    openReportBtn.setEnabled(true);
                    showInExplorerBtn.setEnabled(true);
                    statusBar.set("✓  Done — report saved to: " + outputFile.getAbsolutePath(), Theme.SUCCESS);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    appendLog("ERROR: " + cause.getMessage() + "\n");
                    statusBar.set("✗  Error — check log", Theme.ERROR);
                } finally {
                    runBtn.setEnabled(true);
                    runBtn.setText("▶  Run Comparison");
                }
            }
        };

        worker.execute();
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void openReport() {
        if (lastOutputFile == null) return;
        try {
            Desktop.getDesktop().open(lastOutputFile);
        } catch (Exception ex) {
            showWarn("Cannot open report:\n" + ex.getMessage(), "Error");
        }
    }

    private void showInExplorer() {
        if (lastOutputFile == null) return;
        File dir = lastOutputFile.getParentFile();
        if (dir == null) return;
        try {
            Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            try { new ProcessBuilder("explorer", dir.getAbsolutePath()).start(); }
            catch (Exception ignored) { showWarn("Cannot open explorer.", "Error"); }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void appendLog(String text) {
        logArea.append(text);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showWarn(String msg, String title) {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.WARNING_MESSAGE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELP PAGE
    // ══════════════════════════════════════════════════════════════════════════

    public JPanel createHelpPanel() {
        String html = ""
                + "<h2>Jar Lib Comparator</h2>"
                + "<p>Compares two directories of jar files — typically an old/prod library folder and a "
                + "new candidate — and writes a color-coded Excel report classifying every jar.</p>"
                + "<h3>Steps</h3>"
                + "<ol>"
                + "<li>Browse to the <b>Old / prod lib path</b> directory.</li>"
                + "<li>Browse to the <b>New lib path</b> directory.</li>"
                + "<li>Choose an <b>Output report (.xlsx)</b> path — where the workbook will be written.</li>"
                + "<li>Optionally change the <b>Sheet name</b> (defaults to \"Comparison\").</li>"
                + "<li>Click <b>Run Comparison</b>.</li>"
                + "</ol>"
                + "<h3>How jars are classified</h3>"
                + "<p>Each jar's SHA-256 hash and size decide its status:</p>"
                + "<ul>"
                + "<li><b>Same</b> — identical name and content.</li>"
                + "<li><b>Different (name same, content changed)</b> — same file name, different content.</li>"
                + "<li><b>Only name changed (SHA/size same)</b> — content matches a jar under a different name.</li>"
                + "<li><b>New added in new-lib</b> — no matching jar in the old library.</li>"
                + "<li><b>Removed in new-lib</b> — present in the old library only.</li>"
                + "</ul>"
                + "<p>The generated workbook color-codes each row by status, with an auto-filter and frozen "
                + "header row. Use <b>Open Report</b> to launch the workbook, or <b>Show in Explorer</b> to "
                + "reveal it in its folder.</p>";
        return Theme.helpPage("Jar Lib Comparator — How to Use", PANEL_ACCENT, html,
                () -> launcher.navigateTo(Launcher.JARCOMPARE));
    }
}
