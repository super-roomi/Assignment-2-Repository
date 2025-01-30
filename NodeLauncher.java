import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Scanner;

public class NodeLauncher {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java NodeLauncher <nodeId> <port>");
            System.out.println("Example: java NodeLauncher user1 8080");
            return;
        }

        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Create node
            String nodeId = args[0];
            int port = Integer.parseInt(args[1]);
            Node node = new Node(nodeId, port);
            
            // Authenticate user
            Scanner scanner = new Scanner(System.in);
            boolean authenticated = false;
            while (!authenticated) {
                System.out.print("Enter username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Enter password: ");
                String password = scanner.nextLine().trim();

                if (node.authenticateUser(username, password)) {
                    authenticated = true;
                    System.out.println("Authentication successful! Launching chat...");
                } else {
                    System.out.println("Invalid credentials. Try again.");
                }
            }

            // Create and show GUI
            SwingUtilities.invokeLater(() -> {
                try {
                    ChatGUI gui = new ChatGUI(node);
                    node.setGUI(gui);
                    gui.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            });

        } catch (Exception e) {
            System.err.println("Error starting node: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
