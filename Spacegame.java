/** Project: Solo Lab 7 Assignment
 * Purpose Details: To create a spacegame where a spaceship dodges/destroys obstacles and gains levels
 * Course: IST 242
 * Author: Maximo Caratini
 * Date Developed: April 24th 2024
 * Last Date Changed: April 30th 2024
 * Rev: 1.0
 */

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;

/**
 * To represent main class for spacegame.
 */
public class Spacegame extends JFrame implements KeyListener {
    // Constants for window dimensions
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    // Constants for player dimensions
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 50;
    // Constants for obstacle dimensions
    private static final int OBSTACLE_WIDTH = 20;
    private static final int OBSTACLE_HEIGHT = 20;
    // Constants for projectile dimensions
    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 10;
    // Constants for games dynamics
    private static final int PLAYER_SPEED = 5;
    private static final int OBSTACLE_SPEED = 3;
    private static final int PROJECTILE_SPEED = 10;
    private static final int HEALTH_POWER_UP_VALUE = 10;
    private static final int HEALTH_POWER_UP_WIDTH = 20;
    private static final int HEALTH_POWER_UP_HEIGHT = 20;
    private static final int GAME_DURATION_SECONDS = 60; // Set the game duration in seconds
    // Variables for state of game
    private int score = 0;
    private int playerHealth = 100;

