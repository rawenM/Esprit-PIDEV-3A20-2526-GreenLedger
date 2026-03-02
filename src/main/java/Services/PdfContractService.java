package Services;

import Models.Financement;
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
public class PdfContractService {

    private static final String API_KEY = "sk_0b6b0385c48e47e66fa73e311ec731bd2e781de7";

    private static final String API_URL = "https://api.pdfshift.io/v3/convert/pdf";

    /**
     * Generates a financing contract PDF and saves it to the given path.
     *
     * @param financement  The Financement record selected in the table
     * @param offre        The linked OffreFinancement (can be null if not linked)
     * @param outputPath   Full path where the PDF should be saved (from FileChooser)
     * @throws Exception   If the API call fails or file cannot be written
     */
    public void generateContract(Financement financement,
                                 OffreFinancement offre,
                                 String outputPath) throws Exception {

        // Step 1: Build the HTML content of the contract
        String html = buildContractHtml(financement, offre);

        // Step 2: Build the JSON payload for PDFShift
        // We send the HTML directly as a string (not a URL)
        JSONObject payload = new JSONObject();
        payload.put("source", html);
        payload.put("landscape", false);
        payload.put("use_print", false);

        // Step 3: Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("X-API-Key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        // Step 4: Send the request
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<java.io.InputStream> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        // Step 5: Check response and save the PDF
        int statusCode = response.statusCode();

        if (statusCode == 200 || statusCode == 201) {
            File outputFile = new File(outputPath);
            Files.copy(response.body(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ Contract PDF saved to: " + outputPath);
        } else {
            // Read error body for debugging
            String errorBody = new String(response.body().readAllBytes());
            throw new Exception("PDFShift API error " + statusCode + ": " + errorBody);
        }
    }

    /**
     * Builds the full HTML string for the financing contract.
     * This is a professional contract template styled with inline CSS.
     * PDFShift renders this exactly as a browser would.
     *
     * @param f      Financement object
     * @param offre  Linked OffreFinancement (nullable)
     * @return       Complete HTML string
     */
    private String buildContractHtml(Financement f, OffreFinancement offre) {

        String contractNumber = "GRL-" + f.getId() + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String currentDate = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String offreSection = "";
        if (offre != null) {
            offreSection = """
                <tr>
                    <td class="label">Type d'offre</td>
                    <td class="value">%s</td>
                </tr>
                <tr>
                    <td class="label">Taux d'intérêt</td>
                    <td class="value">%s%%</td>
                </tr>
                <tr>
                    <td class="label">Durée</td>
                    <td class="value">%s mois</td>
                </tr>
            """.formatted(
                    safe(offre.getTypeOffre()),
                    offre.getTaux(),
                    offre.getDuree()
            );
        }

        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8"/>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body {
                    font-family: 'Arial', sans-serif;
                    font-size: 13px;
                    color: #1a1a2e;
                    padding: 40px 60px;
                    background: #ffffff;
                }
                .header {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-start;
                    border-bottom: 3px solid #16213e;
                    padding-bottom: 20px;
                    margin-bottom: 30px;
                }
                .company-name {
                    font-size: 26px;
                    font-weight: bold;
                    color: #16213e;
                    letter-spacing: 1px;
                }
                .company-subtitle {
                    font-size: 12px;
                    color: #4a4a8a;
                    margin-top: 4px;
                }
                .contract-meta {
                    text-align: right;
                    font-size: 12px;
                    color: #555;
                }
                .contract-meta .contract-number {
                    font-size: 14px;
                    font-weight: bold;
                    color: #16213e;
                }
                .title {
                    text-align: center;
                    font-size: 20px;
                    font-weight: bold;
                    color: #16213e;
                    letter-spacing: 2px;
                    text-transform: uppercase;
                    margin: 30px 0 10px 0;
                    border: 2px solid #16213e;
                    padding: 12px;
                    background: #f0f4ff;
                }
                .subtitle {
                    text-align: center;
                    font-size: 12px;
                    color: #666;
                    margin-bottom: 30px;
                }
                .section-title {
                    font-size: 13px;
                    font-weight: bold;
                    color: #ffffff;
                    background: #16213e;
                    padding: 8px 14px;
                    margin: 25px 0 0 0;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                }
                table.data-table {
                    width: 100%%;
                    border-collapse: collapse;
                    margin-bottom: 5px;
                }
                table.data-table td {
                    padding: 10px 14px;
                    border: 1px solid #dce3f0;
                    vertical-align: top;
                }
                table.data-table td.label {
                    width: 40%%;
                    background: #f5f7ff;
                    font-weight: bold;
                    color: #333;
                }
                table.data-table td.value {
                    background: #ffffff;
                    color: #1a1a2e;
                }
                .highlight-box {
                    background: #eaf4ea;
                    border-left: 4px solid #2ecc71;
                    padding: 14px 18px;
                    margin: 25px 0;
                    font-size: 13px;
                }
                .highlight-box .amount {
                    font-size: 22px;
                    font-weight: bold;
                    color: #16213e;
                }
                .clause {
                    margin: 8px 0;
                    padding-left: 16px;
                    border-left: 3px solid #dce3f0;
                    font-size: 12px;
                    color: #444;
                    line-height: 1.6;
                }
                .signatures {
                    display: flex;
                    justify-content: space-between;
                    margin-top: 60px;
                    padding-top: 20px;
                    border-top: 1px solid #dce3f0;
                }
                .signature-block {
                    width: 45%%;
                    text-align: center;
                }
                .signature-line {
                    border-top: 1px solid #333;
                    margin-top: 50px;
                    padding-top: 6px;
                    font-size: 12px;
                    color: #555;
                }
                .footer {
                    margin-top: 40px;
                    padding-top: 12px;
                    border-top: 1px solid #dce3f0;
                    font-size: 10px;
                    color: #999;
                    text-align: center;
                }
                .green-badge {
                    display: inline-block;
                    background: #2ecc71;
                    color: white;
                    padding: 3px 10px;
                    border-radius: 3px;
                    font-size: 11px;
                    font-weight: bold;
                    margin-left: 8px;
                }
            </style>
        </head>
        <body>

            <!-- HEADER -->
            <div class="header">
                <div>
                    <div class="company-name">🌿 GreenLedger</div>
                    <div class="company-subtitle">Plateforme de Financement Vert</div>
                </div>
                <div class="contract-meta">
                    <div class="contract-number">N° %s</div>
                    <div>Date: %s</div>
                    <div>Statut: <strong>Actif</strong></div>
                </div>
            </div>

            <!-- CONTRACT TITLE -->
            <div class="title">
                Contrat de Financement Vert
                <span class="green-badge">GREEN FINANCE</span>
            </div>
            <div class="subtitle">
                Document officiel généré automatiquement par GreenLedger
            </div>

            <!-- AMOUNT HIGHLIGHT -->
            <div class="highlight-box">
                Montant du financement accordé:
                <div class="amount">%,.2f TND</div>
            </div>

            <!-- FINANCING DETAILS -->
            <div class="section-title">📋 Détails du Financement</div>
            <table class="data-table">
                <tr>
                    <td class="label">Référence Financement</td>
                    <td class="value">#%d</td>
                </tr>
                <tr>
                    <td class="label">Projet ID</td>
                    <td class="value">#%d</td>
                </tr>
                <tr>
                    <td class="label">Banque ID</td>
                    <td class="value">#%d</td>
                </tr>
                <tr>
                    <td class="label">Montant</td>
                    <td class="value">%,.2f TND</td>
                </tr>
                <tr>
                    <td class="label">Date de Financement</td>
                    <td class="value">%s</td>
                </tr>
            </table>

            <!-- OFFER DETAILS (if available) -->
            %s

            <!-- CLAUSES -->
            <div class="section-title">📜 Clauses Contractuelles</div>
            <br/>
            <p class="clause">
                <strong>Article 1 — Objet du contrat:</strong>
                Le présent contrat a pour objet de définir les conditions dans lesquelles
                le financement référencé ci-dessus est accordé au projet concerné,
                conformément aux termes convenus entre les parties.
            </p>
            <p class="clause">
                <strong>Article 2 — Utilisation des fonds:</strong>
                Les fonds accordés doivent être utilisés exclusivement pour le financement
                du projet vert identifié. Toute utilisation à d'autres fins doit faire
                l'objet d'un avenant au présent contrat.
            </p>
            <p class="clause">
                <strong>Article 3 — Obligations de l'investisseur:</strong>
                L'investisseur s'engage à respecter les conditions du financement,
                à fournir les justificatifs demandés, et à informer GreenLedger
                de tout changement significatif dans la situation du projet.
            </p>
            <p class="clause">
                <strong>Article 4 — Résiliation:</strong>
                Le présent contrat peut être résilié de plein droit en cas de
                non-respect des obligations contractuelles, après mise en demeure
                restée sans effet pendant un délai de 30 jours.
            </p>

            <!-- SIGNATURES -->
            <div class="signatures">
                <div class="signature-block">
                    <div class="signature-line">
                        Signature de l'Investisseur<br/>
                        <em>Lu et approuvé</em>
                    </div>
                </div>
                <div class="signature-block">
                    <div class="signature-line">
                        Signature GreenLedger<br/>
                        <em>Cachet et signature</em>
                    </div>
                </div>
            </div>

            <!-- FOOTER -->
            <div class="footer">
                Document généré le %s par GreenLedger — Plateforme de Financement Vert |
                Contrat N° %s | Ce document est confidentiel.
            </div>

        </body>
        </html>
        """.formatted(
                contractNumber,           // header contract number
                currentDate,              // header date
                f.getMontant(),           // highlight amount
                f.getId(),                // table: ref
                f.getProjetId(),          // table: projet id
                f.getBanqueId(),          // table: banque id
                f.getMontant(),           // table: montant
                safe(f.getDateFinancement()), // table: date
                offreSection.isEmpty() ? "" :
                        "<div class=\"section-title\">🧾 Offre de Financement Liée</div>" +
                                "<table class=\"data-table\">" + offreSection + "</table>",
                currentDate,              // footer date
                contractNumber            // footer contract number
        );
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }
}