import java.io.*;
import java.net.*;
import java.util.*;

// Peer class representing a node in the P2P network
public class Peer {
    private String id;
    private int port;
    private ServerSocket serverSocket;
    private List<InetSocketAddress> knownPeers = new ArrayList<>();
    private boolean running = true;

    public Peer(int port) {
        this.port = port;
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public void start() {
        // Start server thread to listen for incoming connections
        new Thread(this::startServer).start();

        // Start client thread to handle user input
        new Thread(this::startClient).start();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Peer " + id + " listening on port " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void handleConnection(Socket socket) {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()))) {
            
            String message = in.readLine();
            System.out.println("\nReceived message: " + message);
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private void startClient() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                System.out.println("\nEnter peer port to send message (or 'exit' to quit):");
                String input = scanner.nextLine();
                
                if ("exit".equalsIgnoreCase(input)) {
                    running = false;
                    continue;
                }

                try {
                    int peerPort = Integer.parseInt(input);
                    System.out.println("Enter message:");
                    String message = scanner.nextLine();

                    sendMessage(peerPort, message);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number");
                }
            }
        }
        System.exit(0);
    }

    private void sendMessage(int targetPort, String message) {
        try (Socket socket = new Socket("localhost", targetPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            String formattedMessage = String.format("[%s:%d] %s", id, port, message);
            out.println(formattedMessage);
            System.out.println("Message sent to " + targetPort);
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
        
    }
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Peer <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        Peer peer = new Peer(port);
        peer.start();
    }
}
