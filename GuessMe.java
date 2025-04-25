import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class GuessMe {
    private static int randomNumber;
    private static int attemptsLeft = 5;

    public static void main(String[] args) {
        // Generate a random number between 1 and 100
        generateRandomNumber();

        // Create the start menu JFrame
        JFrame startMenu = new JFrame("Game Start Menu");
        startMenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startMenu.setSize(400, 300);
        startMenu.setLayout(new BorderLayout());

        // Add instructions
        JLabel instructions = new JLabel("<html><center>Welcome to the Number Guessing Game!<br>" +
                "Guess the number between 1 and 100<br>" +
                "You have 5 chances. Good luck!<br>" +
                "Press 'Start' to begin</center></html>", SwingConstants.CENTER);
        instructions.setFont(new Font("Arial", Font.PLAIN, 16));
        startMenu.add(instructions, BorderLayout.CENTER);

        // Add "Start Game" button
        JButton startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Open the game screen and close the start menu
                showGameScreen();
                startMenu.dispose();
            }
        });

        startMenu.add(startButton, BorderLayout.SOUTH);

        // Make the start menu visible
        startMenu.setVisible(true);
    }

    private static void generateRandomNumber() {
        Random random = new Random();
        randomNumber = random.nextInt(100) + 1; // Random number between 1 and 100
    }

    private static void showGameScreen() {
        // Reset attempts
        attemptsLeft = 5;

        // Create the game screen JFrame
        JFrame gameScreen = new JFrame("Number Guessing Game");
        gameScreen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameScreen.setSize(400, 300);
        gameScreen.setLayout(new BorderLayout());

        // JLabel for instructions
        JLabel gameInstructions = new JLabel("Guess a number between 1 and 100:", SwingConstants.CENTER);
        gameInstructions.setFont(new Font("Arial", Font.PLAIN, 14));
        gameScreen.add(gameInstructions, BorderLayout.NORTH);

        // JTextField for player input
        JTextField inputField = new JTextField();
        gameScreen.add(inputField, BorderLayout.CENTER);

        // JLabel to display the result
        JLabel resultLabel = new JLabel("You have 5 chances left.", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gameScreen.add(resultLabel, BorderLayout.SOUTH);

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int playerGuess = Integer.parseInt(inputField.getText());
                    inputField.setText(""); // Clear the input field

                    // Check the player's guess
                    if (playerGuess == randomNumber) {
                        resultLabel.setText("Correct! The number was " + randomNumber + ".");
                        inputField.setEnabled(false); // Disable input after correct guess
                    } else {
                        attemptsLeft--;
                        if (attemptsLeft == 0) {
                            resultLabel.setText("Game over! The number was " + randomNumber + ".");
                            inputField.setEnabled(false); // Disable input after game over
                        } else {
                            String hint = playerGuess < randomNumber ? "Too low!" : "Too high!";
                            resultLabel.setText(hint + " You have " + attemptsLeft + " chances left.");
                        }
                    }
                } catch (NumberFormatException ex) {
                    resultLabel.setText("Invalid input. Please enter a number.");
                }
            }
        });
        gameScreen.setVisible(true);
    }
}
