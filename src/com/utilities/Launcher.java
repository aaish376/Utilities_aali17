package com.utilities;

import com.utilities.InsertCommandFormatter.InsertCommandFormatterPanel;
import com.utilities.LogStatementFilter.LogStatementFilterPanel;
import com.utilities.UnloadToInsert.UnloadToInsertPanel;
import com.utilities.Theme.Theme;
import com.utilities.FindInJars.FindInJarsPanel;
import com.utilities.GzLogSearch.GzLogSearchPanel;
import com.utilities.JarLibComparator.JarLibComparatorPanel;

import javax.swing.*;
import java.awt.*;

public class Launcher extends JFrame {

    public static final String HOME   = "HOME";
    public static final String INSERT = "INSERT";
    public static final String LOG    = "LOG";
    public static final String SCREEN = "SCREEN";
    public static final String UNLOAD = "UNLOAD";
    public static final String JFINDER = "JFINDER";
    public static final String GZSEARCH = "GZSEARCH";
    public static final String JARCOMPARE = "JARCOMPARE";

    public static final String INSERT_HELP     = "INSERT_HELP";
    public static final String LOG_HELP        = "LOG_HELP";
    public static final String UNLOAD_HELP     = "UNLOAD_HELP";
    public static final String JFINDER_HELP    = "JFINDER_HELP";
    public static final String GZSEARCH_HELP   = "GZSEARCH_HELP";
    public static final String JARCOMPARE_HELP = "JARCOMPARE_HELP";

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
        setSize(1320, 800);
        setMinimumSize(new Dimension(1000, 660));
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
        JPanel nav = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER_DIM);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        nav.setOpaque(false);
        nav.setPreferredSize(new Dimension(0, Theme.NAV_HEIGHT));

        // ── Left: wordmark + home ─────────────────────────────────────────────
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));

        JLabel wordmark = new JLabel(
                "<html><span style='color:#2DD4A7'>Utils</span>" +
                        "<span style='color:#EDEFF5'>Hub</span></html>");
        wordmark.setFont(new Font("Segoe UI", Font.BOLD, 17));
        wordmark.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        JButton homeBtn = Theme.navButton("⌂  Home");
        homeBtn.addActionListener(e -> navigateTo(HOME));

        left.add(wordmark);
        left.add(Theme.vDivider());
        left.add(homeBtn);

        // ── Right: tool shortcuts ─────────────────────────────────────────────
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));

        String[][] tools = {
                { INSERT, "Insert Formatter" },
                { LOG,    "Log Filter"       },
                { UNLOAD, "UnloadToInsert"  },
                { JFINDER, "Jar Inspector" },
                { GZSEARCH, "GZ Log Search" },
                { JARCOMPARE, "Jar Lib Comparator" },
        };
        for (String[] t : tools) {
            JButton btn = Theme.navButton(t[1]);
            btn.addActionListener(e -> navigateTo(t[0]));
            navToolBtns.put(t[0], btn);
            right.add(btn);
        }

        nav.add(Theme.vcenter(left),  BorderLayout.WEST);
        nav.add(Theme.vcenter(right), BorderLayout.EAST);
        return nav;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.BG_SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_DIM));
        footer.setPreferredSize(new Dimension(0, 30));

        JPanel credit = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        credit.setOpaque(false);

        JLabel pre = new JLabel("Made with");
        pre.setFont(Theme.FONT_UI_SM);
        pre.setForeground(Theme.TEXT_DIM);

        JLabel heart = new JLabel("♥");
        heart.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 12));
        heart.setForeground(new Color(0xFF5D6C));

        JLabel name = new JLabel("by Asad Ali Aaish");
        name.setFont(Theme.FONT_UI_SM);
        name.setForeground(Theme.TEXT_SECONDARY);

        JLabel pipe = new JLabel("·");
        pipe.setFont(Theme.FONT_UI_SM);
        pipe.setForeground(Theme.TEXT_DIM);

        JLabel version = new JLabel("v1.0");
        version.setFont(Theme.FONT_UI_SM);
        version.setForeground(Theme.TEXT_DIM);

        credit.add(pre);
        credit.add(heart);
        credit.add(name);
        credit.add(pipe);
        credit.add(version);

        footer.add(Theme.vcenter(credit), BorderLayout.CENTER);
        return footer;
    }

    // ── Body ──────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        body.setBackground(Theme.BG_BASE);
        body.add(buildHomePanel(), HOME);

        InsertCommandFormatterPanel insertPanel = new InsertCommandFormatterPanel(this);
        body.add(insertPanel, INSERT);
        body.add(insertPanel.createHelpPanel(), INSERT_HELP);

        LogStatementFilterPanel logPanel = new LogStatementFilterPanel(this);
        body.add(logPanel, LOG);
        body.add(logPanel.createHelpPanel(), LOG_HELP);

        UnloadToInsertPanel unloadPanel = new UnloadToInsertPanel(this);
        body.add(unloadPanel, UNLOAD);
        body.add(unloadPanel.createHelpPanel(), UNLOAD_HELP);

        FindInJarsPanel jfinderPanel = new FindInJarsPanel(this);
        body.add(jfinderPanel, JFINDER);
        body.add(jfinderPanel.createHelpPanel(), JFINDER_HELP);

        GzLogSearchPanel gzSearchPanel = new GzLogSearchPanel(this);
        body.add(gzSearchPanel, GZSEARCH);
        body.add(gzSearchPanel.createHelpPanel(), GZSEARCH_HELP);

        JarLibComparatorPanel jarComparePanel = new JarLibComparatorPanel(this);
        body.add(jarComparePanel, JARCOMPARE);
        body.add(jarComparePanel.createHelpPanel(), JARCOMPARE_HELP);

        return body;
    }

    // ── Home ──────────────────────────────────────────────────────────────────
    private JPanel buildHomePanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.BG_BASE);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel heading = new JLabel("Utils Hub");
        heading.setFont(Theme.FONT_HEADING);
        heading.setForeground(Theme.TEXT_PRIMARY);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Pick a utility to get started");
        sub.setFont(Theme.FONT_SUBHEAD);
        sub.setForeground(Theme.TEXT_SECONDARY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(Box.createVerticalStrut(20));
        inner.add(heading);
        inner.add(Box.createVerticalStrut(10));
        inner.add(sub);
        inner.add(Box.createVerticalStrut(40));

        JPanel grid = new JPanel(new GridLayout(2, 3, 22, 22));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);
        grid.setMaximumSize(new Dimension(1100, 340));

        Object[][] cards = {
                { INSERT, "⌗", "Insert Formatter", "Maps INSERT columns to values in a table.",  Theme.ACCENT },
                { LOG,    "≡", "Log Filter",        "Filter log files by keyword or level.",     new Color(0x4DA6FF) },
                { UNLOAD, "⇄", "Unload → Insert",   "Convert export files back to INSERT SQL.",  new Color(0xFF6FA3) },
                { JFINDER, "⬡", "Jar Inspector",    "Search text across JAR class files.",       new Color(0xFFB454) },
                { GZSEARCH, "◈", "GZ Log Search",   "Search inside .gz logs and copy the matches.", new Color(0xFF8A5C) },
                { JARCOMPARE, "⇔", "Jar Lib Comparator", "Diff two jar library folders and export a report.", new Color(0x9B8CFF) },
        };
        for (Object[] c : cards)
            grid.add(homeCard((String) c[0], (String) c[1], (String) c[2], (String) c[3], (Color) c[4]));

        inner.add(grid);
        outer.add(inner);
        return outer;
    }

    private JPanel homeCard(String view, String icon, String name, String desc, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            float hov = 0f; Timer t;
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
                t = new Timer(14, null);
                t.addActionListener(ev -> {
                    hov = in ? Math.min(1f, hov + 0.14f) : Math.max(0f, hov - 0.1f);
                    repaint();
                    if ((in && hov >= 1f) || (!in && hov <= 0f)) t.stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = new Color(
                        (int) (Theme.BG_ELEVATED.getRed()   + (Theme.BG_CONTROL.getRed()   - Theme.BG_ELEVATED.getRed())   * hov),
                        (int) (Theme.BG_ELEVATED.getGreen() + (Theme.BG_CONTROL.getGreen() - Theme.BG_ELEVATED.getGreen()) * hov),
                        (int) (Theme.BG_ELEVATED.getBlue()  + (Theme.BG_CONTROL.getBlue()  - Theme.BG_ELEVATED.getBlue())  * hov)
                );
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_LG, Theme.RADIUS_LG);

                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (70 + 100 * hov)));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS_LG, Theme.RADIUS_LG);

                g2.setColor(accent);
                g2.fillRoundRect(0, 16, 3, Math.max(0, getHeight() - 32), 3, 3);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setPreferredSize(new Dimension(250, 150));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 18));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 24));
        iconLbl.setForeground(accent);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(Theme.FONT_UI_BOLD);
        nameLbl.setForeground(Theme.TEXT_PRIMARY);

        JLabel descLbl = new JLabel(
                "<html><body style='width:150px;color:#9CA3B8;font-family:Segoe UI;font-size:11px'>"
                        + desc.replace("\n", "<br>") + "</body></html>");

        JLabel hint = new JLabel("Open →");
        hint.setFont(Theme.FONT_LABEL);
        hint.setForeground(accent);

        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.add(iconLbl, BorderLayout.WEST);
        JPanel center = new JPanel(); center.setOpaque(false); center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(nameLbl); center.add(Box.createVerticalStrut(6)); center.add(descLbl);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false); bottom.add(hint, BorderLayout.EAST);

        card.add(top,    BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    public void navigateTo(String view) {
        cardLayout.show(body, view);
        navToolBtns.forEach((k, btn) -> Theme.setNavActive(btn, k.equals(view)));
    }
}
