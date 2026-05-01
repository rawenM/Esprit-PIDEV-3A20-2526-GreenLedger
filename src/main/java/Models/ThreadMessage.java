package Models;

import java.time.LocalDateTime;

/**
 * Thread Message
 * Represents an individual message in a conversation thread
 */
public class ThreadMessage {
    
    private Integer id;
    private Integer threadId;
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;
    private Boolean isRead;
    
    // Relationships
    private ConversationThread thread;
    private User sender;
    
    public ThreadMessage() {
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getThreadId() {
        return threadId;
    }
    
    public void setThreadId(Integer threadId) {
        this.threadId = threadId;
    }
    
    public Long getSenderId() {
        return senderId;
    }
    
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public ConversationThread getThread() {
        return thread;
    }
    
    public void setThread(ConversationThread thread) {
        this.thread = thread;
    }
    
    public User getSender() {
        return sender;
    }
    
    public void setSender(User sender) {
        this.sender = sender;
    }
    
    @Override
    public String toString() {
        return String.format("ThreadMessage[id=%d, thread=%d, sender=%d, read=%b, at=%s]",
                id, threadId, senderId, isRead, sentAt);
    }
}
