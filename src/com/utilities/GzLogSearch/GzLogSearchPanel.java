package com.utilities.GzLogSearch;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class GzLogSearchPanel extends JPanel {

    private final Launcher launcher;

    // ── Accent for this panel ─────────────────────────────────────────────────
    private static final Color PANEL_ACCENT = new Color(0xFF8A5C);   // warm amber

    // ── State ─────────────────────────────────────────────────────────────────
    private JTextField  sourceDirField;
    private JTextField  outputDirField;
    private JTextField  searchField;
    private JTextArea   logArea;
    private JButton     runBtn;
    private JButton     showInExplorerBtn;
    private Theme.StatusBar statusBar;

    private File lastOutputDir = null;

    public GzLogSearchPanel(Launcher launcher) {
        this.launcher = launcher;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_BASE);
        buildUI();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI CONSTRUCTION
    // ══════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        add(buildToolbar(),    BorderLayout.NORTH);
        add(buildMainSplit(),  BorderLayout.CENTER);
        statusBar = Theme.statusBar();
        add(statusBar.panel, BorderLayout.SOUTH);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JLabel title = new JLabel("GZ Log Search");
        title.setFont(Theme.FONT_UI_BOLD);
        title.setForeground(PANEL_ACCENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));

        runBtn = Theme.button("▶  Run Search");
        runBtn.addActionListener(e -> startSearch());

        showInExplorerBtn = Theme.ghostButton("Show in Explorer");
        showInExplorerBtn.setEnabled(false);
        showInExplorerBtn.addActionListener(e -> showInExplorer());

        JPanel left = Theme.toolRow(title, Theme.vDivider(), runBtn, showInExplorerBtn);
        left.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JButton howToUseBtn = Theme.helpButton("How to Use");
        howToUseBtn.addActionListener(e -> launcher.navigateTo(Launcher.GZSEARCH_HELP));

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
                "Source directory",
                "Directory containing .gz files — scanned recursively, subfolders included",
                PANEL_ACCENT,
                field -> sourceDirField = field,
                field -> browseDirectory(field)
        ));
        p.add(Box.createVerticalStrut(14));

        p.add(Theme.pathCard(
                "Output directory  ·  required",
                "Directory where matching .gz files will be copied",
                Theme.WARN,
                field -> outputDirField = field,
                field -> browseDirectory(field)
        ));
        p.add(Box.createVerticalStrut(14));

        p.add(buildSearchCard());

        return p;
    }

    private void browseDirectory(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Theme.setPathValue(field, chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // ── Search string card ────────────────────────────────────────────────────
    private JPanel buildSearchCard() {
        searchField = Theme.textField(20);
        searchField.setToolTipText("Press Enter or click Run Search");
        searchField.addActionListener(e -> startSearch());

        return Theme.formCard(
                "Search string",
                "Any .gz file containing a line with this text gets copied to output",
                PANEL_ACCENT,
                searchField
        );
    }

    // ── Right: log panel ──────────────────────────────────────────────────────
    private JPanel buildLogPanel() {
        logArea = Theme.textArea();
        logArea.setEditable(false);
        logArea.setText("Ready — configure directories and search string, then click Run Search.\n");
        return Theme.logPanel("Scan Log", PANEL_ACCENT, logArea, () -> logArea.setText(""));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SEARCH LOGIC
    // ══════════════════════════════════════════════════════════════════════════

    private void startSearch() {
        String sourceDir = sourceDirField.getText().trim();
        String outputDir = outputDirField.getText().trim();
        String searchText = searchField.getText();

        if (sourceDir.isEmpty() || sourceDir.equals("(not selected)")) {
            showWarn("Please select a source directory.", "No Source"); return;
        }
        if (outputDir.isEmpty() || outputDir.equals("(not selected)")) {
            showWarn("Output directory is required.\nPlease browse and select an output directory.", "Output Path Required"); return;
        }
        if (searchText.isEmpty()) {
            showWarn("Please enter a search string.", "No Search String"); return;
        }

        File srcFile = new File(sourceDir);
        if (!srcFile.exists()) { showWarn("Source directory does not exist:\n" + sourceDir, "Invalid Path"); return; }

        runBtn.setEnabled(false);
        runBtn.setText("  Scanning…");
        statusBar.set("Scanning…", Theme.TEXT_SECONDARY);
        showInExplorerBtn.setEnabled(false);
        lastOutputDir = null;
        logArea.setText("");

        final File finalOutDir = new File(outputDir);

        SwingWorker<Void, String> worker = new SwingWorker<>() {

            int totalFiles = 0;
            int matchedFiles = 0;

            @Override
            protected Void doInBackground() throws Exception {

                Files.createDirectories(finalOutDir.toPath());

                publish("Scanning directory (recursive)...\n");

                try (Stream<Path> paths = Files.walk(Paths.get(sourceDir))) {

                    List<Path> gzFiles = paths
                            .filter(Files::isRegularFile)
                            .filter(p -> p.toString().toLowerCase().endsWith(".gz"))
                            .toList();

                    totalFiles = gzFiles.size();
                    publish("Found " + totalFiles + " .gz file(s)\n");

                    int current = 0;

                    for (Path gzFile : gzFiles) {
                        current++;
                        publish("[" + current + "/" + totalFiles + "] Checking: "
                                + gzFile.getFileName() + "\n");

                        try {
                            if (containsStringInGz(gzFile, searchText)) {
                                matchedFiles++;

                                Path target = Paths.get(outputDir, gzFile.getFileName().toString());
                                Files.copy(gzFile, target, StandardCopyOption.REPLACE_EXISTING);

                                publish("  MATCH -> Copied: " + gzFile.getFileName() + "\n");
                            }
                        } catch (Exception ex) {
                            publish("  ERROR: " + gzFile + "\n");
                            publish("  " + ex.getMessage() + "\n");
                        }
                    }
                }

                publish("\nFinished. Matched files: " + matchedFiles + " / " + totalFiles + "\n");
                publish("Output saved to: " + finalOutDir.getAbsolutePath() + "\n");

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) appendLog(msg);
            }

            @Override
            protected void done() {
                try {
                    get();
                    lastOutputDir = finalOutDir;
                    showInExplorerBtn.setEnabled(true);
                    statusBar.set("✓  Done — " + matchedFiles + " match(es) copied to: " + finalOutDir.getAbsolutePath(), Theme.SUCCESS);
                } catch (Exception ex) {
                    appendLog("ERROR: " + ex.getMessage() + "\n");
                    statusBar.set("✗  Error — check log", Theme.ERROR);
                } finally {
                    runBtn.setEnabled(true);
                    runBtn.setText("▶  Run Search");
                }
            }
        };

        worker.execute();
    }

    private boolean containsStringInGz(Path gzFile, String searchText) throws Exception {
        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new GZIPInputStream(
                                             Files.newInputStream(gzFile))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(searchText)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void showInExplorer() {
        if (lastOutputDir == null) return;
        try {
            Desktop.getDesktop().open(lastOutputDir);
        } catch (Exception ex) {
            try { new ProcessBuilder("explorer", lastOutputDir.getAbsolutePath()).start(); }
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
                + "<h2>GZ Log Search</h2>"
                + "<p>Recursively scans a directory of <code>.gz</code> log files and copies any file that "
                + "contains a matching line to an output folder.</p>"
                + "<h3>Steps</h3>"
                + "<ol>"
                + "<li>Browse to a <b>Source directory</b> — every <code>.gz</code> file in it and its "
                + "subfolders will be checked.</li>"
                + "<li>Browse to (or create) an <b>Output directory</b> — this is required.</li>"
                + "<li>Enter a <b>Search string</b>. Any <code>.gz</code> file containing at least one line "
                + "with that text is copied to the output directory.</li>"
                + "<li>Click <b>Run Search</b> (or press Enter in the search box) and watch progress in the "
                + "Scan Log panel.</li>"
                + "</ol>"
                + "<h3>After it finishes</h3>"
                + "<p>Use <b>Show in Explorer</b> to open the output folder and review the copied files. "
                + "Matching is a simple case-sensitive substring search performed line-by-line inside each "
                + "gzip file — nothing is modified, files are only copied.</p>";
        return Theme.helpPage("GZ Log Search — How to Use", PANEL_ACCENT, html,
                () -> launcher.navigateTo(Launcher.GZSEARCH));
    }
}
