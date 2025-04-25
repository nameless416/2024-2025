import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;


public class NewStart {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(mainMenu::new);
    }
}

class mainMenu {
    private JFrame frame;

    public mainMenu() {
        frame = new JFrame("New Start");
        frame.setSize(1280, 720);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        JPanel mainMenuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(0, 0, Color.BLACK, 0, getHeight(), Color.RED);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainMenuPanel.setLayout(null);

        JLabel title = new JLabel("Trials for Redemption ", SwingConstants.CENTER);
        title.setFont(new Font("SanSerif", Font.BOLD, 60));
        title.setForeground(Color.WHITE);
        title.setBounds(320, 50, 640, 100);
        mainMenuPanel.add(title);

        JButton startButton = createButton("Start Game");
        startButton.setBounds(540, 200, 200, 50);
        startButton.addActionListener(e -> gameTab());
        mainMenuPanel.add(startButton);
        
        JButton settingButton = createButton("Settings");
        settingButton.setBounds(540, 280, 200, 50);
        settingButton.addActionListener(e -> settingsTab());
        mainMenuPanel.add(settingButton);

        JButton exitButton = createButton("Exit Game");
        exitButton.setBounds(540, 360, 200, 50);
        exitButton.addActionListener(e -> System.exit(0));
        mainMenuPanel.add(exitButton);

        frame.add(mainMenuPanel);
        frame.setVisible(true);
    }

    private void gameTab() {
        frame.dispose();
        new GameFrame();
    }

    private void settingsTab() {
        frame.dispose();
        new SettingFrame();
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(50, 50, 50));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 80, 80));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(50, 50, 50));
            }
        });
        return button;
    }
}

class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("Game Screen");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new GamePanel());
        setVisible(true);
    }


}

class SettingFrame extends JFrame {
    public SettingFrame() {
        setTitle("Game Screen");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new SettingPanel());
        setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer GAMETIMER;
    private Player PLAYER;
    private ArrayList<Enemy> ENEMIES;
    private ArrayList<Bullet> BULLETS;
    private int WAVENUMBER = 1;
    private boolean waveTransition = false;  // Prevent multiple level-up triggers

    public GamePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        PLAYER = new Player(600, 350);
        ENEMIES = new ArrayList<>();
        BULLETS = new ArrayList<>();

        GAMETIMER = new Timer(16, this);
        GAMETIMER.start();

