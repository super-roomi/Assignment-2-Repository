import java.net.InetAddress;

public class PeerInfo {
    private String peerId;
    private InetAddress address;
    private int port;
    private NodeStatus status;

    public PeerInfo(String peerId, InetAddress address, int port, NodeStatus status) {
        this.peerId = peerId;
        this.address = address;
        this.port = port;
        this.status = status;
    }

    public String getPeerId() {
        return peerId;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }
}
