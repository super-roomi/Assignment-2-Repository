import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.SwingUtilities;

import java.time.Instant;

public class Node {
    private static final long HEARTBEAT_INTERVAL = 5000;
    private static final long SUSPECTED_TIMEOUT = 15000;
    private static final String ENCRYPTION_KEY = "MySecretKey12345";

    private final String nodeId;
    private final int port;
    private ChatGUI gui;
    
    private final Map<String, PeerInfo> peers;
    private final Map<String, Integer> vectorClock;
    private final PriorityBlockingQueue<Message> messageQueue;
    private final List<Message> messageLog;
    private final Map<String, ChatRoom> chatRooms;
    private final Map<String, Long> lastHeartbeat;
    private final Map<String, Integer> messageCount;
    private final Queue<Message> offlineMessages;

    private final SecretKey encryptionKey;
    private boolean isRunning;
    private DatagramSocket socket;

    public Node(String nodeId, int port) throws Exception {
        this.nodeId = nodeId;
        this.port = port;
        
        this.peers = new ConcurrentHashMap<>();
        this.vectorClock = new ConcurrentHashMap<>();
        this.messageQueue = new PriorityBlockingQueue<>(11, 
            Comparator.comparingLong(Message::getTimestamp));
        this.messageLog = Collections.synchronizedList(new ArrayList<>());
        this.chatRooms = new ConcurrentHashMap<>();
        this.lastHeartbeat = new ConcurrentHashMap<>();
        this.messageCount = new ConcurrentHashMap<>();
        this.offlineMessages = new ConcurrentLinkedQueue<>();

        byte[] keyBytes = ENCRYPTION_KEY.getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        keyBytes = sha.digest(keyBytes);
        keyBytes = Arrays.copyOf(keyBytes, 16);
        this.encryptionKey = new SecretKeySpec(keyBytes, "AES");
        
        this.isRunning = true;
        initializeSocket();
        startServices();
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setGUI(ChatGUI gui) {
        this.gui = gui;
    }

    private void initializeSocket() throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    private void startServices() {
        new Thread(() -> messageListenerService(), "MessageListener").start();
        new Thread(() -> heartbeatService(), "HeartbeatService").start();
        new Thread(() -> messageProcessorService(), "MessageProcessor").start();
    }

    public boolean authenticateUser(String username, String password) {
        return username.equals("admin") && password.equals("password123");
    }

    private void messageListenerService() {
        byte[] buffer = new byte[4096];
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handleIncomingPacket(packet);
            } catch (IOException e) {
                if (isRunning) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleIncomingPacket(DatagramPacket packet) {
        try {
            byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
            byte[] decryptedData = decrypt(data);
            Message message = Message.fromBytes(decryptedData);
            
            System.out.println("DEBUG: Received packet from: " + packet.getAddress() + ":" + packet.getPort());
            System.out.println("DEBUG: Message type: " + message.getType());
            System.out.println("DEBUG: From: " + message.getSenderId() + " To: " + message.getRecipientId());
            System.out.println("DEBUG: Content: " + message.getContent());

            updateVectorClock(message.getVectorClock());

            switch (message.getType()) {
                case CHAT:
                    System.out.println("DEBUG: Processing chat message");
                    if (gui != null) {
                        SwingUtilities.invokeLater(() -> {
                            gui.appendMessage(message.getSenderId(), message.getContent(), nodeId);
                        });
                    }
                    break;
                    
                case HEARTBEAT:
                    updateHeartbeat(message.getSenderId());
                    break;
                    
                case GROUP_CHAT:
                    handleGroupMessage(message);
                    break;

                case JOIN_GROUP:
                    handleGroupJoin(message);
                    break;

                case GROUP_INFO:
                    handleGroupInfo(message);
                    break;
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error processing packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGroupInfo(Message message) {
        String[] parts = message.getContent().split(":", 2);
        if (parts.length == 2) {
            String groupId = parts[0];
            String[] members = parts[1].split(",");
            
            ChatRoom room = chatRooms.get(groupId);
            if (room != null) {
                for (String member : members) {
                    if (!member.isEmpty()) {
                        room.addMember(member);
                    }
                }
                
                // Update GUI
                if (gui != null) {
                    SwingUtilities.invokeLater(() -> gui.updateGroupList());
                }
            }
        }
    }

    private void handleGroupJoin(Message message) {
        String groupId = message.getContent(); // Group ID is in the content
        System.out.println("DEBUG: Handling group join notification for group: " + groupId);
    
        // Create group if it doesn't exist locally
        if (!chatRooms.containsKey(groupId)) {
            ChatRoom newRoom = new ChatRoom(groupId, "Group-" + groupId, message.getSenderId(), false);
            chatRooms.put(groupId, newRoom);
            System.out.println("DEBUG: Created new group locally: " + groupId);
        }
    
        // Get the group
        ChatRoom room = chatRooms.get(groupId);
        
        // Add self to group members
        room.addMember(nodeId);
        
        // Add sender to group members if not already present
        if (!room.getMembers().contains(message.getSenderId())) {
            room.addMember(message.getSenderId());
        }
    
        // Update GUI - Only show join message if we are the one being added
        if (gui != null && message.getRecipientId().equals(nodeId)) {
            SwingUtilities.invokeLater(() -> {
                gui.updateGroupList();
                gui.showSystemMessage("You have added someone to the group: ");
            });
        } else if (gui != null) {
            // Just update the group list for other members
            SwingUtilities.invokeLater(() -> {
                gui.updateGroupList();
            });
        }
    
        System.out.println("DEBUG: Successfully processed group join for: " + groupId);
    }
    private void handleGroupMessage(Message message) {
        String groupId = message.getRecipientId();
        ChatRoom room = chatRooms.get(groupId);
        
        if (room != null && room.getMembers().contains(nodeId)) {
            System.out.println("DEBUG: Received group message for group: " + groupId);
            
            // Display the message in GUI
            if (gui != null) {
                gui.appendMessage(message.getSenderId(), message.getContent(), groupId);
            }
            
            // Store in message log
            messageLog.add(message);
        } else {
            System.out.println("DEBUG: Received message for unknown/non-member group: " + groupId);
        }
    }

    private void updateVectorClock(Map<String, Integer> receivedClock) {
        for (Map.Entry<String, Integer> entry : receivedClock.entrySet()) {
            vectorClock.merge(entry.getKey(), entry.getValue(), Integer::max);
        }
    }

    private void heartbeatService() {
        while (isRunning) {
            try {
                sendHeartbeat();
                checkPeerHealth();
                Thread.sleep(HEARTBEAT_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void messageProcessorService() {
        while (isRunning) {
            try {
                Message message = messageQueue.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    processMessage(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processMessage(Message message) {
        try {
            if (message.getType() == MessageType.CHAT) {
                if (gui != null) {
                    gui.appendMessage(message.getSenderId(), message.getContent(), message.getRecipientId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String content, String recipientId) throws Exception {
        System.out.println("DEBUG: Attempting to send message to: " + recipientId);
        
        // Check if this is a group message
        if (chatRooms.containsKey(recipientId)) {
            // This is a group message
            ChatRoom room = chatRooms.get(recipientId);
            System.out.println("DEBUG: Sending group message to room: " + recipientId);
            
            Message groupMessage = new Message(
                UUID.randomUUID().toString(),
                nodeId,
                recipientId,  // recipientId here is the group ID
                content,
                System.currentTimeMillis(),
                new HashMap<>(vectorClock),
                MessageType.GROUP_CHAT
            );
    
            // Send to all group members except self
            for (String memberId : room.getMembers()) {
                if (!memberId.equals(nodeId) && peers.containsKey(memberId)) {
                    System.out.println("DEBUG: Forwarding group message to member: " + memberId);
                    sendToPeer(groupMessage, memberId);
                }
            }
    
            // Display message in own GUI
            if (gui != null) {
                gui.appendMessage("You", content, recipientId);
            }
        } else {
            // This is a private message
            if (!peers.containsKey(recipientId)) {
                System.out.println("DEBUG: Peer not found: " + recipientId);
                throw new IllegalArgumentException("Peer not found: " + recipientId);
            }
    
            Message privateMessage = new Message(
                UUID.randomUUID().toString(),
                nodeId,
                recipientId,
                content,
                System.currentTimeMillis(),
                new HashMap<>(vectorClock),
                MessageType.CHAT
            );
    
            sendToPeer(privateMessage, recipientId);
            
            if (gui != null) {
                gui.appendMessage("You", content, recipientId);
            }
        }
    }

    private void sendToPeer(Message message, String peerId) throws Exception {
        PeerInfo peer = peers.get(peerId);
        System.out.println("DEBUG: Attempting to send to peer: " + peerId);
        
        if (peer != null) {
            System.out.println("DEBUG: Peer found. Status: " + peer.getStatus());
            System.out.println("DEBUG: Peer address: " + peer.getAddress() + ":" + peer.getPort());
            
            if (peer.getStatus() == NodeStatus.ACTIVE) {
                byte[] encryptedData = encrypt(message.toBytes());
                DatagramPacket packet = new DatagramPacket(
                    encryptedData,
                    encryptedData.length,
                    peer.getAddress(),
                    peer.getPort()
                );
                socket.send(packet);
                System.out.println("DEBUG: Message sent successfully");
            } else {
                System.out.println("DEBUG: Peer is not active");
                offlineMessages.add(message);
            }
        } else {
            System.out.println("DEBUG: Peer is null");
            offlineMessages.add(message);
        }
    }

    private void sendHeartbeat() throws Exception {
        Message heartbeat = new Message(
            UUID.randomUUID().toString(),
            nodeId,
            null,
            "",
            System.currentTimeMillis(),
            new HashMap<>(vectorClock),
            MessageType.HEARTBEAT
        );

        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            if (entry.getValue().getStatus() != NodeStatus.INACTIVE) {
                sendToPeer(heartbeat, entry.getKey());
            }
        }
    }

    private void updateHeartbeat(String senderId) {
        lastHeartbeat.put(senderId, System.currentTimeMillis());
        PeerInfo peer = peers.get(senderId);
        if (peer != null && peer.getStatus() != NodeStatus.ACTIVE) {
            peer.setStatus(NodeStatus.ACTIVE);
        }
    }

    private void checkPeerHealth() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastHeartbeat.entrySet()) {
            if (now - entry.getValue() > SUSPECTED_TIMEOUT) {
                PeerInfo peer = peers.get(entry.getKey());
                if (peer != null && peer.getStatus() != NodeStatus.SUSPECTED) {
                    peer.setStatus(NodeStatus.SUSPECTED);
                }
            }
        }
    }

    private byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
        return cipher.doFinal(data);
    }

    public void addPeer(String peerId, int port) {
        try {
            System.out.println("DEBUG: Adding peer: " + peerId + " on port " + port);
            InetAddress address = InetAddress.getByName("localhost");
            PeerInfo peer = new PeerInfo(peerId, address, port, NodeStatus.ACTIVE);
            peers.put(peerId, peer);
            System.out.println("DEBUG: Peer added successfully");
            System.out.println("DEBUG: Current peers: " + peers.keySet());
        } catch (Exception e) {
            System.out.println("DEBUG: Error adding peer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, ChatRoom> getChatRooms() {
        return chatRooms;
    }

    public void createGroup(String groupId) {
        ChatRoom room = new ChatRoom(groupId, "Group-" + groupId, nodeId, true);
        chatRooms.put(groupId, room);
        room.addMember(nodeId);
    }

    public void addMemberToGroup(String groupId, String newMemberId) {
        System.out.println("DEBUG: Adding member " + newMemberId + " to group " + groupId);
        
        ChatRoom room = chatRooms.get(groupId);
        if (room == null) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
    
        // Add member to local group
        room.addMember(newMemberId);
    
        // Create and send group join notification to the new member
        try {
            Message joinNotification = new Message(
                UUID.randomUUID().toString(),
                nodeId,
                newMemberId,
                groupId,  // Send group ID in content
                System.currentTimeMillis(),
                new HashMap<>(vectorClock),
                MessageType.JOIN_GROUP
            );
    
            // Send notification to the new member
            sendToPeer(joinNotification, newMemberId);
            System.out.println("DEBUG: Sent group join notification to " + newMemberId);
    
            // Also send the current group member list
            StringBuilder memberList = new StringBuilder();
            for (String member : room.getMembers()) {
                memberList.append(member).append(",");
            }
            
            Message memberListMessage = new Message(
                UUID.randomUUID().toString(),
                nodeId,
                newMemberId,
                groupId + ":" + memberList.toString(),
                System.currentTimeMillis(),
                new HashMap<>(vectorClock),
                MessageType.GROUP_INFO
            );
            
            sendToPeer(memberListMessage, newMemberId);
    
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to send group join notification: " + e.getMessage());
            e.printStackTrace();
        }
    
        // Update GUI
        if (gui != null) {
            gui.showSystemMessage("User " + newMemberId + " added to " + groupId);
            gui.updateGroupList();
        }
    }

    public void shutdown() {
        isRunning = false;
        if (socket != null) {
            socket.close();
        }
    }
}