        spawnWave();
    }

    private void spawnWave() {
        ENEMIES.clear();
        // For simplicity, spawn 5 enemies per wave.
        for (int i = 0; i < WAVENUMBER * 2; i++) {
            ENEMIES.add(new Enemy((int)(Math.random() * 1200), (int)(Math.random() * 720)));
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        PLAYER.draw(g);
        for (Enemy enemy : ENEMIES) {
            enemy.draw(g);
        }
        for (Bullet bullet : BULLETS) {
            bullet.draw(g);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PLAYER.update();
        for (Enemy enemy : ENEMIES) {
            enemy.update(PLAYER);
        }
        // Update bullets and remove those off-screen.
        Iterator<Bullet> bulletIter = BULLETS.iterator();
        while(bulletIter.hasNext()){
            Bullet b = bulletIter.next();
            b.update();
            if(b.x < 0 || b.x > 1280 || b.y < 0 || b.y > 720){
                bulletIter.remove();
            }
        }
        // Check collisions: bullet vs enemy.
        for (Iterator<Bullet> bi = BULLETS.iterator(); bi.hasNext();) {
            Bullet b = bi.next();
            for (Iterator<Enemy> ei = ENEMIES.iterator(); ei.hasNext();) {
                Enemy enemy = ei.next();
                if(b.getBounds().intersects(enemy.getBounds())){
                    // Remove enemy and bullet upon collision.
                    ei.remove();
                    bi.remove();
                    break;
                }
            }
        }

        // If all enemies are cleared, level up.
        if (ENEMIES.isEmpty() && !waveTransition) {
            waveTransition = true;
            SwingUtilities.invokeLater(() -> {
                LevelUpDialog dialog = new LevelUpDialog((JFrame) SwingUtilities.getWindowAncestor(this), PLAYER);
                dialog.setVisible(true);
                WAVENUMBER++;
                spawnWave();
                waveTransition = false;
            });
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        PLAYER.keyPressed(e);
        // On space bar, shoot bullet.
        if(e.getKeyCode() == KeyEvent.VK_SPACE) {
            // Create a bullet from the player's center in the direction the player is facing.
            Bullet b = new Bullet(PLAYER.x + 15, PLAYER.y + 15, PLAYER.getDirection());
            BULLETS.add(b);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        PLAYER.keyReleased(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

// Updated Player with movement and a simple notion of facing direction.
class Player {
    int x, y;
    int width = 30, height = 30;
    int speed = 5;
    int dx = 0, dy = 0;
    String direction = "right"; // default direction
    int damage = 10;
    int maxHP = 100;
    int currentHP = 100;
    double attackSpeed = 1.0;
    int dodge = 5;
    int level = 1;
    int exp = 0;

    public Player(int x, int y) { 
        this.x = x; 
        this.y = y; 
    }

    public void update() {
        x += dx;
        y += dy;
        // Clamp position
        x = Math.max(0, Math.min(x, 1280 - width));
        y = Math.max(0, Math.min(y, 720 - height));
    }
    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillOval(x, y, width, height);
    }
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if(code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            dy = -speed;
            direction = "up";
        }
        if(code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            dy = speed;
            direction = "down";
        }
        if(code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            dx = -speed;
            direction = "left";
        }
        if(code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            dx = speed;
            direction = "right";
        }
    }
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if(code == KeyEvent.VK_W || code == KeyEvent.VK_UP || code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            dy = 0;
        }
        if(code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT || code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            dx = 0;
        }
    }
    public String getDirection() {
        return direction;
    }
    public void addExp(int amount) {
        exp += amount;
        int expNeeded = level * 100;
        if(exp >= expNeeded) {
            level++;
            exp -= expNeeded;
        }
    }
}

// Simple Upgrade class.
class Upgrade {
    String name;
    String description;
    int value;
    String rarity;
    Runnable applyUpgrade;

    public Upgrade(String name, String description, int value, String rarity, Runnable applyUpgrade) {
        this.name = name;
        this.description = description;
        this.value = value;
        this.rarity = rarity;
        this.applyUpgrade = applyUpgrade;
    }
}

// LevelUpDialog presents three random upgrade choices.
class LevelUpDialog extends JDialog {
    public LevelUpDialog(JFrame parent, Player player) {
        super(parent, "Level Up! Choose an Upgrade", true);
        setLayout(new GridLayout(1, 3, 10, 10));
        setSize(600, 200);
        setLocationRelativeTo(parent);

        List<Upgrade> options = generateUpgrades(player);
        for (Upgrade up : options) {
            JButton btn = new JButton("<html><center>" + up.name + "<br>" + up.description + "<br>Rarity: " + up.rarity + "</center></html>");
            btn.addActionListener(e -> {
                up.applyUpgrade.run();
                dispose();
            });
            add(btn);
        }
    }

    private List<Upgrade> generateUpgrades(Player player) {
        List<Upgrade> pool = new ArrayList<>();
        pool.add(new Upgrade("Increase Damage", "Increase damage by 5", 5, randomRarity(), () -> { player.damage += 5; }));
        pool.add(new Upgrade("Increase Max HP", "Increase max HP by 10", 10, randomRarity(), () -> { player.maxHP += 10; player.currentHP += 10; }));
        pool.add(new Upgrade("Increase Attack Speed", "Increase attack speed by 0.2", 0, randomRarity(), () -> { player.attackSpeed += 0.2; }));
        pool.add(new Upgrade("Increase Dodge", "Increase dodge chance by 5", 5, randomRarity(), () -> { player.dodge += 5; }));
        Collections.shuffle(pool);
        return pool.subList(0, 3);
    }

    private String randomRarity() {
        double r = Math.random();
        if (r < 0.5) return "Common";
        else if (r < 0.8) return "Rare";
        else if (r < 0.95) return "Epic";
        else return "Legendary";
    }
}

// Enemy moves toward the player.
class Enemy {
    int x, y;
    int width = 20, height = 20;
    int speed = 2;

    public Enemy(int x, int y) {
        this.x = x; 
        this.y = y;
    }

    public void update(Player player) {
        double diffX = player.x - x;
        double diffY = player.y - y;
        double distance = Math.sqrt(diffX * diffX + diffY * diffY);
        if (distance != 0) {
            x += (int)((diffX / distance) * speed);
            y += (int)((diffY / distance) * speed);
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.MAGENTA);
        g.fillRect(x, y, width, height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}

// Boss extends Enemy.
class Boss extends Enemy {
    public Boss(int x, int y) { super(x, y); speed = 1; width = 40; height = 40; }
    @Override
    public void draw(Graphics g) {
        g.setColor(Color.ORANGE);
        g.fillRect(x, y, width, height);
    }
}

// Bullet moves in a specified direction.
class Bullet {
    double x, y;
    double dx, dy;
    int size = 10;
    int speed = 8;

    public Bullet(double x, double y, String direction) {
        this.x = x;
        this.y = y;
        // Set dx, dy based on direction.
        switch(direction) {
            case "up":    dx = 0; dy = -speed; break;
            case "down":  dx = 0; dy = speed; break;
            case "left":  dx = -speed; dy = 0; break;
            case "right": dx = speed; dy = 0; break;
            default:      dx = speed; dy = 0; break;
        }
    }

    public void update() {
        x += dx;
        y += dy;
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillOval((int)x, (int)y, size, size);
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, size, size);
    }
}

// Simple placeholder Item class.
class Item {
    int x = 100, y = 100;
    public void draw(Graphics g) {
        g.setColor(Color.CYAN);
        g.fillRect(x, y, 15, 15);
    }
}


class SettingPanel extends JPanel {
    public SettingPanel() {
        setBackground(Color.DARK_GRAY);
    }
}