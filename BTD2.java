import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;

public class BTD2 {
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartMenu());
    }
}

// Start menu window.
class StartMenu extends JFrame {
    public StartMenu() {
        setTitle("BTD2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(BTD2.WINDOW_WIDTH, BTD2.WINDOW_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(null);
        
        JPanel backgroundPanel = new GradientPanel();
        backgroundPanel.setLayout(null);
        add(backgroundPanel);
        
        // Fix title text and bounds.
        JLabel title = new JLabel("Balloons Tower Defense", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 60));
        title.setForeground(Color.WHITE);
        title.setBounds(200, 50, 880, 100); // Wider bounds to show the full title.
        backgroundPanel.add(title);
        
        JButton startButton = createButton("Start Game", 540, 200, e -> startGame());
        JButton settingsButton = createButton("Settings", 540, 280, e -> showSettings());
        JButton exitButton = createButton("Exit Game", 540, 360, e -> System.exit(0));
        backgroundPanel.add(startButton);
        backgroundPanel.add(settingsButton);
        backgroundPanel.add(exitButton);
        
        setVisible(true);
    }
    
    private JButton createButton(String text, int x, int y, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(50, 50, 50));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        button.setBounds(x, y, 200, 50);
        button.addActionListener(action);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(new Color(80, 80, 80)); }
            public void mouseExited(MouseEvent e) { button.setBackground(new Color(50, 50, 50)); }
        });
        return button;
    }
    
    private void startGame() {
        JFrame gameFrame = new JFrame("Game Screen");
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setSize(BTD2.WINDOW_WIDTH, BTD2.WINDOW_HEIGHT);
        gameFrame.setResizable(false);
        gameFrame.add(new GameScreen());
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setVisible(true);
        dispose();
    }
    
    private void showSettings() {
        JOptionPane.showMessageDialog(this, "Settings menu coming soon!", "Settings", JOptionPane.INFORMATION_MESSAGE);
    }
}

class GradientPanel extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        GradientPaint gradient = new GradientPaint(0, 0, Color.DARK_GRAY, 0, getHeight(), Color.BLACK);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
}

// Main game screen.
class GameScreen extends JPanel implements KeyListener, MouseListener, MouseMotionListener, ActionListener {
    public static final int WINDOW_WIDTH = BTD2.WINDOW_WIDTH;
    public static final int WINDOW_HEIGHT = BTD2.WINDOW_HEIGHT;
    
    private Track track = new Track();
    
    // Shop area constants.
    private static final int SHOP_AREA_HEIGHT = 100;
    private static final int SHOP_BUTTON_WIDTH = 200;
    private static final int SHOP_BUTTON_HEIGHT = 80;
    private static final int SHOP_BASIC_X = 50;
    private static final int SHOP_SNIPER_X = 300;
    private static final int SHOP_RAPID_X = 550;
    private static final int SHOP_SHOTGUN_X = 800;
    
    // Tower costs.
    private static final int BASIC_TOWER_COST = 120;
    private static final int SNIPER_TOWER_COST = 180;
    private static final int RAPID_TOWER_COST = 150;
    private static final int SHOTGUN_TOWER_COST = 200;
    
    // Game state.
    private int money = 100000;
    private int lives = 15;
    private int round = 1;
    
    private ArrayList<Tower> towers = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    
    // Enemy spawning.
    private int enemiesToSpawn = 0;
    private static final int SPAWN_DELAY_FRAMES = 10;
    private int spawnDelayCounter = SPAWN_DELAY_FRAMES;
    private Timer gameTimer;
    private Random random = new Random();
    
    // Currently selected tower type.
    // Options: "BASIC", "SNIPER", "RAPID", "SHOTGUN"
    private String selectedTowerType = "BASIC";
    
    // Pause flag.
    private boolean isPaused = false;
    
    // Minimum distance between towers.
    private static final int MIN_TOWER_DISTANCE = 50;
    
    // Upgrade UI fields.
    private Tower selectedTowerForUpgrade = null;
    private UpgradePanel upgradePanel = null;
    
    // Speed multiplier: normally 1, 2 when speed toggle is active.
    private int speedMultiplier = 1;
    private JToggleButton speedToggleButton;
    
    // Game over flag.
    private boolean gameOver = false;
    private Rectangle restartButtonBounds = new Rectangle(WINDOW_WIDTH/2 - 100, WINDOW_HEIGHT/2 + 50, 200, 50);
    
