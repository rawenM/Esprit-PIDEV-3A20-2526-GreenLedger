package Services;

import Models.OffreFinancement;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PdfOffresReportService
 * ----------------------
 * Generates a professional offline review PDF of all financing offers.
 * Uses PDFShift API to convert a styled HTML report to PDF.
 *
 * CALLED FROM: FinancementController.exporterRapportOffres()
 *
 * @param offres   Full list of OffreFinancement from the table
 * @param outputPath  Where to save the PDF (from FileChooser)
 */
public class PdfOffresReportService {

    private static final String API_KEY = "sk_0b6b0385c48e47e66fa73e311ec731bd2e781de7";
    private static final String API_URL = "https://api.pdfshift.io/v3/convert/pdf";

    public void generateOffresReport(List<OffreFinancement> offres, String outputPath) throws Exception {

        String html = buildReportHtml(offres);

        JSONObject payload = new JSONObject();
        payload.put("source", html);
        payload.put("landscape", false);
        payload.put("use_print", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("X-API-Key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<java.io.InputStream> response = client.send(
                request, HttpResponse.BodyHandlers.ofInputStream()
        );

        int statusCode = response.statusCode();
        if (statusCode == 200 || statusCode == 201) {
            File outputFile = new File(outputPath);
            Files.copy(response.body(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ Offres report saved to: " + outputPath);
        } else {
            String errorBody = new String(response.body().readAllBytes());
            throw new Exception("PDFShift API error " + statusCode + ": " + errorBody);
        }
    }

    private String buildReportHtml(List<OffreFinancement> offres) {

        String generatedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm"));

        String reportId = "RPT-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));

        // ── Compute summary stats ──────────────────────────────────
        int totalOffres = offres.size();

        double avgTaux = offres.stream()
                .mapToDouble(OffreFinancement::getTaux)
                .average().orElse(0);

        double minTaux = offres.stream()
                .mapToDouble(OffreFinancement::getTaux)
                .min().orElse(0);

        double maxTaux = offres.stream()
                .mapToDouble(OffreFinancement::getTaux)
                .max().orElse(0);

        double avgDuree = offres.stream()
                .mapToDouble(OffreFinancement::getDuree)
                .average().orElse(0);

        // Count by type
        java.util.Map<String, Long> byType = offres.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        o -> o.getTypeOffre() != null ? o.getTypeOffre() : "Non défini",
                        java.util.stream.Collectors.counting()
                ));

        // ── Build type breakdown pills ─────────────────────────────
        StringBuilder typePills = new StringBuilder();
        String[] pillColors = {"#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4"};
        int colorIdx = 0;
        for (java.util.Map.Entry<String, Long> entry : byType.entrySet()) {
            String color = pillColors[colorIdx % pillColors.length];
            typePills.append("""
                <div class="pill" style="background:%s">
                    <span class="pill-type">%s</span>
                    <span class="pill-count">%d offre%s</span>
                </div>
            """.formatted(color, entry.getKey(), entry.getValue(),
                    entry.getValue() > 1 ? "s" : ""));
            colorIdx++;
        }

        // ── Build table rows ───────────────────────────────────────
        StringBuilder rows = new StringBuilder();
        int rowNum = 1;
        for (OffreFinancement o : offres) {
            double taux = o.getTaux();

            // Color-code taux: green=low, amber=mid, red=high
            String tauxColor;
            String tauxBg;
            if (taux <= 5.0) {
                tauxColor = "#065f46"; tauxBg = "#d1fae5";
            } else if (taux <= 10.0) {
                tauxColor = "#92400e"; tauxBg = "#fef3c7";
            } else {
                tauxColor = "#991b1b"; tauxBg = "#fee2e2";
            }

            // Color-code duree: short=blue, mid=purple, long=gray
            String dureeLabel;
            String dureeBg;
            int duree = o.getDuree();
            if (duree <= 24) {
                dureeLabel = "Court terme"; dureeBg = "#dbeafe";
            } else if (duree <= 72) {
                dureeLabel = "Moyen terme"; dureeBg = "#ede9fe";
            } else {
                dureeLabel = "Long terme"; dureeBg = "#f3f4f6";
            }

            String rowBg = rowNum % 2 == 0 ? "#f8faff" : "#ffffff";

            rows.append("""
                <tr style="background:%s">
                    <td class="td-center td-id">#%d</td>
                    <td class="td-type">
                        <span class="type-badge">%s</span>
                    </td>
                    <td class="td-center">
                        <span class="taux-badge" style="color:%s; background:%s">
                            %s%%
                        </span>
                    </td>
                    <td class="td-center">
                        <div class="duree-wrap">
                            <strong>%d mois</strong>
                            <span class="duree-tag" style="background:%s">%s</span>
                        </div>
                    </td>
                    <td class="td-center td-fin">#%d</td>
                </tr>
            """.formatted(
                    rowBg,
                    o.getIdOffre(),
                    safe(o.getTypeOffre()),
                    tauxColor, tauxBg, taux,
                    duree, dureeBg, dureeLabel,
                    o.getIdFinancement()
            ));
            rowNum++;
        }

        // ── Full HTML ──────────────────────────────────────────────
        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8"/>
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

                * { margin: 0; padding: 0; box-sizing: border-box; }

                body {
                    font-family: 'Inter', 'Arial', sans-serif;
                    background: #f0f4ff;
                    color: #1e293b;
                    padding: 0;
                }

                /* ── TOP BANNER ── */
                .banner {
                    background: linear-gradient(135deg, #0f172a 0%%, #1e3a5f 60%%, #0d4f3c 100%%);
                    color: white;
                    padding: 36px 48px 30px 48px;
                    position: relative;
                    overflow: hidden;
                }
                .banner::before {
                    content: '';
                    position: absolute;
                    top: -40px; right: -40px;
                    width: 200px; height: 200px;
                    background: rgba(255,255,255,0.04);
                    border-radius: 50%%;
                }
                .banner::after {
                    content: '';
                    position: absolute;
                    bottom: -60px; left: 30%%;
                    width: 300px; height: 300px;
                    background: rgba(16,185,129,0.08);
                    border-radius: 50%%;
                }
                .banner-top {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-start;
                    margin-bottom: 24px;
                }
                .logo {
                    font-size: 24px;
                    font-weight: 700;
                    letter-spacing: 0.5px;
                }
                .logo span {
                    color: #10b981;
                }
                .logo-sub {
                    font-size: 11px;
                    color: rgba(255,255,255,0.5);
                    margin-top: 3px;
                    letter-spacing: 1px;
                    text-transform: uppercase;
                }
                .report-id {
                    text-align: right;
                    font-size: 11px;
                    color: rgba(255,255,255,0.5);
                }
                .report-id strong {
                    display: block;
                    font-size: 13px;
                    color: rgba(255,255,255,0.85);
                    margin-bottom: 2px;
                }
                .banner-title {
                    font-size: 28px;
                    font-weight: 700;
                    letter-spacing: -0.5px;
                    margin-bottom: 6px;
                }
                .banner-title span { color: #10b981; }
                .banner-sub {
                    font-size: 13px;
                    color: rgba(255,255,255,0.55);
                }

                /* ── STATS ROW ── */
                .stats-row {
                    display: flex;
                    gap: 16px;
                    padding: 24px 48px;
                    background: #ffffff;
                    border-bottom: 1px solid #e2e8f0;
                }
                .stat-box {
                    flex: 1;
                    background: #f8faff;
                    border: 1px solid #e2e8f0;
                    border-radius: 10px;
                    padding: 16px 20px;
                    border-top: 3px solid #1e3a5f;
                }
                .stat-box.green { border-top-color: #10b981; }
                .stat-box.amber { border-top-color: #f59e0b; }
                .stat-box.red   { border-top-color: #ef4444; }
                .stat-label {
                    font-size: 10px;
                    font-weight: 600;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    color: #94a3b8;
                    margin-bottom: 6px;
                }
                .stat-value {
                    font-size: 26px;
                    font-weight: 700;
                    color: #0f172a;
                    line-height: 1;
                }
                .stat-value .unit {
                    font-size: 13px;
                    font-weight: 400;
                    color: #64748b;
                    margin-left: 3px;
                }

                /* ── BODY ── */
                .body {
                    padding: 28px 48px 40px 48px;
                    background: #f0f4ff;
                }

                /* ── TYPE BREAKDOWN ── */
                .section-label {
                    font-size: 11px;
                    font-weight: 600;
                    text-transform: uppercase;
                    letter-spacing: 1.5px;
                    color: #64748b;
                    margin-bottom: 12px;
                }
                .pills-row {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                    margin-bottom: 28px;
                }
                .pill {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    padding: 8px 16px;
                    border-radius: 50px;
                    color: white;
                }
                .pill-type {
                    font-size: 12px;
                    font-weight: 600;
                }
                .pill-count {
                    font-size: 11px;
                    opacity: 0.85;
                    background: rgba(255,255,255,0.2);
                    padding: 1px 8px;
                    border-radius: 20px;
                }

                /* ── TABLE ── */
                .table-wrap {
                    background: #ffffff;
                    border-radius: 12px;
                    overflow: hidden;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.08);
                    border: 1px solid #e2e8f0;
                }
                .table-header-bar {
                    padding: 16px 24px;
                    background: #ffffff;
                    border-bottom: 1px solid #e2e8f0;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                .table-header-bar .t-title {
                    font-size: 15px;
                    font-weight: 600;
                    color: #0f172a;
                }
                .table-header-bar .t-count {
                    font-size: 12px;
                    color: #64748b;
                    background: #f1f5f9;
                    padding: 3px 10px;
                    border-radius: 20px;
                }
                table {
                    width: 100%%;
                    border-collapse: collapse;
                }
                thead tr {
                    background: #0f172a;
                }
                thead th {
                    padding: 13px 20px;
                    text-align: left;
                    font-size: 11px;
                    font-weight: 600;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    color: rgba(255,255,255,0.7);
                }
                thead th.th-center { text-align: center; }
                tbody tr {
                    border-bottom: 1px solid #f1f5f9;
                    transition: background 0.1s;
                }
                tbody tr:last-child { border-bottom: none; }
                td {
                    padding: 14px 20px;
                    font-size: 13px;
                    color: #334155;
                    vertical-align: middle;
                }
                .td-center { text-align: center; }
                .td-id {
                    font-weight: 700;
                    color: #94a3b8;
                    font-size: 12px;
                }
                .td-fin {
                    font-weight: 600;
                    color: #475569;
                }
                .type-badge {
                    display: inline-block;
                    background: #f1f5f9;
                    color: #334155;
                    padding: 4px 12px;
                    border-radius: 6px;
                    font-size: 12px;
                    font-weight: 500;
                    border: 1px solid #e2e8f0;
                }
                .taux-badge {
                    display: inline-block;
                    padding: 4px 12px;
                    border-radius: 20px;
                    font-size: 13px;
                    font-weight: 700;
                }
                .duree-wrap {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    gap: 4px;
                }
                .duree-tag {
                    font-size: 10px;
                    padding: 2px 8px;
                    border-radius: 20px;
                    color: #475569;
                    font-weight: 500;
                }

                /* ── LEGEND ── */
                .legend {
                    margin-top: 20px;
                    display: flex;
                    gap: 20px;
                    align-items: center;
                    font-size: 11px;
                    color: #64748b;
                }
                .legend-item {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                }
                .legend-dot {
                    width: 10px;
                    height: 10px;
                    border-radius: 50%%;
                }

                /* ── FOOTER ── */
                .footer {
                    margin-top: 32px;
                    padding-top: 16px;
                    border-top: 1px solid #e2e8f0;
                    display: flex;
                    justify-content: space-between;
                    font-size: 10px;
                    color: #94a3b8;
                }
            </style>
        </head>
        <body>

            <!-- TOP BANNER -->
            <div class="banner">
                <div class="banner-top">
                    <div>
                        <div class="logo">🌿 Green<span>Ledger</span></div>
                        <div class="logo-sub">Plateforme de Financement Vert</div>
                    </div>
                    <div class="report-id">
                        <strong>%s</strong>
                        Généré le %s
                    </div>
                </div>
                <div class="banner-title">Rapport des Offres de <span>Financement</span></div>
                <div class="banner-sub">Document de révision hors-ligne — Usage interne confidentiel</div>
            </div>

            <!-- STATS ROW -->
            <div class="stats-row">
                <div class="stat-box">
                    <div class="stat-label">Total Offres</div>
                    <div class="stat-value">%d <span class="unit">offres</span></div>
                </div>
                <div class="stat-box green">
                    <div class="stat-label">Taux Moyen</div>
                    <div class="stat-value">%.2f <span class="unit">%%</span></div>
                </div>
                <div class="stat-box amber">
                    <div class="stat-label">Taux Min / Max</div>
                    <div class="stat-value">%.1f <span class="unit">/ %.1f%%</span></div>
                </div>
                <div class="stat-box red">
                    <div class="stat-label">Durée Moyenne</div>
                    <div class="stat-value">%.0f <span class="unit">mois</span></div>
                </div>
            </div>

            <!-- BODY -->
            <div class="body">

                <!-- TYPE BREAKDOWN -->
                <div class="section-label">Répartition par type d'offre</div>
                <div class="pills-row">
                    %s
                </div>

                <!-- TABLE -->
                <div class="table-wrap">
                    <div class="table-header-bar">
                        <span class="t-title">📋 Détail des offres de financement</span>
                        <span class="t-count">%d enregistrements</span>
                    </div>
                    <table>
                        <thead>
                            <tr>
                                <th>ID Offre</th>
                                <th>Type d'Offre</th>
                                <th class="th-center">Taux d'Intérêt</th>
                                <th class="th-center">Durée</th>
                                <th class="th-center">Financement ID</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>

                <!-- LEGEND -->
                <div class="legend">
                    <strong>Légende taux:</strong>
                    <div class="legend-item">
                        <div class="legend-dot" style="background:#10b981"></div>
                        ≤ 5%% — Favorable
                    </div>
                    <div class="legend-item">
                        <div class="legend-dot" style="background:#f59e0b"></div>
                        5–10%% — Modéré
                    </div>
                    <div class="legend-item">
                        <div class="legend-dot" style="background:#ef4444"></div>
                        > 10%% — Élevé
                    </div>
                </div>

                <!-- FOOTER -->
                <div class="footer">
                    <span>GreenLedger — Rapport généré automatiquement | Confidentiel</span>
                    <span>%s | %d offres exportées</span>
                </div>

            </div>
        </body>
        </html>
        """.formatted(
                reportId,           // banner report id
                generatedAt,        // banner date
                totalOffres,        // stat: total
                avgTaux,            // stat: avg taux
                minTaux, maxTaux,   // stat: min/max
                avgDuree,           // stat: avg duree
                typePills,          // pills
                totalOffres,        // table count
                rows,               // table rows
                reportId,           // footer id
                totalOffres         // footer count
        );
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }
}