package Services;

import Models.AiSuggestion;
import Models.Evaluation;
import Models.EvaluationResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service de génération de PDF pour les évaluations.
 * - En-tête GreenLedger (bande verte + logo)
 * - Date d'évaluation
 * - Critères et commentaires
 * - Analyse IA + recommandations actionnables
 * - Signature électronique (UUID + hash)
 */
public class PdfService {

    public File generateEvaluationPdf(Evaluation evaluation,
                                      List<EvaluationResult> criteres,
                                      AiSuggestion suggestion,
                                      File outputFile) throws IOException {
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile is null");
        }
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException("Cannot create directories: " + parent.getAbsolutePath());
            }
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();
                float margin = 50;
                float y = height - margin;

                // Header background
                cs.setNonStrokingColor(new Color(218, 247, 234)); // light green
                cs.addRect(0, height - 90, width, 90);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);

                // Logo
                float logoH = 48;
                float logoY = height - 80;
                float logoX = margin;
                byte[] logo = loadResourceBytes("/images/bg.png");
                if (logo != null) {
                    PDImageXObject img = PDImageXObject.createFromByteArray(doc, logo, "logo");
                    float ratio = img.getWidth() == 0 ? 1 : (logoH / img.getHeight());
                    float logoW = img.getWidth() * ratio;
                    cs.drawImage(img, logoX, logoY, logoW, logoH);
                }

                // Header title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(margin + 60, height - 58);
                cs.showText(sanitize("GreenLedger – Rapport d'Évaluation Carbone"));
                cs.endText();

                y = height - 110;

                // Meta
                cs.setFont(PDType1Font.HELVETICA, 11);
                y = writeWrapped(cs, "ID Évaluation: " + evaluation.getIdEvaluation(), margin, y, 12);
                String dateStr = evaluation.getDateEvaluation() != null
                        ? evaluation.getDateEvaluation().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "N/A";
                y = writeWrapped(cs, "Date d'évaluation: " + dateStr, margin, y, 12);
                y = writeWrapped(cs, "Projet ID: " + evaluation.getIdProjet() + (evaluation.getTitreProjet() != null ? (" — " + evaluation.getTitreProjet()) : ""), margin, y, 12);
                y = writeWrapped(cs, "Décision: " + safe(evaluation.getDecision()) + " | Score global: " + String.format(Locale.ROOT, "%.2f", evaluation.getScoreGlobal()), margin, y, 14);

                y -= 6;
                y = writeWrapped(cs, "Observations:", margin, y, 12);
                y = writeParagraph(cs, safe(evaluation.getObservations()), margin, y, 480, 11, 14);

                y -= 4;
                // Criteria summary (no detailed list)
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Synthese des criteres");
                cs.endText();
                y -= 16;

                y = writeWrapped(cs, "Nombre de criteres evalues: " + criteres.size(), margin, y, 12);
                y -= 2;

                // ESG summary
                ProjectEsgService.EsgBreakdown b = new ProjectEsgService().breakdown(criteres);
                int esg100 = (int) Math.round(b.esg10 * 10.0);
                y = writeWrapped(cs, "Score ESG: " + esg100 + "/100", margin, y, 12);
                y = writeWrapped(cs, "Formule: ESG = 50%*E + 30%*S + 20%*G (0-10) puis x10 -> 0-100", margin, y, 12);
                y = writeWrapped(cs, String.format(java.util.Locale.ROOT, "E=%.2f, S=%.2f, G=%.2f, ESG(0-10)=%.2f", b.e, b.s, b.g, b.esg10), margin, y, 12);

                // Penalties (simplified)
                y -= 4;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Penalites appliquees");
                cs.endText();
                y -= 16;
                y = writeWrapped(cs, "- Critere non respecte: note x 0.6", margin, y, 12);
                y = writeWrapped(cs, "- CO2 < 4: penalite forte sur l'impact", margin, y, 12);
                y = writeWrapped(cs, "- Echec environnemental (<4): impact reduit", margin, y, 14);

                // IA
                if (suggestion != null) {
                    y -= 6;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Facteurs cles");
                    cs.endText();
                    y -= 16;

                    if (suggestion.getTopFactors() != null && !suggestion.getTopFactors().isEmpty()) {
                        for (String f : suggestion.getTopFactors()) {
                            y = writeWrapped(cs, "- " + f, margin, y, 12);
                            if (y < 100) y = newPage(doc, cs);
                        }
                    } else {
                        y = writeWrapped(cs, "Facteurs cles non disponibles.", margin, y, 12);
                    }
                }

                // Recommendations
                y -= 8;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Recommandations");
                cs.endText();
                y -= 16;

                Services.AdvancedEvaluationFacade facade = new Services.AdvancedEvaluationFacade();
                java.util.List<String> recs = facade.criterionRecommendations(criteres);
                java.util.List<String> actionable = recs.stream()
                        .filter(r -> !r.contains("OK – Maintenir"))
                        .collect(java.util.stream.Collectors.toList());
                if (actionable.isEmpty()) {
                    y = writeWrapped(cs, "Aucune action prioritaire identifiee. Les bonnes pratiques sont a maintenir.", margin, y, 12);
                } else {
                    for (String r : actionable) {
                        y = writeParagraph(cs, "- " + r, margin, y, 480, 11, 12);
                        if (y < 80) y = newPage(doc, cs);
                    }
                }

                // (No signature in base PDF)
            }

            doc.save(outputFile);
        }

        return outputFile;
    }

    public File generateEvaluationPdfWithSignature(Evaluation evaluation,
                                                   List<EvaluationResult> criteres,
                                                   AiSuggestion suggestion,
                                                   File outputFile,
                                                   byte[] signaturePng,
                                                   String evaluatorName,
                                                   String evaluatorRole) throws IOException {
        // Reuse the same generation flow but add the signature image when available.
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile is null");
        }
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException("Cannot create directories: " + parent.getAbsolutePath());
            }
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();
                float margin = 50;
                float y = height - margin;

                // Header background
                cs.setNonStrokingColor(new java.awt.Color(218, 247, 234));
                cs.addRect(0, height - 90, width, 90);
                cs.fill();
                cs.setNonStrokingColor(java.awt.Color.BLACK);

                // Logo
                float logoH = 48;
                float logoY = height - 80;
                float logoX = margin;
                byte[] logo = loadResourceBytes("/images/bg.png");
                if (logo != null) {
                    PDImageXObject img = PDImageXObject.createFromByteArray(doc, logo, "logo");
                    float ratio = img.getWidth() == 0 ? 1 : (logoH / img.getHeight());
                    float logoW = img.getWidth() * ratio;
                    cs.drawImage(img, logoX, logoY, logoW, logoH);
                }

                // Header title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(margin + 60, height - 58);
                cs.showText(sanitize("GreenLedger – Rapport d'Évaluation Carbone"));
                cs.endText();

                y = height - 110;

                // Meta (project name only, no IDs)
                cs.setFont(PDType1Font.HELVETICA, 11);
                String dateStr = evaluation.getDateEvaluation() != null
                        ? evaluation.getDateEvaluation().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "N/A";
                y = writeWrapped(cs, "Date d'évaluation: " + dateStr, margin, y, 12);
                y = writeWrapped(cs, "Projet: " + safe(evaluation.getTitreProjet()), margin, y, 12);
                y = writeWrapped(cs, "Décision: " + safe(evaluation.getDecision()) + " | Score global: " + String.format(java.util.Locale.ROOT, "%.2f", evaluation.getScoreGlobal()), margin, y, 14);

                y -= 6;
                y = writeWrapped(cs, "Observations:", margin, y, 12);
                y = writeParagraph(cs, safe(evaluation.getObservations()), margin, y, 480, 11, 14);

                y -= 4;
                // Criteria summary (no detailed list)
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Synthese des criteres");
                cs.endText();
                y -= 16;

                y = writeWrapped(cs, "Nombre de criteres evalues: " + criteres.size(), margin, y, 12);
                y -= 2;

                // ESG summary
                ProjectEsgService.EsgBreakdown b = new ProjectEsgService().breakdown(criteres);
                int esg100 = (int) Math.round(b.esg10 * 10.0);
                y = writeWrapped(cs, "Score ESG: " + esg100 + "/100", margin, y, 12);
                y = writeWrapped(cs, "Formule: ESG = 50%*E + 30%*S + 20%*G (0-10) puis x10 -> 0-100", margin, y, 12);
                y = writeWrapped(cs, String.format(java.util.Locale.ROOT, "E=%.2f, S=%.2f, G=%.2f, ESG(0-10)=%.2f", b.e, b.s, b.g, b.esg10), margin, y, 12);

                // Penalties (simplified)
                y -= 4;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Penalites appliquees");
                cs.endText();
                y -= 16;
                y = writeWrapped(cs, "- Critere non respecte: note x 0.6", margin, y, 12);
                y = writeWrapped(cs, "- CO2 < 4: penalite forte sur l'impact", margin, y, 12);
                y = writeWrapped(cs, "- Echec environnemental (<4): impact reduit", margin, y, 14);

                // IA
                if (suggestion != null) {
                    y -= 6;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Facteurs cles");
                    cs.endText();
                    y -= 16;

                    if (suggestion.getTopFactors() != null && !suggestion.getTopFactors().isEmpty()) {
                        for (String f : suggestion.getTopFactors()) {
                            y = writeWrapped(cs, "- " + f, margin, y, 12);
                            if (y < 100) y = newPage(doc, cs);
                        }
                    } else {
                        y = writeWrapped(cs, "Facteurs cles non disponibles.", margin, y, 12);
                    }
                }

                // Recommendations
                y -= 8;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Recommandations");
                cs.endText();
                y -= 16;

                Services.AdvancedEvaluationFacade facade = new Services.AdvancedEvaluationFacade();
                java.util.List<String> recs = facade.criterionRecommendations(criteres);
                java.util.List<String> actionable = recs.stream()
                        .filter(r -> !r.contains("OK – Maintenir"))
                        .collect(java.util.stream.Collectors.toList());
                if (actionable.isEmpty()) {
                    y = writeWrapped(cs, "Aucune action prioritaire identifiee. Les bonnes pratiques sont a maintenir.", margin, y, 12);
                } else {
                    for (String r : actionable) {
                        y = writeParagraph(cs, "- " + r, margin, y, 480, 11, 12);
                        if (y < 80) y = newPage(doc, cs);
                    }
                }

                // Signature image and evaluator info
                y -= 8;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Signature");
                cs.endText();
                y -= 12;

                if (signaturePng != null && signaturePng.length > 0) {
                    PDImageXObject sigImg = PDImageXObject.createFromByteArray(doc, signaturePng, "signature");
                    float sigW = 220, sigH = 80;
                    cs.drawImage(sigImg, margin, y - sigH, sigW, sigH);
                    y -= (sigH + 6);
                }

                // Evaluator line (no fingerprint)
                y = writeWrapped(cs, "Évaluation réalisée par: " + sanitize(evaluatorName) + " — Rôle: " + sanitize(evaluatorRole), margin, y, 12);
            }

            doc.save(outputFile);
        }

        return outputFile;
    }

    private byte[] loadResourceBytes(String path) {
        try (InputStream is = PdfService.class.getResourceAsStream(path);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (is == null) return null;
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private float writeWrapped(PDPageContentStream cs, String text, float x, float y, float lineSpacing) throws IOException {
        String safeText = sanitize(text);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(safeText);
        cs.endText();
        return y - lineSpacing;
    }

    private float writeParagraph(PDPageContentStream cs, String text, float x, float y, float maxWidth, float fontSize, float lineSpacing) throws IOException {
        if (text == null || text.isEmpty()) return y;
        String[] words = sanitize(text).split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String candidate = line.length() == 0 ? w : line + " " + w;
            float width = PDType1Font.HELVETICA.getStringWidth(candidate) / 1000f * fontSize;
            if (width > maxWidth) {
                y = writeLine(cs, line.toString(), x, y, fontSize, lineSpacing);
                line.setLength(0);
                line.append(w);
                if (y < 80) y = newPage(null, cs);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (line.length() > 0) {
            y = writeLine(cs, line.toString(), x, y, fontSize, lineSpacing);
        }
        return y;
    }

    private float writeLine(PDPageContentStream cs, String text, float x, float y, float fontSize, float lineSpacing) throws IOException {
        String safeText = sanitize(text);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(safeText);
        cs.endText();
        return y - lineSpacing;
    }

    private float newPage(PDDocument doc, PDPageContentStream current) throws IOException {
        // Simplified: close and start a new page (in a more advanced version, we should pass PDDocument and create a new stream)
        return 700; // Fallback; this is a simple implementation for single-page-heavy contents
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Replace characters unsupported by WinAnsiEncoding (Helvetica) with safe ASCII fallbacks.
     * Examples:
     *  - CO₂ -> CO2, en dash/em dash -> -, smart quotes -> ', bullets -> -, NBSP -> space.
     */
    private String sanitize(String text) {
        if (text == null) return "";
        String t = text;

        // Subscript digits U+2080..U+2089 -> normal digits
        t = t.replace('\u2080','0').replace('\u2081','1').replace('\u2082','2').replace('\u2083','3')
                .replace('\u2084','4').replace('\u2085','5').replace('\u2086','6').replace('\u2087','7')
                .replace('\u2088','8').replace('\u2089','9');

        // Superscript digits (common) U+00B2, U+00B3, U+2070.. -> map common ones
        t = t.replace('\u00B2','2').replace('\u00B3','3').replace('\u2070','0').replace('\u00B9','1');

        // Dashes and bullets
        t = t.replace('\u2013','-').replace('\u2014','-').replace('\u2212','-').replace('\u2022','-');

        // Quotes
        t = t.replace('\u2018','\'').replace('\u2019','\'').replace('\u201A','\'')
                .replace('\u201C','\"').replace('\u201D','\"').replace('\u201E','\"');

        // Guillemets
        t = t.replace('\u00AB','\"').replace('\u00BB','\"');

        // Ellipsis
        t = t.replace("\u2026","...");

        // Non-breaking space
        t = t.replace('\u00A0',' ');

        // Ligatures
        t = t.replace("\u0152", "OE").replace("\u0153","oe");

        // Currency Euro -> EUR (WinAnsi supports Euro in some cases but ensure fallback)
        t = t.replace("\u20AC","EUR");

        // Control chars -> remove
        t = t.replaceAll("\\p{Cntrl}", "");

        return t;
    }
}

