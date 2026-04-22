package com.utilities.InsertCommandFormatter;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class InsertCommandFormatterGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InsertCommandFormatterGUI().buildUI());
    }

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JTextField searchField;

    private Highlighter.HighlightPainter highlightPainter =
            new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

    private List<Integer> matchPositions = new ArrayList<>();
    private int currentMatchIndex = -1;

    // Storage for clean copy (true SQL INSERT)
    private List<String> lastColumns = null;
    private List<String> lastValues = null;
    private String lastTableName = null;

    // THEME COLORS
    private final Color BG_DARK = new Color(0x0E0F19);
    private final Color BG_PANEL = new Color(0x1A1C27);
    private final Color TEXT_LIGHT = new Color(0x27A749);
    private final Color ACCENT = new Color(0x27A749);
    private final Color BUTTON_HOVER = new Color(0x072D33);

    private void applyTheme(JComponent comp) {
        comp.setBackground(BG_PANEL);
        comp.setForeground(TEXT_LIGHT);

        if (comp instanceof JTextArea || comp instanceof JTextField) {
            comp.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);

        b.setBackground(BG_PANEL);
        b.setForeground(ACCENT);
        b.setFocusPainted(false);

        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 2, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b.setBackground(TEXT_LIGHT);
                b.setForeground(BG_PANEL);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                b.setBackground(BG_PANEL);
                b.setForeground(TEXT_LIGHT);
            }
        });

        return b;
    }

    private void buildUI() {

        JFrame frame = new JFrame("SQL Insert Mapper");
        frame.setSize(1200, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BG_DARK);
        frame.setLayout(new BorderLayout());

        // ------------------------- TOP BAR -------------------------
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_DARK);
        topBar.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        leftButtons.setBackground(BG_DARK);
        leftButtons.setBorder(BorderFactory.createEmptyBorder());

        JButton formatBtn = styledButton("Format");
        JButton clearInputBtn = styledButton("Clear Input");
        JButton cleanCopyBtn = styledButton("Copy Query");

        leftButtons.add(formatBtn);
        leftButtons.add(clearInputBtn);

        // Font size dropdown
        String[] fontSizes = {"10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30"};
        JComboBox<String> fontCombo = new JComboBox<>(fontSizes);
        fontCombo.setSelectedItem("14");
        fontCombo.setBackground(BG_PANEL);
        fontCombo.setForeground(ACCENT);
        fontCombo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        fontCombo.setFocusable(false);
        fontCombo.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
        leftButtons.add(fontCombo);

        fontCombo.addActionListener(e -> {
            int size = Integer.parseInt((String) fontCombo.getSelectedItem());
            inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, size));
            outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, size));
        });

        // Search bar panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        searchPanel.setBackground(BG_DARK);
        searchPanel.setBorder(BorderFactory.createEmptyBorder());

        searchField = new JTextField(20);
        applyTheme(searchField);
        searchField.setCaretColor(ACCENT);
        searchField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        searchField.setBorder(BorderFactory.createLineBorder(ACCENT, 2));

        JButton searchBtn = styledButton("Search");
        JButton nextBtn = styledButton("Next");
        JButton prevBtn = styledButton("Prev");

        searchPanel.add(makeLabel("Find: "));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(prevBtn);
        searchPanel.add(nextBtn);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        rightButtons.setBackground(BG_DARK);
        rightButtons.setBorder(BorderFactory.createEmptyBorder());
        rightButtons.add(cleanCopyBtn);

        topBar.add(leftButtons, BorderLayout.WEST);
        topBar.add(searchPanel, BorderLayout.CENTER);
        topBar.add(rightButtons, BorderLayout.EAST);

        frame.add(topBar, BorderLayout.NORTH);

        // ------------------------ TEXT AREAS ------------------------
        // ------------------------ TEXT AREAS WITH HEADINGS ------------------------

// Input area
        inputArea = new JTextArea();
        applyTheme(inputArea);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setCaretColor(ACCENT);
        inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

// Output area
        outputArea = new JTextArea();
        applyTheme(outputArea);
        outputArea.setLineWrap(false);
        outputArea.setWrapStyleWord(false);
        outputArea.setCaretColor(ACCENT);
        outputArea.setEditable(true);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        outputArea.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

