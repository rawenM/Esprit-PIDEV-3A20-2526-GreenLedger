import java.sql.*;
public class DBCheck {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/greenledger","root","");
        // Check wallet table
        PreparedStatement ps = conn.prepareStatement("SELECT owner_type, owner_id, COUNT(*) cnt, SUM(available_credits) total FROM wallet GROUP BY owner_type, owner_id ORDER BY total DESC LIMIT 10");
        ResultSet rs = ps.executeQuery();
        System.out.println("=== WALLET TABLE ===");
        while(rs.next()) System.out.println("owner_type="+rs.getString(1)+" owner_id="+rs.getInt(2)+" cnt="+rs.getInt(3)+" total="+rs.getDouble(4));
        // Check user table for investisseurs
        ps = conn.prepareStatement("SELECT id, email, type_utilisateur FROM user WHERE type_utilisateur='INVESTISSEUR' LIMIT 5");
        rs = ps.executeQuery();
        System.out.println("=== INVESTISSEUR USERS ===");
        while(rs.next()) System.out.println("id="+rs.getLong(1)+" email="+rs.getString(2));
        // Check financements
        ps = conn.prepareStatement("SELECT investisseur_id, COUNT(*) cnt, COUNT(DISTINCT project_id) projs FROM financements GROUP BY investisseur_id LIMIT 5");
        rs = ps.executeQuery();
        System.out.println("=== FINANCEMENTS ===");
        while(rs.next()) System.out.println("investisseur_id="+rs.getLong(1)+" cnt="+rs.getInt(2)+" projs="+rs.getInt(3));
        conn.close();
    }
}
