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
        return MyConnection.getInstance().conn;
    }
}