// Add headings
        JLabel inputLabel = new JLabel("Input Query");
        inputLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        inputLabel.setForeground(ACCENT);
        inputLabel.setOpaque(true);
        inputLabel.setBackground(BG_DARK);
        inputLabel.setBorder(BorderFactory.createEmptyBorder(3, 6, 5, 5));


        JLabel outputLabel = new JLabel("Formated Col-Value Table");
        outputLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        outputLabel.setForeground(ACCENT);
        outputLabel.setOpaque(true);
        outputLabel.setBackground(BG_DARK);
        outputLabel.setBorder(BorderFactory.createEmptyBorder(3, 6, 5, 5));


// Wrap areas with headings
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(BG_PANEL);
        inputPanel.add(inputLabel, BorderLayout.NORTH);
        JScrollPane leftPane = new JScrollPane(inputArea);
        leftPane.setBorder(BorderFactory.createEmptyBorder());
        leftPane.getViewport().setBackground(BG_PANEL);
        customizeScrollBar(leftPane);
        inputPanel.add(leftPane, BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBackground(BG_PANEL);
        outputPanel.add(outputLabel, BorderLayout.NORTH);
        JScrollPane rightPane = new JScrollPane(outputArea);
        rightPane.setBorder(BorderFactory.createEmptyBorder());
        rightPane.getViewport().setBackground(BG_PANEL);
        customizeScrollBar(rightPane);
        outputPanel.add(rightPane, BorderLayout.CENTER);

// Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, outputPanel);
        splitPane.setDividerLocation(0.5);
        splitPane.setBorder(null);
        splitPane.setBackground(ACCENT);
        splitPane.setDividerSize(3);
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this);
                divider.setBackground(ACCENT);
                return divider;
            }
        });


        frame.add(splitPane, BorderLayout.CENTER);

        // ----------------------- BUTTON ACTIONS -----------------------
        formatBtn.addActionListener(e -> {
            clearHighlights();
            try {
                ParseResult result = parseInsert(inputArea.getText());
                lastColumns = result.columns;
                lastValues = result.values;
                lastTableName = result.tableName;

                String table = buildColValueTable(result.columns, result.values);
                outputArea.setText(table);

            } catch (Exception ex) {
                outputArea.setText("Error:\n" + ex.getMessage());
            }
        });

        clearInputBtn.addActionListener(e -> inputArea.setText(""));

        cleanCopyBtn.addActionListener(e -> {
            try {
                List<String> newCols = new ArrayList<>();
                List<String> newVals = new ArrayList<>();
                String[] lines = outputArea.getText().split("\n");
                for (String line : lines) {
                    if (line.contains("|") && !line.startsWith("COLUMN")) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 2) {
                            String col = parts[0].trim();
                            String val = parts[1].trim();
                            if (!col.isEmpty()) {
                                newCols.add(col);
                                newVals.add(val);
                            }
                        }
                    }
                }
                lastColumns = newCols;
                lastValues = newVals;
                String sql = generateInsertSQL();
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(sql), null);
            } catch (Exception ex) {
                outputArea.setText("Copy Failed: " + ex.getMessage());
            }
        });

        searchBtn.addActionListener(e -> performSearch());
        nextBtn.addActionListener(e -> highlightNext());
        prevBtn.addActionListener(e -> highlightPrev());

        frame.setVisible(true);
    }

    private void customizeScrollBar(JScrollPane scrollPane) {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        JScrollBar horizontal = scrollPane.getHorizontalScrollBar();

        for (JScrollBar bar : new JScrollBar[]{vertical, horizontal}) {
            bar.setUI(new BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = ACCENT;
                    this.trackColor = BG_PANEL.darker();
                }

                @Override
                protected Dimension getMinimumThumbSize() {
                    return bar.getOrientation() == JScrollBar.VERTICAL ? new Dimension(8, 30) : new Dimension(30, 8);
                }

                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
                }

                @Override
                protected JButton createIncreaseButton(int orientation) {
                    return createZeroButton();
                }

                private JButton createZeroButton() {
                    JButton button = new JButton();
                    button.setPreferredSize(new Dimension(0, 0));
                    button.setMinimumSize(new Dimension(0, 0));
                    button.setMaximumSize(new Dimension(0, 0));
                    return button;
                }
            });
        }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT);
        return label;
    }

    private void performSearch() {
        clearHighlights();
        matchPositions.clear();
        currentMatchIndex = -1;
        String text = outputArea.getText();
        String query = searchField.getText();
        if (query.isEmpty()) return;
        int index = text.indexOf(query);
        while (index >= 0) {
            matchPositions.add(index);
            index = text.indexOf(query, index + 1);
        }
        if (!matchPositions.isEmpty()) {
            currentMatchIndex = 0;
            highlightAtCurrentIndex();
        }
    }

    private void highlightNext() {
        if (matchPositions.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + 1) % matchPositions.size();
        highlightAtCurrentIndex();
    }

    private void highlightPrev() {
        if (matchPositions.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex - 1 + matchPositions.size()) % matchPositions.size();
        highlightAtCurrentIndex();
    }

    private void highlightAtCurrentIndex() {
        clearHighlights();
        try {
            int pos = matchPositions.get(currentMatchIndex);
            outputArea.getHighlighter().addHighlight(
                    pos,
                    pos + searchField.getText().length(),
                    highlightPainter
            );
            outputArea.setCaretPosition(pos);
        } catch (Exception ignored) {}
    }

    private void clearHighlights() {
        outputArea.getHighlighter().removeAllHighlights();
    }

    private static class ParseResult {
        String tableName;
        List<String> columns;
        List<String> values;
    }

    private static ParseResult parseInsert(String insertSQL) {
        String sql = insertSQL.trim().replaceAll("\\s+", " ");
        String upper = sql.toUpperCase();
        if (!upper.startsWith("INSERT INTO"))
            throw new IllegalArgumentException("Query must start with INSERT INTO");
        int firstParen = sql.indexOf("(");
        int secondParen = sql.indexOf(")");
        String table = sql.substring("INSERT INTO".length(), firstParen).trim();
        String colsPart = sql.substring(firstParen + 1, secondParen);
        String[] colsArr = colsPart.split(",");
        List<String> columns = new ArrayList<>();
        for (String c : colsArr) columns.add(c.trim());
        int valuesIndex = upper.indexOf("VALUES");
        if (valuesIndex == -1) throw new IllegalArgumentException("VALUES missing");
        int openVals = sql.indexOf("(", valuesIndex);
        int closeVals = sql.lastIndexOf(")");
        String valuesPart = sql.substring(openVals + 1, closeVals);
        List<String> values = splitValues(valuesPart);
        if (values.size() != columns.size())
            throw new IllegalArgumentException("Column/value count mismatch.");
        ParseResult r = new ParseResult();
        r.tableName = table;
        r.columns = columns;
        r.values = values;
        return r;
    }

    private static List<String> splitValues(String valuesPart) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (char ch : valuesPart.toCharArray()) {
            if (ch == '(') depth++;
            if (ch == ')') depth--;
            if (ch == ',' && depth == 0) {
                values.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() > 0) values.add(sb.toString().trim());
        return values;
    }

    private String buildColValueTable(List<String> cols, List<String> vals) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-40s | %s\n", "COLUMN", "VALUE"));
        sb.append("-----------------------------------------------------------------------------------\n");
        for (int i = 0; i < cols.size(); i++) {
            sb.append(String.format("%-40s | %s\n", cols.get(i), vals.get(i)));
        }
        return sb.toString();
    }

    private String generateInsertSQL() {
        if (lastColumns == null || lastValues == null || lastTableName == null)
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(lastTableName).append(" (\n");
        for (int i = 0; i < lastColumns.size(); i++) {
            sb.append("    ").append(lastColumns.get(i));
            if (i < lastColumns.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(") VALUES (\n");
        for (int i = 0; i < lastValues.size(); i++) {
            sb.append("    ").append(lastValues.get(i));
            if (i < lastValues.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(");");
        return sb.toString();
    }
}