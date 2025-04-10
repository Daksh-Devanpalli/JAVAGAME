import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.swing.JOptionPane;

public class Game extends Canvas implements Runnable {

    private static final long serialVersionUID = 1L;

    private boolean isRunning = false;
    private Thread thread;
    private Handler handler;
    private Camera camera;
    private SpriteSheet ss;
    private BufferedImage level = null;
    private BufferedImage sprite_sheet = null;
    private BufferedImage floor = null;

    public int ammo = 10;
    public int killcount = 0;
    public static String playerName = "DAKSH";  // default

    public Game() {
        // Ask for player name
        String inputName = JOptionPane.showInputDialog("Enter your name:");
        if (inputName != null && !inputName.trim().isEmpty()) {
            playerName = inputName.trim();
        }

        new Window(1000, 563, "Wizard Game", this);

        handler = new Handler();
        camera = new Camera(0, 0);
        this.addKeyListener(new KeyInput(handler));

        BufferedImageLoader loader = new BufferedImageLoader();

        try {
            level = loader.loadImage("/wizard_level.png");
        } catch (Exception e) {
            System.out.println("Error loading wizard_level.png");
            e.printStackTrace();
        }

        try {
            sprite_sheet = loader.loadImage("/sprimg.png");
        } catch (Exception e) {
            System.out.println("Error loading sprimg.png");
            e.printStackTrace();
        }

        ss = new SpriteSheet(sprite_sheet);
        floor = ss.grabImage(4, 4, 64, 64);

        this.addMouseListener(new MouseInput(handler, camera, this, ss));

        loadLevel(level);
        start();
    }

    private void start() {
        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    private void stop() {
        isRunning = false;
        try {
            System.out.println("Game is stopping...");
            System.out.println("Saving score for: " + playerName + ", Kills: " + killcount);

            DatabaseManager.saveScore(playerName, killcount);

            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        this.requestFocus();
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                delta--;
            }
            render();
            frames++;

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                frames = 0;
            }
        }

        stop();
    }

    public void tick() {
        for (int i = 0; i < handler.object.size(); i++) {
            if (handler.object.get(i).getId() == ID.Player) {
                camera.tick(handler.object.get(i));
            }
        }
        handler.tick();
    }

    public void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();
        Graphics2D g2d = (Graphics2D) g;

        // Camera movement
        g2d.translate(-camera.getX(), -camera.getY());

        // Draw background floor
        for (int xx = 0; xx < 30 * 72; xx += 32) {
            for (int yy = 0; yy < 30 * 72; yy += 32) {
                g.drawImage(floor, xx, yy, null);
            }
        }

        handler.render(g);

        // Reset translation
        g2d.translate(camera.getX(), camera.getY());

        // Display UI
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Kills: " + killcount, 20, 60);
        g.drawString("Player: " + playerName, 20, 90);

        g.dispose();
        bs.show();
    }

    private void loadLevel(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        for (int xx = 0; xx < w; xx++) {
            for (int yy = 0; yy < h; yy++) {
                int pixel = image.getRGB(xx, yy);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = (pixel) & 0xff;

                if (red == 255) {
                    handler.addObject(new Block(xx * 32, yy * 32, ID.Block, ss));
                }

                if (blue == 255 && green == 0) {
                    handler.addObject(new Wizard(xx * 32, yy * 32, ID.Player, handler, this, ss));
                }

                if (green == 255 && blue == 0) {
                    handler.addObject(new Enemy(xx * 32, yy * 32, ID.Enemy, handler, ss, this));
                }

                if (green == 255 && blue == 255) {
                    handler.addObject(new Crate(xx * 32, yy * 32, ID.Crate, ss));
                }
            }
        }
    }

    public static void main(String[] args) {
        new Game();
    }
}
