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
            System.out.println("Erreur connexion DB");
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
            // Vérifier si la connexion est fermée et la rouvrir si nécessaire
            if (instance.conn == null || instance.conn.isClosed()) {
                System.out.println("[DB] Reconnexion à la base de données...");
                instance.conn = DriverManager.getConnection(instance.url, instance.user, instance.pwd);
                System.out.println("[DB] Reconnexion réussie!");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur lors de la reconnexion: " + e.getMessage());
            try {
                instance.conn = DriverManager.getConnection(instance.url, instance.user, instance.pwd);
            } catch (SQLException ex) {
                System.err.println("[DB] Impossible de se reconnecter: " + ex.getMessage());
            }
        }
        return instance.conn;
    }

    // Fermer la connexion (déconseillé pour le singleton)
    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("[DB] Connexion fermée");
                // Réinitialiser l'instance pour forcer une reconnexion
                conn = null;
            } catch (SQLException e) {
                System.err.println("[DB] Erreur lors de la fermeture de la connexion");
                e.printStackTrace();
            }
        }
    }

    // Tester la connexion
    public boolean testConnection() {
        try {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}