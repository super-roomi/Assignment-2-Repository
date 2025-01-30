import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Main <nodeId> <port>");
            System.out.println("Example: java Main user1 8080");
            return;
        }

        try {
            // Set system look and feel for UI consistency
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Get command line arguments
            String nodeId = args[0];
            int port = Integer.parseInt(args[1]);

            // Create node instance
            Node node = new Node(nodeId, port);

            // Start the login GUI
            SwingUtilities.invokeLater(() -> new LoginGUI(node));

        } catch (Exception e) {
            System.err.println("Error starting node: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
