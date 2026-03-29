package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private Connection conn;
    private static MyConnection instance;

    private String url = "jdbc:mysql://localhost:3306/greenledger";
    private String user = "root";
    private String pwd = "";

    private MyConnection() {
        try {
            conn = DriverManager.getConnection(url, user, pwd);
            System.out.println("Connexion établie !");
        } catch (SQLException e) {
            System.out.println("Erreur connexion DB: " + e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public static Connection getConnection() {
        MyConnection instance = MyConnection.getInstance();
        try {
            return DriverManager.getConnection(instance.url, instance.user, instance.pwd);
        } catch (SQLException e) {
            System.err.println("[DB] Erreur lors de l'ouverture de connexion: " + e.getMessage());
            return null;
        }
    }

    // Fermer la connexion (déconseillé pour le singleton)
    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("[DB] Connexion fermée");
                conn = null;
            } catch (SQLException e) {
                System.err.println("[DB] Erreur lors de la fermeture de la connexion: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Tester la connexion
    public boolean testConnection() {
        try (Connection c = DriverManager.getConnection(url, user, pwd)) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
