package com.utilities.Theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shared modern design system for Utils Hub: palette, typography and
 * reusable component factories. Panels should build their chrome
 * (toolbar / split / cards / log panel / status bar) from these factories
 * instead of hand-rolling borders and paintComponent overrides, so every
 * tool stays visually consistent.
 */
public final class Theme {
    private Theme() {}

    // ── Core Palette ──────────────────────────────────────────────────────────
    public static final Color BG_BASE      = new Color(0x0D0F14);
    public static final Color BG_SURFACE   = new Color(0x11141C);
    public static final Color BG_ELEVATED  = new Color(0x161A24);
    public static final Color BG_CONTROL   = new Color(0x1D212C);
    public static final Color BG_DEEP      = new Color(0x0A0C11);

    // Signature accent — modern emerald/teal, used sparingly for focus & brand
    public static final Color ACCENT       = new Color(0x2DD4A7);
    public static final Color ACCENT_DIM   = new Color(0x1FA07E);
    public static final Color ACCENT_DARK  = new Color(0x123B32);
    public static final Color ACCENT_GLOW  = new Color(0x8BF0D6);

    // Borders
    public static final Color BORDER_DIM   = new Color(0x20242F);
    public static final Color BORDER_MID   = new Color(0x30364A);
    public static final Color BORDER_NEON  = ACCENT;
    public static final Color BORDER_GLOW  = new Color(0x2DD4A7, true);

    // Text
    public static final Color TEXT_PRIMARY   = new Color(0xEDEFF5);
    public static final Color TEXT_SECONDARY = new Color(0x9CA3B8);
    public static final Color TEXT_DIM       = new Color(0x646B80);
    public static final Color TEXT_ON_ACCENT = new Color(0x07231C);

    // Semantic
    public static final Color SUCCESS      = new Color(0x2DD4A7);
    public static final Color ERROR        = new Color(0xFF5D6C);
    public static final Color WARN         = new Color(0xFFB454);
    public static final Color INFO         = new Color(0x5B9DFF);
    public static final Color HELP         = new Color(0xF5D547);   // warm yellow — the "How to Use" affordance

    // ── Typography ────────────────────────────────────────────────────────────
    // UI chrome uses a clean sans font at readable sizes; monospace is reserved
    // for logs, code, hashes and file paths where column alignment matters.
    public static final Font FONT_UI_SM     = uiFont(Font.PLAIN, 12);
    public static final Font FONT_UI_MD     = uiFont(Font.PLAIN, 13);
    public static final Font FONT_UI_BOLD   = uiFont(Font.BOLD, 13);
    public static final Font FONT_LABEL     = labelFont(12);
    public static final Font FONT_HINT      = uiFont(Font.PLAIN, 12);
    public static final Font FONT_HEADING   = uiFont(Font.BOLD, 30);
    public static final Font FONT_SUBHEAD   = uiFont(Font.PLAIN, 13);

    public static final Font FONT_MONO_SM  = monoFont(Font.PLAIN, 12);
    public static final Font FONT_MONO_MD  = monoFont(Font.PLAIN, 13);
    public static final Font FONT_MONO_LG  = monoFont(Font.BOLD,  16);

