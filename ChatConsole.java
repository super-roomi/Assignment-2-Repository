import java.util.Scanner;

public class ChatConsole {
    private final Node node;
    private final Scanner scanner;
    private boolean running = true;
    private boolean isAuthenticated = false;

    public ChatConsole(Node node) {
        this.node = node;
        this.scanner = new Scanner(System.in);
        authenticateUser();
    }

    private void authenticateUser() {
        System.out.println("Welcome to the Chat Application!");
        while (!isAuthenticated) {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();

            if (node.authenticateUser(username, password)) {
                isAuthenticated = true;
                System.out.println("Authentication successful! Type /help to see available commands.");
                start();
            } else {
                System.out.println("Invalid credentials. Please try again.");
            }
        }
    }

    public void start() {
        printHelp();
        new Thread(this::inputLoop).start();
    }

    private void inputLoop() {
        while (running) {
            try {
                String command = scanner.nextLine().trim();
                processCommand(command);
            } catch (Exception e) {
                System.out.println("Error processing command: " + e.getMessage());
            }
        }
    }

    private void processCommand(String command) throws Exception {
        if (command.isEmpty()) return;

        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/help":
                printHelp();
                break;
                
            case "/msg":
                if (parts.length < 2) {
                    System.out.println("Usage: /msg <userId> <message>");
                    return;
                }
                String[] msgParts = parts[1].split("\\s+", 2);
                if (msgParts.length < 2) {
                    System.out.println("Usage: /msg <userId> <message>");
                    return;
                }
                node.sendMessage(msgParts[1], msgParts[0]);
                break;
                
            case "/logout":
                isAuthenticated = false;
                System.out.println("Logged out successfully.");
                authenticateUser();
                break;

            case "/exit":
                running = false;
                node.shutdown();
                System.out.println("Exiting chat...");
                System.exit(0);
                break;

            default:
                System.out.println("Unknown command. Type /help for available commands.");
        }
    }

    private void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("/msg <userId> <message>   - Send private message");
        System.out.println("/logout                   - Logout from the system");
        System.out.println("/help                     - Show available commands");
        System.out.println("/exit                     - Exit application");
        System.out.println();
    }
}
