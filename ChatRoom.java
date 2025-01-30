import java.io.Serializable;
import java.util.*;

public class ChatRoom implements Serializable {
    private final String roomId;
    private final String name;
    private final Set<String> members;
    private final Set<String> allowedUsers;  // New field for restricted access
    private final String adminId;
    private final List<Message> messageHistory;
    private final boolean isRestricted;  // New field to indicate if group is restricted
    
    public ChatRoom(String roomId, String name, String adminId, boolean isRestricted) {
        this.roomId = roomId;
        this.name = name;
        this.adminId = adminId;
        this.isRestricted = isRestricted;
        this.members = Collections.synchronizedSet(new HashSet<>());
        this.allowedUsers = Collections.synchronizedSet(new HashSet<>());
        this.messageHistory = Collections.synchronizedList(new ArrayList<>());
        this.members.add(adminId);
        if (isRestricted) {
            this.allowedUsers.add(adminId);
        }
        System.out.println("Created new " + (isRestricted ? "restricted " : "") + "group: " + name);
    }
    
    public void addAllowedUser(String userId) {
        if (isRestricted) {
            allowedUsers.add(userId);
            System.out.println("Added " + userId + " to allowed users in group " + name);
        }
    }
    
    public boolean canJoin(String userId) {
        return !isRestricted || allowedUsers.contains(userId);
    }
    
    public void addMember(String nodeId) {
        if (nodeId != null && !nodeId.trim().isEmpty() && canJoin(nodeId)) {
            boolean added = members.add(nodeId);
            if (added) {
                System.out.println("Added member " + nodeId + " to group " + name);
            }
        } else {
            System.out.println("User " + nodeId + " not allowed to join restricted group " + name);
        }
    }
    
    public void removeMember(String nodeId) {
        if (!nodeId.equals(adminId)) {
            members.remove(nodeId);
            if (isRestricted) {
                allowedUsers.remove(nodeId);
            }
            System.out.println("Removed member " + nodeId + " from group " + name);
        }
    }
    
    public void addMessage(Message message) {
        if (members.contains(message.getSenderId())) {
            messageHistory.add(message);
        }
    }
    
    public Set<String> getMembers() {
        return new HashSet<>(members);
    }
    
    public Set<String> getAllowedUsers() {
        return new HashSet<>(allowedUsers);
    }
    
    public List<Message> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
    
    public String getRoomId() { return roomId; }
    public String getName() { return name; }
    public String getAdminId() { return adminId; }
    public boolean isRestricted() { return isRestricted; }
    public boolean isMember(String nodeId) { return members.contains(nodeId); }
}