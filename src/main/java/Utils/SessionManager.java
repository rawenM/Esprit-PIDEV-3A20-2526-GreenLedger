package Utils;

import Models.User;

public class SessionManager {

    private static final SessionManager instance = new SessionManager();

    private User currentUser;
    private String token;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return instance;
    }

    public synchronized String createSession(User user) {
        if (user == null) return null;
        this.currentUser = user;
        this.token = java.util.UUID.randomUUID().toString();
        System.out.println("[CLEAN] Session créée pour: " + user.getEmail());
        return this.token;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized String getToken() {
        return token;
    }

    public synchronized boolean isLogged() {
        return currentUser != null && token != null;
    }

    public synchronized void invalidate() {
        if (currentUser != null) {
            System.out.println("🔓 Session invalidée pour: " + currentUser.getEmail());
        }
        currentUser = null;
        token = null;
    }
}