    public GameScreen() {
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        // Use absolute positioning.
        setLayout(null);
        setBackground(new Color(34, 139, 34));
        
        // Create and add speed toggle button at top–right.
        speedToggleButton = new JToggleButton("2x Speed");
        speedToggleButton.setBounds(WINDOW_WIDTH - 120, 10, 100, 30);
        speedToggleButton.setFocusable(false); // Prevent stealing focus.
        speedToggleButton.addActionListener(e -> {
            speedMultiplier = speedToggleButton.isSelected() ? 2 : 1;
        });
        add(speedToggleButton);
        
        // Request focus for the panel so key events (like pause) are processed.
        requestFocusInWindow();
        
        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
        startNewRound();
    }
    
    private void startNewRound() {
        enemiesToSpawn = round * 4;
        spawnDelayCounter = SPAWN_DELAY_FRAMES;
    }
    
    private void spawnOneEnemy() {
        int health = 50 + round * 15;
        double speed = 1.0 + round * 0.15;
        Point start = track.getStartPoint();
        int type = random.nextInt(10);
        if (type < 2) { // BossEnemy
            enemies.add(new BossEnemy(start.x, start.y, health * 3, speed * 0.75));
        } else if (type < 4) { // FastEnemy
            enemies.add(new FastEnemy(start.x, start.y, health / 2, speed * 1.5));
        } else {
            enemies.add(new Enemy(start.x, start.y, health, speed));
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) {
            repaint();
            return;
        }
        if (isPaused) {
            repaint();
            return;
        }
        // Spawn enemies with speed multiplier.
        if (enemiesToSpawn > 0) {
            spawnDelayCounter -= speedMultiplier;
            if (spawnDelayCounter <= 0) {
                spawnOneEnemy();
                enemiesToSpawn--;
                spawnDelayCounter = SPAWN_DELAY_FRAMES;
            }
        }
        // Update towers.
        for (Tower t : towers) {
            Projectile p = t.update();
            if (p != null) {
                projectiles.add(p);
            }
        }
        // Update projectiles.
        Iterator<Projectile> projIter = projectiles.iterator();
        while (projIter.hasNext()) {
            Projectile p = projIter.next();
            p.update();
            if (p.isOffScreen()) {
                projIter.remove();
            }
        }
        // Update enemies.
        Iterator<Enemy> enemyIter = enemies.iterator();
        while (enemyIter.hasNext()) {
            Enemy enemy = enemyIter.next();
            enemy.update();
            if (enemy.reachedEnd(track)) {
                lives--;
                enemyIter.remove();
                if (lives <= 0) {
                    gameOver = true;
                    gameTimer.stop();
                }
            }
        }
        // Collision detection.
        Iterator<Projectile> pIter = projectiles.iterator();
        while (pIter.hasNext()) {
            Projectile p = pIter.next();
            boolean hit = false;
            Iterator<Enemy> enIter = enemies.iterator();
            while (enIter.hasNext()) {
                Enemy enemy = enIter.next();
                if (p.hits(enemy)) {
                    enemy.takeDamage(p.damage);
                    hit = true;
                    if (enemy.health <= 0) {
                        int bonus = p.sourceTower.isWellPositioned(track) ? 20 : 0;
                        money += 75 + round * 10 + bonus;
                        enIter.remove();
                    }
                    break;
                }
            }
            if (hit) {
                pIter.remove();
            }
        }
        if (enemies.isEmpty() && enemiesToSpawn == 0) {
            round++;
            startNewRound();
        }
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw background.
        g.setColor(new Color(34, 139, 34));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        // Draw enemy path.
        track.draw((Graphics2D) g);
        // Draw towers.
        for (Tower t : towers) {
            t.draw(g);
        }
        // Draw enemies.
        for (Enemy enemy : enemies) {
            enemy.draw(g);
        }
        // Draw projectiles.
        for (Projectile p : projectiles) {
            p.draw(g);
        }
        // Draw HUD.
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Round: " + round, 10, 20);
        g.drawString("Money: $" + money, 10, 40);
        g.drawString("Lives: " + lives, 10, 60);
        g.drawString("Press 'P' to Pause/Resume", 10, 80);
        // Draw shop area.
        drawShop(g);
        
        // If game over, draw game over overlay.
        if (gameOver) {
            drawGameOverScreen(g);
        }
    }
    
