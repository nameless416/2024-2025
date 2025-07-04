import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PETA extends JFrame {
    private ArrayList<Transaction> transactions = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable transactionTable;
    private String currentUser;
    private static final String USER_FILE = "data/users.txt";
    private static final Map<String, String> userPasswords = new HashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PETA app = new PETA();
            app.loadUsers();
            app.showLoginDialog();
        });
    }

    public PETA() {
        setTitle("Expense Tracker");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void loadUsers() {
        try {
            File file = new File(USER_FILE);
            if (!file.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    userPasswords.put(parts[0], parts[1]);
                }
            }
            reader.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading user data.");
        }
    }

    private void saveUsers() {
        try {
            Files.createDirectories(Paths.get("data"));
            PrintWriter writer = new PrintWriter(new FileWriter(USER_FILE));
            for (String username : userPasswords.keySet()) {
                writer.println(username + ":" + userPasswords.get(username));
            }
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving user data.");
        }
    }

    private void showLoginDialog() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        Object[] fields = {
                "Username:", usernameField,
                "Password:", passwordField
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "Login / Register", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String user = usernameField.getText().trim().toLowerCase();
            String pass = new String(passwordField.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password required.");
                showLoginDialog();
                return;
            }

            String hashedPass = hashPassword(pass);

            if (userPasswords.containsKey(user)) {
                if (!userPasswords.get(user).equals(hashedPass)) {
                    JOptionPane.showMessageDialog(this, "Wrong password.");
                    showLoginDialog();
                    return;
                }
            } else {
                userPasswords.put(user, hashedPass); // Register new user
                saveUsers();
                JOptionPane.showMessageDialog(this, "New account created.");
            }

            currentUser = user;
            loadTransactions();
            initUI();
            setVisible(true);
        } else {
            System.exit(0);
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported");
        }
    }

    private void initUI() {
        String[] columns = {"Type", "Category", "Amount", "Description", "Date"};
        tableModel = new DefaultTableModel(columns, 0);
        transactionTable = new JTable(tableModel);
        add(new JScrollPane(transactionTable), BorderLayout.CENTER);

        for (Transaction t : transactions) {
            tableModel.addRow(new Object[]{
                    t.getType(), t.getCategory(), t.getAmount(), t.getDescription(), t.getDate()
            });
        }

        JButton addButton = new JButton("Add Transaction");
        addButton.addActionListener(e -> openAddDialog());

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveTransactions());

        JButton summaryButton = new JButton("Summary");
        summaryButton.addActionListener(e -> showSummary());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(addButton);
        bottomPanel.add(saveButton);
        bottomPanel.add(summaryButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void openAddDialog() {
        JDialog dialog = new JDialog(this, "Add Transaction", true);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setSize(350, 250);
        dialog.setLocationRelativeTo(this);

        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Income", "Expense"});
        JTextField categoryField = new JTextField();
        JTextField amountField = new JTextField();
        JTextField descriptionField = new JTextField();

        dialog.add(new JLabel("Type:")); dialog.add(typeBox);
        dialog.add(new JLabel("Category:")); dialog.add(categoryField);
        dialog.add(new JLabel("Amount:")); dialog.add(amountField);
        dialog.add(new JLabel("Description:")); dialog.add(descriptionField);

        JButton addBtn = new JButton("Add");
        JButton cancelBtn = new JButton("Cancel");

        dialog.add(addBtn); dialog.add(cancelBtn);

        addBtn.addActionListener(e -> {
            try {
                String type = typeBox.getSelectedItem().toString();
                String category = categoryField.getText().trim();
                double amount = Double.parseDouble(amountField.getText().trim());
                String desc = descriptionField.getText().trim();
                LocalDate date = LocalDate.now();

                if (category.isEmpty() || desc.isEmpty()) throw new Exception();

                Transaction t = new Transaction(type, category, amount, desc, date);
                transactions.add(t);
                tableModel.addRow(new Object[]{type, category, amount, desc, date.toString()});
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid data.");
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void saveTransactions() {
        try {
            Files.createDirectories(Paths.get("data"));
            PrintWriter writer = new PrintWriter(new FileWriter("data/" + currentUser + ".csv"));
            for (Transaction t : transactions) {
                writer.printf("%s,%s,%.2f,%s,%s\n",
                        t.getType(), t.getCategory(), t.getAmount(), t.getDescription(), t.getDate());
            }
            writer.close();
            JOptionPane.showMessageDialog(this, "Transactions saved.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving transactions.");
        }
    }

    private void loadTransactions() {
        transactions.clear();
        Path file = Paths.get("data/" + currentUser + ".csv");
        if (!Files.exists(file)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 5);
                Transaction t = new Transaction(
                        parts[0],
                        parts[1],
                        Double.parseDouble(parts[2]),
                        parts[3],
                        LocalDate.parse(parts[4])
                );
                transactions.add(t);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading transactions.");
        }
    }

    private void showSummary() {
        double income = 0, expense = 0;
        for (Transaction t : transactions) {
            if (t.getType().equalsIgnoreCase("income")) income += t.getAmount();
            else expense += t.getAmount();
        }
        JOptionPane.showMessageDialog(this,
                "Income: $" + income + "\nExpenses: $" + expense + "\nBalance: $" + (income - expense),
                "Summary", JOptionPane.INFORMATION_MESSAGE);
    }
}

class Transaction {
    private String type, category, description;
    private double amount;
    private LocalDate date;

    public Transaction(String type, String category, double amount, String description, LocalDate date) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.date = date;
    }

    public String getType() { return type; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDate getDate() { return date; }
}