    private static Font uiFont(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    /** "Segoe UI Semibold" is a real installed family on Windows; falls back gracefully elsewhere. */
    private static Font labelFont(int size) {
        Font f = new Font("Segoe UI Semibold", Font.PLAIN, size);
        if (!f.getFamily().equals("Segoe UI Semibold")) {
            f = new Font("Segoe UI", Font.BOLD, size);
        }
        return f;
    }

    private static Font monoFont(int style, int size) {
        return new Font("Consolas", style, size);
    }

    // ── Dimensions ────────────────────────────────────────────────────────────
    public static final int NAV_HEIGHT = 56;
    public static final int RADIUS     = 8;
    public static final int RADIUS_LG  = 12;

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Primary button — solid accent fill, dark readable label
    // ═════════════════════════════════════════════════════════════════════════
    public static JButton button(String text) {
        JButton b = new JButton(text) {
            private float hov = 0f;
            private Timer t;
            {
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { animate(true); }
                    public void mouseExited(MouseEvent e)  { animate(false); }
                });
            }
            void animate(boolean in) {
                if (t != null) t.stop();
                t = new Timer(14, null);
                t.addActionListener(ev -> {
                    hov = in ? Math.min(1f, hov + 0.18f) : Math.max(0f, hov - 0.14f);
                    repaint();
                    if ((in && hov >= 1f) || (!in && hov <= 0f)) t.stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                Color fill = getModel().isPressed() ? ACCENT_DIM : lighten(ACCENT, hov * 0.18f);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_LABEL);
        b.setForeground(TEXT_ON_ACCENT);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Ghost button — outline / secondary action
    // ═════════════════════════════════════════════════════════════════════════
    public static JButton ghostButton(String text) {
        JButton b = new JButton(text) {
            private float hov = 0f;
            private Timer t;
            {
                setContentAreaFilled(false); setFocusPainted(false);
                setBorderPainted(false); setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { anim(true); }
                    public void mouseExited(MouseEvent e)  { anim(false); }
                });
            }
            void anim(boolean in) {
                if (t != null) t.stop();
                t = new Timer(14, null);
                t.addActionListener(ev -> {
                    hov = in ? Math.min(1f, hov + 0.18f) : Math.max(0f, hov - 0.14f);
                    repaint();
                    if ((in && hov >= 1f) || (!in && hov <= 0f)) t.stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (hov > 0) {
                    g2.setColor(new Color(BG_CONTROL.getRed(), BG_CONTROL.getGreen(), BG_CONTROL.getBlue(), (int) (200 * hov)));
                    g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                }
                g2.setColor(blend(BORDER_MID, ACCENT, hov * 0.7f));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_LABEL);
        b.setForeground(TEXT_SECONDARY);
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Help button — the yellow "How to Use" affordance, distinct
    // from the neutral ghost buttons so it always stands out in a toolbar.
    // ═════════════════════════════════════════════════════════════════════════
    public static JButton helpButton(String text) {
        JButton b = new JButton(text) {
            private float hov = 0f;
            private Timer t;
            {
                setContentAreaFilled(false); setFocusPainted(false);
                setBorderPainted(false); setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { anim(true); }
                    public void mouseExited(MouseEvent e)  { anim(false); }
                });
            }
            void anim(boolean in) {
                if (t != null) t.stop();
                t = new Timer(14, null);
                t.addActionListener(ev -> {
                    hov = in ? Math.min(1f, hov + 0.18f) : Math.max(0f, hov - 0.14f);
                    repaint();
                    if ((in && hov >= 1f) || (!in && hov <= 0f)) t.stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(HELP.getRed(), HELP.getGreen(), HELP.getBlue(), (int) (22 + 26 * hov)));
                g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                g2.setColor(new Color(HELP.getRed(), HELP.getGreen(), HELP.getBlue(), (int) (140 + 115 * hov)));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_LABEL);
        b.setForeground(HELP);
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Nav button — flat text with active underline
    // ═════════════════════════════════════════════════════════════════════════
    public static JButton navButton(String text) {
        JButton b = new JButton(text) {
            private float hov = 0f;
            private Timer t;
            {
                setContentAreaFilled(false); setFocusPainted(false);
                setBorderPainted(false); setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        if (getClientProperty("active") != Boolean.TRUE) anim(true);
                    }
                    public void mouseExited(MouseEvent e) {
                        if (getClientProperty("active") != Boolean.TRUE) anim(false);
                    }
                });
            }
            void anim(boolean in) {
                if (t != null) t.stop();
                t = new Timer(14, null);
                t.addActionListener(ev -> {
                    hov = in ? Math.min(1f, hov + 0.2f) : Math.max(0f, hov - 0.16f);
                    repaint();
                    if ((in && hov >= 1f) || (!in && hov <= 0f)) t.stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                boolean active = getClientProperty("active") == Boolean.TRUE;
                if (active) {
                    g2.setColor(ACCENT_DARK);
                    g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                    g2.setColor(ACCENT);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(2, h - 2, w - 2, h - 2);
                } else if (hov > 0) {
                    g2.setColor(new Color(BG_CONTROL.getRed(), BG_CONTROL.getGreen(), BG_CONTROL.getBlue(), (int) (170 * hov)));
                    g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_LABEL);
        b.setForeground(TEXT_SECONDARY);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return b;
    }

    public static void setNavActive(JButton b, boolean active) {
        b.putClientProperty("active", active);
        b.setForeground(active ? TEXT_PRIMARY : TEXT_SECONDARY);
        b.repaint();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Editable text field — rounded, clear focus ring
    // ═════════════════════════════════════════════════════════════════════════
    public static JTextField textField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(BG_CONTROL);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(ACCENT);
        f.setFont(FONT_UI_MD);
        f.setSelectionColor(ACCENT_DARK);
        f.setSelectedTextColor(ACCENT_GLOW);
        applyFieldBorder(f, false);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { applyFieldBorder(f, true); }
            public void focusLost(FocusEvent e)   { applyFieldBorder(f, false); }
        });
        return f;
    }

    private static void applyFieldBorder(JComponent c, boolean focus) {
        Color col = focus ? ACCENT : BORDER_MID;
        c.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(col, focus ? 1.6f : 1f),
                BorderFactory.createEmptyBorder(8, 11, 8, 11)
        ));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Read-only path/value field — used for file & directory pickers
    // ═════════════════════════════════════════════════════════════════════════
    public static JTextField pathField(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setEditable(false);
        f.setFocusable(false);
        f.setFont(FONT_MONO_MD);
        f.setForeground(TEXT_DIM);
        f.setBackground(BG_DEEP);
        f.setCaretColor(TEXT_DIM);
        f.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(BORDER_MID, 1f),
                BorderFactory.createEmptyBorder(8, 11, 8, 11)));
        return f;
    }

    /** Marks a path field as filled with a real value (vs. its placeholder). */
    public static void setPathValue(JTextField field, String value) {
        field.setText(value);
        field.setForeground(TEXT_PRIMARY);
    }

    /**
     * A complete labeled card for a directory/file picker: title, hint,
     * the value field and a Browse button, wired to the given click handler.
     * Replaces the hand-rolled TitledBorder "card" pattern used previously.
     */
    public static JPanel pathCard(String title, String hint, Color accent,
                                   java.util.function.Consumer<JTextField> fieldSetter,
                                   java.util.function.Consumer<JTextField> onBrowse) {
        JTextField field = pathField("(not selected)");
        fieldSetter.accept(field);

        JButton browse = ghostButton("Browse…");
        browse.addActionListener(e -> onBrowse.accept(field));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(field, BorderLayout.CENTER);
        row.add(browse, BorderLayout.EAST);

        return formCard(title, hint, accent, row);
    }

    /**
     * A labeled card wrapping arbitrary content (a field, a row, a checkbox…),
     * styled as a soft rounded panel with a small accent-colored heading.
     */
    public static JPanel formCard(String title, String hint, Color accent, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_ELEVATED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS_LG, RADIUS_LG);
                g2.setColor(BORDER_DIM);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS_LG, RADIUS_LG);
                g2.setColor(accent);
                g2.fillRoundRect(0, 14, 3, Math.max(0, getHeight() - 28), 3, 3);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 16));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_LABEL);
        titleLbl.setForeground(TEXT_PRIMARY);
        head.add(titleLbl);

        if (hint != null && !hint.isBlank()) {
            head.add(Box.createVerticalStrut(3));
            JLabel hintLbl = new JLabel(hint);
            hintLbl.setFont(FONT_HINT);
            hintLbl.setForeground(TEXT_SECONDARY);
            head.add(hintLbl);
        }
        head.add(Box.createVerticalStrut(10));

        card.add(head, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, hint == null ? 96 : 116));
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: TextArea (logs / code / output)
    // ═════════════════════════════════════════════════════════════════════════
    public static JTextArea textArea() {
        JTextArea a = new JTextArea();
        a.setBackground(BG_DEEP);
        a.setForeground(TEXT_PRIMARY);
        a.setCaretColor(ACCENT);
        a.setFont(FONT_MONO_MD);
        a.setSelectionColor(ACCENT_DARK);
        a.setSelectedTextColor(ACCENT_GLOW);
        a.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return a;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Toolbar — flat surface bar for a panel's top action row
    // ═════════════════════════════════════════════════════════════════════════
    public static JPanel toolbar(JComponent left) {
        return toolbar(left, null);
    }

    /** Toolbar with an optional right-aligned component (e.g. a help button), kept clear of the panel edge. */
    public static JPanel toolbar(JComponent left, JComponent right) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BG_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_DIM);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.add(vcenter(left), BorderLayout.WEST);
        if (right != null) {
            JPanel rightWrap = vcenter(right);
            rightWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));
            bar.add(rightWrap, BorderLayout.EAST);
        }
        bar.setPreferredSize(new Dimension(0, NAV_HEIGHT + 4));
        return bar;
    }

    public static JPanel toolRow(JComponent... items) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row.setOpaque(false);
        for (JComponent c : items) row.add(c);
        return row;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Section panel with a flat, colored heading bar
    // ═════════════════════════════════════════════════════════════════════════
    public static JPanel sectionHeader(String title, Color accentColor) {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CONTROL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(accentColor);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                g2.dispose();
            }
        };
        h.setOpaque(false);

        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(accentColor);
        lbl.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        h.add(lbl, BorderLayout.WEST);
        return h;
    }

    /**
     * Full log/output panel: header with title + Clear button, plus the
     * scrollable text area — replaces the repeated buildLogPanel boilerplate.
     */
    public static JPanel logPanel(String title, Color accent, JTextArea logArea, Runnable onClear) {
        JPanel header = sectionHeader(title, accent);

        JButton clearBtn = ghostButton("Clear");
        clearBtn.addActionListener(e -> onClear.run());

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(header, BorderLayout.CENTER);
        JPanel clearWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        clearWrap.setOpaque(false);
        clearWrap.add(clearBtn);
        headerRow.add(clearWrap, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(logArea);
        styleScrollPane(scroll, accent);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_DEEP);
        p.add(headerRow, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Help page — a dedicated "How to Use" view for a tool
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * A full-page help view: a toolbar with a title and a Back button, plus a
     * scrollable HTML body. {@code bodyHtml} is the inner HTML only (no
     * &lt;html&gt;/&lt;body&gt; wrapper needed) — headings/paragraphs/lists
     * are styled automatically to match the theme.
     */
    public static JPanel helpPage(String title, Color accent, String bodyHtml, Runnable onBack) {
        JButton backBtn = ghostButton("←  Back to Tool");
        backBtn.addActionListener(e -> onBack.run());

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_UI_BOLD);
        titleLbl.setForeground(accent);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        JPanel left = toolRow(backBtn, vDivider(), titleLbl);
        left.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        JPanel toolbar = toolbar(left);

        String accentHex = String.format("#%02X%02X%02X", accent.getRed(), accent.getGreen(), accent.getBlue());
        String textHex = String.format("#%02X%02X%02X", TEXT_PRIMARY.getRed(), TEXT_PRIMARY.getGreen(), TEXT_PRIMARY.getBlue());
        String dimHex = String.format("#%02X%02X%02X", TEXT_SECONDARY.getRed(), TEXT_SECONDARY.getGreen(), TEXT_SECONDARY.getBlue());
        String codeHex = String.format("#%02X%02X%02X", BG_CONTROL.getRed(), BG_CONTROL.getGreen(), BG_CONTROL.getBlue());
        String html = "<html><head><style>"
                + "body{font-family:Segoe UI;font-size:12px;color:" + textHex + ";}"
                + "h2{color:" + accentHex + ";font-size:16px;margin-top:4px;margin-bottom:8px;}"
                + "h3{color:" + dimHex + ";font-size:12px;margin-top:18px;margin-bottom:6px;}"
                + "p{color:" + textHex + ";line-height:150%;margin-top:0;margin-bottom:10px;}"
                + "li{color:" + textHex + ";line-height:150%;margin-bottom:6px;}"
                + "code{background-color:" + codeHex + ";color:" + textHex + ";font-family:Consolas;padding:1px 4px;}"
                + "</style></head><body>"
                + bodyHtml
                + "</body></html>";

        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(BG_BASE);
        pane.setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));
        pane.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(pane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_BASE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollPane(scroll, accent);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG_BASE);
        page.add(toolbar, BorderLayout.NORTH);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Split pane — flat, thin divider matching the theme
    // ═════════════════════════════════════════════════════════════════════════
    public static JSplitPane split(JComponent left, JComponent right, int dividerLoc, double resizeWeight) {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        sp.setDividerLocation(dividerLoc);
        sp.setResizeWeight(resizeWeight);
        sp.setDividerSize(4);
        sp.setBorder(null);
        sp.setBackground(BORDER_MID);
        sp.setUI(new BasicSplitPaneUI() {
            @Override public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider d = new BasicSplitPaneDivider(this);
                d.setBackground(BORDER_MID);
                d.setBorder(null);
                return d;
            }
        });
        return sp;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Status bar
    // ═════════════════════════════════════════════════════════════════════════
    public static final class StatusBar {
        public final JPanel panel;
        public final JLabel label;
        private StatusBar(JPanel panel, JLabel label) { this.panel = panel; this.label = label; }
        public void set(String message, Color color) {
            label.setText(message);
            label.setForeground(color);
        }
    }

    public static StatusBar statusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        bar.setBackground(BG_SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_DIM));
        JLabel lbl = new JLabel("Ready");
        lbl.setFont(FONT_UI_SM);
        lbl.setForeground(TEXT_DIM);
        bar.add(lbl);
        return new StatusBar(bar, lbl);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Neon-bordered ScrollPane
    // ═════════════════════════════════════════════════════════════════════════
    public static void styleScrollPane(JScrollPane sp) {
        styleScrollPane(sp, ACCENT);
    }

    public static void styleScrollPane(JScrollPane sp, Color accent) {
        sp.setBorder(BorderFactory.createLineBorder(BORDER_DIM, 1));
        sp.getViewport().setBackground(BG_DEEP);
        sp.setBackground(BG_DEEP);

        for (JScrollBar bar : new JScrollBar[]{sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) {
            bar.setBackground(BG_ELEVATED);
            bar.setUI(new BasicScrollBarUI() {
                @Override protected void configureScrollBarColors() { thumbColor = ACCENT_DIM; trackColor = BG_ELEVATED; }
                @Override protected Dimension getMinimumThumbSize() { return new Dimension(4, 24); }
                @Override protected JButton createDecreaseButton(int o) { return zero(); }
                @Override protected JButton createIncreaseButton(int o) { return zero(); }
                private JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
                @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isDragging ? ACCENT : thumbColor);
                    g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 5, 5);
                    g2.dispose();
                }
                @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(trackColor);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.dispose();
                }
            });
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: ComboBox — fully custom UI so it stays on-theme under any L&F
    // ═════════════════════════════════════════════════════════════════════════
    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        styleComboBox(cb);
        return cb;
    }

    public static void styleComboBox(JComboBox<?> cb) {
        cb.setBackground(BG_CONTROL);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_UI_MD);
        cb.setFocusable(false);
        cb.setOpaque(false);
        cb.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        cb.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton btn = new JButton("▾") {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(TEXT_SECONDARY);
                        g2.setFont(FONT_UI_SM);
                        FontMetrics fm = g2.getFontMetrics();
                        String s = "▾";
                        int tx = (getWidth() - fm.stringWidth(s)) / 2;
                        int ty = (getHeight() + fm.getAscent()) / 2 - 2;
                        g2.drawString(s, tx, ty);
                        g2.dispose();
                    }
                };
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return btn;
            }
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CONTROL);
                g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, RADIUS, RADIUS);
                g2.setColor(hasFocus ? ACCENT : BORDER_MID);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, RADIUS, RADIUS);
                g2.dispose();
            }
            @Override protected ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox) {
                    @Override protected JScrollPane createScroller() {
                        JScrollPane sp = super.createScroller();
                        sp.getViewport().setBackground(BG_ELEVATED);
                        sp.setBorder(BorderFactory.createEmptyBorder());
                        return sp;
                    }
                };
                popup.getList().setBackground(BG_ELEVATED);
                popup.getList().setForeground(TEXT_PRIMARY);
                popup.getList().setSelectionBackground(ACCENT_DARK);
                popup.getList().setSelectionForeground(ACCENT_GLOW);
                popup.getList().setFont(FONT_UI_MD);
                popup.setBorder(BorderFactory.createLineBorder(BORDER_MID, 1));
                return popup;
            }
        });
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                      boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setOpaque(true);
                l.setFont(FONT_UI_MD);
                l.setBorder(BorderFactory.createEmptyBorder(7, 11, 7, 11));
                l.setBackground(isSelected ? ACCENT_DARK : BG_ELEVATED);
                l.setForeground(isSelected ? ACCENT_GLOW : TEXT_PRIMARY);
                return l;
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Separators
    // ═════════════════════════════════════════════════════════════════════════
    public static JSeparator vDivider() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setForeground(BORDER_MID);
        s.setPreferredSize(new Dimension(1, 24));
        return s;
    }
    public static JSeparator hDivider() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER_DIM);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Pill label
    // ═════════════════════════════════════════════════════════════════════════
    public static JLabel pillLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(ACCENT_GLOW);
        l.setOpaque(true);
        l.setBackground(ACCENT_DARK);
        l.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(ACCENT_DIM, 1f),
                BorderFactory.createEmptyBorder(4, 11, 4, 11)));
        return l;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Checkbox with a clean custom icon
    // ═════════════════════════════════════════════════════════════════════════
    public static void styleCheckbox(JCheckBox cb) {
        cb.setBackground(new Color(0, 0, 0, 0));
        cb.setForeground(TEXT_SECONDARY);
        cb.setFont(FONT_UI_MD);
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.setIcon(new Icon() {
            public int getIconWidth() { return 17; } public int getIconHeight() { return 17; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CONTROL); g2.fillRoundRect(x, y, 16, 16, 5, 5);
                g2.setColor(BORDER_MID); g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(x, y, 15, 15, 5, 5);
                g2.dispose();
            }
        });
        cb.setSelectedIcon(new Icon() {
            public int getIconWidth() { return 17; } public int getIconHeight() { return 17; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT); g2.fillRoundRect(x, y, 16, 16, 5, 5);
                g2.setColor(TEXT_ON_ACCENT);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 3, x + 6, x + 13}, new int[]{y + 8, y + 12, y + 4}, 3);
                g2.dispose();
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Layout helper
    // ═════════════════════════════════════════════════════════════════════════
    public static JPanel vcenter(JComponent inner) {
        JPanel w = new JPanel(new GridBagLayout());
        w.setOpaque(false);
        w.add(inner);
        return w;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Color helpers
    // ═════════════════════════════════════════════════════════════════════════
    private static Color lighten(Color c, float amount) {
        return blend(c, Color.WHITE, amount);
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RoundedLineBorder — flat, single-stroke rounded border (fields/cards)
    // ═════════════════════════════════════════════════════════════════════════
    public static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final float width;

        public RoundedLineBorder(Color color, float width) {
            this.color = color;
            this.width = width;
        }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(width));
            g2.drawRoundRect(x, y, w - 1, h - 1, RADIUS, RADIUS);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(2, 2, 2, 2); return i; }
    }

    /** Kept for backward compatibility with older call sites; a soft rounded glow border. */
    public static class NeonBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int glowLayers;

        public NeonBorder(Color color, int glowLayers) {
            this.color = color;
            this.glowLayers = glowLayers;
        }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (glowLayers > 0) {
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                g2.setStroke(new BasicStroke(glowLayers * 1.2f));
                g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, RADIUS_LG, RADIUS_LG);
            }
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, w - 1, h - 1, RADIUS_LG, RADIUS_LG);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(3, 3, 3, 3); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(3, 3, 3, 3); return i; }
    }
}