    private void drawShop(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, WINDOW_HEIGHT - SHOP_AREA_HEIGHT, WINDOW_WIDTH, SHOP_AREA_HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Tower Shop", 10, WINDOW_HEIGHT - SHOP_AREA_HEIGHT + 20);
        Rectangle basicButton = new Rectangle(SHOP_BASIC_X, WINDOW_HEIGHT - SHOP_AREA_HEIGHT + 10, SHOP_BUTTON_WIDTH, SHOP_BUTTON_HEIGHT);
        Rectangle sniperButton = new Rectangle(SHOP_SNIPER_X, WINDOW_HEIGHT - SHOP_AREA_HEIGHT + 10, SHOP_BUTTON_WIDTH, SHOP_BUTTON_HEIGHT);
        Rectangle rapidButton = new Rectangle(SHOP_RAPID_X, WINDOW_HEIGHT - SHOP_AREA_HEIGHT + 10, SHOP_BUTTON_WIDTH, SHOP_BUTTON_HEIGHT);
        Rectangle shotgunButton = new Rectangle(SHOP_SHOTGUN_X, WINDOW_HEIGHT - SHOP_AREA_HEIGHT + 10, SHOP_BUTTON_WIDTH, SHOP_BUTTON_HEIGHT);
        drawShopButton(g, basicButton, "Basic ($" + BASIC_TOWER_COST + ")", selectedTowerType.equals("BASIC"));
        drawShopButton(g, sniperButton, "Sniper ($" + SNIPER_TOWER_COST + ")", selectedTowerType.equals("SNIPER"));
        drawShopButton(g, rapidButton, "Rapid ($" + RAPID_TOWER_COST + ")", selectedTowerType.equals("RAPID"));
        drawShopButton(g, shotgunButton, "Shotgun ($" + SHOTGUN_TOWER_COST + ")", selectedTowerType.equals("SHOTGUN"));
    }
    
    private void drawShopButton(Graphics g, Rectangle rect, String text, boolean selected) {
        g.setColor(selected ? Color.YELLOW : Color.GRAY);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        g.setColor(Color.BLACK);
        g.drawString(text, rect.x + 10, rect.y + 45);
    }
    
