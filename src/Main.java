import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class Main extends JFrame {
    private static final String CORRECT_PASSWORD = "apple";

    private JTextField usernameField;
    private JTextArea outputArea;
    private JButton dictionaryButton;
    private JButton bruteForceButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JScrollPane scrollPane;
    private SwingWorker<Boolean, String> currentWorker;

    public Main() {
        super("Password Cracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Username:"));
        usernameField = new JTextField(15);
        inputPanel.add(usernameField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dictionaryButton = new JButton("Dictionary Attack");
        bruteForceButton = new JButton("Brute Force Attack");
        buttonPanel.add(dictionaryButton);
        buttonPanel.add(bruteForceButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        scrollPane = new JScrollPane(outputArea);

        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusLabel = new JLabel("Ready");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel);

        dictionaryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText().trim();
                if (username.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            Main.this,
                            "Please enter a username",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                startDictionaryAttack(username);
            }
        });

        bruteForceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText().trim();
                if (username.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            Main.this,
                            "Please enter a username",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                startBruteForceAttack(username);
            }
        });
    }

    private void startDictionaryAttack(String username) {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        outputArea.setText("");
        progressBar.setValue(0);
        setButtonsEnabled(false);

        appendOutput("Attempting to crack password for user: " + username + "\n");
        appendOutput("Starting dictionary attack...\n");

        currentWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return dictionaryAttack();
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    appendOutput(chunk);
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (!success) {
                        appendOutput("\nDictionary attack failed. Try brute force attack.\n");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    appendOutput("\nError: " + e.getMessage() + "\n");
                } catch (CancellationException e) {
                    appendOutput("\nAttack was cancelled.\n");
                }

                setButtonsEnabled(true);
                statusLabel.setText("Finished Dictionary Attack");
                progressBar.setValue(100);
            }

            private boolean dictionaryAttack() {
                try {
                    File file = new File("dictionary.txt");
                    if (!file.exists()) {
                        publish("Error: dictionary.txt file not found.\n");
                        publish("Creating a sample dictionary file with common passwords...\n");

                        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                            String[] commonPasswords = {
                                    "password", "123456", "qwerty", "admin", "welcome",
                                    "monkey", "letmein", "dragon", "baseball", "football",
                                    "shadow", "master", "superman", "apple", "iloveyou"
                            };

                            for (String pass : commonPasswords) {
                                writer.println(pass);
                            }

                            publish("Created dictionary.txt with 15 common passwords.\n");
                        } catch (IOException e) {
                            publish("Failed to create dictionary file: " + e.getMessage() + "\n");
                            return false;
                        }
                    }

                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String word;
                        int totalWords = countLines(file.getPath());

                        publish("Dictionary contains " + totalWords + " words.\n");

                        int attemptCount = 0;
                        while ((word = reader.readLine()) != null) {
                            if (isCancelled()) {
                                return false;
                            }

                            attemptCount++;
                            final int currentAttempt = attemptCount;

                            int progress = (int) ((double) currentAttempt / totalWords * 100);
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(progress);
                                statusLabel.setText("Dictionary attack: " + currentAttempt + "/" + totalWords);
                            });

                            publish("Trying: " + word + "\n");

                            if (word.equals(CORRECT_PASSWORD)) {
                                publish("\nPassword found after " + currentAttempt + " dictionary attempts!\n");
                                publish("The password is: " + word + "\n");
                                return true;
                            }

                            Thread.sleep(50);
                        }

                        publish("\nDictionary attack failed after " + attemptCount + " attempts.\n");
                        return false;
                    }
                } catch (IOException | InterruptedException e) {
                    publish("Error in dictionary attack: " + e.getMessage() + "\n");
                    return false;
                }
            }

            private int countLines(String filename) throws IOException {
                try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                    int lines = 0;
                    while (reader.readLine() != null) lines++;
                    return lines;
                }
            }
        };

        currentWorker.execute();
    }

    private void startBruteForceAttack(String username) {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        outputArea.setText("");
        progressBar.setValue(0);
        setButtonsEnabled(false);

        appendOutput("Attempting to crack password for user: " + username + "\n");
        appendOutput("Starting brute force attack...\n");

        currentWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return bruteForceAttack();
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    appendOutput(chunk);
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (!success) {
                        appendOutput("\nBrute force attack failed after exhausting all possibilities.\n");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    appendOutput("\nError: " + e.getMessage() + "\n");
                } catch (CancellationException e) {
                    appendOutput("\nAttack was cancelled.\n");
                }

                setButtonsEnabled(true);
                statusLabel.setText("Finished Brute Force Attack");
                progressBar.setValue(100);
            }

            private boolean bruteForceAttack() throws InterruptedException {
                publish("Starting brute force attack...\n");
                publish("Trying all possible 5-character combinations (A-Z, a-z)...\n");

                char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
                char[] password = new char[5];

                int firstLetterCount = chars.length;

                for (int i = 0; i < chars.length; i++) {
                    if (isCancelled()) {
                        return false;
                    }

                    final int currentFirstLetter = i + 1;
                    password[0] = chars[i];

                    int progress = (int) ((double) currentFirstLetter / firstLetterCount * 100);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        statusLabel.setText("Brute force: " + currentFirstLetter + "/" + firstLetterCount + " (first letter variations)");
                    });

                    publish("Trying combinations starting with: " + chars[i] + "\n");

                    if (bruteForceRecursive(chars, password, 1)) {
                        return true;
                    }
                }

                publish("\nBrute force attack failed after exhausting all possibilities.\n");
                return false;
            }

            private boolean bruteForceRecursive(char[] chars, char[] password, int position) throws InterruptedException {
                if (isCancelled()) {
                    return false;
                }

                if (position == password.length) {
                    String attempt = new String(password);

                    if (Math.random() < 0.01) {
                        publish("Trying: " + attempt + "\n");
                        Thread.sleep(10);
                    }

                    if (attempt.equals(CORRECT_PASSWORD)) {
                        publish("\nPassword found through brute force!\n");
                        publish("The password is: " + attempt + "\n");
                        return true;
                    }
                    return false;
                }

                for (char c : chars) {
                    password[position] = c;
                    if (bruteForceRecursive(chars, password, position + 1)) {
                        return true;
                    }
                }

                return false;
            }
        };

        currentWorker.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        dictionaryButton.setEnabled(enabled);
        bruteForceButton.setEnabled(enabled);
    }

    private void appendOutput(String text) {
        outputArea.append(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            Main gui = new Main();
            gui.setVisible(true);
        });
    }
}
