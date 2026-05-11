package tools;
import DataBase.MyConnection;
import java.sql.*;
public class DBDiag {
    public static void main(String[] args) throws Exception {
        Connection conn = MyConnection.getConnection();
        if (conn == null) { System.err.println("NO DB"); return; }

        System.out.println("=== Projects funded by investor 14 with avoided_tco2 ===");
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT p.id, p.titre, p.roi, p.avoided_tco2, p.montant_demande, f.montant " +
            "FROM financements f JOIN projet p ON p.id=f.project_id " +
            "WHERE f.investisseur_id=14")) {
            ResultSet rs = ps.executeQuery();
            double sumMontant=0, sumAvoided=0, sumRoi=0;
            int cnt=0;
            while (rs.next()) {
                double m = rs.getDouble("montant");
                double a = rs.getDouble("avoided_tco2");
                double r = rs.getDouble("roi");
                sumMontant += m; sumAvoided += a; sumRoi += r; cnt++;
                System.out.printf("  %-25s roi=%-6.2f avoided=%-10.4f montant=%.0f%n",
                    rs.getString("titre"), r, a, m);
            }
            System.out.printf("  TOTAL: cnt=%d sumMontant=%.0f sumAvoided=%.4f sumRoi=%.4f%n",
                cnt, sumMontant, sumAvoided, sumRoi);
            System.out.printf("  avoided/montant*100 = %.4f%%%n", sumAvoided/Math.max(1,sumMontant)*100);
            System.out.printf("  sumRoi/cnt = %.4f%%%n", cnt>0 ? sumRoi/cnt : 0);
        }

        System.out.println("\n=== Check if web uses score_esg as proxy for ROI ===");
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT AVG(p.score_esg) avg_esg, AVG(p.avoided_tco2/NULLIF(p.montant_demande,0))*100 ratio " +
            "FROM financements f JOIN projet p ON p.id=f.project_id " +
            "WHERE f.investisseur_id=14")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                System.out.printf("  avg_esg=%s ratio=%s%n", rs.getString(1), rs.getString(2));
        }

        System.out.println("\n=== Check dispatched_green_credits / montant ===");
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT SUM(p.dispatched_green_credits) dgc, SUM(f.montant) total " +
            "FROM financements f JOIN projet p ON p.id=f.project_id " +
            "WHERE f.investisseur_id=14")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double dgc = rs.getDouble(1), total = rs.getDouble(2);
                System.out.printf("  dgc=%.4f total=%.0f ratio=%.4f%%%n", dgc, total, dgc/Math.max(1,total)*100);
            }
        }

        conn.close();
    }
}
