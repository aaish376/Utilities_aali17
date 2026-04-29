package com.utilities;

import com.utilities.InsertCommandFormatter.InsertCommandFormatterPanel;
import com.utilities.LogStatementFilter.LogStatementFilterPanel;
import com.utilities.UnloadToInsert.UnloadToInsertPanel;
import com.utilities.Theme.Theme;
import com.utilities.FindInJars.FindInJarsPanel;

import javax.swing.*;
import java.awt.*;

public class Launcher extends JFrame {

    public static final String HOME   = "HOME";
    public static final String INSERT = "INSERT";
    public static final String LOG    = "LOG";
    public static final String SCREEN = "SCREEN";
    public static final String UNLOAD = "UNLOAD";
    public static final String JFINDER = "JFINDER";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel body = new JPanel(cardLayout);
    private final java.util.Map<String, JButton> navToolBtns = new java.util.LinkedHashMap<>();

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new Launcher().setVisible(true));
    }

    public Launcher() {
        setTitle("Utilities Hub");
        setSize(1280, 780);
        setMinimumSize(new Dimension(960, 640));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_BASE);
        setLayout(new BorderLayout());
        add(buildNavBar(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        navigateTo(HOME);
    }

    // ── NavBar ────────────────────────────────────────────────────────────────
    private JPanel buildNavBar() {
        // Distinct background: one step darker than BG_SURFACE, with a visible
        // left accent stripe and a stronger neon bottom line to separate it from body.
        final Color NAV_BG      = new Color(0x07080E);   // darker than BG_BASE itself
        final Color NAV_STRIPE  = Theme.ACCENT;           // left edge stripe

        JPanel nav = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                // Fill — noticeably darker than the body's BG_BASE
                g2.setColor(NAV_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle scanlines
                g2.setColor(new Color(0, 0, 0, 25));
                for (int y = 0; y < getHeight(); y += 3)
                    g2.drawLine(0, y, getWidth(), y);

                // Left accent stripe
                g2.setColor(NAV_STRIPE);
                g2.fillRect(0, 0, 3, getHeight());

                // Neon bottom separator — stronger than before
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                for (int i = 1; i <= 6; i++) {
                    g2.setColor(new Color(0, 255, 136, 22 - i * 3));
                    g2.drawLine(0, getHeight() - 1 + i, getWidth(), getHeight() - 1 + i);
                }

                g2.dispose();
            }
        };
        nav.setOpaque(false);
        nav.setPreferredSize(new Dimension(0, Theme.NAV_HEIGHT));

        // ── Left: wordmark + home ─────────────────────────────────────────────
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0)); // extra left pad for stripe

        JLabel wordmark = new JLabel(
                "<html><span style='color:#00FF88'>utils</span>" +
                        "<span style='color:#2A2D40'>hub</span></html>");
        wordmark.setFont(new Font("Courier New", Font.BOLD, 17));
        wordmark.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));

        JButton homeBtn = Theme.navButton("⌂  home");
        homeBtn.addActionListener(e -> navigateTo(HOME));

        left.add(wordmark);
        left.add(Theme.vDivider());
        left.add(homeBtn);

        // ── Right: tool shortcuts ─────────────────────────────────────────────
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));

        String[][] tools = {
                { INSERT, "Insert Formatter" },
                { LOG,    "Log Filter"       },
                { UNLOAD, "Unload→Insert"    },
                { JFINDER, "Jar Inspector" },
        };
        for (String[] t : tools) {
            JButton btn = Theme.navButton(t[1]);
            btn.addActionListener(e -> navigateTo(t[0]));
            navToolBtns.put(t[0], btn);
            right.add(btn);
        }

        nav.add(vcenter(left),  BorderLayout.WEST);
        nav.add(vcenter(right), BorderLayout.EAST);
        return nav;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x07080E));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Top neon line (mirrors navbar bottom)
                g2.setColor(new Color(0, 255, 136, 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(0, 28));

        // Heart + credit — painted so we can use the actual ♥ glyph in red
        JPanel credit = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        credit.setOpaque(false);

        JLabel pre  = new JLabel("with");
        pre.setFont(Theme.FONT_LABEL);
        pre.setForeground(Theme.TEXT_DIM);

        JLabel heart = new JLabel("♥");
        heart.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 12));
        heart.setForeground(new Color(0xFF3355));   // vivid red heart

        JLabel name = new JLabel("ASAD ALI AAiSH");
        name.setFont(Theme.FONT_LABEL);
        name.setForeground(Theme.TEXT_SECONDARY);

        JLabel pipe = new JLabel("|");
        pipe.setFont(Theme.FONT_LABEL);
        pipe.setForeground(Theme.TEXT_DIM);

        JLabel version = new JLabel("version 1.0");
        version.setFont(Theme.FONT_LABEL);
        version.setForeground(Theme.TEXT_DIM);

        credit.add(pre);
        credit.add(heart);
        credit.add(name);
        credit.add(pipe);
        credit.add(version);

        footer.add(vcenter(credit), BorderLayout.CENTER);
        return footer;
    }

    // ── Body ──────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        body.setBackground(Theme.BG_BASE);
        body.add(buildHomePanel(),                      HOME);
        body.add(new InsertCommandFormatterPanel(this),  INSERT);
        body.add(new LogStatementFilterPanel(this),      LOG);
        body.add(new UnloadToInsertPanel(this),          UNLOAD);
        body.add(new FindInJarsPanel(this), JFINDER);
        return body;
    }

    // ── Home ──────────────────────────────────────────────────────────────────
    private JPanel buildHomePanel() {
        JPanel outer = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_BASE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0, 255, 136, 8));
                for (int x = 0; x < getWidth(); x += 28)
                    for (int y = 0; y < getHeight(); y += 28)
                        g2.fillOval(x, y, 2, 2);
                g2.dispose();
            }
        };
        outer.setOpaque(false);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel heading = new JLabel("UTILS HUB");
        heading.setFont(new Font("Courier New", Font.BOLD, 36));
        heading.setForeground(Theme.ACCENT);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("select a utility to launch");
        sub.setFont(Theme.FONT_MONO_SM);
        sub.setForeground(Theme.TEXT_SECONDARY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel underline = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int cx = getWidth() / 2;
                g2.setColor(new Color(0, 255, 136, 30));
                g2.setStroke(new BasicStroke(4f));
                g2.drawLine(cx - 80, 4, cx + 80, 4);
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(cx - 80, 4, cx + 80, 4);
                g2.dispose();
            }
        };
        underline.setOpaque(false);
        underline.setPreferredSize(new Dimension(300, 12));
        underline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));

        inner.add(Box.createVerticalStrut(12));
        inner.add(heading);
        inner.add(Box.createVerticalStrut(6));
        inner.add(underline);
        inner.add(Box.createVerticalStrut(8));
        inner.add(sub);
        inner.add(Box.createVerticalStrut(48));

        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);
        grid.setMaximumSize(new Dimension(700, 320));

        Object[][] cards = {
                { INSERT, "⌗", "INSERT FORMATTER", "Maps INSERT columns\nto values in a table.",  Theme.ACCENT       },
                { LOG,    "≡", "LOG FILTER",        "Filter log files by\nkeyword or level.",       new Color(0x00CCFF) },
                { UNLOAD, "⇄", "UNLOAD→INSERT",     "Convert export files\nback to INSERT SQL.",    new Color(0xFF4488) },
                { JFINDER, "⬡", "JAR INSPECTOR", "Search text across\nJAR class files.", new Color(0xFFAA00) },
        };
        for (Object[] c : cards)
            grid.add(homeCard((String)c[0], (String)c[1], (String)c[2], (String)c[3], (Color)c[4]));

        inner.add(grid);
        outer.add(inner);
        return outer;
    }

    private JPanel homeCard(String view, String icon, String name, String desc, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            float hov = 0f; javax.swing.Timer t;
            {
                setOpaque(false);
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { anim(true);  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
                    public void mouseExited (java.awt.event.MouseEvent e) { anim(false); setCursor(Cursor.getDefaultCursor()); }
                    public void mouseClicked(java.awt.event.MouseEvent e) { navigateTo(view); }
                });
            }
            void anim(boolean in) {
                if (t != null) t.stop();
                t = new javax.swing.Timer(16, null);
                t.addActionListener(ev -> {
                    hov = in ? Math.min(1f, hov+0.1f) : Math.max(0f, hov-0.08f);
                    repaint(); if ((in && hov>=1f) || (!in && hov<=0f)) t.stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(
                        (int)(Theme.BG_ELEVATED.getRed()   + (Theme.BG_CONTROL.getRed()   - Theme.BG_ELEVATED.getRed())   * hov),
                        (int)(Theme.BG_ELEVATED.getGreen() + (Theme.BG_CONTROL.getGreen() - Theme.BG_ELEVATED.getGreen()) * hov),
                        (int)(Theme.BG_ELEVATED.getBlue()  + (Theme.BG_CONTROL.getBlue()  - Theme.BG_ELEVATED.getBlue())  * hov)
                ));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_LG, Theme.RADIUS_LG);
                g2.setColor(new Color(0, 0, 0, 12));
                for (int y = 0; y < getHeight(); y += 3) g2.drawLine(0, y, getWidth(), y);
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(12*i*hov)));
                    g2.setStroke(new BasicStroke(i*1.5f));
                    g2.drawRoundRect(i, i, getWidth()-i*2-1, getHeight()-i*2-1, Theme.RADIUS_LG, Theme.RADIUS_LG);
                }
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(80+175*hov)));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, Theme.RADIUS_LG, Theme.RADIUS_LG);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(180+75*hov)));
                g2.fillRoundRect(0, 0, getWidth(), 3, Theme.RADIUS, Theme.RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setPreferredSize(new Dimension(240, 145));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 22));
        iconLbl.setForeground(accent);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Courier New", Font.BOLD, 13));
        nameLbl.setForeground(Theme.TEXT_PRIMARY);

        JLabel descLbl = new JLabel(
                "<html><body style='width:140px;color:#6B7094;font-family:Courier New;font-size:10px'>"
                        + desc.replace("\n","<br>") + "</body></html>");

        JLabel hint = new JLabel("open →");
        hint.setFont(Theme.FONT_LABEL);
        hint.setForeground(accent);

        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.add(iconLbl, BorderLayout.WEST);
        JPanel center = new JPanel(); center.setOpaque(false); center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(nameLbl); center.add(Box.createVerticalStrut(5)); center.add(descLbl);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false); bottom.add(hint, BorderLayout.EAST);

        card.add(top,    BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel placeholder(String title, String sub) {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(Theme.BG_BASE);
        JPanel box = new JPanel(); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createCompoundBorder(
                new Theme.NeonBorder(new Color(0, 255, 136, 50), 2),
                BorderFactory.createEmptyBorder(8, 40, 8, 40)));
        box.setBackground(Theme.BG_ELEVATED);
        JLabel t = new JLabel(title); t.setFont(Theme.FONT_MONO_LG); t.setForeground(Theme.TEXT_PRIMARY); t.setAlignmentX(CENTER_ALIGNMENT);
        JLabel s = new JLabel(sub);   s.setFont(Theme.FONT_MONO_SM); s.setForeground(Theme.TEXT_SECONDARY); s.setAlignmentX(CENTER_ALIGNMENT);
        JLabel badge = Theme.pillLabel("COMING SOON"); badge.setAlignmentX(CENTER_ALIGNMENT);
        box.add(Box.createVerticalStrut(32)); box.add(t); box.add(Box.createVerticalStrut(10));
        box.add(s); box.add(Box.createVerticalStrut(16)); box.add(badge); box.add(Box.createVerticalStrut(32));
        p.add(box); return p;
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    public void navigateTo(String view) {
        cardLayout.show(body, view);
        navToolBtns.forEach((k, btn) -> Theme.setNavActive(btn, k.equals(view)));
    }

    private JPanel vcenter(JPanel inner) {
        JPanel w = new JPanel(new GridBagLayout()); w.setOpaque(false); w.add(inner); return w;
    }
}