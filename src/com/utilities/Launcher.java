package com.utilities;

import com.utilities.InsertCommandFormatter.InsertCommandFormatterPanel;
// import com.utilities.LogStatementFilter.LogStatementFilterPanel;   // add as you port each tool
// import com.utilities.ScreenAwake.ScreenAwakePanel;
// import com.utilities.UnloadToInsert.UnloadToInsertPanel;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class Launcher extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    public static final Color BG        = new Color(0x0E0F19);
    public static final Color NAV_BG    = new Color(0x13141F);
    public static final Color CARD_BG   = new Color(0x1A1C27);
    public static final Color ACCENT    = new Color(0x27A749);
    public static final Color ACCENT2   = new Color(0x6C63FF);
    public static final Color TEXT      = new Color(0xDDDDEE);
    public static final Color SUBTEXT   = new Color(0x7777AA);

    // ── Card names (used with CardLayout) ─────────────────────────────────────
    public static final String HOME    = "HOME";
    public static final String INSERT  = "INSERT";
    public static final String LOG     = "LOG";
    public static final String SCREEN  = "SCREEN";
    public static final String UNLOAD  = "UNLOAD";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     body       = new JPanel(cardLayout);
    private String           currentView = HOME;

    // Nav quick-nav buttons (we keep refs to style active state)
    private final Map<String, JButton> navToolBtns = new LinkedHashMap<>();

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new Launcher().setVisible(true));
    }

    public Launcher() {
        setTitle("Utilities Hub");
        setSize(1200, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildNavBar(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        navigateTo(HOME);
    }

    // ── NavBar ────────────────────────────────────────────────────────────────
    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(NAV_BG);
        nav.setPreferredSize(new Dimension(0, 48));
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT));

        // Left: Back + Home
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
        left.setBackground(NAV_BG);

        JButton backBtn = navBtn("← Back", false);
        backBtn.addActionListener(e -> navigateTo(HOME));

        JButton homeBtn = navBtn("⌂ Home", false);
        homeBtn.addActionListener(e -> navigateTo(HOME));

        left.add(backBtn);
        left.add(sep());
        left.add(homeBtn);

        // Center: App title
        JLabel title = new JLabel("Utilities Hub");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(ACCENT);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        // Right: Tool shortcut buttons
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        right.setBackground(NAV_BG);

        String[][] tools = {
                { INSERT, "Insert Formatter" },
                { LOG,    "Log Filter"       },
                { SCREEN, "Screen Awake"     },
                { UNLOAD, "Unload→Insert"    },
        };
        for (String[] t : tools) {
            JButton btn = navBtn(t[1], false);
            btn.addActionListener(e -> navigateTo(t[0]));
            navToolBtns.put(t[0], btn);
            right.add(btn);
        }

        nav.add(left,  BorderLayout.WEST);
        nav.add(title, BorderLayout.CENTER);
        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    private JButton navBtn(String text, boolean active) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleNavBtn(b, active);

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(ACCENT.darker().darker());
                b.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                styleNavBtn(b, b.getClientProperty("active") == Boolean.TRUE);
            }
        });
        return b;
    }

    private void styleNavBtn(JButton b, boolean active) {
        b.putClientProperty("active", active);
        b.setBackground(active ? ACCENT.darker() : NAV_BG);
        b.setForeground(active ? Color.WHITE : SUBTEXT);
        b.setBorderPainted(active);
        b.setBorder(active
                ? BorderFactory.createLineBorder(ACCENT, 1, true)
                : BorderFactory.createEmptyBorder(4, 10, 4, 10));
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 24));
        s.setForeground(new Color(0x2A2D40));
        return s;
    }

    // ── Body ──────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        body.setBackground(BG);

        body.add(buildHomePanel(),                HOME);
        body.add(new InsertCommandFormatterPanel(this), INSERT);
        // body.add(new LogStatementFilterPanel(this),    LOG);    // add when ported
        // body.add(new ScreenAwakePanel(this),            SCREEN);
        // body.add(new UnloadToInsertPanel(this),         UNLOAD);

        // Placeholder panels for not-yet-ported tools
        body.add(placeholder("Log Statement Filter — coming soon"),    LOG);
        body.add(placeholder("Screen Awake — coming soon"),            SCREEN);
        body.add(placeholder("Unload → Insert — coming soon"),         UNLOAD);

        return body;
    }

    private JPanel buildHomePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(BG);

        JLabel heading = new JLabel("Choose a Utility");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heading.setForeground(TEXT);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Select any tool below to get started");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(SUBTEXT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(heading);
        inner.add(Box.createVerticalStrut(8));
        inner.add(sub);
        inner.add(Box.createVerticalStrut(40));

        // 2×2 card grid
        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setBackground(BG);

        Object[][] cards = {
                { INSERT, "⌗", "Insert Formatter",   "Maps INSERT columns to values\nin a readable table.",    new Color(0x27A749) },
                { LOG,    "⚙", "Log Filter",          "Filter log files by keyword\nor log level.",             new Color(0x00C9A7) },
                { SCREEN, "◉", "Screen Awake",        "Prevent display sleep\nduring long tasks.",              new Color(0xF5A623) },
                { UNLOAD, "⇄", "Unload → Insert",     "Convert export/unload files\nback to INSERT SQL.",       new Color(0xF06292) },
        };

        for (Object[] c : cards) {
            grid.add(homeCard(
                    (String) c[0], (String) c[1],
                    (String) c[2], (String) c[3], (Color) c[4]
            ));
        }

        inner.add(grid);
        p.add(inner);
        return p;
    }

    private JPanel homeCard(String view, String icon, String name, String desc, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            boolean hovered = false;
            { setOpaque(false);
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e)
                    { hovered = true;  repaint(); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
                    public void mouseExited (java.awt.event.MouseEvent e)
                    { hovered = false; repaint(); setCursor(Cursor.getDefaultCursor()); }
                    public void mouseClicked(java.awt.event.MouseEvent e) { navigateTo(view); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(0x22243A) : CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(hovered ? accent : new Color(0x2A2D3E));
                g2.setStroke(new BasicStroke(hovered ? 2f : 1f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setPreferredSize(new Dimension(220, 130));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 26));
        iconLbl.setForeground(accent);

        JLabel nameLbl = new JLabel("<html>" + name.replace("\n","<br>") + "</html>");
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(TEXT);

        JLabel descLbl = new JLabel("<html><body style='width:140px'>" + desc.replace("\n","<br>") + "</body></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLbl.setForeground(SUBTEXT);

        JLabel hint = new JLabel("Open →");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(accent);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(nameLbl);
        center.add(Box.createVerticalStrut(4));
        center.add(descLbl);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(hint, BorderLayout.EAST);

        card.add(iconLbl, BorderLayout.NORTH);
        card.add(center,  BorderLayout.CENTER);
        card.add(bottom,  BorderLayout.SOUTH);
        return card;
    }

    private JPanel placeholder(String msg) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);
        JLabel l = new JLabel(msg);
        l.setForeground(SUBTEXT);
        l.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        p.add(l);
        return p;
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    public void navigateTo(String view) {
        currentView = view;
        cardLayout.show(body, view);

        // Update active state on nav tool buttons
        navToolBtns.forEach((k, btn) -> styleNavBtn(btn, k.equals(view)));
    }
}