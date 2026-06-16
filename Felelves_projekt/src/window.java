import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Path2D;
import java.time.LocalTime;

public class window extends JFrame {

    // Pixel alapú méretek a skálázás helyett
    private int dialSize = 70;
    private int frameThickness = 10;
    private int strapWidth = 40;
    private int strapThickness = 4;
    private int armWidth = 140;
    private int armHeight = 80;

    private int hours = 10;
    private int minutes = 10;

    // Animációhoz tartozó változók
    private double animatedMinutes = 10 * 60 + 10;
    private double targetMinutesForAnim = animatedMinutes;
    private Timer animTimer;
    private boolean isUpdatingTime = false; // Végtelen ciklusok elkerülésére

    private Color dialColor = Color.WHITE;
    private Color strapColor = Color.DARK_GRAY;
    private Color frameColor = Color.LIGHT_GRAY;
    private Color handsColor = Color.BLACK;

    private JSlider dialSlider;
    private JSlider frameThicknessSlider;
    private JSlider timeSlider;

    public window() {
        setTitle("Karóra Konfigurátor Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800); // Kicsit megnövelt ablakmagasság
        setLocationRelativeTo(null);

        // Fő panel GridLayout-tal
        JPanel mainContainer = new JPanel(new GridLayout(2, 2, 10, 10));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(mainContainer);

        // --- ANIMÁCIÓS IDŐZÍTŐ ---
        animTimer = new Timer(16, e -> {
            if (animatedMinutes < targetMinutesForAnim) {
                double diff = targetMinutesForAnim - animatedMinutes;
                double step = Math.max(3.0, diff * 0.1);
                animatedMinutes += step;

                if (animatedMinutes >= targetMinutesForAnim) {
                    animatedMinutes = targetMinutesForAnim;
                    animTimer.stop();
                }
                repaint();
            } else {
                animTimer.stop();
            }
        });

        // ==========================================
        // 1. Oszlop, 1. Sor: Felülnézet
        // ==========================================
        JPanel topViewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int frameSize = dialSize + (2 * frameThickness);

                // Szíj
                g2d.setColor(strapColor);
                g2d.fillRect(cx - (strapWidth / 2), 10, strapWidth, getHeight() - 20);

                // Óratest
                g2d.setColor(frameColor);
                g2d.fillOval(cx - (frameSize / 2), cy - (frameSize / 2), frameSize, frameSize);

                // Számlap
                g2d.setColor(dialColor);
                g2d.fillOval(cx - (dialSize / 2), cy - (dialSize / 2), dialSize, dialSize);

                double hAngle = Math.toRadians((animatedMinutes / 60.0) * 30);
                double mAngle = Math.toRadians((animatedMinutes % 60) * 6);

                int hLength = (int)(18 * (dialSize / 70.0));
                int mLength = (int)(28 * (dialSize / 70.0));

                int hx = cx + (int)(hLength * Math.sin(hAngle));
                int hy = cy - (int)(hLength * Math.cos(hAngle));

                int mx = cx + (int)(mLength * Math.sin(mAngle));
                int my = cy - (int)(mLength * Math.cos(mAngle));

                g2d.setColor(handsColor);
                g2d.setStroke(new BasicStroke(Math.max(1, (int)(3 * (dialSize / 70.0)))));
                g2d.drawLine(cx, cy, hx, hy); // Óramutató

                g2d.setStroke(new BasicStroke(Math.max(1, (int)(2 * (dialSize / 70.0)))));
                g2d.drawLine(cx, cy, mx, my); // Percmutató
            }
        };
        topViewPanel.setBorder(BorderFactory.createTitledBorder("Felülnézet"));
        topViewPanel.setBackground(Color.WHITE);

        // ==========================================
        // 2. Oszlop, 1. Sor: Alulnézet (Hátlap)
        // ==========================================
        JPanel bottomViewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int frameSize = dialSize + (2 * frameThickness);

