package com.utilities.FindInJars;

import com.utilities.Launcher;
import com.utilities.Theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.jar.*;

public class FindInJarsPanel extends JPanel {

    private final Launcher launcher;

    // ── Accent for this panel ─────────────────────────────────────────────────
    private static final Color PANEL_ACCENT = new Color(0x00C2D9);   // cyan
    private static final Color TYPE_ACCENT  = new Color(0x9B8CFF);   // violet, for the file-type card

    // The file-type options offered in the UI. "ALL" scans every extension listed here.
    private static final String[] FILE_TYPE_OPTIONS = {
            "ALL  (.class .java .js .xml)",
            ".class",
            ".java",
            ".js",
            ".xml"
    };
    private static final List<String> ALL_EXTENSIONS =
            Arrays.asList(".class", ".java", ".js", ".xml");

    // ── State ─────────────────────────────────────────────────────────────────
    private JTextField  libPathField;
    private JTextField  outputPathField;
    private JTextField  keywordInputField;
    private JComboBox<String> fileTypeCombo;
    private JPanel      tagsPanel;
    private JScrollPane tagsScroll;
    private JTextArea   logArea;
    private JButton     findBtn;
    private JButton     openReportBtn;
    private JButton     showInExplorerBtn;
    private Theme.StatusBar statusBar;

    private final List<String> keywords = new ArrayList<>();
    private File lastOutputDir = null;

    public FindInJarsPanel(Launcher launcher) {
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
        JLabel title = new JLabel("Jar Inspector");
        title.setFont(Theme.FONT_UI_BOLD);
        title.setForeground(PANEL_ACCENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));

        findBtn = Theme.button("▶  Find in Jars");
        findBtn.addActionListener(e -> runSearch());

        openReportBtn = Theme.ghostButton("Open Report");
        openReportBtn.setEnabled(false);
        openReportBtn.addActionListener(e -> openReport());

        showInExplorerBtn = Theme.ghostButton("Show in Explorer");
        showInExplorerBtn.setEnabled(false);
        showInExplorerBtn.addActionListener(e -> showInExplorer());