    private int countdownTimer = GAME_DURATION_SECONDS * 1000;
    private int currentLevel = 1;
    private int levelChallengeScore = 500;
    // Image variables
    private BufferedImage spaceshipImage;
    private BufferedImage[] obstacleImages;
    private BufferedImage healthPowerUpImage;
    // JPanel/label components
    private JPanel gamePanel;
    private JLabel scoreLabel;
    private JLabel timerLabel;
    private JLabel healthLabel;
    // Timers
    private Timer timer;
    private Timer shieldTimer;
    // Game boolean variables
    private boolean isGameOver;
    private boolean isProjectileVisible;
    private boolean isFiring;
    private boolean isShieldActive;
    // Positions of player/projectiles
    private int playerX, playerY;
    private int projectileX, projectileY;
    // Variables for stars properties
    private List<Color> starColors;
    private List<Point> stars;
    // Variables for obstacle properties
    private List<Integer> obstacleImageIndices;
    private List<Point> healthPowerUpPositions;
    private java.util.List<Point> obstacles;
    // Sound clips
    private Clip fireSoundClip;
    private Clip collisionSoundClip;
    // Constructor
    public Spacegame() {
        // Sets up game window
        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        // Initialize game state
        isShieldActive = false;
        isProjectileVisible = false;
        isGameOver = false;
        isFiring = false;
        // to set up timer for shield
        shieldTimer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isShieldActive = false; // Deactivate shield when timer expires
            }
        });
        // to game panel
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g); // Call draw method for rendering game objects
            }
        };
        // To set up the score label
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.BLUE);
        gamePanel.add(scoreLabel, BorderLayout.NORTH);

        // Add game panel to the frame
        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);
        gamePanel.setLayout(new BorderLayout());

        // Initialize player/projectile positions
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;

        // Initialize game object lists
        obstacles = new java.util.ArrayList<>();
        healthPowerUpPositions = new ArrayList<>();
        stars = new ArrayList<>();

        // Initialize countdown timer using game duration in seconds
        countdownTimer = GAME_DURATION_SECONDS * 1000;
        // To create a label for displaying the time left in the game
        timerLabel = new JLabel("Time left: " + (countdownTimer / 1000) + "seconds");
        timerLabel.setForeground(Color.WHITE);
        // To add timer label in the center position
        gamePanel.add(timerLabel, BorderLayout.CENTER);

        // Start game timer
        timer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver) {
                    update(); // Update game
                    gamePanel.repaint(); // Repaint game panel
                }
            }
        });
        timer.start();

        try {
            spaceshipImage = ImageIO.read(new File("newspaceship.png")); // To load spaceship image
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Initialize star colors/generate stars
        starColors = new ArrayList<>();
        starColors.add(Color.WHITE);
        starColors.add(Color.GREEN);
        starColors.add(Color.CYAN);


        generateStars(100);

        try { // Load 4 obstacle image sprites
            obstacleImages = new BufferedImage[4];
            obstacleImages[0] = ImageIO.read(new File("pb&j.png")); // pb&j sprite
            obstacleImages[1] = ImageIO.read(new File("Rocket.png")); // Rocket sprite
            obstacleImages[2] = ImageIO.read(new File("Poop.png")); // Poop sprite
            obstacleImages[3] = ImageIO.read(new File("Banana.png")); // Banana sprite
        } catch (IOException e) {
            e.printStackTrace();
        } // Initialize health label
        healthLabel = new JLabel("Health: " + playerHealth);
        healthLabel.setForeground(Color.WHITE);
        gamePanel.add(healthLabel, BorderLayout.SOUTH);
        // Initialize layout/health label size
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        healthLabel.setPreferredSize(new Dimension(100, 20));

        try {
            healthPowerUpImage = ImageIO.read(new File("Health.png")); // Load health power up image sprite
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Initialize arraylist to store indices of obstacle images
        obstacleImageIndices = new ArrayList<>();

        try {
            // Load firing sound
            AudioInputStream fireSoundStream = AudioSystem.getAudioInputStream(new File("fire.wav"));
            fireSoundClip = AudioSystem.getClip();
            fireSoundClip.open(fireSoundStream);

            // Load collision sound
            AudioInputStream collisionSoundStream = AudioSystem.getAudioInputStream(new File("crash.wav"));
            collisionSoundClip = AudioSystem.getClip();
            collisionSoundClip.open(collisionSoundStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    private void playFireSound() {
        if (fireSoundClip != null) {
            fireSoundClip.stop(); // Stop any previous playback
            fireSoundClip.setFramePosition(0); // Rewind to the beginning
            fireSoundClip.start(); // Play fire sound
        }
    }
    private void playCollisionSound() {
        if (collisionSoundClip != null) {
            collisionSoundClip.stop(); // Stop any previous playback
            collisionSoundClip.setFramePosition(0); // Rewind to the beginning
            collisionSoundClip.start(); // Play collision sound
        }
    }

    /**
     * Generates stars in random positions.
     * @param numStars the number of stars to generate.
     */

    private void generateStars(int numStars) {
        Random random = new Random();
        for (int i = 0; i < numStars; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            stars.add(new Point(x, y));
        }
    }

    /**
     * Draws stars on game panel.
     * @param g the graphics used for rendering.
     */
    private void drawStars(Graphics g) {
        for (Point star : stars) {
            Color color = starColors.get(new Random().nextInt(starColors.size()));
            g.setColor(color);
            g.fillRect(star.x, star.y, 2, 2);
        }
    }

    /**
     * Ends game, displays "Game over, try again!" message.
     */
    private void endGame() {
        isGameOver = true;
        timer.stop(); // Stop the main game loop
        JOptionPane.showMessageDialog(this, "Game Over! Try again.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Draws game objects.
     * @param g the graphics used for rendering
     */

    private void draw(Graphics g) {
        g.setColor(Color.BLACK); // Set background color to black
        g.fillRect(0, 0, WIDTH, HEIGHT); // Fill whole screen with black

        g.drawString("Level: " + currentLevel, 10, 20); // Show current level
        g.drawString("Challenge Score: " + levelChallengeScore, 10, 40); // Display challenge score

        drawStars(g); // To show stars on game panel
        // Draw obstacles
        for (int i = 0; i < obstacles.size(); i++) {
            Point obstacle = obstacles.get(i);
            BufferedImage obstacleImage = obstacleImages[i % obstacleImages.length]; // Select obstacle image
            g.drawImage(obstacleImage, obstacle.x, obstacle.y, null); // Draw obstacle image
        }

        if (isProjectileVisible) {
            g.setColor(Color.GREEN); // Set projectile color to green
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT); // Draw projectile
        }
        if (spaceshipImage != null) {
            g.drawImage(spaceshipImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null); // Draw spaceship image
        }

        if (isGameOver) {
            g.setColor(Color.WHITE); // Set color to white
            g.setFont(new Font("Arial", Font.BOLD, 24)); // Set font for game over message
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2); // Display "Game Over!" message in center
        }
        if (isShieldActive) {
            g.setColor(Color.BLUE); // Shield color
            g.drawRect(playerX - 5, playerY - 5, PLAYER_WIDTH + 10, PLAYER_HEIGHT + 10); // Draw shield around player
        }
        if (healthPowerUpImage != null) {
            // Draw health power up images
            for (Point healthPowerUp : healthPowerUpPositions) {
                g.drawImage(healthPowerUpImage, healthPowerUp.x, healthPowerUp.y, null);
            }
        }
    }

    /**
     * Generates health power ups with random positions.
     */
    private void generateHealthPowerUp() {
        if (Math.random() < 0.01) { // Adjust the probability as needed
            int powerUpX = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH)); // Random X position within game bounds
            int powerUpY = -HEALTH_POWER_UP_HEIGHT; // Start health power up above game panel
            healthPowerUpPositions.add(new Point(powerUpX, powerUpY)); // Add health power up in random position
        }
    }

    /**
     * Updates the positions/states for health power ups.
     */
    private void updateHealthPowerUps() {
        for (int i = 0; i < healthPowerUpPositions.size(); i++) {
            // Get the position of the health power up
            Point powerUpPosition = healthPowerUpPositions.get(i);
            // Move health power up down
            powerUpPosition.y += OBSTACLE_SPEED; // Move health power-up down
            if (powerUpPosition.y > HEIGHT) {
                healthPowerUpPositions.remove(i); // Remove if health power-up is off-screen
                i--;
            }
        }
    }

    /**
     * To check collisions with player and health power ups.
     */
    private void checkCollisionWithHealthPowerUps() {
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT); // Creates rectangle which represents player
        for (Point healthPowerUp : healthPowerUpPositions) { // Iterate over every health power up
            Rectangle healthPowerUpRect = new Rectangle(healthPowerUp.x, healthPowerUp.y, HEALTH_POWER_UP_WIDTH, HEALTH_POWER_UP_HEIGHT); // Creates a rectangle which represents current health power up
            if (playerRect.intersects(healthPowerUpRect)) { // Checks if player and health powerup collides
                healthPowerUpPositions.remove(healthPowerUp); // Remove health powerup
                playerHealth += HEALTH_POWER_UP_VALUE; // Increase player health
                break;
            }
        }
    }

    /**
     * Updates game state.
     */
    private void update() {
        if (!isGameOver) {
            // Move obstacles
            for (int i = 0; i < obstacles.size(); i++) {
                obstacles.get(i).y += OBSTACLE_SPEED;
                // Remove obstacle when it goes off screen
                if (obstacles.get(i).y > HEIGHT) {
                    obstacles.remove(i);
                    i--;
                }
            }
            if (!isShieldActive) { // Checks collision with obstacles and handle scoring
                Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT); // Creates rectangle representing projectile
                for (int i = 0; i < obstacles.size(); i++) { // Iterate over every obstacle
                    Rectangle obstacleRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT); // Creates rectangle representing current obstacle
                    if (projectileRect.intersects(obstacleRect)) { // Checks if there's a collision in between obstacle and projectile
                        obstacles.remove(i);
                        score += 50;
                        isProjectileVisible = false;
                        playCollisionSound(); // Play collision sound
                        break;
                    }
                }
            }

            // Generate new obstacles
            if (Math.random() < 0.02) {
                int obstacleX = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
                obstacles.add(new Point(obstacleX, 0));
            }

            // Move projectile
            if (isProjectileVisible) {
                projectileY -= PROJECTILE_SPEED;
                if (projectileY < 0) {
                    isProjectileVisible = false;
                }
            } // Creates rectangle which represents player
            Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            for (Point healthPowerUp : healthPowerUpPositions) { // Check collisions with health power ups/updates player health
                Rectangle healthPowerUpRect = new Rectangle(healthPowerUp.x, healthPowerUp.y, HEALTH_POWER_UP_WIDTH, HEALTH_POWER_UP_HEIGHT); // Creates a rectangle whcih represents current health power up
                if (playerRect.intersects(healthPowerUpRect)) { //Checks if theres collision between player and health power up
                    healthPowerUpPositions.remove(healthPowerUp); // Remove health power up
                    playerHealth += HEALTH_POWER_UP_VALUE; // Increase player health
                    break;
                }
            }

            // Check collision with obstacles and decreases player health
            for (Point obstacle : obstacles) {
                Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT); // Creates rectangle which represents current obstacle
                if (playerRect.intersects(obstacleRect)) { // Checks if collisison between player and obstacle happens
                    if (!isShieldActive) {
                        playerHealth -= 20; // Decrease health by 20
                        if (playerHealth <= 0) { // Check if player health is 0 or below
                            isGameOver = true;
                            endGame();
                        }
                } else { // Removes obstacle
                    obstacles.remove(obstacle);
                    score += 10; // increases score by 10
                    playCollisionSound(); // Plays collision sound
                    }

                }
                break;
            }

            // Check collision with obstacle and updates score
            Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
            for (int i = 0; i < obstacles.size(); i++) {
                Rectangle obstacleRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT); // Creates rectangle representing current obstacle
                if (projectileRect.intersects(obstacleRect)) { // Checks if projectile collides with obstacle
                    obstacles.remove(i); // Removes obstacle
                    score += 10; // Increases score
                    isProjectileVisible = false; // Hides projectile
                    playCollisionSound(); // Plays collision sound
                    break;
                }
            } // Generates health powerups, updates health power ups posiion, and checks for collisions
            generateHealthPowerUp();
            updateHealthPowerUps();
            checkCollisionWithHealthPowerUps();
            // Updates score and health labels
            scoreLabel.setText("Score: " + score);
            healthLabel.setText("Health: " + playerHealth);
        } // Checks if player reached challenge score to level up
        if (score >= levelChallengeScore) {
            currentLevel++; // Increase current level/challenge score
            levelChallengeScore += 350; // Increase challenge score for the next level
            JOptionPane.showMessageDialog(this, "YOU HAVE BEEN PROMOTED! LEVEL 100 MAFIA BOSS " + currentLevel, "Level Up", JOptionPane.INFORMATION_MESSAGE); // Displays level up message

            JLabel levelLabel = new JLabel("Level: " + currentLevel); // Creates new label which displays game level
            levelLabel.setForeground(Color.BLUE); // Sets text color for level to blue
            gamePanel.add(levelLabel, BorderLayout.WEST); // Adds level label to game panel in west position
            levelLabel.setText("Level: " + currentLevel); // Updates text for curent level
        }
    }

    /**
     * Handles key pressed event.
     * @param e The Key event object representing key press event.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode(); // Retrieve code of key pressed
        switch (keyCode) { // Switch case to handle different key
            case KeyEvent.VK_LEFT: // Left arrow key
                if (playerX > 0) { // Checks if player is within bounds on left side
                    playerX -= PLAYER_SPEED; // Moves player left by decreasing x coordinate using players speed
                }
                break;
            case KeyEvent.VK_RIGHT: // Right arrow key
                if (playerX < WIDTH - PLAYER_WIDTH) { // Checks if player is within bounds on right side
                    playerX += PLAYER_SPEED; // Moves player right by increasing x coordinate using player speed
                }
                break;
            case KeyEvent.VK_SPACE: // Space bar key code
                if (!isFiring) { // Checks if firing mechanism is active
                    isFiring = true; // Sets firing flag true
                    projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2; // Position projectile at middle of player sprite
                    projectileY = playerY;
                    isProjectileVisible = true; // Makes projectile visible
                    playFireSound(); // Plays fire sound
                    new Thread(() -> {
                        try {
                            Thread.sleep(500); // Makes thread sleep for half a second
                            isFiring = false; // Resets firing flag to false
                        } catch (InterruptedException ex) {
                            ex.printStackTrace(); // Prints stack trace if interruption exception occurs
                        }
                    }).start();
                }
                break;
            case KeyEvent.VK_S: // 'S' key used to activate shield
                activateShield();
                break;
        }
    }

    /**
     * Handles the key released event.
     * @param e Key event object representing key release event.
     */
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_S) { // Checks if 'S' key is released
            deactivateShield(); // Deactivates shield
        }
    }

    /**
     * Handles key typed event
     * @param e the keyevent object representing key typed event.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        System.out.println("Key typed: " + e.getKeyChar());
    } // Handles key typed events
    // Method to activate shield
    public void activateShield() {
        isShieldActive = true; // Set shield active flag true
    }

    // Method to deactivate the shield
    public void deactivateShield() {
        isShieldActive = false; // Sets shield active flag false
    }

    /**
     * Main method to start game.
     * @param args The command line arguments.
     */
    public static void main(String[] args) { // Main method to start game
        SwingUtilities.invokeLater(new Runnable() { // Use swing utilities
            @Override
            public void run() {
                new Spacegame().setVisible(true); // Create and display main game window
            }
        });
    }
}
