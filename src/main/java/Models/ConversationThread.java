package Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation Thread
 * Represents a message thread between investor and project holder
 */
public class ConversationThread {
    
    private Integer id;
    private Integer projectId;
    private Long investisseurId;
    private Long porteurId;
    private LocalDateTime createdAt;
    
    // Relationships
    private Projet projet;
    private User investisseur;
    private User porteur;
    private List<ThreadMessage> messages;
    
    public ConversationThread() {
        this.messages = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }
    
    public Long getInvestisseurId() {
        return investisseurId;
    }
    
    public void setInvestisseurId(Long investisseurId) {
        this.investisseurId = investisseurId;
    }
    
    public Long getPorteurId() {
        return porteurId;
    }
    
    public void setPorteurId(Long porteurId) {
        this.porteurId = porteurId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Projet getProjet() {
        return projet;
    }
    
    public void setProjet(Projet projet) {
        this.projet = projet;
    }
    
    public User getInvestisseur() {
        return investisseur;
    }
    
    public void setInvestisseur(User investisseur) {
        this.investisseur = investisseur;
    }
    
    public User getPorteur() {
        return porteur;
    }
    
    public void setPorteur(User porteur) {
        this.porteur = porteur;
    }
    
    public List<ThreadMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ThreadMessage> messages) {
        this.messages = messages;
    }
    
    public void addMessage(ThreadMessage message) {
        this.messages.add(message);
        message.setThread(this);
    }
    
    @Override
    public String toString() {
        return String.format("ConversationThread[id=%d, project=%d, investor=%d, holder=%d, messages=%d]",
                id, projectId, investisseurId, porteurId, messages.size());
    }
}