                g2d.setColor(strapColor);
                g2d.fillRect(cx - (strapWidth / 2), 10, strapWidth, getHeight() - 20);

                g2d.setColor(frameColor);
                g2d.fillOval(cx - (frameSize / 2), cy - (frameSize / 2), frameSize, frameSize);

                int backSize = Math.max(10, frameSize - 20);
                g2d.setColor(Color.GRAY);
                g2d.fillOval(cx - (backSize / 2), cy - (backSize / 2), backSize, backSize);

                int innerBackSize = Math.max(5, backSize - 10);
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawOval(cx - (innerBackSize / 2), cy - (innerBackSize / 2), innerBackSize, innerBackSize);
            }
        };
        bottomViewPanel.setBorder(BorderFactory.createTitledBorder("Alulnézet"));
        bottomViewPanel.setBackground(Color.WHITE);

        // ==========================================
        // 1. Oszlop, 2. Sor: Karon lévő nézet
        // ==========================================
        JPanel armViewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int frameSize = dialSize + (2 * frameThickness);

                // --- STABIL, ÚJ POZICIONÁLÁS ---
                // Megkeressük a kar abszolút legfelső pontját Y tengelyen
                int armTopY = cy - (armHeight / 2);

                // A keret (téglalap) fix magassága, ami mostantól nem függ a kar méretétől!
                int watchBoxHeight = 12 + (frameThickness / 2);
                int watchWidth = frameSize;

                int watchX = cx - (watchWidth / 2);
                // Az óratest alja pontosan rásimul a kar legtetejére (enyhén besüllyesztve a szíjba)
                int watchY = armTopY - watchBoxHeight + 2;

                // 1. Szíj alap (sötétszürke kitöltött ellipszis) a kar körül
                // Ez az a réteg, ami a kar mögött és mellett van, és az óratest le fedi.
                g2d.setColor(strapColor);
                int strapOuterX = cx - (armWidth / 2) - strapThickness;
                int strapOuterY = cy - (armHeight / 2) - strapThickness;
                int strapOuterW = armWidth + (2 * strapThickness);
                int strapOuterH = armHeight + (2 * strapThickness);
                g2d.fillOval(strapOuterX, strapOuterY, strapOuterW, strapOuterH);

                // 2. Kar kirajzolása (bőrszín ellipszis) - a szíj belsejében
                g2d.setColor(new Color(233, 194, 166));
                g2d.fillOval(cx - (armWidth / 2), cy - (armHeight / 2), armWidth, armHeight);

                // 3. Óratest (Téglalap) kirajzolása fixen a kar tetején
                // Ez le fogja fedni a szíj felső részét az óratest területén.
                g2d.setColor(frameColor);
                g2d.fillRect(watchX, watchY, watchWidth, watchBoxHeight);

                // 4. Üveg (domború ív az óratok tetején)
                g2d.setColor(new Color(173, 216, 230, 150));
                int visibleGlassHeight = Math.max(4, (int)(dialSize * 0.075));
                int arcBoundingHeight = visibleGlassHeight * 2;
                int glassWidth = dialSize - 10;
                int glassX = cx - (glassWidth / 2);
                int glassTopY = watchY - visibleGlassHeight;
                g2d.fillArc(glassX, glassTopY, glassWidth, arcBoundingHeight, 0, 180);
            }
        };
        armViewPanel.setBorder(BorderFactory.createTitledBorder("Karon lévő nézet"));
        armViewPanel.setBackground(Color.WHITE);

        // ==========================================
        // 2. Oszlop, 2. Sor: Vezérlők
        // ==========================================
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        controlsPanel.setBorder(BorderFactory.createTitledBorder("Vezérlők"));

        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.insets = new Insets(8, 6, 8, 6);
        cGbc.fill = GridBagConstraints.HORIZONTAL;

        int gridRow = 0;

        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.weightx = 0.2; controlsPanel.add(new JLabel("Számlap (px)"), cGbc);
        dialSlider = new JSlider(40, 150, 70);
        cGbc.gridx = 1; cGbc.weightx = 0.6; controlsPanel.add(dialSlider, cGbc);
        ColorIcon dialIcon = new ColorIcon(dialColor);
        JButton dialColorBtn = new JButton(dialIcon);
        cGbc.gridx = 2; cGbc.weightx = 0.2; controlsPanel.add(dialColorBtn, cGbc);
        dialColorBtn.addActionListener(e -> { Color c = JColorChooser.showDialog(this, "Számlap színe", dialColor); if (c != null) { dialColor = c; dialIcon.setColor(c); repaint(); } });
        dialSlider.addChangeListener(e -> { dialSize = dialSlider.getValue(); repaint(); });
        gridRow++;

        cGbc.gridx = 0; cGbc.gridy = gridRow; controlsPanel.add(new JLabel("Keret (1-15px)"), cGbc);
        frameThicknessSlider = new JSlider(1, 15, 10);
        cGbc.gridx = 1; controlsPanel.add(frameThicknessSlider, cGbc);
        ColorIcon frameIcon = new ColorIcon(frameColor);
        JButton frameColorBtn = new JButton(frameIcon);
        cGbc.gridx = 2; controlsPanel.add(frameColorBtn, cGbc);
        frameColorBtn.addActionListener(e -> { Color c = JColorChooser.showDialog(this, "Keret színe", frameColor); if (c != null) { frameColor = c; frameIcon.setColor(c); repaint(); } });
        frameThicknessSlider.addChangeListener(e -> { frameThickness = frameThicknessSlider.getValue(); repaint(); });
        gridRow++;

        cGbc.gridx = 0; cGbc.gridy = gridRow; controlsPanel.add(new JLabel("Szíj szél. (px)"), cGbc);
        JSlider strapWidthSlider = new JSlider(20, 80, 40);
        cGbc.gridx = 1; controlsPanel.add(strapWidthSlider, cGbc);
        ColorIcon strapIcon = new ColorIcon(strapColor);
        JButton strapColorBtn = new JButton(strapIcon);
        cGbc.gridx = 2; controlsPanel.add(strapColorBtn, cGbc);
        strapWidthSlider.addChangeListener(e -> { strapWidth = strapWidthSlider.getValue(); repaint(); });
        strapColorBtn.addActionListener(e -> { Color c = JColorChooser.showDialog(this, "Szíj színe", strapColor); if (c != null) { strapColor = c; strapIcon.setColor(c); repaint(); } });
        gridRow++;

        cGbc.gridx = 0; cGbc.gridy = gridRow; controlsPanel.add(new JLabel("Szíj vast. (px)"), cGbc);
        JSlider strapThicknessSlider = new JSlider(1, 10, 4);
        cGbc.gridx = 1; controlsPanel.add(strapThicknessSlider, cGbc);
        cGbc.gridx = 2; controlsPanel.add(new JLabel(""), cGbc);
        strapThicknessSlider.addChangeListener(e -> { strapThickness = strapThicknessSlider.getValue(); repaint(); });
        gridRow++;

        cGbc.gridx = 0; cGbc.gridy = gridRow; controlsPanel.add(new JLabel("Kar mérete (px)"), cGbc);
        JSlider armSlider = new JSlider(80, 220, 140);
        cGbc.gridx = 1; controlsPanel.add(armSlider, cGbc);
        cGbc.gridx = 2; controlsPanel.add(new JLabel(""), cGbc);
        armSlider.addChangeListener(e -> { armWidth = armSlider.getValue(); armHeight = (int)(armWidth * (80.0 / 140.0)); repaint(); });
        gridRow++;

        // --- Idő beállító Sor ---
        JPanel timeRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JTextField hourField = new JTextField("10", 3);
        JTextField minuteField = new JTextField("10", 3);
        JButton nowButton = new JButton("Mostani idő");

        timeRowPanel.add(hourField);
        timeRowPanel.add(new JLabel(":"));
        timeRowPanel.add(minuteField);
        timeRowPanel.add(nowButton);

        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.gridwidth = 3; cGbc.weightx = 1.0;
        cGbc.insets = new Insets(15, 6, 4, 6);
        controlsPanel.add(timeRowPanel, cGbc);
        gridRow++;

        timeSlider = new JSlider(0, 1439, hours * 60 + minutes);
        timeSlider.setMajorTickSpacing(120);
        timeSlider.setPaintTicks(true);

        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.gridwidth = 3; cGbc.weightx = 1.0;
        cGbc.insets = new Insets(0, 6, 15, 6);
        controlsPanel.add(timeSlider, cGbc);

        DocumentListener timeListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { parseTimeFromFields(); }
            public void removeUpdate(DocumentEvent e) { parseTimeFromFields(); }
            public void insertUpdate(DocumentEvent e) { parseTimeFromFields(); }

            private void parseTimeFromFields() {
                if (isUpdatingTime) return;
                try {
                    int h = Integer.parseInt(hourField.getText().trim());
                    int m = Integer.parseInt(minuteField.getText().trim());
                    h = Math.max(0, Math.min(23, h));
                    m = Math.max(0, Math.min(59, m));

                    isUpdatingTime = true;
                    timeSlider.setValue(h * 60 + m);
                    isUpdatingTime = false;

                    triggerTimeAnimation(h, m);
                } catch (NumberFormatException ex) {}
            }
        };
        hourField.getDocument().addDocumentListener(timeListener);
        minuteField.getDocument().addDocumentListener(timeListener);

        timeSlider.addChangeListener(e -> {
            if (isUpdatingTime) return;
            int totalMins = timeSlider.getValue();
            int h = totalMins / 60;
            int m = totalMins % 60;

            isUpdatingTime = true;
            hourField.setText(String.format("%02d", h));
            minuteField.setText(String.format("%02d", m));
            isUpdatingTime = false;

            triggerTimeAnimation(h, m);
        });

        nowButton.addActionListener(e -> {
            LocalTime now = LocalTime.now();
            int h = now.getHour();
            int m = now.getMinute();

            isUpdatingTime = true;
            hourField.setText(String.format("%02d", h));
            minuteField.setText(String.format("%02d", m));
            timeSlider.setValue(h * 60 + m);
            isUpdatingTime = false;

            triggerTimeAnimation(h, m);
        });

        gridRow++;
        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.weighty = 1.0;
        controlsPanel.add(Box.createVerticalGlue(), cGbc);

        mainContainer.add(topViewPanel);
        mainContainer.add(bottomViewPanel);
        mainContainer.add(armViewPanel);
        mainContainer.add(controlsPanel);
    }

    private void triggerTimeAnimation(int h, int m) {
        hours = h;
        minutes = m;

        double currentMod = animatedMinutes % 720;
        double targetMod = (h * 60 + m) % 720;

        double diff = targetMod - currentMod;
        if (diff < 0) {
            diff += 720;
        }

        if (diff > 0 || animTimer.isRunning()) {
            targetMinutesForAnim = animatedMinutes + diff;
            if (!animTimer.isRunning()) {
                animTimer.start();
            }
        } else {
            repaint();
        }
    }

    static class ColorIcon implements Icon {
        private Color color;
        public ColorIcon(Color color) { this.color = color; }
        public void setColor(Color color) { this.color = color; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            g.setColor(Color.GRAY);
            g.drawRect(x, y, getIconWidth(), getIconHeight());
        }
        @Override
        public int getIconWidth() { return 16; }
        @Override
        public int getIconHeight() { return 16; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new window().setVisible(true);
        });
    }
}