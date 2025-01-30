import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginGUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckBox;
    private JButton loginButton;
    private JLabel statusLabel;
    
    private Node node;

    public LoginGUI(Node node) {
        this.node = node;
        initUI();
    }

    private void initUI() {
        setTitle("ChatApp - Login");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // **Main Panel with Light Theme**
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Welcome to ChatApp", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        // **Username Field**
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // **Password Field**
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // **Show Password Checkbox**
        showPasswordCheckBox = new JCheckBox("Show Password");
        showPasswordCheckBox.setBackground(Color.WHITE);
        showPasswordCheckBox.addActionListener(e -> {
            if (showPasswordCheckBox.isSelected()) {
                passwordField.setEchoChar((char) 0); // Show password
            } else {
                passwordField.setEchoChar('•'); // Hide password
            }
        });

        // **Login Button**
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setBackground(new Color(30, 144, 255)); // DodgerBlue
        loginButton.setForeground(Color.BLACK);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        loginButton.addActionListener(new LoginAction());

        // **Status Message**
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);

        // **Adding Components**
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);
        gbc.gridy++;
        mainPanel.add(usernameLabel, gbc);
        gbc.gridy++;
        mainPanel.add(usernameField, gbc);
        gbc.gridy++;
        mainPanel.add(passwordLabel, gbc);
        gbc.gridy++;
        mainPanel.add(passwordField, gbc);
        gbc.gridy++;
        mainPanel.add(showPasswordCheckBox, gbc);
        gbc.gridy++;
        mainPanel.add(loginButton, gbc);
        gbc.gridy++;
        mainPanel.add(statusLabel, gbc);

        add(mainPanel, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (node.authenticateUser(username, password)) {
                statusLabel.setText("Login Successful!");
                dispose(); // Close the login window

                // **Launch Chat GUI**
                SwingUtilities.invokeLater(() -> {
                    ChatGUI chatGUI = new ChatGUI(node);
                    node.setGUI(chatGUI);
                    chatGUI.setVisible(true);
                });

            } else {
                statusLabel.setText("Invalid credentials. Try again.");
            }
        }
    }
}
