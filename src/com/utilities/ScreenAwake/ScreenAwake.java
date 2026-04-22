package com.utilities.ScreenAwake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenAwake extends JFrame {

    private Robot robot;
    private Timer timer;
    private boolean isRunning = false;
    private int interval = 30; // seconds
    private int tickCount = 0;

    // UI Components
    private JButton toggleBtn;
    private JLabel statusLabel;
    private JLabel countLabel;
    private JSlider intervalSlider;
    private JLabel intervalLabel;
    private JPanel pulsePanel;
    private Timer pulseTimer;
    private float pulseAlpha = 0f;
    private boolean pulseGrowing = true;

    // Colors
    private static final Color BG_DARK      = new Color(15, 17, 26);
    private static final Color BG_CARD      = new Color(22, 25, 40);
    private static final Color ACCENT_ON    = new Color(0, 230, 180);
    private static final Color ACCENT_OFF   = new Color(100, 110, 140);
    private static final Color TEXT_PRIMARY = new Color(220, 225, 245);
    private static final Color TEXT_MUTED   = new Color(100, 110, 150);
    private static final Color BTN_ON       = new Color(0, 200, 160);
    private static final Color BTN_OFF      = new Color(55, 65, 100);

    public ScreenAwake() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null, "Robot init failed: " + e.getMessage());
            System.exit(1);
        }

        setupWindow();
        buildUI();
        setVisible(true);
    }

    private void setupWindow() {
        setTitle("KeepAwake");
        setSize(400, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        getRootPane().setOpaque(false);

        // Draggable window
        final int[] dragStart = new int[2];
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragStart[0] = e.getX();
                dragStart[1] = e.getY();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(getX() + e.getX() - dragStart[0],
                        getY() + e.getY() - dragStart[1]);
            }
        });
    }

    private void buildUI() {
        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_DARK);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        root.setLayout(new BorderLayout());
        root.setOpaque(false);
        setContentPane(root);

        // ── Title Bar ──
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        titleBar.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 16));

        JLabel title = new JLabel("☕  KeepAwake");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);

        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        closeBtn.setForeground(TEXT_MUTED);
        closeBtn.setBackground(null);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setOpaque(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.addActionListener(e -> System.exit(0));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.RED); }
            public void mouseExited(MouseEvent e)  { closeBtn.setForeground(TEXT_MUTED); }
        });

        titleBar.add(title, BorderLayout.WEST);
        titleBar.add(closeBtn, BorderLayout.EAST);

        // ── Center Card ──
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 24, 20, 24));

        // Pulse circle panel
        pulsePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                if (isRunning) {
                    int r = (int)(40 + pulseAlpha * 20);
                    g2.setColor(new Color(0, 230, 180, (int)(60 * (1 - pulseAlpha))));
                    g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                    g2.setColor(new Color(0, 200, 160, 80));
                    g2.fillOval(cx - 42, cy - 42, 84, 84);
                    g2.setColor(ACCENT_ON);
                } else {
                    g2.setColor(ACCENT_OFF);
                }
                g2.fillOval(cx - 32, cy - 32, 64, 64);
                // Icon
                g2.setColor(BG_DARK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                String icon = isRunning ? "◉" : "○";
                g2.drawString(icon, cx - fm.stringWidth(icon)/2, cy + fm.getAscent()/2 - 2);
                g2.dispose();
            }
        };
        pulsePanel.setOpaque(false);
        pulsePanel.setPreferredSize(new Dimension(140, 140));
        pulsePanel.setMaximumSize(new Dimension(200, 140));
        pulsePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status label
        statusLabel = new JLabel("Inactive");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        statusLabel.setForeground(ACCENT_OFF);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Mouse nudge every:");
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLabel.setForeground(TEXT_MUTED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Interval slider
        intervalSlider = new JSlider(5, 120, 30);
        intervalSlider.setOpaque(false);
        intervalSlider.setForeground(ACCENT_ON);
        intervalSlider.setMaximumSize(new Dimension(300, 40));
        intervalSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        intervalLabel = new JLabel("30 seconds");
        intervalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        intervalLabel.setForeground(TEXT_PRIMARY);
        intervalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        intervalSlider.addChangeListener(e -> {
            interval = intervalSlider.getValue();
            intervalLabel.setText(interval + " seconds");
        });

        // Toggle button
        toggleBtn = new JButton("Start") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isRunning ? BTN_ON : BTN_OFF);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(isRunning ? BG_DARK : TEXT_PRIMARY);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth() - fm.stringWidth(t))/2, (getHeight() + fm.getAscent())/2 - 3);
                g2.dispose();
            }
        };
        toggleBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        toggleBtn.setText("▶  Start");
        toggleBtn.setPreferredSize(new Dimension(160, 44));
        toggleBtn.setMaximumSize(new Dimension(200, 44));
        toggleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        toggleBtn.setBorder(BorderFactory.createEmptyBorder());
        toggleBtn.setOpaque(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleBtn.addActionListener(e -> toggleKeepAwake());

        // Count label
        countLabel = new JLabel("Nudges: 0");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(TEXT_MUTED);
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 45, 65));
        sep.setMaximumSize(new Dimension(320, 1));

        center.add(Box.createVerticalStrut(10));
        center.add(pulsePanel);
        center.add(Box.createVerticalStrut(10));
        center.add(statusLabel);
        center.add(Box.createVerticalStrut(18));
        center.add(sep);
        center.add(Box.createVerticalStrut(16));
        center.add(subLabel);
        center.add(Box.createVerticalStrut(6));
        center.add(intervalLabel);
        center.add(Box.createVerticalStrut(4));
        center.add(intervalSlider);
        center.add(Box.createVerticalStrut(20));
        center.add(toggleBtn);
        center.add(Box.createVerticalStrut(12));
        center.add(countLabel);

        // Footer
        JLabel footer = new JLabel("Don't forget to STOP while leaving your computer unattended!");
        footer.setFont(new Font("SansSerif", Font.BOLD, 11));
        footer.setForeground(BTN_ON);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        root.add(titleBar, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
    }

    private void toggleKeepAwake() {
        if (!isRunning) {
            isRunning = true;
            tickCount = 0;
            statusLabel.setText("Active");
            statusLabel.setForeground(ACCENT_ON);
            toggleBtn.setText("■  Stop");
            intervalSlider.setEnabled(false);

            // Pulse animation
            pulseTimer = new Timer();
            pulseTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    pulseAlpha += pulseGrowing ? 0.04f : -0.04f;
                    if (pulseAlpha >= 1f) { pulseAlpha = 1f; pulseGrowing = false; }
                    if (pulseAlpha <= 0f) { pulseAlpha = 0f; pulseGrowing = true; }
                    pulsePanel.repaint();
                }
            }, 0, 30);

            // Mouse nudge timer
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    nudgeMouse();
                    tickCount++;
                    SwingUtilities.invokeLater(() ->
                            countLabel.setText("Nudges: " + tickCount));
                }
            }, interval * 1000L, interval * 1000L);

        } else {
            stopKeepAwake();
        }
        toggleBtn.repaint();
        pulsePanel.repaint();
    }

    private void stopKeepAwake() {
        isRunning = false;
        if (timer != null) { timer.cancel(); timer = null; }
        if (pulseTimer != null) { pulseTimer.cancel(); pulseTimer = null; }
        statusLabel.setText("Inactive");
        statusLabel.setForeground(ACCENT_OFF);
        toggleBtn.setText("▶  Start");
        intervalSlider.setEnabled(true);
        pulsePanel.repaint();
        toggleBtn.repaint();
    }

    private void nudgeMouse() {
        try {
            Point p = MouseInfo.getPointerInfo().getLocation();
            robot.mouseMove(p.x + 1, p.y);
            Thread.sleep(50);
            robot.mouseMove(p.x, p.y);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(ScreenAwake::new);
    }
}
