package Services;

import DataBase.MyConnection;
import Models.SignatureData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Generates full ESG evaluation PDFs using Apache PDFBox 2.0.29.
 * All string literals use ASCII only to avoid encoding issues.
 */
public class EvaluationPdfService {

    private static final float W      = PDRectangle.A4.getWidth();
    private static final float H      = PDRectangle.A4.getHeight();
    private static final float MARGIN = 45f;
    private static final float LINE   = 14f;
    private static final Color GREEN  = new Color(45, 95, 63);
    private static final Color LGREY  = new Color(243, 244, 246);
    private static final Color DGREY  = new Color(107, 114, 128);

    private final SignatureService sigService = new SignatureService();

    // =========================================================
    // Public API
    // =========================================================

    public byte[] generatePdf(long evaluationId) throws IOException {
        PdfData d = loadData(evaluationId);
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PageState ps = new PageState(doc);
            addHeader(ps, d);
            addProjectInfo(ps, d);
            addScoreSection(ps, d);
            addEmissionsSection(ps, d);
            addCriteriaTable(ps, d);
            addCommentSection(ps, d);
            addFraudSection(ps, d);
            addSignatureSection(ps, d);
            ps.cs.close();          // ← close the last open stream before saving
            addFooters(doc, d.reportDate);
            doc.save(out);
            return out.toByteArray();
        }
    }

    // =========================================================
    // Data loading
    // =========================================================

    private PdfData loadData(long evaluationId) {
        PdfData d = new PdfData();
        d.evaluationId = evaluationId;
        d.reportDate   = LocalDateTime.now();

        try (Connection conn = MyConnection.getConnection()) {
            String sql =
                "SELECT e.id_evaluation, e.id_projet, e.score_final, e.est_valide, " +
                "e.observations_globales, e.date_evaluation, " +
                "p.titre, p.secteur, p.localisation, p.score_esg, " +
                "p.baseline_tco2, p.actual_tco2, p.avoided_tco2, " +
                "p.dispatched_green_credits, p.fraud_risk_score, p.fraud_flag, p.fraud_reasons " +
                "FROM evaluation e LEFT JOIN projet p ON p.id = e.id_projet " +
                "WHERE e.id_evaluation = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, evaluationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        d.projectId       = rs.getInt("id_projet");
                        d.projectName     = rs.getString("titre");
                        d.projectSector   = rs.getString("secteur");
                        d.projectLocation = rs.getString("localisation");
                        d.baselineTco2    = nullDouble(rs, "baseline_tco2");
                        d.actualTco2      = nullDouble(rs, "actual_tco2");
                        d.avoidedTco2     = nullDouble(rs, "avoided_tco2");
                        d.greenCredits    = nullDouble(rs, "dispatched_green_credits");
                        d.fraudRiskScore  = nullDouble(rs, "fraud_risk_score");
                        d.fraudFlag       = rs.getBoolean("fraud_flag");
                        d.fraudReasons    = rs.getString("fraud_reasons");
                        d.evalDate        = rs.getTimestamp("date_evaluation");
                        double sf = rs.getDouble("score_final");
                        d.scoreFinal = rs.wasNull() ? null : sf;
                        d.decision   = rs.getBoolean("est_valide") ? "APPROVED" : "REJECTED";
                        String obs = rs.getString("observations_globales");
                        d.signature = sigService.extractSignature(obs);
                    }
                }
            }

            // carbon_metric
            String cmSql =
                "SELECT scope1_tco2, scope2_tco2, scope3_tco2, total_tco2, method, data_quality_score " +
                "FROM carbon_metric WHERE project_id=? AND evaluation_id=? ORDER BY id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(cmSql)) {
                ps.setInt(1, d.projectId);
                ps.setLong(2, evaluationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        d.scope1       = nullDouble(rs, "scope1_tco2");
                        d.scope2       = nullDouble(rs, "scope2_tco2");
                        d.scope3       = nullDouble(rs, "scope3_tco2");
                        d.totalTco2    = nullDouble(rs, "total_tco2");
                        d.carbonMethod = rs.getString("method");
                        d.dataQuality  = nullDouble(rs, "data_quality_score");
                    }
                }
            }

            // ml_predictions fallback for score
            if (d.scoreFinal == null || d.scoreFinal == 0) {
                String mlSql =
                    "SELECT predicted_esg_score FROM ml_predictions " +
                    "WHERE project_id=? AND evaluation_id=? ORDER BY id DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(mlSql)) {
                    ps.setInt(1, d.projectId);
                    ps.setLong(2, evaluationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int sc = rs.getInt(1);
                            if (!rs.wasNull()) d.scoreFinal = (double) sc;
                        }
                    }
                }
            }

            // criteria
            String crSql =
                "SELECT er.note, er.commentaire_expert, er.est_respecte, cr.nom, cr.poids " +
                "FROM evaluation_resultat er " +
                "LEFT JOIN critere_reference cr ON cr.id_critere = er.id_critere " +
                "WHERE er.id_evaluation = ? ORDER BY er.id_resultat";
            try (PreparedStatement ps = conn.prepareStatement(crSql)) {
                ps.setLong(1, evaluationId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CriteriaRow row = new CriteriaRow();
                        row.name      = rs.getString("nom");
                        row.weight    = nullDouble(rs, "poids");
                        row.score     = rs.getInt("note");
                        row.comment   = rs.getString("commentaire_expert");
                        row.respected = rs.getBoolean("est_respecte");
                        d.criteria.add(row);
                    }
                }
            }

        } catch (SQLException ex) {
            System.err.println("[EvaluationPdf] loadData failed: " + ex.getMessage());
        }
        return d;
    }

    // =========================================================
    // PDF Sections
    // =========================================================

    private void addHeader(PageState ps, PdfData d) throws IOException {
        ps.cs.setNonStrokingColor(GREEN);
        ps.cs.addRect(0, H - 72, W, 72);
        ps.cs.fill();

        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        ps.cs.setNonStrokingColor(Color.WHITE);
        ps.cs.newLineAtOffset(MARGIN, H - 42);
        ps.cs.showText("GreenLedger - Rapport d'Evaluation ESG");
        ps.cs.endText();

        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA, 11);
        ps.cs.setNonStrokingColor(new Color(209, 250, 229));
        ps.cs.newLineAtOffset(MARGIN, H - 58);
        ps.cs.showText(sanitize(safe(d.projectName)));
        ps.cs.endText();

        boolean approved = "APPROVED".equalsIgnoreCase(d.decision);
        String badge = approved ? "APPROUVE" : "REJETE";
        Color badgeBg = approved ? new Color(16, 185, 129) : new Color(239, 68, 68);
        float bw = 80, bh = 22, bx = W - MARGIN - bw, by = H - 52;
        ps.cs.setNonStrokingColor(badgeBg);
        ps.cs.addRect(bx, by, bw, bh);
        ps.cs.fill();
        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        ps.cs.setNonStrokingColor(Color.WHITE);
        ps.cs.newLineAtOffset(bx + 8, by + 7);
        ps.cs.showText(badge);
        ps.cs.endText();

        ps.cs.setNonStrokingColor(Color.BLACK);
        ps.y = H - 90;
    }

    private void addProjectInfo(PageState ps, PdfData d) throws IOException {
        ps.y = sectionTitle(ps, "1. Informations Projet", ps.y);
        String dateStr = d.evalDate != null
            ? d.evalDate.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "-";
        String signer = (d.signature != null && d.signature.isSigned())
            ? d.signature.getSignedByName() : "Non signe";
        ps.y = kv(ps, "Projet",       safe(d.projectName),    ps.y);
        ps.y = kv(ps, "Secteur",      safe(d.projectSector),  ps.y);
        ps.y = kv(ps, "Localisation", safe(d.projectLocation),ps.y);
        ps.y = kv(ps, "Date eval.",   dateStr,                 ps.y);
        ps.y = kv(ps, "Expert",       signer,                  ps.y);
        ps.y -= 6;
    }

    private void addScoreSection(PageState ps, PdfData d) throws IOException {
        ps.y = sectionTitle(ps, "2. Score ESG", ps.y);
        String scoreStr = d.scoreFinal != null
            ? String.format(Locale.ROOT, "%.2f / 10", d.scoreFinal) : "-";
        ps.y = kv(ps, "Score ESG",  scoreStr,         ps.y);
        ps.y = kv(ps, "Decision",   safe(d.decision), ps.y);
        if (d.dataQuality != null)
            ps.y = kv(ps, "Qualite donnees",
                String.format(Locale.ROOT, "%.0f%%", d.dataQuality), ps.y);
        ps.y -= 6;
    }

    private void addEmissionsSection(PageState ps, PdfData d) throws IOException {
        ps.y = sectionTitle(ps, "3. Emissions Carbone", ps.y);
        ps.y = kv(ps, "Scope 1 (transport)", fmt(d.scope1) + " tCO2e", ps.y);
        ps.y = kv(ps, "Scope 2 (energie)",   fmt(d.scope2) + " tCO2e", ps.y);
        ps.y = kv(ps, "Scope 3 (materiaux)", fmt(d.scope3) + " tCO2e", ps.y);
        ps.y = kv(ps, "Total",               fmt(d.totalTco2) + " tCO2e", ps.y);
        if (d.carbonMethod != null)
            ps.y = kv(ps, "Methode", sanitize(d.carbonMethod), ps.y);
        ps.y = kv(ps, "Baseline",   fmt(d.baselineTco2) + " tCO2e", ps.y);
        ps.y = kv(ps, "Reel",       fmt(d.actualTco2)   + " tCO2e", ps.y);
        ps.y = kv(ps, "Evite",      fmt(d.avoidedTco2)  + " tCO2e", ps.y);
        ps.y = kv(ps, "Credits verts", fmt(d.greenCredits) + " credits", ps.y);
        ps.y -= 6;
    }

    private void addCriteriaTable(PageState ps, PdfData d) throws IOException {
        if (d.criteria.isEmpty()) return;
        ps.y = sectionTitle(ps, "4. Criteres d'Evaluation", ps.y);

        float[] cols = {180, 50, 40, 180, 60};
        String[] headers = {"Critere", "Poids", "Note", "Commentaire", "Respecte"};
        ps.y = tableRow(ps, headers, cols, true, ps.y);

        double totalWeight = 0, weightedSum = 0;
        for (CriteriaRow row : d.criteria) {
            if (ps.y < 80) ps = newPage(ps);
            double w = row.weight != null ? row.weight : 1.0;
            totalWeight += w;
            weightedSum += row.score * w;
            String[] cells = {
                safe(row.name),
                row.weight != null ? String.format(Locale.ROOT, "%.1f", row.weight) : "1",
                String.valueOf(row.score),
                safe(row.comment),
                row.respected ? "Oui" : "Non"
            };
            ps.y = tableRow(ps, cells, cols, false, ps.y);
        }

        double avg = totalWeight > 0 ? weightedSum / totalWeight : 0;
        String[] total = {"TOTAL (moy. ponderee)", "",
            String.format(Locale.ROOT, "%.2f", avg), "", ""};
        ps.y = tableRow(ps, total, cols, true, ps.y);
        ps.y -= 6;
    }

    private void addCommentSection(PageState ps, PdfData d) throws IOException {
        if (d.signature == null) return;
        String comment = d.signature.getCommentaireGlobal();
        if (comment == null || comment.isBlank()) return;
        if (ps.y < 100) ps = newPage(ps);
        ps.y = sectionTitle(ps, "5. Commentaire Expert", ps.y);
        ps.y = paragraph(ps, comment, ps.y);
        ps.y -= 6;
    }

    private void addFraudSection(PageState ps, PdfData d) throws IOException {
        if (ps.y < 100) ps = newPage(ps);
        ps.y = sectionTitle(ps, "6. Evaluation Fraude", ps.y);
        String riskStr = d.fraudRiskScore != null
            ? String.format(Locale.ROOT, "%.4f", d.fraudRiskScore)
              + " (" + fraudLevel(d.fraudRiskScore) + ")"
            : "-";
        ps.y = kv(ps, "Score de risque", riskStr, ps.y);
        ps.y = kv(ps, "Fraude detectee", d.fraudFlag ? "OUI" : "NON", ps.y);
        if (d.fraudReasons != null && !d.fraudReasons.isBlank()) {
            for (String reason : d.fraudReasons.split("\\|")) {
                reason = reason.trim();
                if (!reason.isEmpty()) ps.y = textLine(ps, "  - " + reason, ps.y);
            }
        }
        ps.y -= 6;
    }

    private void addSignatureSection(PageState ps, PdfData d) throws IOException {
        if (ps.y < 160) ps = newPage(ps);
        ps.y = sectionTitle(ps, "7. Signature Electronique", ps.y);

        if (d.signature == null || !d.signature.isSigned()) {
            ps.cs.setNonStrokingColor(new Color(254, 243, 199));
            ps.cs.addRect(MARGIN, ps.y - 28, W - 2 * MARGIN, 28);
            ps.cs.fill();
            ps.cs.setNonStrokingColor(Color.BLACK);
            ps.cs.beginText();
            ps.cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            ps.cs.newLineAtOffset(MARGIN + 8, ps.y - 18);
            ps.cs.showText("Cette evaluation n'a pas ete signee electroniquement.");
            ps.cs.endText();
            ps.y -= 36;
            return;
        }

        // Signed box
        ps.cs.setNonStrokingColor(new Color(240, 253, 244));
        ps.cs.addRect(MARGIN, ps.y - 26, W - 2 * MARGIN, 26);
        ps.cs.fill();
        ps.cs.setNonStrokingColor(Color.BLACK);
        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        ps.cs.newLineAtOffset(MARGIN + 8, ps.y - 17);
        ps.cs.showText("Signe electroniquement par "
            + sanitize(d.signature.getSignedByName())
            + "  le  "
            + sanitize(d.signature.getSignedAt()));
        ps.cs.endText();
        ps.y -= 34;

        // Signature image
        String imgData = d.signature.getSignatureImage();
        if (imgData != null && imgData.contains(",")) {
            try {
                String b64 = imgData.substring(imgData.indexOf(',') + 1);
                byte[] imgBytes = Base64.getDecoder().decode(b64);
                PDImageXObject img = PDImageXObject.createFromByteArray(ps.doc, imgBytes, "sig");
                float sigW = 200, sigH = 70;
                ps.cs.drawImage(img, MARGIN, ps.y - sigH, sigW, sigH);
                ps.y -= sigH + 8;
            } catch (Exception e) {
                System.err.println("[EvaluationPdf] Could not embed signature image: " + e.getMessage());
            }
        }

        // Hash line
        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA, 7);
        ps.cs.setNonStrokingColor(DGREY);
        ps.cs.newLineAtOffset(MARGIN, ps.y);
        ps.cs.showText("Hash SHA-256: " + sanitize(safe(d.signature.getSignatureHash())));
        ps.cs.endText();
        ps.y -= 12;

        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA, 8);
        ps.cs.setNonStrokingColor(DGREY);
        ps.cs.newLineAtOffset(MARGIN, ps.y);
        ps.cs.showText("Document signe electroniquement. Verifiez l'authenticite avec le hash ci-dessus.");
        ps.cs.endText();
        ps.cs.setNonStrokingColor(Color.BLACK);
        ps.y -= 14;
    }

    private void addFooters(PDDocument doc, LocalDateTime generated) throws IOException {
        int total = doc.getNumberOfPages();
        String genStr = generated.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        for (int i = 0; i < total; i++) {
            PDPage page = doc.getPage(i);
            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                cs.setStrokingColor(new Color(229, 231, 235));
                cs.moveTo(MARGIN, 36);
                cs.lineTo(W - MARGIN, 36);
                cs.stroke();
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.setNonStrokingColor(DGREY);
                cs.newLineAtOffset(MARGIN, 24);
                cs.showText("GreenLedger - Rapport ESG Confidentiel  |  Genere le " + genStr);
                cs.endText();
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.setNonStrokingColor(DGREY);
                cs.newLineAtOffset(W - MARGIN - 50, 24);
                cs.showText("Page " + (i + 1) + " / " + total);
                cs.endText();
            }
        }
    }

    // =========================================================
    // Layout helpers
    // =========================================================

    private float sectionTitle(PageState ps, String title, float y) throws IOException {
        y -= 4;
        ps.cs.setNonStrokingColor(GREEN);
        ps.cs.addRect(MARGIN, y - 2, W - 2 * MARGIN, 18);
        ps.cs.fill();
        ps.cs.setNonStrokingColor(Color.WHITE);
        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        ps.cs.newLineAtOffset(MARGIN + 6, y + 4);
        ps.cs.showText(sanitize(title));
        ps.cs.endText();
        ps.cs.setNonStrokingColor(Color.BLACK);
        return y - 22;
    }

    private float kv(PageState ps, String key, String value, float y) throws IOException {
        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        ps.cs.setNonStrokingColor(DGREY);
        ps.cs.newLineAtOffset(MARGIN + 4, y);
        ps.cs.showText(sanitize(key) + ":");
        ps.cs.endText();
        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA, 9);
        ps.cs.setNonStrokingColor(Color.BLACK);
        ps.cs.newLineAtOffset(MARGIN + 130, y);
        ps.cs.showText(sanitize(value));
        ps.cs.endText();
        return y - LINE;
    }

    private float textLine(PageState ps, String text, float y) throws IOException {
        ps.cs.beginText();
        ps.cs.setFont(PDType1Font.HELVETICA, 9);
        ps.cs.setNonStrokingColor(Color.BLACK);
        ps.cs.newLineAtOffset(MARGIN + 4, y);
        ps.cs.showText(sanitize(text));
        ps.cs.endText();
        return y - LINE;
    }

    private float paragraph(PageState ps, String text, float y) throws IOException {
        if (text == null || text.isBlank()) return y;
        String[] words = sanitize(text).split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String candidate = cur.length() == 0 ? w : cur + " " + w;
            float tw = PDType1Font.HELVETICA.getStringWidth(candidate) / 1000f * 9f;
            if (tw > W - 2 * MARGIN - 8) {
                y = textLine(ps, cur.toString(), y);
                cur.setLength(0);
                cur.append(w);
                if (y < 60) { ps = newPage(ps); }
            } else {
                cur.setLength(0);
                cur.append(candidate);
            }
        }
        if (cur.length() > 0) y = textLine(ps, cur.toString(), y);
        return y;
    }

    private float tableRow(PageState ps, String[] cells, float[] widths,
                            boolean header, float y) throws IOException {
        float x = MARGIN;
        if (header) {
            ps.cs.setNonStrokingColor(LGREY);
            ps.cs.addRect(MARGIN, y - 14, W - 2 * MARGIN, 16);
            ps.cs.fill();
            ps.cs.setNonStrokingColor(Color.BLACK);
        }
        for (int i = 0; i < cells.length; i++) {
            ps.cs.beginText();
            ps.cs.setFont(header ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 8);
            ps.cs.newLineAtOffset(x + 3, y - 10);
            String cell = cells[i] != null ? sanitize(cells[i]) : "";
            if (cell.length() > 28) cell = cell.substring(0, 26) + "..";
            ps.cs.showText(cell);
            ps.cs.endText();
            x += widths[i];
        }
        ps.cs.setStrokingColor(new Color(229, 231, 235));
        ps.cs.moveTo(MARGIN, y - 14);
        ps.cs.lineTo(W - MARGIN, y - 14);
        ps.cs.stroke();
        return y - 16;
    }

    private PageState newPage(PageState old) throws IOException {
        old.cs.close();
        PDPage page = new PDPage(PDRectangle.A4);
        old.doc.addPage(page);
        old.cs = new PDPageContentStream(old.doc, page);
        old.y  = H - MARGIN;
        return old;
    }

    // =========================================================
    // Utilities
    // =========================================================

    private Double nullDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private String fmt(Double v) {
        return v != null ? String.format(Locale.ROOT, "%.3f", v) : "-";
    }

    private String fraudLevel(double risk) {
        if (risk >= 0.65) return "ELEVE";
        if (risk >= 0.35) return "MOYEN";
        return "FAIBLE";
    }

    private String safe(String s) { return s != null ? s : ""; }

    /** Replace non-WinAnsi characters with ASCII equivalents. */
    private String sanitize(String text) {
        if (text == null) return "";
        return text
            .replace('\u00e9', 'e').replace('\u00e8', 'e').replace('\u00ea', 'e')
            .replace('\u00eb', 'e').replace('\u00e0', 'a').replace('\u00e2', 'a')
            .replace('\u00e4', 'a').replace('\u00f4', 'o').replace('\u00f6', 'o')
            .replace('\u00f9', 'u').replace('\u00fb', 'u').replace('\u00fc', 'u')
            .replace('\u00ee', 'i').replace('\u00ef', 'i').replace('\u00e7', 'c')
            .replace('\u00c9', 'E').replace('\u00c8', 'E').replace('\u00c0', 'A')
            .replace('\u00c2', 'A').replace('\u00d4', 'O').replace('\u00ce', 'I')
            .replace('\u2013', '-').replace('\u2014', '-')
            .replace('\u2018', '\'').replace('\u2019', '\'')
            .replace('\u201c', '"').replace('\u201d', '"')
            .replace('\u2026', '.').replace('\u00a0', ' ')
            .replace('\u2080', '0').replace('\u2082', '2')
            .replace('\u20ac', 'E')
            .replaceAll("[\\p{Cntrl}&&[^\n\r\t]]", "");
    }

    // =========================================================
    // Inner classes
    // =========================================================

    private static class PageState {
        PDDocument doc;
        PDPageContentStream cs;
        float y;

        PageState(PDDocument doc) throws IOException {
            this.doc = doc;
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            this.cs = new PDPageContentStream(doc, page);
            this.y  = H - MARGIN;
        }
    }

    public static class PdfData {
        long   evaluationId;
        int    projectId;
        String projectName, projectSector, projectLocation;
        Double scoreFinal;
        String decision;
        Timestamp evalDate;
        SignatureData signature = new SignatureData("", false);
        Double scope1, scope2, scope3, totalTco2;
        String carbonMethod;
        Double dataQuality;
        Double baselineTco2, actualTco2, avoidedTco2, greenCredits;
        Double fraudRiskScore;
        boolean fraudFlag;
        String fraudReasons;
        List<CriteriaRow> criteria = new ArrayList<>();
        LocalDateTime reportDate;
    }

    public static class CriteriaRow {
        String  name;
        Double  weight;
        int     score;
        String  comment;
        boolean respected;
    }
}