    private void drawGameOverScreen(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 64));
        FontMetrics fm = g2d.getFontMetrics();
        String gameOverText = "GAME OVER";
        int textWidth = fm.stringWidth(gameOverText);
        g2d.drawString(gameOverText, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2 - 50);
        g2d.setColor(Color.GRAY);
        g2d.fillRect(restartButtonBounds.x, restartButtonBounds.y, restartButtonBounds.width, restartButtonBounds.height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(restartButtonBounds.x, restartButtonBounds.y, restartButtonBounds.width, restartButtonBounds.height);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String restartText = "Restart";
        int rTextWidth = g2d.getFontMetrics().stringWidth(restartText);
        g2d.drawString(restartText, restartButtonBounds.x + (restartButtonBounds.width - rTextWidth) / 2, restartButtonBounds.y + 33);
    }
    
    private void restartGame() {
        money = 400;
        lives = 15;
        round = 1;
        towers.clear();
        enemies.clear();
        projectiles.clear();
        removeUpgradePanel();
        gameOver = false;
        speedMultiplier = speedToggleButton.isSelected() ? 2 : 1;
        startNewRound();
        gameTimer.start();
    }
    
    private boolean canPlaceTower(int x, int y) {
        for (Tower t : towers) {
            int dx = t.x - x;
            int dy = t.y - y;
            if (dx * dx + dy * dy < MIN_TOWER_DISTANCE * MIN_TOWER_DISTANCE) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        if (gameOver) {
            if (restartButtonBounds.contains(mx, my)) {
                restartGame();
            }
            return;
        }
        if (my >= WINDOW_HEIGHT - SHOP_AREA_HEIGHT) {
            if (mx >= SHOP_BASIC_X && mx <= SHOP_BASIC_X + SHOP_BUTTON_WIDTH) {
                selectedTowerType = "BASIC";
            } else if (mx >= SHOP_SNIPER_X && mx <= SHOP_SNIPER_X + SHOP_BUTTON_WIDTH) {
                selectedTowerType = "SNIPER";
            } else if (mx >= SHOP_RAPID_X && mx <= SHOP_RAPID_X + SHOP_BUTTON_WIDTH) {
                selectedTowerType = "RAPID";
            } else if (mx >= SHOP_SHOTGUN_X && mx <= SHOP_SHOTGUN_X + SHOP_BUTTON_WIDTH) {
                selectedTowerType = "SHOTGUN";
            }
            repaint();
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            for (Tower t : towers) {
                if (t.contains(mx, my)) {
                    selectedTowerForUpgrade = t;
                    openUpgradePanel(t);
                    return;
                }
            }
        } else if (SwingUtilities.isRightMouseButton(e)) {
            // If the click is not near the path and towers are not overlapping,
            // then place the tower. Otherwise, silently do nothing.
            if (!track.isNearPath(mx, my) && canPlaceTower(mx, my)) {
                int cost = 0;
                Tower newTower = null;
                switch (selectedTowerType) {
                    case "BASIC":
                        cost = BASIC_TOWER_COST;
                        newTower = new Tower(mx, my);
                        break;
                    case "SNIPER":
                        cost = SNIPER_TOWER_COST;
                        newTower = new SniperTower(mx, my);
                        break;
                    case "RAPID":
                        cost = RAPID_TOWER_COST;
                        newTower = new RapidFireTower(mx, my);
                        break;
                    case "SHOTGUN":
                        cost = SHOTGUN_TOWER_COST;
                        newTower = new ShotgunTower(mx, my);
                        break;
                }
                if (money >= cost) {
                    money -= cost;
                    towers.add(newTower);
                }
            }
            // No pop-up is shown if the placement is invalid.
        }
    }
    
    private void openUpgradePanel(Tower tower) {
        removeUpgradePanel();
        upgradePanel = new UpgradePanel(tower);
        // Position the panel below the HUD (y = 100).
        upgradePanel.setBounds(10, 100, 280, 150);
        add(upgradePanel);
        upgradePanel.repaint();
        revalidate();
    }
    
    private void removeUpgradePanel() {
        if (upgradePanel != null) {
            remove(upgradePanel);
            upgradePanel = null;
            selectedTowerForUpgrade = null;
            revalidate();
            repaint();
        }
    }
    
    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
    @Override public void mouseDragged(MouseEvent e) { }
    @Override public void mouseMoved(MouseEvent e) { }
    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_P) {
            isPaused = !isPaused;
            // Request focus to ensure key events remain handled by the panel.
            requestFocusInWindow();
        }
    }
    @Override public void keyReleased(KeyEvent e) { }
    
    // --- Upgrade Panel Inner Class ---
    class UpgradePanel extends JPanel implements ActionListener {
        private Tower tower;
        private JButton damageButton, rangeButton, fireRateButton, closeButton;
        public UpgradePanel(Tower tower) {
            this.tower = tower;
            setBackground(new Color(220, 220, 220, 230));
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            setLayout(new GridLayout(4, 1, 5, 5));
            damageButton = new JButton();
            rangeButton = new JButton();
            fireRateButton = new JButton();
            closeButton = new JButton("Close");
            damageButton.addActionListener(this);
            rangeButton.addActionListener(this);
            fireRateButton.addActionListener(this);
            closeButton.addActionListener(this);
            updateButtons();
            add(damageButton);
            add(rangeButton);
            add(fireRateButton);
            add(closeButton);
        }
        private int getUpgradeCost(int currentLevel) {
            return 50 + currentLevel * 25;
        }
        private void updateButtons() {
            damageButton.setText("Upgrade Damage (Lv " + tower.damageUpgradeLevel +
                ") Cost: $" + getUpgradeCost(tower.damageUpgradeLevel));
            rangeButton.setText("Upgrade Range (Lv " + tower.rangeUpgradeLevel +
                ") Cost: $" + getUpgradeCost(tower.rangeUpgradeLevel));
            fireRateButton.setText("Upgrade Fire Rate (Lv " + tower.fireRateUpgradeLevel +
                ") Cost: $" + getUpgradeCost(tower.fireRateUpgradeLevel));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == damageButton) {
                if (tower.damageUpgradeLevel < 5 && money >= getUpgradeCost(tower.damageUpgradeLevel)) {
                    money -= getUpgradeCost(tower.damageUpgradeLevel);
                    tower.damage += 10;
                    tower.damageUpgradeLevel++;
                    updateButtons();
                }
            } else if (e.getSource() == rangeButton) {
                if (tower.rangeUpgradeLevel < 5 && money >= getUpgradeCost(tower.rangeUpgradeLevel)) {
                    money -= getUpgradeCost(tower.rangeUpgradeLevel);
                    tower.range += 15;
                    tower.rangeUpgradeLevel++;
                    updateButtons();
                }
            } else if (e.getSource() == fireRateButton) {
                if (tower.fireRateUpgradeLevel < 5 && money >= getUpgradeCost(tower.fireRateUpgradeLevel)) {
                    money -= getUpgradeCost(tower.fireRateUpgradeLevel);
                    if (tower.fireRate > 20) { tower.fireRate -= 5; }
                    tower.fireRateUpgradeLevel++;
                    updateButtons();
                }
            } else if (e.getSource() == closeButton) {
                removeUpgradePanel();
            }
        }
    }
    
    // --- Tower and Derived Classes ---
    class Tower {
        int x, y, range, damage, fireRate, cooldownCounter, level, size;
        int damageUpgradeLevel, rangeUpgradeLevel, fireRateUpgradeLevel;
        public Tower(int x, int y) {
            this.x = x; this.y = y;
            level = 1; range = 100; damage = 20; fireRate = 60;
            cooldownCounter = 0; size = 40;
            damageUpgradeLevel = 0;
            rangeUpgradeLevel = 0;
            fireRateUpgradeLevel = 0;
        }
        public Projectile update() {
            if (cooldownCounter > 0) {
                cooldownCounter -= speedMultiplier;
            } else {
                Enemy target = null;
                double minDistance = Double.MAX_VALUE;
                for (Enemy enemy : enemies) {
                    double dx = enemy.x - x, dy = enemy.y - y;
                    double distSq = dx * dx + dy * dy;
                    if (distSq <= range * range && distSq < minDistance) {
                        target = enemy;
                        minDistance = distSq;
                    }
                }
                if (target != null) {
                    double angle = Math.atan2(target.y - y, target.x - x);
                    double projSpeed = 8;
                    double dx = projSpeed * Math.cos(angle);
                    double dy = projSpeed * Math.sin(angle);
                    cooldownCounter = fireRate;
                    return new Projectile(x, y, dx, dy, damage, this);
                }
            }
            return null;
        }
        public void draw(Graphics g) {
            g.setColor(Color.GRAY);
            g.fillOval(x - size/2, y - size/2, size, size);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("Lv" + level, x - size/4, y);
            if (GameScreen.this.selectedTowerForUpgrade == this) {
                g.setColor(new Color(0, 255, 0, 50));
                g.drawOval(x - range, y - range, range * 2, range * 2);
            }
        }
        public boolean contains(int mx, int my) {
            int dx = mx - x, dy = my - y;
            return dx * dx + dy * dy <= (size/2) * (size/2);
        }
        public boolean isWellPositioned(Track track) {
            int threshold = 60;
            for (Point wp : track.getPoints()) {
                if (Math.hypot(x - wp.x, y - wp.y) < threshold) {
                    return true;
                }
            }
            return false;
        }
    }
    
    class SniperTower extends Tower {
        public SniperTower(int x, int y) {
            super(x, y);
            range = 150; damage = 3; fireRate = 0;
        }
    }
    
    class RapidFireTower extends Tower {
        public RapidFireTower(int x, int y) {
            super(x, y);
            range = 90; damage = 1; fireRate = 0;
        }
    }
    
    class ShotgunTower extends Tower {
        public ShotgunTower(int x, int y) {
            super(x, y);
            range = 80; damage = 1; fireRate = 0;
        }
        @Override
        public Projectile update() {
            if (cooldownCounter > 0) {
                cooldownCounter -= speedMultiplier;
            } else {
                Enemy target = null;
                double minDistance = Double.MAX_VALUE;
                for (Enemy enemy : enemies) {
                    double dx = enemy.x - x, dy = enemy.y - y;
                    double distSq = dx * dx + dy * dy;
                    if (distSq <= range * range && distSq < minDistance) {
                        target = enemy;
                        minDistance = distSq;
                    }
                }
                if (target != null) {
                    cooldownCounter = fireRate;
                    double baseAngle = Math.atan2(target.y - y, target.x - x);
                    for (int i = -2; i <= 2; i++) {
                        double angle = baseAngle + i * Math.toRadians(10);
                        double projSpeed = 7;
                        double dx = projSpeed * Math.cos(angle);
                        double dy = projSpeed * Math.sin(angle);
                        projectiles.add(new Projectile(x, y, dx, dy, damage, this));
                    }
                }
            }
            return null;
        }
    }
    
    // --- Enemy Classes ---
    class Enemy {
        double x, y; int health; double speed; int size; int waypointIndex;
        public Enemy(double x, double y, int health, double speed) {
            this.x = x; this.y = y; this.health = health; this.speed = speed;
            size = 30; waypointIndex = 0;
        }
        public void update() {
            if (waypointIndex < track.getPoints().size()) {
                Point target = track.getPoints().get(waypointIndex);
                double dx = target.x - x, dy = target.y - y;
                double distance = Math.hypot(dx, dy);
                if (distance < speed * speedMultiplier) {
                    x = target.x; y = target.y; waypointIndex++;
                } else {
                    x += speed * speedMultiplier * dx / distance;
                    y += speed * speedMultiplier * dy / distance;
                }
            }
        }
        public void takeDamage(int dmg) { health -= dmg; }
        public boolean reachedEnd(Track track) { return waypointIndex >= track.getPoints().size(); }
        public void draw(Graphics g) {
            g.setColor(Color.MAGENTA);
            g.fillOval((int)x - size/2, (int)y - size/2, size, size);
            g.setColor(Color.RED);
            int maxHealth = 50 + round * 15;
            int barWidth = (int)((double)health / maxHealth * size);
            g.fillRect((int)x - size/2, (int)y - size/2 - 10, barWidth, 5);
        }
    }
    
    class BossEnemy extends Enemy {
        public BossEnemy(double x, double y, int health, double speed) {
            super(x, y, health, speed);
            size = 50;
        }
        @Override
        public void draw(Graphics g) {
            g.setColor(Color.ORANGE);
            g.fillOval((int)x - size/2, (int)y - size/2, size, size);
            g.setColor(Color.RED);
            int maxHealth = (50 + round * 15) * 3;
            int barWidth = (int)((double)health / maxHealth * size);
            g.fillRect((int)x - size/2, (int)y - size/2 - 10, barWidth, 5);
        }
    }
    
    class FastEnemy extends Enemy {
        public FastEnemy(double x, double y, int health, double speed) {
            super(x, y, health, speed);
            size = 25;
        }
        @Override
        public void draw(Graphics g) {
            g.setColor(Color.CYAN);
            g.fillOval((int)x - size/2, (int)y - size/2, size, size);
            g.setColor(Color.RED);
            int maxHealth = (50 + round * 15) / 2;
            int barWidth = (int)((double)health / maxHealth * size);
            g.fillRect((int)x - size/2, (int)y - size/2 - 10, barWidth, 5);
        }
    }
    
    // --- Projectile Class ---
    class Projectile {
        double x, y, dx, dy; int damage, size; Tower sourceTower;
        public Projectile(double x, double y, double dx, double dy, int damage, Tower sourceTower) {
            this.x = x; this.y = y; this.dx = dx; this.dy = dy;
            this.damage = damage; this.sourceTower = sourceTower;
            size = 8;
        }
        public void update() { 
            x += dx * speedMultiplier; 
            y += dy * speedMultiplier; 
        }
        public boolean isOffScreen() { return x < 0 || x > WINDOW_WIDTH || y < 0 || y > WINDOW_HEIGHT; }
        public boolean hits(Enemy enemy) {
            double dx = x - enemy.x, dy = y - enemy.y;
            return dx * dx + dy * dy <= Math.pow(enemy.size / 2, 2);
        }
        public void draw(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillOval((int)x - size/2, (int)y - size/2, size, size);
        }
    }
}

