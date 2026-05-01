package tools;

import DataBase.MyConnection;
import java.sql.*;

/**
 * Quick diagnostic: prints all columns in the projet table
 */
public class CheckProjetColumns {
    public static void main(String[] args) throws Exception {
        try (Connection conn = MyConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet cols = meta.getColumns(null, null, "projet", null);
            System.out.println("=== COLUMNS IN 'projet' TABLE ===");
            while (cols.next()) {
                System.out.printf("  %-40s %s%n",
                    cols.getString("COLUMN_NAME"),
                    cols.getString("TYPE_NAME"));
            }

            // Also check a sample row
            System.out.println("\n=== SAMPLE ROW ===");
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, titre, secteur, date_creation, fraud_risk_score, " +
                    "dispatched_green_credits, statut_financement FROM projet LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("  id:                      " + rs.getObject("id"));
                    System.out.println("  titre:                   " + rs.getObject("titre"));
                    System.out.println("  secteur:                 " + rs.getObject("secteur"));
                    System.out.println("  date_creation:           " + rs.getObject("date_creation"));
                    System.out.println("  fraud_risk_score:        " + rs.getObject("fraud_risk_score"));
                    System.out.println("  dispatched_green_credits:" + rs.getObject("dispatched_green_credits"));
                    System.out.println("  statut_financement:      " + rs.getObject("statut_financement"));
                } else {
                    System.out.println("  (no rows)");
                }
            } catch (SQLException e) {
                System.out.println("  Sample query failed: " + e.getMessage());
            }
        }
    }
}
