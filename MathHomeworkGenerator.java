import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.Timer;

// A class to store the details of each question.
class Question {
    String questionText;
    String correctAnswer;
    String explanation;
    
    public Question(String questionText, String correctAnswer, String explanation) {
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }
}

public class MathHomeworkGenerator extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    // Configuration Panel components
    private JPanel configPanel;
    private JTextField numQuestionsField;
    // Removed difficultyComboBox.
    private JCheckBox additionCheckBox;
    private JCheckBox subtractionCheckBox;
    private JCheckBox multiplicationCheckBox;
    private JCheckBox divisionCheckBox;
    private JCheckBox equationWithXCheckBox;
    private JCheckBox squaringCheckBox;
    // Additional types:
    private JCheckBox fractionCheckBox;
    private JCheckBox exponentCheckBox;
    private JCheckBox percentageCheckBox;
    // Timer configuration components
    private JCheckBox timerEnabledCheckBox;
    private JTextField timerDurationField;
    // Complexity slider now controls number of variables
    private JSlider complexitySlider;
    private JButton generateButton;
    
    // Test Panel components
    private JPanel testPanel;
    private JPanel questionsPanel;
    private JScrollPane questionsScrollPane;
    private JButton submitAnswersButton;
    private ArrayList<Question> questionsList;
    private ArrayList<JTextField> answerFields;
    
    // Timer components
    private JLabel timerLabel;
    private Timer countdownTimer;
    private int timeRemaining; // in seconds
    
    // Result Panel components
    private JPanel resultPanel;
    private JLabel percentLabel;
    private JLabel timeTakenLabel;
    private DefaultListModel<String> wrongListModel;
    private JList<String> wrongList;
    private JButton backToConfigButton;
    private ArrayList<Integer> wrongIndices;
    private JTextArea previousResultsArea;
    
    // Random generator
    private Random random;
    
    // For tracking test start time
    private long testStartTime;
    
    public MathHomeworkGenerator() {
        super("Math Homework Generator");
        random = new Random();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setNimbusLookAndFeel();
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        buildConfigPanel();
        buildTestPanel();
        buildResultPanel();
        
        mainPanel.add(configPanel, "CONFIG");
        mainPanel.add(testPanel, "TEST");
        mainPanel.add(resultPanel, "RESULT");
        add(mainPanel);
    }
    
    // Attempt to set Nimbus Look and Feel.
    private void setNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to default.
        }
    }
    
    // Build the configuration panel.
    private void buildConfigPanel() {
        configPanel = new JPanel(new BorderLayout(15, 15));
        configPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Title label.
        JLabel titleLabel = new JLabel("Configure Your Math Test", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 24));
        configPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Instructions area.
        JTextArea instructionsArea = new JTextArea(
            "Instructions:\n" +
            "1. Enter the number of questions.\n" +
            "2. Select the types of problems you want (e.g., Addition, Subtraction, etc.).\n" +
            "3. Enable the Timer if desired and set its duration (in seconds).\n" +
            "4. Adjust the Problem Complexity slider to increase the number of terms in some problems.\n" +
            "   (Low: 2 operands, Medium: 3 operands, High: 4 operands)\n" +
            "5. Click 'Generate Test' to start.\n\nGood luck!"
        );
        instructionsArea.setEditable(false);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setFont(new Font("Tahoma", Font.PLAIN, 14));
        instructionsArea.setBackground(configPanel.getBackground());
        instructionsArea.setBorder(new TitledBorder(new EtchedBorder(), "Instructions"));
        instructionsArea.setPreferredSize(new Dimension(800, 120));
        
        // Options panel.
        JPanel optionsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        optionsPanel.setBorder(new TitledBorder(new EtchedBorder(), "Test Options"));
        
        optionsPanel.add(new JLabel("Number of Questions:"));
        numQuestionsField = new JTextField("10");
        numQuestionsField.setToolTipText("Enter how many questions you want");
        optionsPanel.add(numQuestionsField);
        
        // Removed the difficulty combo box for timer control.
        
        additionCheckBox = new JCheckBox("Addition", true);
        additionCheckBox.setToolTipText("Include addition problems");
        optionsPanel.add(additionCheckBox);
        subtractionCheckBox = new JCheckBox("Subtraction", true);
        subtractionCheckBox.setToolTipText("Include subtraction problems");
        optionsPanel.add(subtractionCheckBox);
        multiplicationCheckBox = new JCheckBox("Multiplication", true);
        multiplicationCheckBox.setToolTipText("Include multiplication problems");
        optionsPanel.add(multiplicationCheckBox);
        divisionCheckBox = new JCheckBox("Division", true);
        divisionCheckBox.setToolTipText("Include division problems");
        optionsPanel.add(divisionCheckBox);
        equationWithXCheckBox = new JCheckBox("Equation with X", false);
        equationWithXCheckBox.setToolTipText("Include equations with unknown x");
        optionsPanel.add(equationWithXCheckBox);
        squaringCheckBox = new JCheckBox("Squaring", false);
        squaringCheckBox.setToolTipText("Include squaring problems");
        optionsPanel.add(squaringCheckBox);
        
        fractionCheckBox = new JCheckBox("Fractions", false);
        fractionCheckBox.setToolTipText("Include fraction problems");
        optionsPanel.add(fractionCheckBox);
        exponentCheckBox = new JCheckBox("Exponents", false);
        exponentCheckBox.setToolTipText("Include exponent problems");
        optionsPanel.add(exponentCheckBox);
        percentageCheckBox = new JCheckBox("Percentages", false);
        percentageCheckBox.setToolTipText("Include percentage problems");
        optionsPanel.add(percentageCheckBox);
        
        // Timer options.
        optionsPanel.add(new JLabel("Enable Timer:"));
        timerEnabledCheckBox = new JCheckBox("", true);
        timerEnabledCheckBox.setToolTipText("Check to enable a timer for the test");
        optionsPanel.add(timerEnabledCheckBox);
        
        optionsPanel.add(new JLabel("Timer Duration (sec):"));
        timerDurationField = new JTextField("300");
        timerDurationField.setToolTipText("Enter duration in seconds for the timer");
        optionsPanel.add(timerDurationField);
        
        // Complexity slider (affects number of operands in problems).
        optionsPanel.add(new JLabel("Problem Complexity:"));
        complexitySlider = new JSlider(1, 10, 5);
        complexitySlider.setMajorTickSpacing(1);
        complexitySlider.setPaintTicks(true);
        complexitySlider.setPaintLabels(true);
        complexitySlider.setToolTipText("Adjust the complexity (number of terms in some problems)");
        optionsPanel.add(complexitySlider);
        
        // Wrap the options panel in a scroll pane.
        JScrollPane optionsScrollPane = new JScrollPane(optionsPanel);
        optionsScrollPane.setBorder(null);
        
        // Combine instructions and options (instructions on top).
        JPanel configContainer = new JPanel(new BorderLayout(15, 15));
        configContainer.add(instructionsArea, BorderLayout.NORTH);
        configContainer.add(optionsScrollPane, BorderLayout.CENTER);
        
        configPanel.add(configContainer, BorderLayout.CENTER);
        
        // Button panel.
        JPanel buttonPanel = new JPanel();
        generateButton = new JButton("Generate Test");
        generateButton.setPreferredSize(new Dimension(150, 35));
        generateButton.setFont(new Font("Tahoma", Font.PLAIN, 16));
        buttonPanel.add(generateButton);
        configPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                generateTest();
            }
        });
    }
    
    // Build the test panel.
    private void buildTestPanel() {
        testPanel = new JPanel(new BorderLayout(15, 15));
        testPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel testTitle = new JLabel("Solve the Following Problems", SwingConstants.CENTER);
        testTitle.setFont(new Font("Tahoma", Font.BOLD, 22));
        topPanel.add(testTitle, BorderLayout.CENTER);
        
        timerLabel = new JLabel("Time Remaining: ", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Tahoma", Font.BOLD, 16));
        topPanel.add(timerLabel, BorderLayout.SOUTH);
        testPanel.add(topPanel, BorderLayout.NORTH);
        
        questionsPanel = new JPanel();
        questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));
        questionsScrollPane = new JScrollPane(questionsPanel);
        questionsScrollPane.setBorder(new TitledBorder(new EtchedBorder(), "Questions"));
        testPanel.add(questionsScrollPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        submitAnswersButton = new JButton("Submit Answers");
        submitAnswersButton.setPreferredSize(new Dimension(150, 35));
        submitAnswersButton.setFont(new Font("Tahoma", Font.PLAIN, 16));
        bottomPanel.add(submitAnswersButton);
        testPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        submitAnswersButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                submitTest();
            }
        });
    }
    
    // Build the result panel.
    private void buildResultPanel() {
        resultPanel = new JPanel(new BorderLayout(15, 15));
        resultPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel resultTitle = new JLabel("Test Results", SwingConstants.CENTER);
        resultTitle.setFont(new Font("Tahoma", Font.BOLD, 24));
        resultPanel.add(resultTitle, BorderLayout.NORTH);
        
        percentLabel = new JLabel("Percentage Correct: ", SwingConstants.CENTER);
        percentLabel.setFont(new Font("Tahoma", Font.BOLD, 20));
        timeTakenLabel = new JLabel("Time Taken: ", SwingConstants.CENTER);
        timeTakenLabel.setFont(new Font("Tahoma", Font.PLAIN, 18));
        
        // List for incorrect answers.
        wrongListModel = new DefaultListModel<>();
        wrongList = new JList<>(wrongListModel);
        wrongList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        wrongList.setFont(new Font("Tahoma", Font.PLAIN, 16));
        wrongList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setForeground(Color.RED);
                return label;
            }
        });
        JScrollPane wrongScrollPane = new JScrollPane(wrongList);
        wrongScrollPane.setBorder(new TitledBorder(new EtchedBorder(), "Incorrect Answers (Double-click for details)"));
        
        // Results summary panel.
        JPanel summaryPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        summaryPanel.add(percentLabel);
        summaryPanel.add(timeTakenLabel);
        summaryPanel.add(wrongScrollPane);
        
        // Previous test scores area.
        previousResultsArea = new JTextArea(8, 30);
        previousResultsArea.setEditable(false);
        previousResultsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane prevScrollPane = new JScrollPane(previousResultsArea);
        prevScrollPane.setBorder(new TitledBorder(new EtchedBorder(), "Previous Test Scores"));
        summaryPanel.add(prevScrollPane);
        
        resultPanel.add(summaryPanel, BorderLayout.CENTER);
        
        JPanel backPanel = new JPanel();
        backToConfigButton = new JButton("Back to Configuration");
        backToConfigButton.setPreferredSize(new Dimension(200, 35));
        backToConfigButton.setFont(new Font("Tahoma", Font.PLAIN, 16));
        backPanel.add(backToConfigButton);
        resultPanel.add(backPanel, BorderLayout.SOUTH);
        
        wrongList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = wrongList.getSelectedIndex();
                    if (index != -1) {
                        int questionIndex = wrongIndices.get(index);
                        Question q = questionsList.get(questionIndex);
                        String message = "Problem: " + q.questionText +
                                         "\n\nCorrect Answer: " + q.correctAnswer +
                                         "\n\nExplanation: " + q.explanation;
                        JOptionPane.showMessageDialog(resultPanel, message, "Question " + (questionIndex + 1) + " Details", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
        
        backToConfigButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "CONFIG");
            }
        });
    }
    
    // Generate the test based on selected options.
    private void generateTest() {
        int numQuestions;
        try {
            numQuestions = Integer.parseInt(numQuestionsField.getText());
        } catch(NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for questions.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        ArrayList<String> types = new ArrayList<>();
        if (additionCheckBox.isSelected()) types.add("addition");
        if (subtractionCheckBox.isSelected()) types.add("subtraction");
        if (multiplicationCheckBox.isSelected()) types.add("multiplication");
        if (divisionCheckBox.isSelected()) types.add("division");
        if (equationWithXCheckBox.isSelected()) types.add("equation");
        if (squaringCheckBox.isSelected()) types.add("squaring");
        if (fractionCheckBox.isSelected()) types.add("fraction");
        if (exponentCheckBox.isSelected()) types.add("exponent");
        if (percentageCheckBox.isSelected()) types.add("percentage");
        
        if (types.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one type of question.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Timer settings.
        if (timerEnabledCheckBox.isSelected()) {
            try {
                timeRemaining = Integer.parseInt(timerDurationField.getText());
            } catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid timer duration in seconds.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            timerLabel.setText("Time Remaining: " + formatTime(timeRemaining));
            
            if (countdownTimer != null && countdownTimer.isRunning()) {
                countdownTimer.stop();
            }
            countdownTimer = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timeRemaining--;
                    timerLabel.setText("Time Remaining: " + formatTime(timeRemaining));
                    if (timeRemaining <= 0) {
                        countdownTimer.stop();
                        JOptionPane.showMessageDialog(testPanel, "Time is up! Submitting your answers.", "Time Up", JOptionPane.INFORMATION_MESSAGE);
                        submitTest();
                    }
                }
            });
            countdownTimer.start();
        } else {
            timerLabel.setText("Timer Disabled");
        }
        
        questionsList = new ArrayList<>();
        answerFields = new ArrayList<>();
        questionsPanel.removeAll();
        
        for (int i = 0; i < numQuestions; i++) {
            String type = types.get(random.nextInt(types.size()));
            Question q = createQuestion(type);
            questionsList.add(q);
            
            JPanel qPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            qPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            JLabel qLabel = new JLabel((i + 1) + ". " + q.questionText);
            qLabel.setFont(new Font("Tahoma", Font.PLAIN, 16));
            JTextField answerField = new JTextField(10);
            answerField.setFont(new Font("Tahoma", Font.PLAIN, 16));
            answerFields.add(answerField);
            qPanel.add(qLabel);
            qPanel.add(new JLabel("Your Answer:"));
            qPanel.add(answerField);
            questionsPanel.add(qPanel);
        }
        
        questionsPanel.revalidate();
        questionsPanel.repaint();
        testStartTime = System.currentTimeMillis();
        cardLayout.show(mainPanel, "TEST");
    }
    
    // Format time in seconds to mm:ss.
    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
    
    // Create a question based on type.
    private Question createQuestion(String type) {
        int scale = complexitySlider.getValue();
        // Determine number of operands: 2 for slider 1-4, 3 for 5-8, 4 for 9-10.
        int numOperands = 2 + (scale - 1) / 4;
        switch(type) {
            case "addition": {
                int[] nums = new int[numOperands];
                int sum = 0;
                StringBuilder sb = new StringBuilder("What is ");
                for (int i = 0; i < numOperands; i++) {
                    nums[i] = random.nextInt(100 * scale);
                    sum += nums[i];
                    sb.append(nums[i]);
                    if (i < numOperands - 1) {
                        sb.append(" + ");
                    }
                }
                sb.append(" ?");
                return new Question(sb.toString(), Integer.toString(sum),
                                    "Add the numbers together to get " + sum + ".");
            }
            case "subtraction": {
                // For subtraction, ensure non-negative result.
                int[] nums = new int[numOperands - 1];
                int subtrahend = 0;
                StringBuilder sb = new StringBuilder("What is x");
                for (int i = 0; i < numOperands - 1; i++) {
                    nums[i] = random.nextInt(50 * scale);
                    subtrahend += nums[i];
                    sb.append(" - ").append(nums[i]);
                }
                // Let x be subtrahend plus an extra amount.
                int extra = random.nextInt(50 * scale);
                int first = subtrahend + extra;
                sb.insert(8, first + ""); // Insert x = first
                sb.append(" ?");
                return new Question(sb.toString(), Integer.toString(extra),
                                    "Solve for x: x = " + extra + " because " + first + " - " + subtrahend + " = " + extra + ".");
            }
            case "multiplication": {
                int[] nums = new int[numOperands];
                int product = 1;
                StringBuilder sb = new StringBuilder("What is ");
                for (int i = 0; i < numOperands; i++) {
                    nums[i] = random.nextInt(10 * scale) + 1;
                    product *= nums[i];
                    sb.append(nums[i]);
                    if (i < numOperands - 1) {
                        sb.append(" * ");
                    }
                }
                sb.append(" ?");
                return new Question(sb.toString(), Integer.toString(product),
                                    "Multiply the numbers together to get " + product + ".");
            }
            case "division": {
                // Keep division as two operands for simplicity.
                int b = random.nextInt(19 * scale) + 1;
                int c = random.nextInt(20 * scale);
                int a = b * c;
                return new Question("What is " + a + " / " + b + " ?",
                                    Integer.toString(c),
                                    "Divide " + a + " by " + b + " to get " + c + ".");
            }
            case "equation": {
                // Generate an equation: x + a + b (+ c if numOperands==4) = total.
                int sumOthers = 0;
                StringBuilder sb = new StringBuilder("Solve for x: x");
                for (int i = 0; i < numOperands - 1; i++) {
                    int term = random.nextInt(50 * scale);
                    sumOthers += term;
                    sb.append(" + ").append(term);
                }
                int total = sumOthers + random.nextInt(50 * scale);
                sb.append(" = ").append(total).append(" ?");
                int x = total - sumOthers;
                return new Question(sb.toString(), Integer.toString(x),
                                    "Subtract the sum of the known terms (" + sumOthers + ") from " + total + " to find x = " + x + ".");
            }
            case "squaring": {
                int a = random.nextInt(20 * scale);
                return new Question("What is " + a + "² ?",
                                    Integer.toString(a * a),
                                    "Square " + a + " to get " + (a * a) + ".");
            }
            case "fraction": {
                int numerator1 = random.nextInt(10 * scale) + 1;
                int denominator1 = random.nextInt(9 * scale) + 2;
                int numerator2 = random.nextInt(10 * scale) + 1;
                int denominator2 = random.nextInt(9 * scale) + 2;
                int commonDenom = denominator1;
                int adjustedNumerator2 = numerator2 * commonDenom / denominator2;
                int sum = numerator1 + adjustedNumerator2;
                String question = "What is " + numerator1 + "/" + commonDenom + " + " + numerator2 + "/" + denominator2 + " ?";
                return new Question(question, sum + "/" + commonDenom,
                                    "Convert fractions to have the same denominator and add the numerators.");
            }
            case "exponent": {
                int a = random.nextInt(5 * scale) + 1;
                int b = random.nextInt(3) + 2;
                int result = (int) Math.pow(a, b);
                return new Question("What is " + a + "^" + b + " ?",
                                    Integer.toString(result),
                                    "Raise " + a + " to the power of " + b + " to get " + result + ".");
            }
            case "percentage": {
                int a = random.nextInt(100 * scale) + 1;
                int percent = random.nextInt(50) + 1;
                double answer = a * (percent / 100.0);
                return new Question("What is " + percent + "% of " + a + " ?",
                                    String.format("%.2f", answer),
                                    "Multiply " + a + " by " + (percent/100.0) + " to get the answer.");
            }
            default:
                return new Question("Undefined question type.", "", "No explanation available.");
        }
    }
    
    // Evaluate the answers and show results.
    private void evaluateAnswers() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        int total = questionsList.size();
        int correctCount = 0;
        wrongIndices = new ArrayList<>();
        wrongListModel.clear();
        
        for (int i = 0; i < total; i++) {
            String userAns = answerFields.get(i).getText().trim();
            String correctAns = questionsList.get(i).correctAnswer;
            boolean isCorrect;
            try {
                double userVal = Double.parseDouble(userAns);
                double correctVal = Double.parseDouble(correctAns);
                isCorrect = Math.abs(userVal - correctVal) < 0.0001;
            } catch (NumberFormatException e) {
                isCorrect = userAns.equals(correctAns);
            }
            if (isCorrect) {
                correctCount++;
            } else {
                wrongIndices.add(i);
                wrongListModel.addElement("Question " + (i + 1));
            }
        }
        int percent = (int)((correctCount / (double) total) * 100);
        percentLabel.setText("Percentage Correct: " + percent + "% (" + correctCount + " out of " + total + ")");
        
        long timeTakenMillis = System.currentTimeMillis() - testStartTime;
        long secondsTaken = timeTakenMillis / 1000;
        timeTakenLabel.setText("Time Taken: " + formatTime((int) secondsTaken));
        
        saveResult(percent, secondsTaken);
        loadPreviousResults();
        
        cardLayout.show(mainPanel, "RESULT");
    }
    
    // Called when user submits test manually or time runs out.
    private void submitTest() {
        evaluateAnswers();
    }
    
    // Save the test result to a file.
    private void saveResult(int percent, long secondsTaken) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String record = sdf.format(new Date()) +
                        " - Score: " + percent + "% - Time: " + formatTime((int) secondsTaken) + "\n";
        try (FileWriter fw = new FileWriter("results.txt", true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(record);
        } catch (IOException e) {
            System.err.println("Error saving result: " + e.getMessage());
        }
    }
    
    // Load previous results from file.
    private void loadPreviousResults() {
        StringBuilder sb = new StringBuilder();
        File file = new File("results.txt");
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    sb.append(scanner.nextLine()).append("\n");
                }
            } catch (IOException e) {
                sb.append("Error loading previous results.");
            }
        } else {
            sb.append("No previous results found.");
        }
        previousResultsArea.setText(sb.toString());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MathHomeworkGenerator().setVisible(true));
    }
}