// Track class.
class Track {
    private ArrayList<Point> pathPoints;
    private static final int PATH_PROXIMITY_THRESHOLD = 30;
    public Track() {
        pathPoints = new ArrayList<>();
        pathPoints.add(new Point(0, 250));
        pathPoints.add(new Point(300, 250));
        pathPoints.add(new Point(300, 550));
        pathPoints.add(new Point(600, 550));
        pathPoints.add(new Point(600, 150));
        pathPoints.add(new Point(900, 150));
        pathPoints.add(new Point(900, 450));
        pathPoints.add(new Point(1200, 450));
        pathPoints.add(new Point(1200, 300));
        pathPoints.add(new Point(BTD2.WINDOW_WIDTH, 300));
    }
    public Point getStartPoint() { return pathPoints.get(0); }
    public ArrayList<Point> getPoints() { return pathPoints; }
    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(50, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point p1 = pathPoints.get(i);
            Point p2 = pathPoints.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }
    public boolean isNearPath(int x, int y) {
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point p1 = pathPoints.get(i);
            Point p2 = pathPoints.get(i + 1);
            double distance = pointToSegmentDistance(x, y, p1.x, p1.y, p2.x, p2.y);
            if (distance < PATH_PROXIMITY_THRESHOLD) {
                return true;
            }
        }
        return false;
    }
    private double pointToSegmentDistance(int x, int y, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1, dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            return Math.hypot(x - x1, y - y1);
        }
        double t = ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        return Math.hypot(x - projX, y - projY);
    }
}
