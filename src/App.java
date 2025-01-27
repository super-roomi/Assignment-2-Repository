
public class App {
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
