package Models;

import java.util.ArrayList;
import java.util.List;

public class PolicyOutcome {

    public enum Status {
        OK, WARN, BLOCK
    }

    private Status status;
    private List<String> messages = new ArrayList<>();

    public PolicyOutcome() {
        this.status = Status.OK;
    }

    public PolicyOutcome(Status status) {
        this.status = status;
    }

    public PolicyOutcome(Status status, List<String> messages) {
        this.status = status;
        if (messages != null) {
            this.messages = messages;
        }
    }

    public static PolicyOutcome ok() {
        return new PolicyOutcome(Status.OK);
    }

    public static PolicyOutcome warn(String message) {
        PolicyOutcome o = new PolicyOutcome(Status.WARN);
        o.addMessage(message);
        return o;
    }

    public static PolicyOutcome block(String message) {
        PolicyOutcome o = new PolicyOutcome(Status.BLOCK);
        o.addMessage(message);
        return o;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void addMessage(String message) {
        if (message != null && !message.isEmpty()) {
            this.messages.add(message);
        }
    }
}
