import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class ChatGUI extends JFrame {
    private final Node node;
    private JTextArea chatArea;
    private JTextField messageField;
    private DefaultListModel<String> peerListModel;
    private DefaultListModel<String> groupListModel;
    private JList<String> peerList;
    private JList<String> groupList;
    private String currentChat = null;
    private Map<String, List<String>> messageHistory = new HashMap<>();
    private JLabel currentChatLabel;

    public ChatGUI(Node node) {
        this.node = node;
        initUI();
    }

    private void initUI() {
        setTitle("Modern Chat - " + node.getNodeId());
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Current Chat Label at the top
        currentChatLabel = new JLabel("Select a chat to start messaging", SwingConstants.CENTER);
        currentChatLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentChatLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(currentChatLabel, BorderLayout.NORTH);

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Sidebar Panel
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Peers Section
        JPanel peersSection = new JPanel(new BorderLayout());
        peersSection.setBorder(BorderFactory.createTitledBorder("Users"));
        peerListModel = new DefaultListModel<>();
        peerList = new JList<>(peerListModel);
        peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane peerScrollPane = new JScrollPane(peerList);
        JButton addPeerButton = new JButton("Add User");
        peersSection.add(peerScrollPane, BorderLayout.CENTER);
        peersSection.add(addPeerButton, BorderLayout.SOUTH);

        // Groups Section
        JPanel groupsSection = new JPanel(new BorderLayout());
        groupsSection.setBorder(BorderFactory.createTitledBorder("Groups"));
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane groupScrollPane = new JScrollPane(groupList);
        JPanel groupButtonsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JButton createGroupButton = new JButton("Create Group");
        JButton addMemberButton = new JButton("Add Member");
        groupButtonsPanel.add(createGroupButton);
        groupButtonsPanel.add(addMemberButton);
        groupsSection.add(groupScrollPane, BorderLayout.CENTER);
        groupsSection.add(groupButtonsPanel, BorderLayout.SOUTH);

        // Add action listeners
        addPeerButton.addActionListener(e -> addPeer());
        createGroupButton.addActionListener(e -> createGroup());
        addMemberButton.addActionListener(e -> addMemberToGroup());

        // List Selection Listeners
        peerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedPeer = peerList.getSelectedValue();
                if (selectedPeer != null) {
                    groupList.clearSelection();
                    switchChat(selectedPeer, false);
                }
            }
        });

        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedGroup = groupList.getSelectedValue();
                if (selectedGroup != null) {
                    peerList.clearSelection();
                    switchChat(selectedGroup, true);
                }
            }
        });

        // Add sections to sidebar
        sidebarPanel.add(peersSection);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebarPanel.add(groupsSection);

        // Add exit button
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> exitApplication());
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebarPanel.add(exitButton);

        // Add components to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(sidebarPanel, BorderLayout.EAST);

        add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void switchChat(String chatId, boolean isGroup) {
        currentChat = chatId;
        chatArea.setText("");
        currentChatLabel.setText(isGroup ? "Group Chat: " + chatId : "Chat with: " + chatId);
        
        List<String> messages = messageHistory.get(chatId);
        if (messages != null) {
            for (String msg : messages) {
                chatArea.append(msg + "\n");
            }
        }
        messageField.requestFocus();
    }

    private void sendMessage() {
        if (currentChat == null) {
            showSystemMessage("Please select a chat first");
            return;
        }

        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                node.sendMessage(message, currentChat);
                messageField.setText("");
            } catch (Exception e) {
                showSystemMessage("Failed to send message: " + e.getMessage());
            }
        }
    }

    public void appendMessage(String sender, String message, String recipient) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date());
            String formattedMessage = String.format("[%s] %s: %s", timestamp, sender, message);

            messageHistory.computeIfAbsent(recipient, k -> new ArrayList<>()).add(formattedMessage);

            if (currentChat != null && (currentChat.equals(recipient) || currentChat.equals(sender))) {
                chatArea.append(formattedMessage + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
            
            System.out.println("DEBUG: Appending message to GUI - " + formattedMessage);
        });
    }

    private void addPeer() {
        String peerId = JOptionPane.showInputDialog(this, "Enter Peer ID:");
        String peerPort = JOptionPane.showInputDialog(this, "Enter Peer Port:");
        if (peerId != null && peerPort != null) {
            try {
                node.addPeer(peerId, Integer.parseInt(peerPort));
                peerListModel.addElement(peerId);
                showSystemMessage("Peer " + peerId + " added.");
            } catch (Exception e) {
                showSystemMessage("Error adding peer: " + e.getMessage());
            }
        }
    }

    private void createGroup() {
        String groupName = JOptionPane.showInputDialog(this, "Enter Group Name:");
        if (groupName != null) {
            try {
                node.createGroup(groupName);
                groupListModel.addElement(groupName);
                showSystemMessage("Group " + groupName + " created.");
            } catch (Exception e) {
                showSystemMessage("Error creating group: " + e.getMessage());
            }
        }
    }

    private void addMemberToGroup() {
        String selectedGroup = groupList.getSelectedValue();
        if (selectedGroup == null) {
            showSystemMessage("Select a group first.");
            return;
        }

        String newMember = JOptionPane.showInputDialog(this, "Enter New Member ID:");
        if (newMember != null && !newMember.trim().isEmpty()) {
            try {
                node.addMemberToGroup(selectedGroup, newMember);
                showSystemMessage("User " + newMember + " added to " + selectedGroup);
            } catch (Exception e) {
                showSystemMessage("Error adding member: " + e.getMessage());
            }
        }
    }

    public void updateGroupList() {
        groupListModel.clear();
        for (String group : node.getChatRooms().keySet()) {
            groupListModel.addElement(group);
        }
    }

    private void exitApplication() {
        node.shutdown();
        System.exit(0);
    }

    public void showSystemMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}