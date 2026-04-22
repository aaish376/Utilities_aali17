package com.utilities.LogStatementFilter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class LogStatementFilterGUI extends JFrame {

    private JTextField fileField;
    private JTextArea searchArea;
    private JTextArea statusArea;
    private JCheckBox caseCheckBox;

    private final Map<String, Integer> keywordMatchCount = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LogStatementFilterGUI().setVisible(true));
    }

    public LogStatementFilterGUI() {
        setTitle("Log Statement Filter");
        setSize(480, 300);          // ~4 x 2.5 inches
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLookAndFeel();
        buildUI();
    }

    private void setLookAndFeel() {
        UIManager.put("Panel.background", new Color(20, 20, 20));
        UIManager.put("Label.foreground", new Color(0, 200, 0));
        UIManager.put("TextField.background", new Color(30, 30, 30));
        UIManager.put("TextField.foreground", new Color(0, 200, 0));
        UIManager.put("TextArea.background", new Color(30, 30, 30));
        UIManager.put("TextArea.foreground", new Color(0, 200, 0));
        UIManager.put("TextArea.caretForeground", new Color(0, 255, 0));
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        /* ---------- TOP ---------- */
        JPanel top = new JPanel(new BorderLayout(5, 5));
        fileField = new JTextField();
        fileField.setEditable(false);

        JButton browseBtn = createButton("Browse Log");
        browseBtn.addActionListener(this::browseFile);

        top.add(fileField, BorderLayout.CENTER);
        top.add(browseBtn, BorderLayout.EAST);

        /* ---------- LEFT ---------- */
        JPanel left = new JPanel(new BorderLayout(6, 6));

        JButton filterBtn = createButton("Filter");
        filterBtn.addActionListener(this::processFile);

        caseCheckBox = new JCheckBox("Case Sensitive");
        styleCheckBox(caseCheckBox);

        JPanel controls = new JPanel(new GridLayout(2, 1, 4, 4));
        controls.add(filterBtn);
        controls.add(caseCheckBox);

        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setBorder(BorderFactory.createTitledBorder("Status"));

        left.add(controls, BorderLayout.NORTH);
        left.add(new JScrollPane(statusArea), BorderLayout.CENTER);

        /* ---------- RIGHT ---------- */
        searchArea = new JTextArea();
        searchArea.setBorder(BorderFactory.createTitledBorder("Search Strings"));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                left,
                new JScrollPane(searchArea)
        );
        splitPane.setDividerLocation(170);
        splitPane.setOneTouchExpandable(true);

        main.add(top, BorderLayout.NORTH);
        main.add(splitPane, BorderLayout.CENTER);

        add(main);
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(10, 40, 10));
        btn.setForeground(new Color(0, 200, 0));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 0)));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(0, 80, 0));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(10, 40, 10));
            }
        });
        return btn;
    }

    private void styleCheckBox(JCheckBox cb) {
        cb.setBackground(new Color(20, 20, 20));
        cb.setForeground(new Color(0, 200, 0));
        cb.setFocusPainted(false);
    }

    private void browseFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Log files", "log"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void processFile(ActionEvent e) {
        if (fileField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a log file");
            return;
        }

        List<String> keywords = getSearchStrings();
        if (keywords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter search strings");
            return;
        }

        boolean caseSensitive = caseCheckBox.isSelected();

        keywordMatchCount.clear();
        keywords.forEach(k -> keywordMatchCount.put(k, 0));

        File inputFile = new File(fileField.getText());
        File outputFile = new File(
                inputFile.getParent(),
                inputFile.getName().replaceFirst("(\\.log)?$", "_filtered.log")
        );

        new SwingWorker<Void, Void>() {

            int matchedStatements = 0;

            @Override
            protected Void doInBackground() throws Exception {

                try (BufferedReader br = Files.newBufferedReader(inputFile.toPath());
                     BufferedWriter bw = Files.newBufferedWriter(outputFile.toPath())) {

                    String line;
                    StringBuilder statement = new StringBuilder();

                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("[")) {
                            if (statement.length() > 0) {
                                if (matchAndCount(statement.toString(), keywords, caseSensitive)) {
                                    bw.write(statement.toString());
                                    matchedStatements++;
                                }
                                statement.setLength(0);
                            }
                        }
                        statement.append(line).append(System.lineSeparator());
                    }

                    if (statement.length() > 0 &&
                            matchAndCount(statement.toString(), keywords, caseSensitive)) {
                        bw.write(statement.toString());
                        matchedStatements++;
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                updateStatus(matchedStatements, outputFile);
                JOptionPane.showMessageDialog(
                        LogStatementFilterGUI.this,
                        "Filtering completed successfully!",
                        "Done",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        }.execute();
    }

    private boolean matchAndCount(String statement, List<String> keywords, boolean caseSensitive) {
        boolean matched = false;
        String src = caseSensitive ? statement : statement.toLowerCase();

        for (String k : keywords) {
            String key = caseSensitive ? k : k.toLowerCase();
            if (src.contains(key)) {
                keywordMatchCount.put(k, keywordMatchCount.get(k) + 1);
                matched = true;
            }
        }
        return matched;
    }

    private void updateStatus(int total, File out) {
        StringBuilder sb = new StringBuilder();
        sb.append("Output: ").append(out.getName()).append("\n");
        sb.append("Total matched statements: ").append(total).append("\n\n");
        sb.append("Matches per keyword:\n");

        keywordMatchCount.forEach((k, v) ->
                sb.append("  ").append(k).append(" : ").append(v).append("\n"));

        statusArea.setText(sb.toString());
    }

    private List<String> getSearchStrings() {
        List<String> list = new ArrayList<>();
        for (String s : searchArea.getText().split("\\R")) {
            if (!s.trim().isEmpty()) {
                list.add(s.trim());
            }
        }
        return list;
    }
}
