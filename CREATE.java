import java.awt.*;
import java.time.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

class StudySession {
    String subject;
    int priority;
    int time;
    LocalDate date;
    int startTime;

    public StudySession(String subject, int priority, int time, LocalDate date, int startTime) {
        this.subject = subject;
        this.priority = priority;
        this.time = time;
        this.date = date;
        this.startTime = startTime;
    }
}

public class CREATE extends JFrame {
    private JTextField subjectField, timeField;
    private JComboBox<Integer> priorityBox;
    private JSpinner dateSpinner, timeSpinner;
    private DefaultListModel<String> scheduleListModel;
    private ArrayList<StudySession> sessions;

    public CREATE() {
        setTitle("Study Session Scheduler");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        sessions = new ArrayList<>();

        // Input Panel with improved layout and padding
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Subject Label and Field
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Subject:"), gbc);
        subjectField = new JTextField(15);
        gbc.gridx = 1;
        inputPanel.add(subjectField, gbc);

        // Study Time Label and Field
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Study Time (mins):"), gbc);
        timeField = new JTextField(10);
        gbc.gridx = 1;
        inputPanel.add(timeField, gbc);

        // Priority Label and ComboBox
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Priority (1-5):"), gbc);
        priorityBox = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5 });
        gbc.gridx = 1;
        inputPanel.add(priorityBox, gbc);

        // Date Label and Spinner
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("Date:"), gbc);
        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        gbc.gridx = 1;
        inputPanel.add(dateSpinner, gbc);

        // Start Time Label and Spinner (the value here is used as a range indicator)
        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(new JLabel("Start Time Range:"), gbc);
        timeSpinner = new JSpinner(new SpinnerNumberModel(15, 15, 240, 15));
        gbc.gridx = 1;
        inputPanel.add(timeSpinner, gbc);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addButton = new JButton("Add Study Session");
        JButton randomizeButton = new JButton("Randomize Inputs");
        JButton sortButton = new JButton("Generate Schedule");
        buttonsPanel.add(addButton);
        buttonsPanel.add(randomizeButton);
        buttonsPanel.add(sortButton);

        // Main panel for inputs and buttons
        JPanel mainInputPanel = new JPanel(new BorderLayout());
        mainInputPanel.add(inputPanel, BorderLayout.CENTER);
        mainInputPanel.add(buttonsPanel, BorderLayout.SOUTH);
        add(mainInputPanel, BorderLayout.NORTH);

        // Schedule List Panel with padding
        scheduleListModel = new DefaultListModel<>();
        JList<String> scheduleList = new JList<>(scheduleListModel);
        JScrollPane scrollPane = new JScrollPane(scheduleList);
        scrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Button actions
        addButton.addActionListener(e -> addStudySession());
        randomizeButton.addActionListener(e -> randomizeInputs());
        sortButton.addActionListener(e -> generateSchedule());
    }

    private void addStudySession() {
        String subject = subjectField.getText().trim();
        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Subject cannot be empty.");
            return;
        }
        int time;
        try {
            time = Integer.parseInt(timeField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid study time. Please enter a number.");
            return;
        }
        int priority = (int) priorityBox.getSelectedItem();
        LocalDate date = ((Date) dateSpinner.getValue()).toInstant()
                           .atZone(ZoneId.systemDefault()).toLocalDate();
        int startTime = validateTime(date);

        if (startTime == -1) {
            JOptionPane.showMessageDialog(this, "Invalid start time for the chosen date.");
            return;
        }

        StudySession session = new StudySession(subject, priority, time, date, startTime);
        sessions.add(session);
        scheduleListModel.addElement(formatSession(session));
        subjectField.setText("");
        timeField.setText("");
    }

    private String formatSession(StudySession session) {
        return String.format("%s - Priority: %d - Time: %d min - Date: %s - Start: %d:00",
                session.subject, session.priority, session.time, session.date, session.startTime);
    }

    private int validateTime(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        Random rand = new Random();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return rand.nextInt(7) + 1; // Generates 1 to 7 (1 PM - 7 PM)
        } else {
            return rand.nextInt(6) + 3; // Generates 3 to 8 (3 PM - 8 PM)
        }
    }

    private void generateSchedule() {
        scheduleListModel.clear();
        // Sorting by priority (descending), then by date (ascending), then by start time (ascending)
        sessions.sort(Comparator.comparingInt((StudySession s) -> -s.priority)
                .thenComparing(s -> s.date)
                .thenComparingInt(s -> s.startTime));
        for (StudySession session : sessions) {
            scheduleListModel.addElement(formatSession(session));
        }
    }

    private void randomizeInputs() {
        String[] subjects = { "Math", "Science", "History", "English", "Computer Science" };
        Random rand = new Random();
        subjectField.setText(subjects[rand.nextInt(subjects.length)]);
        timeField.setText(String.valueOf((rand.nextInt(4) + 1) * 15));
        priorityBox.setSelectedIndex(rand.nextInt(5));
        dateSpinner.setValue(Date.from(LocalDate.now().plusDays(rand.nextInt(7))
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CREATE().setVisible(true));
    }
}
