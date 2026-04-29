package com.utilities.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public final class Theme {
    private Theme() {}

    // ── Core Palette ──────────────────────────────────────────────────────────
    public static final Color BG_BASE      = new Color(0x0A0B10);
    public static final Color BG_SURFACE   = new Color(0x0F1018);
    public static final Color BG_ELEVATED  = new Color(0x141720);
    public static final Color BG_CONTROL   = new Color(0x1C1F2E);
    public static final Color BG_DEEP      = new Color(0x07080D);

    // Neon green family
    public static final Color ACCENT       = new Color(0x00FF88);   // pure neon green
    public static final Color ACCENT_DIM   = new Color(0x00A855);   // mid green
    public static final Color ACCENT_DARK  = new Color(0x003D1F);   // fill behind neon
    public static final Color ACCENT_GLOW  = new Color(0x80FFB8);   // bright highlight text

    // Borders
    public static final Color BORDER_DIM   = new Color(0x1A1D2E);
    public static final Color BORDER_MID   = new Color(0x252A3D);
    public static final Color BORDER_NEON  = new Color(0x00FF88);   // neon border full
    public static final Color BORDER_GLOW  = new Color(0x00FF8840, true); // neon glow alpha

    // Text
    public static final Color TEXT_PRIMARY  = new Color(0xE2E4F0);
    public static final Color TEXT_SECONDARY = new Color(0x6B7094);
    public static final Color TEXT_DIM      = new Color(0x3A3D52);
    public static final Color TEXT_ON_ACCENT = new Color(0x001A0D);

    // Semantic
    public static final Color SUCCESS      = new Color(0x00FF88);
    public static final Color ERROR        = new Color(0xFF4466);
    public static final Color WARN         = new Color(0xFFAA00);
    public static final Color INFO         = new Color(0x4488FF);

    // ── Typography ────────────────────────────────────────────────────────────
    public static final Font FONT_MONO_SM  = new Font("Courier New", Font.PLAIN,  11);
    public static final Font FONT_MONO_MD  = new Font("Courier New", Font.PLAIN,  13);
    public static final Font FONT_MONO_LG  = new Font("Courier New", Font.BOLD,   15);
    public static final Font FONT_UI_SM    = new Font("Segoe UI",    Font.PLAIN,  12);
    public static final Font FONT_UI_MD    = new Font("Segoe UI",    Font.PLAIN,  14);
    public static final Font FONT_UI_BOLD  = new Font("Segoe UI",    Font.BOLD,   14);
    public static final Font FONT_LABEL    = new Font("Courier New", Font.BOLD,   10);
    public static final Font FONT_HEADING  = new Font("Courier New", Font.BOLD,   20);
    public static final Font FONT_SUBHEAD  = new Font("Segoe UI",    Font.PLAIN,  12);

    // ── Dimensions ────────────────────────────────────────────────────────────
    public static final int NAV_HEIGHT = 50;
    public static final int RADIUS     = 6;
    public static final int RADIUS_LG  = 10;

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Neon Button (primary action)
    // ═════════════════════════════════════════════════════════════════════════
    public static JButton button(String text) {
        JButton b = new JButton(text) {
            private float glowAlpha = 0f;
            private javax.swing.Timer glowTimer;

            {
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { animateGlow(true); }
                    public void mouseExited (java.awt.event.MouseEvent e) { animateGlow(false); }
                });
            }

            void animateGlow(boolean in) {
                if (glowTimer != null) glowTimer.stop();
                glowTimer = new javax.swing.Timer(16, null);
                glowTimer.addActionListener(ev -> {
                    glowAlpha = in ? Math.min(1f, glowAlpha + 0.12f)
                            : Math.max(0f, glowAlpha - 0.10f);
                    repaint();
                    if ((in && glowAlpha >= 1f) || (!in && glowAlpha <= 0f))
                        glowTimer.stop();
                });
                glowTimer.start();
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Outer glow
                if (glowAlpha > 0) {
                    g2.setColor(new Color(0, 255, 136, (int)(40 * glowAlpha)));
                    g2.fillRoundRect(-3, -3, w+6, h+6, RADIUS_LG+4, RADIUS_LG+4);
                }

                // Fill
                Color fill = getModel().isPressed() ? ACCENT_DARK
                        : new Color(
                        (int)(ACCENT_DARK.getRed()   + (BG_CONTROL.getRed()   - ACCENT_DARK.getRed())   * (1-glowAlpha)),
                        (int)(ACCENT_DARK.getGreen() + (BG_CONTROL.getGreen() - ACCENT_DARK.getGreen()) * (1-glowAlpha)),
                        (int)(ACCENT_DARK.getBlue()  + (BG_CONTROL.getBlue()  - ACCENT_DARK.getBlue())  * (1-glowAlpha))
                );
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, RADIUS_LG, RADIUS_LG);

                // Neon border
                float borderAlpha = 0.5f + 0.5f * glowAlpha;
                g2.setColor(new Color(
                        ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(),
                        (int)(255 * borderAlpha)));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w-1, h-1, RADIUS_LG, RADIUS_LG);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_MONO_SM);
        b.setForeground(ACCENT_GLOW);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Ghost Button (secondary)
    // ═════════════════════════════════════════════════════════════════════════
    public static JButton ghostButton(String text) {
        JButton b = new JButton(text) {
            private float hov = 0f;
            private javax.swing.Timer t;

            {
                setContentAreaFilled(false); setFocusPainted(false);
                setBorderPainted(false); setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { anim(true); }
                    public void mouseExited (java.awt.event.MouseEvent e) { anim(false); }
                });
            }
            void anim(boolean in) {
                if (t != null) t.stop();
                t = new javax.swing.Timer(16, null);
                t.addActionListener(ev -> {
                    hov = in ? Math.min(1f, hov+0.12f) : Math.max(0f, hov-0.10f);
                    repaint();
                    if ((in && hov>=1f)||(!in && hov<=0f)) t.stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(28, 31, 46, (int)(180*hov)));
                g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                g2.setColor(new Color(
                        BORDER_MID.getRed()+(ACCENT.getRed()-BORDER_MID.getRed())*(int)(hov*255)/255,
                        BORDER_MID.getGreen()+(ACCENT.getGreen()-BORDER_MID.getGreen())*(int)(hov*255)/255,
                        BORDER_MID.getBlue()+(ACCENT.getBlue()-BORDER_MID.getBlue())*(int)(hov*255)/255));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w-1, h-1, RADIUS, RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_LABEL);
        b.setForeground(TEXT_SECONDARY);
        b.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Nav Button
    // ═════════════════════════════════════════════════════════════════════════
    public static JButton navButton(String text) {
        JButton b = new JButton(text) {
            private float hov = 0f;
            private javax.swing.Timer t;
            {
                setContentAreaFilled(false); setFocusPainted(false);
                setBorderPainted(false); setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        if (getClientProperty("active") != Boolean.TRUE) anim(true);
                    }
                    public void mouseExited (java.awt.event.MouseEvent e) {
                        if (getClientProperty("active") != Boolean.TRUE) anim(false);
                    }
                });
            }
            void anim(boolean in) {
                if (t != null) t.stop();
                t = new javax.swing.Timer(16, null);
                t.addActionListener(ev -> {
                    hov = in ? Math.min(1f, hov+0.15f) : Math.max(0f, hov-0.12f);
                    repaint();
                    if ((in && hov>=1f)||(!in && hov<=0f)) t.stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                boolean active = getClientProperty("active") == Boolean.TRUE;
                if (active) {
                    // Neon pill background
                    g2.setColor(ACCENT_DARK);
                    g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                    // Neon border with glow layers
//                    for (int i = 3; i >= 1; i--) {
//                        g2.setColor(new Color(0, 255, 136, 18 * i));
//                        g2.setStroke(new BasicStroke(i * 1.5f));
//                        g2.drawRoundRect(i, i, w-i*2-1, h-i*2-1, RADIUS, RADIUS);
//                    }
//                    g2.setColor(BORDER_NEON);
//                    g2.setStroke(new BasicStroke(1.2f));
//                    g2.drawRoundRect(0, 0, w-1, h-1, RADIUS, RADIUS);
                    // Bottom neon underline
                    g2.setColor(ACCENT);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(0, h-2, w, h-2);
                } else if (hov > 0) {
                    g2.setColor(new Color(28, 31, 46, (int)(160*hov)));
                    g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_LABEL);
        b.setForeground(TEXT_SECONDARY);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return b;
    }

    public static void setNavActive(JButton b, boolean active) {
        b.putClientProperty("active", active);
        b.setForeground(TEXT_PRIMARY);
        b.repaint();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: NeonTextField
    // ═════════════════════════════════════════════════════════════════════════
    public static JTextField textField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(BG_DEEP);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(ACCENT);
        f.setFont(FONT_MONO_MD);
        f.setSelectionColor(ACCENT_DARK);
        f.setSelectedTextColor(ACCENT_GLOW);
        applyFieldBorder(f, false);
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) { applyFieldBorder(f, true); }
            public void focusLost (java.awt.event.FocusEvent e) { applyFieldBorder(f, false); }
        });
        return f;
    }

    private static void applyFieldBorder(JComponent c, boolean focus) {
        Color col = focus ? ACCENT : BORDER_MID;
        c.setBorder(BorderFactory.createCompoundBorder(
                new NeonBorder(col, focus ? 3 : 0),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)
        ));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: TextArea
    // ═════════════════════════════════════════════════════════════════════════
    public static JTextArea textArea() {
        JTextArea a = new JTextArea();
        a.setBackground(BG_DEEP);
        a.setForeground(TEXT_PRIMARY);
        a.setCaretColor(ACCENT);
        a.setFont(FONT_MONO_MD);
        a.setSelectionColor(ACCENT_DARK);
        a.setSelectedTextColor(ACCENT_GLOW);
        a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return a;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Section panel with neon-titled header
    // ═════════════════════════════════════════════════════════════════════════
    public static JPanel sectionPanel(String title, Color accentColor) {
        return new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_ELEVATED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS_LG, RADIUS_LG);
                // Neon border
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, RADIUS_LG, RADIUS_LG);
                g2.dispose();
            }
        };
    }

    /** Header bar for a section panel */
    public static JPanel sectionHeader(String title, Color accentColor) {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CONTROL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight()+RADIUS_LG, RADIUS_LG, RADIUS_LG);
                // Bottom accent line
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        h.setOpaque(false);

        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(accentColor);
        lbl.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        h.add(lbl, BorderLayout.WEST);
        return h;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Neon-bordered ScrollPane
    // ═════════════════════════════════════════════════════════════════════════
    public static void styleScrollPane(JScrollPane sp) {
        styleScrollPane(sp, ACCENT);
    }

    public static void styleScrollPane(JScrollPane sp, Color accent) {
        sp.setBorder(new NeonBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50), 0));
        sp.getViewport().setBackground(BG_DEEP);
        sp.setBackground(BG_DEEP);

        for (JScrollBar bar : new JScrollBar[]{sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) {
            bar.setBackground(BG_ELEVATED);
            bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                @Override protected void configureScrollBarColors()
                { thumbColor = ACCENT_DIM; trackColor = BG_ELEVATED; }
                @Override protected Dimension getMinimumThumbSize() { return new Dimension(4, 20); }
                @Override protected JButton createDecreaseButton(int o) { return zero(); }
                @Override protected JButton createIncreaseButton(int o) { return zero(); }
                private JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
                @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isDragging ? ACCENT : thumbColor);
                    g2.fillRoundRect(r.x+2, r.y+2, r.width-4, r.height-4, 4, 4);
                    if (isDragging) {
                        g2.setColor(new Color(0,255,136,40));
                        g2.fillRoundRect(r.x, r.y, r.width, r.height, 4, 4);
                    }
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
    // FACTORY: ComboBox
    // ═════════════════════════════════════════════════════════════════════════
    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setBackground(BG_CONTROL);
        cb.setForeground(ACCENT_GLOW);
        cb.setFont(FONT_MONO_SM);
        cb.setFocusable(false);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_MID, 1));
        return cb;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Separators
    // ═════════════════════════════════════════════════════════════════════════
    public static JSeparator vDivider() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setForeground(BORDER_MID);
        s.setPreferredSize(new Dimension(1, 22));
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
                BorderFactory.createLineBorder(ACCENT_DIM, 1, true),
                BorderFactory.createEmptyBorder(3, 9, 3, 9)));
        return l;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY: Checkbox with neon custom icon
    // ═════════════════════════════════════════════════════════════════════════
    public static void styleCheckbox(JCheckBox cb) {
        cb.setBackground(new Color(0,0,0,0));
        cb.setForeground(TEXT_SECONDARY);
        cb.setFont(FONT_LABEL);
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.setIcon(new Icon() {
            public int getIconWidth() { return 15; } public int getIconHeight() { return 15; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_DEEP); g2.fillRoundRect(x, y, 14, 14, 3, 3);
                g2.setColor(BORDER_MID); g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, 13, 13, 3, 3);
                g2.dispose();
            }
        });
        cb.setSelectedIcon(new Icon() {
            public int getIconWidth() { return 15; } public int getIconHeight() { return 15; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_DARK); g2.fillRoundRect(x, y, 14, 14, 3, 3);
                // Glow
                g2.setColor(new Color(0,255,136,30)); g2.fillRoundRect(x-1, y-1, 16, 16, 4, 4);
                g2.setColor(ACCENT); g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(x, y, 13, 13, 3, 3);
                // Checkmark
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x+2, x+5, x+11}, new int[]{y+7, y+11, y+3}, 3);
                g2.dispose();
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // NeonBorder — painted glow border used on panels and fields
    // ═════════════════════════════════════════════════════════════════════════
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
            // Glow layers
            for (int i = glowLayers; i >= 1; i--) {
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                        Math.min(255, 15 * i)));
                g2.setStroke(new BasicStroke(i * 1.5f));
                g2.drawRoundRect(x+i, y+i, w-i*2-1, h-i*2-1, RADIUS_LG, RADIUS_LG);
            }
            // Crisp 1px border
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, w-1, h-1, RADIUS_LG, RADIUS_LG);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(glowLayers+2, glowLayers+2, glowLayers+2, glowLayers+2); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(glowLayers+2, glowLayers+2, glowLayers+2, glowLayers+2); return i;
        }
    }
}