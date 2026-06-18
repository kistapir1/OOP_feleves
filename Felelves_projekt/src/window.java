import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.*;
import java.time.LocalTime;
import java.util.Scanner;
import javax.swing.filechooser.FileNameExtensionFilter;

// --------------------------------------------------------
// THE MAIN APP (This is the only public class in the file)
// --------------------------------------------------------
public class window extends JFrame {

    public window() {
        setTitle("Karóra Konfigurátor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        WatchModel model = new WatchModel(this::repaint);

        TopViewPanel topView = new TopViewPanel(model);
        BottomViewPanel bottomView = new BottomViewPanel(model);
        ArmViewPanel armView = new ArmViewPanel(model);
        ControlsPanel controls = new ControlsPanel(model, this::repaint);

        // ==========================================
        // MENÜSÁV (Fájl, Méret, Generálás, Súgó)
        // ==========================================
        JMenuBar menuBar = new JMenuBar();

        // --- 1. Fájl ---
        JMenu fileMenu = new JMenu("Fájl");
        JMenuItem saveItem = new JMenuItem("Mentés");
        JMenuItem openItem = new JMenuItem("Megnyitás");
        JMenuItem deleteItem = new JMenuItem("Fájl törlése"); // Frissített név
        fileMenu.add(saveItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(deleteItem);

        // Mentés logikája
        saveItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Konfiguráció mentése");

            // Csak .ora fájlok engedélyezése
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Óra konfiguráció fájlok (*.ora)", "ora");
            fileChooser.setFileFilter(filter);

            if (fileChooser.showSaveDialog(window.this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Ha a felhasználó nem írta be a .ora kiterjesztést, automatikusan hozzáadjuk
                if (!file.getName().toLowerCase().endsWith(".ora")) {
                    file = new File(file.getParentFile(), file.getName() + ".ora");
                }

                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.println(model.getDialSize());
                    writer.println(model.getFrameThickness());
                    writer.println(model.getStrapWidth());
                    writer.println(model.getStrapThickness());
                    writer.println(model.getArmWidth());
                    writer.println(model.getDialColor().getRGB());
                    writer.println(model.getStrapColor().getRGB());
                    writer.println(model.getFrameColor().getRGB());
                    writer.println(model.getHandsColor().getRGB());
                    writer.println(model.isDigital());
                    writer.println(model.isSquare());
                    writer.println(model.getHours());
                    writer.println(model.getMinutes());
                    JOptionPane.showMessageDialog(window.this, "Sikeres mentés a következő fájlba: " + file.getName(), "Infó", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(window.this, "Hiba a mentés során:\n" + ex.getMessage(), "Hiba", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Megnyitás logikája
        openItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Konfiguráció megnyitása");

            // Szűrés, hogy csak .ora fájlokat mutasson
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Óra konfiguráció fájlok (*.ora)", "ora");
            fileChooser.setFileFilter(filter);

            if (fileChooser.showOpenDialog(window.this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (Scanner scanner = new Scanner(file)) {
                    model.setDialSize(Integer.parseInt(scanner.nextLine()));
                    model.setFrameThickness(Integer.parseInt(scanner.nextLine()));
                    model.setStrapWidth(Integer.parseInt(scanner.nextLine()));
                    model.setStrapThickness(Integer.parseInt(scanner.nextLine()));
                    model.setArmWidth(Integer.parseInt(scanner.nextLine()));
                    model.setDialColor(new Color(Integer.parseInt(scanner.nextLine())));
                    model.setStrapColor(new Color(Integer.parseInt(scanner.nextLine())));
                    model.setFrameColor(new Color(Integer.parseInt(scanner.nextLine())));
                    model.setHandsColor(new Color(Integer.parseInt(scanner.nextLine())));
                    model.setDigital(Boolean.parseBoolean(scanner.nextLine()));
                    model.setSquare(Boolean.parseBoolean(scanner.nextLine()));

                    int h = Integer.parseInt(scanner.nextLine());
                    int m = Integer.parseInt(scanner.nextLine());
                    model.triggerTimeAnimation(h, m);

                    controls.updateUIControls();
                    repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(window.this, "Hiba a fájl beolvasásakor. Lehet, hogy sérült vagy nem érvényes .ora fájl.", "Hiba", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Fájl törlése logikája
        deleteItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Fájl törlése");

            // Itt is csak a .ora fájlokat mutatjuk
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Óra konfiguráció fájlok (*.ora)", "ora");
            fileChooser.setFileFilter(filter);

            if (fileChooser.showOpenDialog(window.this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Megerősítő ablak
                int confirm = JOptionPane.showConfirmDialog(
                        window.this,
                        "Biztosan véglegesen törölni akarod ezt a fájlt: " + file.getName() + "?",
                        "Megerősítés",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    if (file.delete()) {
                        JOptionPane.showMessageDialog(window.this, "A fájl sikeresen törölve.");
                    } else {
                        JOptionPane.showMessageDialog(window.this, "Hiba történt a törlés során (lehet, hogy a fájl írásvédett vagy használatban van).", "Hiba", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // --- 2. Méret ---
        JMenu sizeMenu = new JMenu("Méret");
        sizeMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                double armWR = model.getArmWidth() / 2.0;
                double armHR = model.getArmHeight() / 2.0;
                int frameSize = model.getDialSize() + (2 * model.getFrameThickness());
                int szijHossz = (int) (2 * Math.PI * Math.sqrt((armWR * armWR + armHR * armHR) / 2.0));

                String info = String.format(
                        "Szíj hossza (kar kerületéből): ~%d px\n" +
                                "Szíj szélessége: %d px\n" +
                                "Szíj vastagsága (magassága): %d px\n" +
                                "Keret mérete (vastagsága): %d px\n" +
                                "Számlap átmérője: %d px\n" +
                                "Óra átmérője: %d px",
                        szijHossz, model.getStrapWidth(), model.getStrapThickness(),
                        model.getFrameThickness(), model.getDialSize(), frameSize
                );
                JOptionPane.showMessageDialog(window.this, info, "Óra méretei", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // --- 3. Generálás ---
        JMenu genMenu = new JMenu("Generálás");
        JMenuItem colorGenItem = new JMenuItem("Szín generálás");
        JMenuItem sizeGenItem = new JMenuItem("Méret generálás");
        JMenuItem shapeGenItem = new JMenuItem("Alak generálás");
        JMenuItem allGenItem = new JMenuItem("Minden generálás");
        genMenu.add(colorGenItem);
        genMenu.add(sizeGenItem);
        genMenu.add(shapeGenItem);
        genMenu.addSeparator();
        genMenu.add(allGenItem);

        java.util.Random rnd = new java.util.Random();

        colorGenItem.addActionListener(e -> {
            model.setDialColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            model.setStrapColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            model.setFrameColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            model.setHandsColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            controls.updateUIControls();
            repaint();
        });

        sizeGenItem.addActionListener(e -> {
            model.setDialSize(rnd.nextInt(111) + 40);
            model.setFrameThickness(rnd.nextInt(15) + 1);
            model.setStrapWidth(rnd.nextInt(61) + 20);
            model.setStrapThickness(rnd.nextInt(10) + 1);
            model.setArmWidth(rnd.nextInt(141) + 80);
            controls.updateUIControls();
            repaint();
        });

        shapeGenItem.addActionListener(e -> {
            model.setDigital(rnd.nextBoolean());
            model.setSquare(rnd.nextBoolean());
            controls.updateUIControls();
            repaint();
        });

        allGenItem.addActionListener(e -> {
            model.setDialColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            model.setStrapColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            model.setFrameColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            model.setHandsColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            model.setDialSize(rnd.nextInt(111) + 40);
            model.setFrameThickness(rnd.nextInt(15) + 1);
            model.setStrapWidth(rnd.nextInt(61) + 20);
            model.setStrapThickness(rnd.nextInt(10) + 1);
            model.setArmWidth(rnd.nextInt(141) + 80);
            model.setDigital(rnd.nextBoolean());
            model.setSquare(rnd.nextBoolean());
            controls.updateUIControls();
            repaint();
        });

        // --- 4. Súgó ---
        JMenu helpMenu = new JMenu("Súgó");
        helpMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                String helpText =
                        "Az alkalmazás segítségével egy karórát lehet testre szabni. A vezérlőpanelen\n" +
                                "található csúszkák és gombok segítségével méretet, színeket, formát (analóg/digitális,\n" +
                                "kör/négyzet) és az időt állíthatod be.\n\n" +
                                "- Fájl menü: Mentés, megnyitás, fájl törlése.\n" +
                                "- Méret menü: Az óra méreteinek listázása.\n" +
                                "- Generálás menü: Véletlenszerű konfiguráció létrehozása.\n\n" +
                                "A programot készítette: Horváth Márk és Benczúr Gábor.\n";
                JOptionPane.showMessageDialog(window.this, helpText, "Súgó", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menuBar.add(fileMenu);
        menuBar.add(sizeMenu);
        menuBar.add(genMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        JPanel mainContainer = new JPanel(new GridLayout(2, 2, 10, 10));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(mainContainer);

        mainContainer.add(topView);
        mainContainer.add(bottomView);
        mainContainer.add(armView);
        mainContainer.add(controls);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new window().setVisible(true);
        });
    }
}

// THE MODEL
class WatchModel {
    private int dialSize = 70;
    private int frameThickness = 10;
    private int strapWidth = 40;
    private int strapThickness = 4;
    private int armWidth = 140;
    private int armHeight = 80;

    private Color dialColor = Color.WHITE;
    private Color strapColor = Color.DARK_GRAY;
    private Color frameColor = Color.LIGHT_GRAY;
    private Color handsColor = Color.BLACK;

    private boolean isDigital = false;
    private boolean isSquare = false;

    private int hours = 10;
    private int minutes = 10;
    private double animatedMinutes = 10 * 60 + 10;

    private double targetMinutesForAnim = animatedMinutes;
    private Timer animTimer;
    private Runnable updateCallback;

    private Watch currentWatch;

    public WatchModel(Runnable updateCallback) {
        this.updateCallback = updateCallback;
        updateWatchType();

        animTimer = new Timer(16, e -> {
            if (animatedMinutes < targetMinutesForAnim) {
                double diff = targetMinutesForAnim - animatedMinutes;
                double step = Math.max(3.0, diff * 0.1);
                animatedMinutes += step;

                if (animatedMinutes >= targetMinutesForAnim) {
                    animatedMinutes = targetMinutesForAnim;
                    animTimer.stop();
                }
                updateCallback.run();
            } else {
                animTimer.stop();
            }
        });
    }

    public void updateWatchType() {
        if (isDigital) {
            if (isSquare) currentWatch = new SquareDigitalWatch(this);
            else currentWatch = new RoundDigitalWatch(this);
        } else {
            if (isSquare) currentWatch = new SquareAnalogWatch(this);
            else currentWatch = new RoundAnalogWatch(this);
        }
    }

    public void triggerTimeAnimation(int h, int m) {
        this.hours = h;
        this.minutes = m;
        double currentMod = animatedMinutes % 1440;
        double targetMod = (h * 60 + m) % 1440;
        double diff = targetMod - currentMod;
        if (diff < 0) diff += 1440;

        if (diff > 0 || animTimer.isRunning()) {
            targetMinutesForAnim = animatedMinutes + diff;
            if (!animTimer.isRunning()) animTimer.start();
        } else {
            updateCallback.run();
        }
    }

    public Watch getCurrentWatch() { return currentWatch; }
    public int getDialSize() { return dialSize; }
    public void setDialSize(int dialSize) { this.dialSize = dialSize; }
    public int getFrameThickness() { return frameThickness; }
    public void setFrameThickness(int frameThickness) { this.frameThickness = frameThickness; }
    public int getStrapWidth() { return strapWidth; }
    public void setStrapWidth(int strapWidth) { this.strapWidth = strapWidth; }
    public int getStrapThickness() { return strapThickness; }
    public void setStrapThickness(int strapThickness) { this.strapThickness = strapThickness; }
    public int getArmWidth() { return armWidth; }
    public void setArmWidth(int armWidth) { this.armWidth = armWidth; this.armHeight = (int)(armWidth * (80.0 / 140.0)); }
    public int getArmHeight() { return armHeight; }
    public Color getDialColor() { return dialColor; }
    public void setDialColor(Color dialColor) { this.dialColor = dialColor; }
    public Color getStrapColor() { return strapColor; }
    public void setStrapColor(Color strapColor) { this.strapColor = strapColor; }
    public Color getFrameColor() { return frameColor; }
    public void setFrameColor(Color frameColor) { this.frameColor = frameColor; }
    public Color getHandsColor() { return handsColor; }
    public void setHandsColor(Color handsColor) { this.handsColor = handsColor; }
    public boolean isDigital() { return isDigital; }
    public void setDigital(boolean digital) { this.isDigital = digital; updateWatchType(); }
    public boolean isSquare() { return isSquare; }
    public void setSquare(boolean square) { this.isSquare = square; updateWatchType(); }
    public int getHours() { return hours; }
    public int getMinutes() { return minutes; }
    public double getAnimatedMinutes() { return animatedMinutes; }
}

// --------------------------------------------------------
// THE OOP INHERITANCE HIERARCHY
// --------------------------------------------------------
abstract class Watch {
    protected WatchModel model;

    public Watch(WatchModel model) {
        this.model = model;
    }

    public abstract void drawTopView(Graphics2D g2d, int cx, int cy);
    public abstract void drawBottomView(Graphics2D g2d, int cx, int cy);
    public abstract void drawArmView(Graphics2D g2d, int cx, int cy, int armTopY);

    protected void drawAnalogHands(Graphics2D g2d, int cx, int cy) {
        double hAngle = Math.toRadians((model.getAnimatedMinutes() / 60.0) * 30);
        double mAngle = Math.toRadians((model.getAnimatedMinutes() % 60) * 6);

        int hLength = (int) (18 * (model.getDialSize() / 70.0));
        int mLength = (int) (28 * (model.getDialSize() / 70.0));

        int hx = cx + (int) (hLength * Math.sin(hAngle));
        int hy = cy - (int) (hLength * Math.cos(hAngle));
        int mx = cx + (int) (mLength * Math.sin(mAngle));
        int my = cy - (int) (mLength * Math.cos(mAngle));

        g2d.setColor(model.getHandsColor());
        g2d.setStroke(new BasicStroke(Math.max(1, (int) (3 * (model.getDialSize() / 70.0)))));
        g2d.drawLine(cx, cy, hx, hy);

        g2d.setStroke(new BasicStroke(Math.max(1, (int) (2 * (model.getDialSize() / 70.0)))));
        g2d.drawLine(cx, cy, mx, my);
    }

    protected void drawDigitalDisplay(Graphics2D g2d, int cx, int cy) {
        g2d.setColor(model.getHandsColor());
        int currentH = ((int) (model.getAnimatedMinutes() / 60)) % 24;
        int currentM = (int) (model.getAnimatedMinutes() % 60);
        String timeStr = String.format("%02d:%02d", currentH, currentM);

        Font font = new Font("SansSerif", Font.BOLD, Math.max(12, model.getDialSize() / 3));
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(timeStr);
        int textHeight = fm.getAscent();

        g2d.drawString(timeStr, cx - (textWidth / 2), cy + (textHeight / 3));
    }

    protected int drawBaseArmProfile(Graphics2D g2d, int cx, int cy, int armTopY) {
        int watchBoxHeight = 12 + (model.getFrameThickness() / 2);
        int frameSize = model.getDialSize() + (2 * model.getFrameThickness());
        int watchX = cx - (frameSize / 2);
        int watchY = armTopY - watchBoxHeight + 2;

        g2d.setColor(model.getFrameColor());
        g2d.fillRect(watchX, watchY, frameSize, watchBoxHeight);
        return watchY;
    }
}

// --- ABSZTRAKT KEREK ÓRA ---
abstract class RoundWatch extends Watch {
    public RoundWatch(WatchModel model) { super(model); }

    @Override
    public void drawBottomView(Graphics2D g2d, int cx, int cy) {
        int frameSize = model.getDialSize() + (2 * model.getFrameThickness());
        g2d.setColor(model.getFrameColor());
        g2d.fillOval(cx - (frameSize / 2), cy - (frameSize / 2), frameSize, frameSize);

        int backSize = Math.max(10, frameSize - 20);
        g2d.setColor(Color.GRAY);
        g2d.fillOval(cx - (backSize / 2), cy - (backSize / 2), backSize, backSize);

        int innerBackSize = Math.max(5, backSize - 10);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(cx - (innerBackSize / 2), cy - (innerBackSize / 2), innerBackSize, innerBackSize);
    }

    protected void drawRoundTopFrame(Graphics2D g2d, int cx, int cy) {
        int frameSize = model.getDialSize() + (2 * model.getFrameThickness());
        g2d.setColor(model.getFrameColor());
        g2d.fillOval(cx - (frameSize / 2), cy - (frameSize / 2), frameSize, frameSize);

        g2d.setColor(model.getDialColor());
        g2d.fillOval(cx - (model.getDialSize() / 2), cy - (model.getDialSize() / 2), model.getDialSize(), model.getDialSize());
    }
}

// --- ABSZTRAKT SZÖGLETES ÓRA ---
abstract class SquareWatch extends Watch {
    public SquareWatch(WatchModel model) { super(model); }

    @Override
    public void drawBottomView(Graphics2D g2d, int cx, int cy) {
        int frameSize = model.getDialSize() + (2 * model.getFrameThickness());
        g2d.setColor(model.getFrameColor());
        g2d.fillRoundRect(cx - (frameSize / 2), cy - (frameSize / 2), frameSize, frameSize, 20, 20);

        int backSize = Math.max(10, frameSize - 20);
        g2d.setColor(Color.GRAY);
        g2d.fillRoundRect(cx - (backSize / 2), cy - (backSize / 2), backSize, backSize, 10, 10);

        int innerBackSize = Math.max(5, backSize - 10);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(cx - (innerBackSize / 2), cy - (innerBackSize / 2), innerBackSize, innerBackSize, 5, 5);
    }

    protected void drawSquareTopFrame(Graphics2D g2d, int cx, int cy) {
        int frameSize = model.getDialSize() + (2 * model.getFrameThickness());
        g2d.setColor(model.getFrameColor());
        g2d.fillRoundRect(cx - (frameSize / 2), cy - (frameSize / 2), frameSize, frameSize, 20, 20);

        g2d.setColor(model.getDialColor());
        g2d.fillRoundRect(cx - (model.getDialSize() / 2), cy - (model.getDialSize() / 2), model.getDialSize(), model.getDialSize(), 15, 15);
    }
}

// --- KONKRÉT IMPLEMENTÁCIÓK ---

class RoundAnalogWatch extends RoundWatch {
    public RoundAnalogWatch(WatchModel model) { super(model); }
    @Override
    public void drawTopView(Graphics2D g2d, int cx, int cy) {
        drawRoundTopFrame(g2d, cx, cy);
        drawAnalogHands(g2d, cx, cy);
    }
    @Override
    public void drawArmView(Graphics2D g2d, int cx, int cy, int armTopY) {
        int watchY = drawBaseArmProfile(g2d, cx, cy, armTopY);
        g2d.setColor(new Color(173, 216, 230, 150));
        int visibleGlassHeight = Math.max(4, (int) (model.getDialSize() * 0.075));
        g2d.fillArc(cx - ((model.getDialSize() - 10) / 2), watchY - visibleGlassHeight, model.getDialSize() - 10, visibleGlassHeight * 2, 0, 180);
    }
}

class RoundDigitalWatch extends RoundWatch {
    public RoundDigitalWatch(WatchModel model) { super(model); }
    @Override
    public void drawTopView(Graphics2D g2d, int cx, int cy) {
        drawRoundTopFrame(g2d, cx, cy);
        drawDigitalDisplay(g2d, cx, cy);
    }
    @Override
    public void drawArmView(Graphics2D g2d, int cx, int cy, int armTopY) {
        int watchY = drawBaseArmProfile(g2d, cx, cy, armTopY);
        g2d.setColor(new Color(173, 216, 230, 150));
        int visibleGlassHeight = Math.max(4, (int) (model.getDialSize() * 0.075));
        g2d.fillRect(cx - ((model.getDialSize() - 10) / 2), watchY - visibleGlassHeight / 2, model.getDialSize() - 10, visibleGlassHeight / 2);
    }
}

class SquareAnalogWatch extends SquareWatch {
    public SquareAnalogWatch(WatchModel model) { super(model); }
    @Override
    public void drawTopView(Graphics2D g2d, int cx, int cy) {
        drawSquareTopFrame(g2d, cx, cy);
        drawAnalogHands(g2d, cx, cy);
    }
    @Override
    public void drawArmView(Graphics2D g2d, int cx, int cy, int armTopY) {
        int watchY = drawBaseArmProfile(g2d, cx, cy, armTopY);
        g2d.setColor(new Color(173, 216, 230, 150));
        int visibleGlassHeight = Math.max(4, (int) (model.getDialSize() * 0.075));
        g2d.fillArc(cx - ((model.getDialSize() - 10) / 2), watchY - visibleGlassHeight, model.getDialSize() - 10, visibleGlassHeight * 2, 0, 180);
    }
}

class SquareDigitalWatch extends SquareWatch {
    public SquareDigitalWatch(WatchModel model) { super(model); }
    @Override
    public void drawTopView(Graphics2D g2d, int cx, int cy) {
        drawSquareTopFrame(g2d, cx, cy);
        drawDigitalDisplay(g2d, cx, cy);
    }
    @Override
    public void drawArmView(Graphics2D g2d, int cx, int cy, int armTopY) {
        int watchY = drawBaseArmProfile(g2d, cx, cy, armTopY);
        g2d.setColor(new Color(173, 216, 230, 150));
        int visibleGlassHeight = Math.max(4, (int) (model.getDialSize() * 0.075));
        g2d.fillRect(cx - ((model.getDialSize() - 10) / 2), watchY - visibleGlassHeight / 2, model.getDialSize() - 10, visibleGlassHeight / 2);
    }
}

// --------------------------------------------------------
// VIEW PANELS
// --------------------------------------------------------
class TopViewPanel extends JPanel {
    private WatchModel model;
    public TopViewPanel(WatchModel model) { this.model = model; setBorder(BorderFactory.createTitledBorder("Felülnézet")); setBackground(Color.WHITE); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        g2d.setColor(model.getStrapColor());
        g2d.fillRect(cx - (model.getStrapWidth() / 2), 10, model.getStrapWidth(), getHeight() - 20);

        model.getCurrentWatch().drawTopView(g2d, cx, getHeight() / 2);
    }
}

class BottomViewPanel extends JPanel {
    private WatchModel model;
    public BottomViewPanel(WatchModel model) { this.model = model; setBorder(BorderFactory.createTitledBorder("Alulnézet")); setBackground(Color.WHITE); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        g2d.setColor(model.getStrapColor());
        g2d.fillRect(cx - (model.getStrapWidth() / 2), 10, model.getStrapWidth(), getHeight() - 20);

        model.getCurrentWatch().drawBottomView(g2d, cx, getHeight() / 2);
    }
}

class ArmViewPanel extends JPanel {
    private WatchModel model;
    public ArmViewPanel(WatchModel model) { this.model = model; setBorder(BorderFactory.createTitledBorder("Karon lévő nézet")); setBackground(Color.WHITE); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        g2d.setColor(model.getStrapColor());
        g2d.fillOval(cx - (model.getArmWidth() / 2) - model.getStrapThickness(), cy - (model.getArmHeight() / 2) - model.getStrapThickness(), model.getArmWidth() + (2 * model.getStrapThickness()), model.getArmHeight() + (2 * model.getStrapThickness()));

        g2d.setColor(new Color(233, 194, 166));
        g2d.fillOval(cx - (model.getArmWidth() / 2), cy - (model.getArmHeight() / 2), model.getArmWidth(), model.getArmHeight());

        model.getCurrentWatch().drawArmView(g2d, cx, cy, cy - (model.getArmHeight() / 2));
    }
}

// --------------------------------------------------------
// CONTROLS PANEL
// --------------------------------------------------------
class ControlsPanel extends JPanel {
    private WatchModel model;
    private Runnable updateCallback;
    private boolean isUpdatingTime = false;

    private JCheckBox digitalCheck;
    private JCheckBox squareCheck;
    private JSlider dialSlider;
    private ColorIcon dialIcon;
    private JButton dialColorBtn;
    private JSlider frameThicknessSlider;
    private ColorIcon frameIcon;
    private JButton frameColorBtn;
    private JSlider strapWidthSlider;
    private ColorIcon strapIcon;
    private JButton strapColorBtn;
    private JSlider strapThicknessSlider;
    private JSlider armSlider;
    private JTextField hourField;
    private JTextField minuteField;
    private JSlider timeSlider;

    public ControlsPanel(WatchModel model, Runnable updateCallback) {
        this.model = model;
        this.updateCallback = updateCallback;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Vezérlők"));

        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.insets = new Insets(8, 6, 8, 6);
        cGbc.fill = GridBagConstraints.HORIZONTAL;
        int gridRow = 0;

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        digitalCheck = new JCheckBox("Digitális mód", model.isDigital());
        squareCheck = new JCheckBox("Négyzetes forma", model.isSquare());
        checkPanel.add(digitalCheck);
        checkPanel.add(squareCheck);

        digitalCheck.addActionListener(e -> { model.setDigital(digitalCheck.isSelected()); updateCallback.run(); });
        squareCheck.addActionListener(e -> { model.setSquare(squareCheck.isSelected()); updateCallback.run(); });

        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.gridwidth = 3;
        add(checkPanel, cGbc);
        gridRow++;
        cGbc.gridwidth = 1;

        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.weightx = 0.2; add(new JLabel("Számlap"), cGbc);
        dialSlider = new JSlider(40, 150, model.getDialSize());
        cGbc.gridx = 1; cGbc.weightx = 0.6; add(dialSlider, cGbc);
        dialIcon = new ColorIcon(model.getDialColor());
        dialColorBtn = new JButton(dialIcon);
        cGbc.gridx = 2; cGbc.weightx = 0.2; add(dialColorBtn, cGbc);

        dialColorBtn.addActionListener(e -> { Color c = JColorChooser.showDialog(this, "Számlap színe", model.getDialColor()); if (c != null) { model.setDialColor(c); dialIcon.setColor(c); updateCallback.run(); } });
        dialSlider.addChangeListener(e -> { model.setDialSize(dialSlider.getValue()); updateCallback.run(); });
        gridRow++;

        cGbc.gridx = 0; cGbc.gridy = gridRow; add(new JLabel("Keret"), cGbc);
        frameThicknessSlider = new JSlider(1, 15, model.getFrameThickness());
        cGbc.gridx = 1; add(frameThicknessSlider, cGbc);
        frameIcon = new ColorIcon(model.getFrameColor());
        frameColorBtn = new JButton(frameIcon);
        cGbc.gridx = 2; add(frameColorBtn, cGbc);

        frameColorBtn.addActionListener(e -> { Color c = JColorChooser.showDialog(this, "Keret színe", model.getFrameColor()); if (c != null) { model.setFrameColor(c); frameIcon.setColor(c); updateCallback.run(); } });
        frameThicknessSlider.addChangeListener(e -> { model.setFrameThickness(frameThicknessSlider.getValue()); updateCallback.run(); });
        gridRow++;

        cGbc.gridx = 0; cGbc.gridy = gridRow; add(new JLabel("Szíj szél."), cGbc);
        strapWidthSlider = new JSlider(20, 80, model.getStrapWidth());
        cGbc.gridx = 1; add(strapWidthSlider, cGbc);
        strapIcon = new ColorIcon(model.getStrapColor());
        strapColorBtn = new JButton(strapIcon);
        cGbc.gridx = 2; add(strapColorBtn, cGbc);

        strapWidthSlider.addChangeListener(e -> { model.setStrapWidth(strapWidthSlider.getValue()); updateCallback.run(); });
        strapColorBtn.addActionListener(e -> { Color c = JColorChooser.showDialog(this, "Szíj színe", model.getStrapColor()); if (c != null) { model.setStrapColor(c); strapIcon.setColor(c); updateCallback.run(); } });
        gridRow++;

        cGbc.gridx = 0; cGbc.gridy = gridRow; add(new JLabel("Szíj vast."), cGbc);
        strapThicknessSlider = new JSlider(1, 10, model.getStrapThickness());
        cGbc.gridx = 1; add(strapThicknessSlider, cGbc);
        cGbc.gridx = 2; add(new JLabel(""), cGbc);
        strapThicknessSlider.addChangeListener(e -> { model.setStrapThickness(strapThicknessSlider.getValue()); updateCallback.run(); });
        gridRow++;

        cGbc.gridx = 0; cGbc.gridy = gridRow; add(new JLabel("Kar mérete"), cGbc);
        armSlider = new JSlider(80, 220, model.getArmWidth());
        cGbc.gridx = 1; add(armSlider, cGbc);
        cGbc.gridx = 2; add(new JLabel(""), cGbc);
        armSlider.addChangeListener(e -> { model.setArmWidth(armSlider.getValue()); updateCallback.run(); });
        gridRow++;

        JPanel timeRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hourField = new JTextField("10", 3);
        minuteField = new JTextField("10", 3);
        JButton nowButton = new JButton("Mostani idő");
        timeRowPanel.add(hourField); timeRowPanel.add(new JLabel(":")); timeRowPanel.add(minuteField); timeRowPanel.add(nowButton);

        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.gridwidth = 3; cGbc.weightx = 1.0;
        cGbc.insets = new Insets(15, 6, 4, 6); add(timeRowPanel, cGbc);
        gridRow++;

        timeSlider = new JSlider(0, 1439, model.getHours() * 60 + model.getMinutes());
        timeSlider.setMajorTickSpacing(120); timeSlider.setPaintTicks(true);
        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.gridwidth = 3; cGbc.weightx = 1.0;
        cGbc.insets = new Insets(0, 6, 15, 6); add(timeSlider, cGbc);

        DocumentListener timeListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { parseTime(); }
            public void removeUpdate(DocumentEvent e) { parseTime(); }
            public void insertUpdate(DocumentEvent e) { parseTime(); }
            private void parseTime() {
                if (isUpdatingTime) return;
                try {
                    int h = Integer.parseInt(hourField.getText().trim());
                    int m = Integer.parseInt(minuteField.getText().trim());
                    h = Math.max(0, Math.min(23, h)); m = Math.max(0, Math.min(59, m));
                    isUpdatingTime = true; timeSlider.setValue(h * 60 + m); isUpdatingTime = false;
                    model.triggerTimeAnimation(h, m);
                } catch (NumberFormatException ex) {}
            }
        };
        hourField.getDocument().addDocumentListener(timeListener);
        minuteField.getDocument().addDocumentListener(timeListener);

        timeSlider.addChangeListener(e -> {
            if (isUpdatingTime) return;
            int totalMins = timeSlider.getValue();
            int h = totalMins / 60; int m = totalMins % 60;
            isUpdatingTime = true;
            hourField.setText(String.format("%02d", h)); minuteField.setText(String.format("%02d", m));
            isUpdatingTime = false;
            model.triggerTimeAnimation(h, m);
        });

        nowButton.addActionListener(e -> {
            LocalTime now = LocalTime.now();
            int h = now.getHour(); int m = now.getMinute();
            isUpdatingTime = true;
            hourField.setText(String.format("%02d", h)); minuteField.setText(String.format("%02d", m));
            timeSlider.setValue(h * 60 + m);
            isUpdatingTime = false;
            model.triggerTimeAnimation(h, m);
        });

        gridRow++;
        cGbc.gridx = 0; cGbc.gridy = gridRow; cGbc.weighty = 1.0; add(Box.createVerticalGlue(), cGbc);
    }

    public void updateUIControls() {
        digitalCheck.setSelected(model.isDigital());
        squareCheck.setSelected(model.isSquare());

        dialSlider.setValue(model.getDialSize());
        dialIcon.setColor(model.getDialColor());
        dialColorBtn.repaint();

        frameThicknessSlider.setValue(model.getFrameThickness());
        frameIcon.setColor(model.getFrameColor());
        frameColorBtn.repaint();

        strapWidthSlider.setValue(model.getStrapWidth());
        strapIcon.setColor(model.getStrapColor());
        strapColorBtn.repaint();

        strapThicknessSlider.setValue(model.getStrapThickness());
        armSlider.setValue(model.getArmWidth());

        // Betöltésnél és törlésnél frissítsük az idő UI-t is
        isUpdatingTime = true;
        hourField.setText(String.format("%02d", model.getHours()));
        minuteField.setText(String.format("%02d", model.getMinutes()));
        timeSlider.setValue(model.getHours() * 60 + model.getMinutes());
        isUpdatingTime = false;
    }

    private static class ColorIcon implements Icon {
        private Color color;
        public ColorIcon(Color color) { this.color = color; }
        public void setColor(Color color) { this.color = color; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) { g.setColor(color); g.fillRect(x, y, 16, 16); g.setColor(Color.GRAY); g.drawRect(x, y, 16, 16); }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
    }
}