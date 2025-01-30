import java.io.*;
import java.util.Map;

public class Message implements Serializable {
    private final String id;
    private final String senderId;
    private final String recipientId;
    private final String content;
    private final long timestamp;
    private final Map<String, Integer> vectorClock;
    private final MessageType type;
    
    public Message(String id, String senderId, String recipientId, String content, 
                  long timestamp, Map<String, Integer> vectorClock, MessageType type) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.timestamp = timestamp;
        this.vectorClock = vectorClock;
        this.type = type;
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        return baos.toByteArray();
    }
    
    public static Message fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (Message) ois.readObject();
    }
    
    // Getters
    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getRecipientId() { return recipientId; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Integer> getVectorClock() { return vectorClock; }
    public MessageType getType() { return type; }
}