        JPanel left = Theme.toolRow(title, Theme.vDivider(), findBtn, openReportBtn, showInExplorerBtn);
        left.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JButton howToUseBtn = Theme.helpButton("How to Use");
        howToUseBtn.addActionListener(e -> launcher.navigateTo(Launcher.JFINDER_HELP));

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
                "Libs path",
                "Directory (scanned recursively — JARs & subfolders included) or a single file",
                PANEL_ACCENT,
                field -> libPathField = field,
                this::browseLibPath
        ));
        p.add(Box.createVerticalStrut(14));

        p.add(buildFileTypeCard());
        p.add(Box.createVerticalStrut(14));

        p.add(Theme.pathCard(
                "Output path  ·  required",
                "Directory where search_results.js and viewer.html will be saved",
                Theme.WARN,
                field -> outputPathField = field,
                this::browseOutputDir
        ));
        p.add(Box.createVerticalStrut(14));

        p.add(buildKeywordsCard());

        return p;
    }

    private void browseLibPath(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JAR files, source files, or directories", "jar", "class", "java", "js", "xml"));
        chooser.setAcceptAllFileFilterUsed(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Theme.setPathValue(field, chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseOutputDir(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Theme.setPathValue(field, chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // ── File-type filter card ────────────────────────────────────────────────
    private JPanel buildFileTypeCard() {
        fileTypeCombo = new JComboBox<>(FILE_TYPE_OPTIONS);
        Theme.styleComboBox(fileTypeCombo);
        fileTypeCombo.setSelectedIndex(0);

        return Theme.formCard(
                "File types",
                "Only these extensions are scanned — inside JARs and loose on disk",
                TYPE_ACCENT,
                fileTypeCombo
        );
    }

    // ── Keywords card ─────────────────────────────────────────────────────────
    private JPanel buildKeywordsCard() {
        // Input row
        keywordInputField = Theme.textField(20);
        keywordInputField.setToolTipText("Press Enter to add keyword");
        keywordInputField.addActionListener(e -> addKeyword(keywordInputField.getText().trim()));

        JButton addBtn = Theme.button("+ Add");
        addBtn.addActionListener(e -> addKeyword(keywordInputField.getText().trim()));

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(keywordInputField, BorderLayout.CENTER);
        inputRow.add(addBtn, BorderLayout.EAST);

        // Tags container — wrapping flow layout
        tagsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 6));
        tagsPanel.setBackground(Theme.BG_DEEP);
        tagsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel emptyHint = new JLabel("No keywords yet — add some above");
        emptyHint.setFont(Theme.FONT_HINT);
        emptyHint.setForeground(Theme.TEXT_DIM);
        emptyHint.setName("emptyHint");
        tagsPanel.add(emptyHint);

        tagsScroll = new JScrollPane(tagsPanel);
        tagsScroll.setPreferredSize(new Dimension(0, 100));
        tagsScroll.setMinimumSize(new Dimension(0, 80));
        Theme.styleScrollPane(tagsScroll, PANEL_ACCENT);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(inputRow,   BorderLayout.NORTH);
        content.add(tagsScroll, BorderLayout.CENTER);

        JPanel wrap = Theme.formCard(
                "Search keywords",
                "Type a keyword and press Enter to add — click × to remove",
                PANEL_ACCENT,
                content
        );
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        return wrap;
    }

    // ── Right: log panel ──────────────────────────────────────────────────────
    private JPanel buildLogPanel() {
        logArea = Theme.textArea();
        logArea.setEditable(false);
        logArea.setText("Ready — configure paths, file types and keywords, then click Find in Jars.\n");
        return Theme.logPanel("Scan Log", PANEL_ACCENT, logArea, () -> logArea.setText(""));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // KEYWORD TAG MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    private void addKeyword(String kw) {
        if (kw.isEmpty() || keywords.contains(kw)) {
            keywordInputField.setText("");
            return;
        }
        keywords.add(kw);
        keywordInputField.setText("");
        refreshTags();
    }

    private void removeKeyword(String kw) {
        keywords.remove(kw);
        refreshTags();
    }

    private void refreshTags() {
        tagsPanel.removeAll();
        if (keywords.isEmpty()) {
            JLabel hint = new JLabel("No keywords yet — add some above");
            hint.setFont(Theme.FONT_HINT); hint.setForeground(Theme.TEXT_DIM);
            hint.setName("emptyHint");
            tagsPanel.add(hint);
        } else {
            for (String kw : new ArrayList<>(keywords)) {
                tagsPanel.add(buildTag(kw));
            }
        }
        tagsPanel.revalidate(); tagsPanel.repaint();
    }

    /** A small rounded "chip" showing one keyword with a hover-sensitive × remove control. */
    private JPanel buildTag(String kw) {
        JPanel tag = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CONTROL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
            }
        };
        tag.setOpaque(false);
        tag.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 8));

        JLabel lbl = new JLabel(kw);
        lbl.setFont(Theme.FONT_UI_MD);
        lbl.setForeground(Theme.TEXT_PRIMARY);

        // × remove control
        JLabel removeBtn = new JLabel("×");
        removeBtn.setFont(Theme.FONT_UI_BOLD);
        removeBtn.setForeground(Theme.TEXT_DIM);
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeBtn.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        removeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { removeBtn.setForeground(Theme.ERROR); }
            public void mouseExited (MouseEvent e) { removeBtn.setForeground(Theme.TEXT_DIM); }
            public void mouseClicked(MouseEvent e) { removeKeyword(kw); }
        });

        tag.add(lbl); tag.add(removeBtn);
        return tag;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILE-TYPE FILTER HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Returns the list of lowercase extensions (e.g. ".java") the user wants scanned. */
    private List<String> getSelectedExtensions() {
        String sel = (String) fileTypeCombo.getSelectedItem();
        if (sel == null || sel.startsWith("ALL")) {
            return new ArrayList<>(ALL_EXTENSIONS);
        }
        return Collections.singletonList(sel.trim().toLowerCase());
    }

    private static String describeFilter(List<String> exts) {
        return String.join(" ", exts);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RECURSIVE FILE COLLECTION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Recursively walks {@code node}. Every .jar file found anywhere in the tree is
     * added to {@code jarsOut}. Every other file whose name ends with one of
     * {@code filterExts} is added to {@code plainsOut}. Sub-directories at any depth
     * are entered automatically.
     */
    private void collectTargets(File node, List<String> filterExts,
                                List<File> jarsOut, List<File> plainsOut) {
        if (node.isDirectory()) {
            File[] children = node.listFiles();
            if (children == null) return;
            Arrays.sort(children, Comparator.comparing(File::getName));
            for (File child : children) {
                collectTargets(child, filterExts, jarsOut, plainsOut);
            }
            return;
        }
        String lower = node.getName().toLowerCase();
        if (lower.endsWith(".jar")) {
            jarsOut.add(node);
            return;
        }
        for (String ext : filterExts) {
            if (lower.endsWith(ext)) { plainsOut.add(node); break; }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SEARCH LOGIC
    // ══════════════════════════════════════════════════════════════════════════

    private void runSearch() {
        // Validate
        String libPath = libPathField.getText().trim();
        String outPath = outputPathField.getText().trim();

        if (libPath.isEmpty() || libPath.equals("(not selected)")) {
            showWarn("Please select a libs directory or a .jar file.", "No Source"); return;
        }
        if (outPath.isEmpty() || outPath.equals("(not selected)")) {
            showWarn("Output path is required.\nPlease browse and select an output directory.", "Output Path Required"); return;
        }
        if (keywords.isEmpty()) {
            showWarn("Please add at least one search keyword.", "No Keywords"); return;
        }

        File libFile  = new File(libPath);
        File outDir   = new File(outPath);
        if (!libFile.exists())  { showWarn("Libs path does not exist:\n" + libPath, "Invalid Path"); return; }
        if (!outDir.exists())   { outDir.mkdirs(); }

        findBtn.setEnabled(false);
        findBtn.setText("  Scanning…");
        statusBar.set("Scanning…", Theme.TEXT_SECONDARY);
        openReportBtn.setEnabled(false);
        showInExplorerBtn.setEnabled(false);
        lastOutputDir = null;

        final File finalOutDir = outDir;
        final List<String> kws = new ArrayList<>(keywords);
        final File finalLibFile = libFile;
        final List<String> filterExts = getSelectedExtensions();

        new SwingWorker<String, String>() {
            @Override protected String doInBackground() throws Exception {
                // ── Collect scan targets: jars + loose matching files, recursively ──
                List<File> jars = new ArrayList<>();
                List<File> plainFiles = new ArrayList<>();

                File rootDir = finalLibFile.isDirectory()
                        ? finalLibFile
                        : (finalLibFile.getParentFile() != null ? finalLibFile.getParentFile() : finalLibFile);

                if (finalLibFile.isFile()) {
                    String lower = finalLibFile.getName().toLowerCase();
                    if (lower.endsWith(".jar")) {
                        jars.add(finalLibFile);
                    } else {
                        for (String ext : filterExts) {
                            if (lower.endsWith(ext)) { plainFiles.add(finalLibFile); break; }
                        }
                    }
                } else {
                    collectTargets(finalLibFile, filterExts, jars, plainFiles);
                }

                publish("// File type filter: " + describeFilter(filterExts) + "\n");
                publish("// Found " + jars.size() + " JAR(s) and " + plainFiles.size()
                        + " loose file(s) to scan (recursive)\n");

                // Build JSON
                StringBuilder json = new StringBuilder("[\n");
                for (int i = 0; i < kws.size(); i++) {
                    String kw = kws.get(i);
                    publish("//\n// ── Searching for: " + kw + "\n");
                    String slashedKeyword = kw.replace('.', '/');

                    Map<String, List<Map<String, Object>>> jarResults = new LinkedHashMap<>();

                    for (File jar : jars) {
                        List<Map<String, Object>> matches = scanJar(jar, kw, slashedKeyword, filterExts);
                        if (!matches.isEmpty()) {
                            jarResults.put(relativize(rootDir, jar), matches);
                        }
                    }

                    if (!plainFiles.isEmpty()) {
                        Map<String, List<Map<String, Object>>> looseGroups =
                                scanPlainFiles(plainFiles, kw, slashedKeyword, rootDir);
                        jarResults.putAll(looseGroups);
                    }

                    json.append("  {\n");
                    json.append("    \"searchText\": ").append(jsonString(kw)).append(",\n");
                    json.append("    \"jars\": [\n");
                    List<String> jarNames = new ArrayList<>(jarResults.keySet());
                    int totOcc=0;
                    for (int j = 0; j < jarNames.size(); j++) {
                        String jarName = jarNames.get(j);
                        List<Map<String, Object>> matches = jarResults.get(jarName);
                        json.append("      {\n");
                        json.append("        \"jar\": ").append(jsonString(jarName)).append(",\n");
                        json.append("        \"classes\": [\n");
                        for (int k = 0; k < matches.size(); k++) {
                            Map<String, Object> m = matches.get(k);
                            json.append("          {\n");
                            json.append("            \"className\": ").append(jsonString((String) m.get("className"))).append(",\n");
                            json.append("            \"classPath\": ").append(jsonString((String) m.get("classPath"))).append(",\n");
                            json.append("            \"occurrences\": ").append(m.get("occurrences")).append("\n");
                            json.append("          }");
                            if (k < matches.size()-1) json.append(",");
                            json.append("\n");
                            totOcc=totOcc + (int)m.get("occurrences");

                        }
                        json.append("        ]\n      }");
                        if (j < jarNames.size()-1) json.append(",");
                        json.append("\n");
                    }
                    json.append("    ]\n  }");
                    if (i < kws.size()-1) json.append(",");
                    json.append("\n");

                    int total = jarResults.values().stream().mapToInt(List::size).sum();
                    publish("// → "+ totOcc + " hits in " + jarResults.size() + " container(s) and " + total + " item(s)\n");
                }
                json.append("]\n");

                // Write search_results.js
                File jsFile = new File(finalOutDir, "search_results.js");
                try (FileWriter fw = new FileWriter(jsFile)) {
                    fw.write("var resultData = ");
                    fw.write(json.toString());
                    fw.write(";\n");
                }
                publish("//\n// ✓ Written: " + jsFile.getAbsolutePath() + "\n");

                // Write viewer.html
                File htmlFile = new File(finalOutDir, "viewer.html");
                try (FileWriter fw = new FileWriter(htmlFile)) {
                    fw.write(VIEWER_HTML);
                }
                publish("// ✓ Written: " + htmlFile.getAbsolutePath() + "\n");
                publish("//\n// ✓ Done. Output saved to: " + finalOutDir.getAbsolutePath() + "\n");

                return finalOutDir.getAbsolutePath();
            }

            @Override protected void process(List<String> chunks) {
                for (String chunk : chunks) appendLog(chunk);
            }

            @Override protected void done() {
                try {
                    String outPath2 = get();
                    lastOutputDir = new File(outPath2);
                    openReportBtn.setEnabled(true);
                    showInExplorerBtn.setEnabled(true);
                    statusBar.set("✓  Done — report saved to: " + outPath2, Theme.SUCCESS);
                } catch (Exception ex) {
                    appendLog("// ERROR: " + ex.getMessage() + "\n");
                    statusBar.set("✗  Error — check log", Theme.ERROR);
                } finally {
                    findBtn.setEnabled(true);
                    findBtn.setText("▶  Find in Jars");
                }
            }
        }.execute();
    }

    /**
     * Scans a single jar's entries, but only those whose name ends with one of
     * {@code filterExts}. ".class" entries are searched using {@code slashedKeyword}
     * (since class references appear as "com/foo/Bar" in bytecode); every other entry
     * type is searched using the literal {@code rawKeyword}.
     */
    private List<Map<String, Object>> scanJar(File jarFile, String rawKeyword, String slashedKeyword,
                                              List<String> filterExts) {
        List<Map<String, Object>> matches = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            boolean anyFound = false;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                String entryLower = entry.getName().toLowerCase();
                String matchedExt = null;
                for (String ext : filterExts) {
                    if (entryLower.endsWith(ext)) { matchedExt = ext; break; }
                }
                if (matchedExt == null) continue;

                String textToSearch = matchedExt.equals(".class") ? slashedKeyword : rawKeyword;

                int occ = 0;
                try (InputStream is = jar.getInputStream(entry);
                     java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null)
                        if (line.contains(textToSearch)) occ++;
                } catch (IOException skip) {
                    continue; // unreadable / binary entry — skip it
                }

                if (occ > 0) {
                    anyFound = true;
                    String fullPath = entry.getName();
                    String className = fullPath;
                    int slash = className.lastIndexOf('/');
                    if (slash >= 0) className = className.substring(slash+1);
                    if (className.toLowerCase().endsWith(matchedExt))
                        className = className.substring(0, className.length() - matchedExt.length());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("className", className); m.put("classPath", fullPath); m.put("occurrences", occ);
                    matches.add(m);
                    publish("//   [" + occ + "]  " + jarFile.getName() + " → " + fullPath + "\n");
                }
            }
            if (!anyFound) publish("//   (no match) " + jarFile.getName() + "\n");
        } catch (IOException e) {
            publish("//   ERROR reading " + jarFile.getName() + ": " + e.getMessage() + "\n");
        }
        return matches;
    }

    /**
     * Scans loose (non-jar) files found on disk, grouping the hits by their parent
     * directory (relative to {@code rootDir}) so the report reads the same way a
     * jar's contents do. ".class" files use {@code slashedKeyword}; everything else
     * uses the literal {@code rawKeyword}.
     */
    private Map<String, List<Map<String, Object>>> scanPlainFiles(List<File> files, String rawKeyword,
                                                                  String slashedKeyword, File rootDir) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (File f : files) {
            String lower = f.getName().toLowerCase();
            String textToSearch = lower.endsWith(".class") ? slashedKeyword : rawKeyword;

            int occ = 0;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(f))) {
                String line;
                while ((line = reader.readLine()) != null)
                    if (line.contains(textToSearch)) occ++;
            } catch (IOException e) {
                publish("//   ERROR reading " + f.getName() + ": " + e.getMessage() + "\n");
                continue;
            }

            if (occ > 0) {
                String relPath  = relativize(rootDir, f);
                int lastSlash   = relPath.lastIndexOf('/');
                String parentKey = lastSlash >= 0 ? relPath.substring(0, lastSlash) : ".";
                String groupKey  = "📁 " + parentKey;

                String fileName = f.getName();
                int dot = fileName.lastIndexOf('.');
                String className = dot > 0 ? fileName.substring(0, dot) : fileName;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("className", className);
                m.put("classPath", relPath);
                m.put("occurrences", occ);

                groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(m);
                publish("//   [" + occ + "]  " + relPath + "\n");
            }
        }
        return groups;
    }

    private static String relativize(File root, File file) {
        try {
            String rel = root.toPath().toAbsolutePath().normalize()
                    .relativize(file.toPath().toAbsolutePath().normalize()).toString();
            return rel.replace('\\', '/');
        } catch (Exception e) {
            return file.getName();
        }
    }

    // ── called from inner class ───────────────────────────────────────────────
    private void publish(String msg) { /* used via SwingWorker — forwarded via process() */ }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void openReport() {
        if (lastOutputDir == null) return;
        File html = new File(lastOutputDir, "viewer.html");
        if (!html.exists()) { showWarn("viewer.html not found.", "File Missing"); return; }
        try {
            Desktop.getDesktop().browse(html.toURI());
        } catch (Exception ex) {
            // Fallback: try edge/chrome
            try { new ProcessBuilder("cmd", "/c", "start", "msedge", html.getAbsolutePath()).start(); }
            catch (Exception ignored) { showWarn("Cannot open browser.\nFile: " + html.getAbsolutePath(), "Open Failed"); }
        }
    }

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

    private static String jsonString(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r").replace("\t","\\t") + "\"";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WrapLayout — FlowLayout that wraps to next line
    // ══════════════════════════════════════════════════════════════════════════
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }
        @Override public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - hgap*2;
                int x = 0, y = insets.top + vgap, rowHeight = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component c = target.getComponent(i);
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (x == 0 || x + d.width <= maxWidth) {
                        x += d.width + hgap; rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        y += rowHeight + vgap; x = d.width + hgap; rowHeight = d.height;
                    }
                }
                y += rowHeight + vgap + insets.bottom;
                return new Dimension(targetWidth, y);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMBEDDED viewer.html  (written to disk at scan-time)
    // ══════════════════════════════════════════════════════════════════════════
    private static final String VIEWER_HTML =
            "<!DOCTYPE html>\n" +
                    "<html lang='en'>\n" +
                    "<head>\n" +
                    "<meta charset='UTF-8'/>\n" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>\n" +
                    "<title>JAR Inspector — Results</title>\n" +
                    "<link href='https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600;700&family=Syne:wght@400;700;800&display=swap' rel='stylesheet'/>\n" +
                    "<style>\n" +
                    ":root{--bg:#07080D;--surface:#0F1018;--card:#13141F;--border:#1A1D2E;--border2:#252737;--accent:#00FF88;--accent2:#00E5FF;--warn:#FFAA00;--pink:#FF4488;--text:#E2E4F0;--muted:#6B7094;--dim:#3A3D52;--hit:#00FF8812;--radius:8px;}\n" +
                    "*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}\n" +
                    "body{background:var(--bg);color:var(--text);font-family:'Syne',sans-serif;min-height:100vh;overflow-x:hidden;}\n" +
                    "body::before{content:'';position:fixed;inset:0;pointer-events:none;z-index:0;\n" +
                    "  background-image:linear-gradient(rgba(0,255,136,.025) 1px,transparent 1px),linear-gradient(90deg,rgba(0,255,136,.025) 1px,transparent 1px);\n" +
                    "  background-size:32px 32px;}\n" +
                    ".glow{position:fixed;width:700px;height:700px;border-radius:50%;\n" +
                    "  background:radial-gradient(circle,rgba(0,229,255,.12) 0%,transparent 70%);\n" +
                    "  top:-250px;right:-200px;pointer-events:none;z-index:0;\n" +
                    "  animation:drift 14s ease-in-out infinite alternate;}\n" +
                    "@keyframes drift{to{transform:translate(-80px,100px);}}\n" +
                    ".wrapper{position:relative;z-index:1;max-width:1140px;margin:0 auto;padding:40px 24px 80px;}\n" +
                    "header{margin-bottom:48px;}\n" +
                    ".logo{font-size:10px;letter-spacing:.3em;color:var(--accent);text-transform:uppercase;font-family:'JetBrains Mono',monospace;margin-bottom:14px;}\n" +
                    "h1{font-size:clamp(26px,5vw,48px);font-weight:800;line-height:1.05;\n" +
                    "  background:linear-gradient(135deg,var(--text) 30%,var(--accent2));\n" +
                    "  -webkit-background-clip:text;-webkit-text-fill-color:transparent;}\n" +
                    ".subtitle{margin-top:10px;color:var(--muted);font-size:13px;font-family:'JetBrains Mono',monospace;}\n" +
                    ".upload-zone{border:1px dashed var(--border2);border-radius:var(--radius);padding:40px 28px;text-align:center;cursor:pointer;\n" +
                    "  transition:border-color .2s,background .2s;margin-bottom:36px;background:var(--surface);}\n" +
                    ".upload-zone:hover,.upload-zone.drag{border-color:var(--accent);background:#00FF8808;}\n" +
                    ".upload-zone svg{width:36px;height:36px;color:var(--muted);margin-bottom:12px;}\n" +
                    ".upload-zone p{color:var(--muted);font-size:13px;font-family:'JetBrains Mono',monospace;}\n" +
                    ".upload-zone p span{color:var(--accent);}\n" +
                    "#fileInput{display:none;}\n" +
                    ".stats{display:flex;gap:12px;flex-wrap:wrap;margin-bottom:28px;opacity:0;transform:translateY(10px);transition:opacity .4s,transform .4s;}\n" +
                    ".stats.show{opacity:1;transform:none;}\n" +
                    ".stat-pill{background:var(--card);border:1px solid var(--border2);border-radius:100px;padding:7px 16px;\n" +
                    "  font-size:12px;font-family:'JetBrains Mono',monospace;display:flex;align-items:center;gap:8px;}\n" +
                    ".stat-pill .num{color:var(--accent);font-weight:700;font-size:15px;}\n" +
                    ".search-wrap{position:relative;margin-bottom:28px;}\n" +
                    ".search-wrap input{width:100%;background:var(--surface);border:1px solid var(--border2);border-radius:var(--radius);\n" +
                    "  padding:11px 16px 11px 42px;color:var(--text);font-family:'JetBrains Mono',monospace;font-size:12px;outline:none;transition:border-color .2s;}\n" +
                    ".search-wrap input:focus{border-color:var(--accent);}\n" +
                    ".search-wrap input::placeholder{color:var(--muted);}\n" +
                    ".search-wrap svg{position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--muted);width:16px;}\n" +
                    ".search-block{margin-bottom:32px;opacity:0;transform:translateY(14px);animation:fadeUp .4s forwards;}\n" +
                    "@keyframes fadeUp{to{opacity:1;transform:none;}}\n" +
                    ".search-label{background:linear-gradient(90deg,#003A30,#001A30);border-left:3px solid var(--accent);\n" +
                    "  border-radius:var(--radius) var(--radius) 0 0;padding:10px 16px;\n" +
                    "  font-size:10px;letter-spacing:.18em;text-transform:uppercase;font-weight:700;\n" +
                    "  font-family:'JetBrains Mono',monospace;color:var(--accent);display:flex;align-items:center;gap:8px;}\n" +
                    ".search-text-value{background:var(--surface);border:1px solid var(--border2);border-top:none;\n" +
                    "  padding:11px 16px;font-family:'JetBrains Mono',monospace;font-size:13px;color:var(--accent2);word-break:break-all;}\n" +
                    ".jar-card{border:1px solid var(--border);border-top:none;background:var(--bg);transition:border-color .15s;}\n" +
                    ".jar-card:last-child{border-radius:0 0 var(--radius) var(--radius);}\n" +
                    ".jar-card:hover{border-color:var(--border2);}\n" +
                    ".jar-header{display:flex;align-items:center;justify-content:space-between;padding:11px 16px;cursor:pointer;user-select:none;}\n" +
                    ".jar-name{font-family:'JetBrains Mono',monospace;font-size:12px;font-weight:600;color:var(--warn);display:flex;align-items:center;gap:8px;}\n" +
                    ".jar-name svg{width:15px;opacity:.7;}\n" +
                    ".jar-badge{background:var(--border);color:var(--muted);font-size:10px;font-family:'JetBrains Mono',monospace;\n" +
                    "  padding:3px 10px;border-radius:100px;display:flex;align-items:center;gap:6px;}\n" +
                    ".jar-badge .count{color:var(--text);font-weight:700;}\n" +
                    ".chevron{width:15px;color:var(--muted);transition:transform .2s;}\n" +
                    ".jar-card.open .chevron{transform:rotate(180deg);}\n" +
                    ".class-table-wrap{overflow:hidden;max-height:0;transition:max-height .3s ease;}\n" +
                    ".jar-card.open .class-table-wrap{max-height:3000px;}\n" +
                    "table{width:100%;border-collapse:collapse;font-family:'JetBrains Mono',monospace;font-size:11px;}\n" +
                    "thead tr{background:#0A0B12;}\n" +
                    "th{padding:7px 16px;text-align:left;color:var(--muted);font-size:9px;letter-spacing:.14em;text-transform:uppercase;border-bottom:1px solid var(--border2);}\n" +
                    "tbody tr{transition:background .12s;}\n" +
                    "tbody tr:hover{background:var(--hit);}\n" +
                    "td{padding:8px 16px;border-bottom:1px solid #0E1018;color:var(--text);}\n" +
                    "td:last-child{text-align:right;}\n" +
                    ".td-class{color:var(--accent2);font-weight:600;}\n" +
                    ".td-path{color:var(--muted);word-break:break-all;}\n" +
                    ".td-occ{display:inline-block;background:linear-gradient(135deg,#003A20,#001A30);border:1px solid var(--accent);\n" +
                    "  color:var(--accent);border-radius:100px;padding:2px 12px;font-weight:700;font-size:11px;}\n" +
                    ".filter-row{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:20px;opacity:0;transition:opacity .3s;}\n" +
                    ".filter-row.show{opacity:1;}\n" +
                    ".filter-btn{background:var(--surface);border:1px solid var(--border2);color:var(--muted);border-radius:100px;\n" +
                    "  padding:4px 14px;font-size:10px;font-family:'JetBrains Mono',monospace;cursor:pointer;transition:all .15s;}\n" +
                    ".filter-btn:hover,.filter-btn.active{border-color:var(--accent);color:var(--accent);background:var(--hit);}\n" +
                    ".no-match{text-align:center;padding:56px 24px;color:var(--muted);font-family:'JetBrains Mono',monospace;font-size:12px;display:none;}\n" +
                    "::-webkit-scrollbar{width:5px;} ::-webkit-scrollbar-track{background:var(--bg);} ::-webkit-scrollbar-thumb{background:var(--border2);border-radius:3px;}\n" +
                    "</style>\n" +
                    "<script src='search_results.js' onerror='window._jsFileMissing=true'></script>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class='glow'></div>\n" +
                    "<div class='wrapper'>\n" +
                    "<header>\n" +
                    "  <div class='logo'>⬡ utils hub — jar inspector</div>\n" +
                    "  <h1>Search Results</h1>\n" +
                    "  <p class='subtitle'>// place search_results.js next to this file for instant load</p>\n" +
                    "</header>\n" +
                    "<div class='upload-zone' id='dropZone' onclick=\"document.getElementById('fileInput').click()\">\n" +
                    "  <input type='file' id='fileInput' accept='.js,.json'/>\n" +
                    "  <svg fill='none' stroke='currentColor' stroke-width='1.5' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' d='M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5'/></svg>\n" +
                    "  <p>Click or drag &amp; drop your <span>search_results.js</span></p>\n" +
                    "  <p style='margin-top:5px;font-size:10px;'>Auto-loads if placed in the same directory as this file</p>\n" +
                    "</div>\n" +
                    "<div class='stats' id='statsBar'>\n" +
                    "  <div class='stat-pill'><span class='num' id='stSearches'>0</span> Search Terms</div>\n" +
                    "  <div class='stat-pill'><span class='num' id='stJars'>0</span> Containers Hit</div>\n" +
                    "  <div class='stat-pill'><span class='num' id='stClasses'>0</span> Items Found</div>\n" +
                    "  <div class='stat-pill'><span class='num' id='stOcc'>0</span> Total Occurrences</div>\n" +
                    "</div>\n" +
                    "<div class='search-wrap' id='filterWrap' style='display:none'>\n" +
                    "  <svg fill='none' stroke='currentColor' stroke-width='2' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' d='M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 15.803 7.5 7.5 0 0015.803 15.803z'/></svg>\n" +
                    "  <input type='text' id='filterInput' placeholder='Filter by class name, path, or JAR…'/>\n" +
                    "</div>\n" +
                    "<div class='filter-row' id='filterRow'></div>\n" +
                    "<div id='results'></div>\n" +
                    "<div class='no-match' id='noMatch'>No matches for your filter.</div>\n" +
                    "</div>\n" +
                    "<script>\n" +
                    "let allData=[],activeFilter=null;\n" +
                    "window.addEventListener('DOMContentLoaded',()=>{\n" +
                    "  if(typeof resultData!=='undefined'&&Array.isArray(resultData)){\n" +
                    "    document.getElementById('dropZone').style.display='none';\n" +
                    "    allData=resultData; render(resultData);\n" +
                    "  }\n" +
                    "});\n" +
                    "document.getElementById('fileInput').addEventListener('change',e=>loadFile(e.target.files[0]));\n" +
                    "const dropZone=document.getElementById('dropZone');\n" +
                    "dropZone.addEventListener('dragover',e=>{e.preventDefault();dropZone.classList.add('drag');});\n" +
                    "dropZone.addEventListener('dragleave',()=>dropZone.classList.remove('drag'));\n" +
                    "dropZone.addEventListener('drop',e=>{e.preventDefault();dropZone.classList.remove('drag');if(e.dataTransfer.files[0])loadFile(e.dataTransfer.files[0]);});\n" +
                    "function loadFile(file){\n" +
                    "  if(!file)return;\n" +
                    "  const r=new FileReader();\n" +
                    "  r.onload=e=>{\n" +
                    "    try{\n" +
                    "      let data,raw=e.target.result.trim();\n" +
                    "      if(file.name.endsWith('.js')){const m=raw.match(/var\\s+resultData\\s*=\\s*(\\[[\\s\\S]*\\])\\s*;?/);if(!m)throw new Error('resultData not found');data=JSON.parse(m[1]);}\n" +
                    "      else data=JSON.parse(raw);\n" +
                    "      allData=data;dropZone.style.display='none';render(data);\n" +
                    "    }catch(err){alert('Could not read file:\\n'+err.message);}\n" +
                    "  };\n" +
                    "  r.readAsText(file);\n" +
                    "}\n" +
                    "document.getElementById('filterInput').addEventListener('input',function(){renderResults(allData,this.value.trim().toLowerCase(),activeFilter);});\n" +
                    "function render(data){\n" +
                    "  let tj=0,tc=0,to=0;\n" +
                    "  data.forEach(s=>s.jars.forEach(j=>{tj++;j.classes.forEach(c=>{tc++;to+=c.occurrences;});}));\n" +
                    "  document.getElementById('stSearches').textContent=data.length;\n" +
                    "  document.getElementById('stJars').textContent=tj;\n" +
                    "  document.getElementById('stClasses').textContent=tc;\n" +
                    "  document.getElementById('stOcc').textContent=to;\n" +
                    "  document.getElementById('statsBar').classList.add('show');\n" +
                    "  const fr=document.getElementById('filterRow');fr.innerHTML='';\n" +
                    "  if(data.length>1){\n" +
                    "    const all=document.createElement('button');all.className='filter-btn active';all.textContent='All';\n" +
                    "    all.onclick=()=>setFilter(null,all);fr.appendChild(all);\n" +
                    "    data.forEach((s,i)=>{\n" +
                    "      const btn=document.createElement('button');btn.className='filter-btn';\n" +
                    "      btn.textContent=s.searchText.split('.').pop();btn.title=s.searchText;\n" +
                    "      btn.onclick=()=>setFilter(i,btn);fr.appendChild(btn);\n" +
                    "    });fr.classList.add('show');\n" +
                    "  }\n" +
                    "  document.getElementById('filterWrap').style.display='';\n" +
                    "  renderResults(data,'',null);\n" +
                    "}\n" +
                    "function setFilter(idx,btn){\n" +
                    "  activeFilter=idx;\n" +
                    "  document.querySelectorAll('.filter-btn').forEach(b=>b.classList.remove('active'));\n" +
                    "  btn.classList.add('active');\n" +
                    "  renderResults(allData,document.getElementById('filterInput').value.toLowerCase(),idx);\n" +
                    "}\n" +
                    "function renderResults(data,query,filterIdx){\n" +
                    "  const container=document.getElementById('results');container.innerHTML='';\n" +
                    "  let any=false;\n" +
                    "  data.forEach((si,i)=>{\n" +
                    "    if(filterIdx!==null&&filterIdx!==undefined&&Number(filterIdx)!==i)return;\n" +
                    "    const fj=si.jars.map(j=>({...j,classes:j.classes.filter(c=>!query||c.className.toLowerCase().includes(query)||c.classPath.toLowerCase().includes(query)||j.jar.toLowerCase().includes(query))})).filter(j=>j.classes.length>0);\n" +
                    "    if(fj.length===0&&query)return;\n" +
                    "    any=true;\n" +
                    "    const block=document.createElement('div');block.className='search-block';block.style.animationDelay=(i*70)+'ms';\n" +
                    "    block.innerHTML='<div class=\\'search-label\\'>⬡ &nbsp;Search Term '+(i+1)+'</div>'+'<div class=\\'search-text-value\\'>'+escHtml(si.searchText)+'</div>';\n" +
                    "    if(fj.length===0){const nd=document.createElement('div');nd.style.cssText='background:var(--surface);border:1px solid var(--border);border-top:none;padding:16px;font-family:JetBrains Mono,monospace;font-size:11px;color:var(--muted);border-radius:0 0 8px 8px;';nd.textContent='⚠ No matches in any JAR.';block.appendChild(nd);}\n" +
                    "    else fj.forEach((jar,ji)=>{\n" +
                    "      const card=document.createElement('div');card.className='jar-card'+(ji===0?' open':'');\n" +
                    "      if(ji===fj.length-1)card.style.borderRadius='0 0 8px 8px';\n" +
                    "      const tot=jar.classes.reduce((s,c)=>s+c.occurrences,0);\n" +
                    "      card.innerHTML=`<div class='jar-header'><div class='jar-name'><svg fill='none' stroke='currentColor' stroke-width='2' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' d='M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z'/></svg>${escHtml(jar.jar)}</div><div style='display:flex;align-items:center;gap:8px'><div class='jar-badge'><span class='count'>${jar.classes.length}</span> items &middot; <span class='count'>${tot}</span> hits</div><svg class='chevron' fill='none' stroke='currentColor' stroke-width='2.5' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' d='M19.5 8.25l-7.5 7.5-7.5-7.5'/></svg></div></div><div class='class-table-wrap'><table><thead><tr><th>#</th><th>Name</th><th>Path</th><th>Hits</th></tr></thead><tbody>${jar.classes.map((c,ci)=>`<tr><td style='color:var(--muted);width:36px'>${ci+1}</td><td class='td-class'>${escHtml(c.className)}</td><td class='td-path'>${escHtml(c.classPath)}</td><td><span class='td-occ'>${c.occurrences}</span></td></tr>`).join('')}</tbody></table></div>`;\n" +
                    "      card.querySelector('.jar-header').addEventListener('click',()=>card.classList.toggle('open'));\n" +
                    "      block.appendChild(card);\n" +
                    "    });\n" +
                    "    container.appendChild(block);\n" +
                    "  });\n" +
                    "  document.getElementById('noMatch').style.display=any?'none':'block';\n" +
                    "}\n" +
                    "function escHtml(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;');}\n" +
                    "</script>\n" +
                    "</body>\n" +
                    "</html>\n";

    // ══════════════════════════════════════════════════════════════════════════
    // HELP PAGE
    // ══════════════════════════════════════════════════════════════════════════

    public JPanel createHelpPanel() {
        String html = ""
                + "<h2>Jar Inspector</h2>"
                + "<p>Searches for keywords across class files, either loose on disk or packed inside jars, "
                + "and writes an interactive HTML report of every match.</p>"
                + "<h3>Steps</h3>"
                + "<ol>"
                + "<li>Browse to a <b>Libs path</b> — a directory containing jars and/or class files.</li>"
                + "<li>Pick the <b>File types</b> to scan (which extensions count as \"code\" to search).</li>"
                + "<li>Add one or more <b>Keywords</b> — press Enter or click <b>+ Add</b> after typing each "
                + "one; click the <b>×</b> on a chip to remove it.</li>"
                + "<li>Browse to an <b>Output path</b> for the report.</li>"
                + "<li>Click <b>Find in Jars</b> and watch progress in the Scan Log.</li>"
                + "</ol>"
                + "<h3>Viewing results</h3>"
                + "<p>Click <b>Open Report</b> to launch the generated HTML report in your browser — it groups "
                + "matches by jar/class with occurrence counts. Use <b>Show in Explorer</b> to reveal the "
                + "output folder instead.</p>";
        return Theme.helpPage("Jar Inspector — How to Use", PANEL_ACCENT, html,
                () -> launcher.navigateTo(Launcher.JFINDER));
    }
}
