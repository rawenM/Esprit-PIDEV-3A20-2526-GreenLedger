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

    public static Connection getConnection() throws SQLException {
        MyConnection inst = MyConnection.getInstance();
        return DriverManager.getConnection(inst.url, inst.user, inst.pwd);
    }

    // Fermer la connexion
    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("[CLEAN] Connexion fermée");
            } catch (SQLException e) {
                System.err.println("[CLEAN] Erreur lors de la fermeture de la connexion: " + e.getMessage());
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
