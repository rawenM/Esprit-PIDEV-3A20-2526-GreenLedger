package Utils;

import Models.Budget;
import Models.ProjectDocument;
import Models.Projet;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.nio.file.*;

public class ProjetPdfGenerator {

    private static final String BRAND_EMAIL = "greenledger@greenledger.com";
    private static final String BRAND_FAX = "77 554 441";
    private static final String BRAND_ADDRESS = "Esprit Tunis";

    // ✅ Logo (mets-le ici : src/main/resources/images/logo_greenledger.png)
    private static final String LOGO_RESOURCE = "/images/logo_greenledger.png";

    // ✅ On force un séparateur ASCII (espace normal) pour éviter U+202F
    private static final DecimalFormat MONEY_FMT = buildMoneyFormatter();

    private static DecimalFormat buildMoneyFormatter() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator(' ');
        sym.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", sym);
        df.setGroupingUsed(true);
        return df;
    }

    // ---------------------------
    // PREVIEW
    // ---------------------------
    public static String generatePreviewPdf(Projet p) throws IOException {
        Path folder = Paths.get("uploads", "preview");
        Files.createDirectories(folder);

        String pdfName = "Preview_Projet_" + System.currentTimeMillis() + ".pdf";
        Path pdfPath = folder.resolve(pdfName);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = 790;

                y = drawHeader(document, cs, page, margin, y, "Aperçu Projet");
                y = drawIntro(cs, margin, y,
                        "Ce document est généré automatiquement par GreenLedger. Il présente les informations du projet.");

                y = drawMainContent(cs, margin, y, p);
                drawFooter(cs, page);
            }

            document.save(pdfPath.toFile());
        }

        return pdfPath.toString();
    }

    // ---------------------------
    // FINAL SUBMITTED
    // ---------------------------
    public static String generateSubmittedPdf(Projet p, List<ProjectDocument> docs) throws IOException {
        Path folder = Paths.get("uploads", "projects", String.valueOf(p.getId()));
        Files.createDirectories(folder);

        String pdfName = "Projet_" + p.getId() + "_SUBMITTED.pdf";
        Path pdfPath = folder.resolve(pdfName);

        try (PDDocument document = new PDDocument()) {

            PDPage page1 = new PDPage(PDRectangle.A4);
            document.addPage(page1);

            try (PDPageContentStream cs = new PDPageContentStream(document, page1)) {
                float margin = 50;
                float y = 790;

                y = drawHeader(document, cs, page1, margin, y, "Dossier Projet (SUBMITTED)");
                y = drawIntro(cs, margin, y,
                        "Ce document représente la version soumise du projet. Il contient les informations principales ainsi que la liste des pièces jointes.");

                y = drawMainContent(cs, margin, y, p);

                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                y -= 4;
                y = writeLine(cs, margin, y, sanitize("Pièces jointes"), 18);

                cs.setFont(PDType1Font.HELVETICA, 11);
                if (docs == null || docs.isEmpty()) {
                    y = writeLine(cs, margin, y, sanitize("- Aucune pièce jointe"), 14);
                } else {
                    int i = 1;
                    for (ProjectDocument d : docs) {
                        String tag = d.isImage() ? "🖼" : "📄";
                        // emojis peuvent poser souci -> on les remplace par ASCII
                        tag = d.isImage() ? "[IMAGE]" : "[DOC]";
                        y = writeLine(cs, margin, y, sanitize(i + ") " + tag + " " + safe(d.getFileName())), 14);
                        i++;
                        if (y < 120) {
                            y = writeLine(cs, margin, y, sanitize("... (liste tronquée sur cette page)"), 14);
                            break;
                        }
                    }
                }

                drawFooter(cs, page1);
            }

            // Pages images (centrées entre header/footer)
            if (docs != null) {
                for (ProjectDocument d : docs) {
                    if (!d.isImage()) continue;
                    if (d.getFilePath() == null) continue;

                    File imgFile = new File(d.getFilePath());
                    if (!imgFile.exists()) continue;

                    PDPage imgPage = new PDPage(PDRectangle.A4);
                    document.addPage(imgPage);

                    PDImageXObject img = PDImageXObject.createFromFileByContent(imgFile, document);

                    try (PDPageContentStream csImg = new PDPageContentStream(document, imgPage)) {
                        float margin = 50;
                        float y = 790;

                        csImg.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        y = writeLine(csImg, margin, y, sanitize("Image jointe : " + safe(d.getFileName())), 18);
                        y = drawDivider(csImg, margin, y - 2);
                        y -= 10;

                        float footerTop = 70;
                        float availableH = (y - footerTop);
                        float availableW = imgPage.getMediaBox().getWidth() - 2 * margin;

                        float w = img.getWidth();
                        float h = img.getHeight();

                        float scale = Math.min(availableW / w, availableH / h);
                        float drawW = w * scale;
                        float drawH = h * scale;

                        float startX = (imgPage.getMediaBox().getWidth() - drawW) / 2;
                        float startY = footerTop + (availableH - drawH) / 2;

                        csImg.drawImage(img, startX, startY, drawW, drawH);

                        drawFooter(csImg, imgPage);
                    }
                }
            }

            document.save(pdfPath.toFile());
        }

        return pdfPath.toString();
    }

    // ---------------------------
    // Layout helpers
    // ---------------------------
    private static float drawHeader(PDDocument document, PDPageContentStream cs, PDPage page, float x, float y, String title) throws IOException {
        float logoW = 70;
        float logoH = 30;

        boolean logoDrawn = false;
        try (InputStream is = ProjetPdfGenerator.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                PDImageXObject logo = PDImageXObject.createFromByteArray(document, bytes, "logo");
                cs.drawImage(logo, x, y - logoH, logoW, logoH);
                logoDrawn = true;
            }
        } catch (Exception ignored) {}

        cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        cs.beginText();
        cs.newLineAtOffset(logoDrawn ? (x + 85) : x, y - 18);
        cs.showText(sanitize("GreenLedger — " + title));
        cs.endText();

        return y - 45;
    }

    private static float drawIntro(PDPageContentStream cs, float x, float y, String introText) throws IOException {
        cs.setFont(PDType1Font.HELVETICA, 11);
        y -= 8;
        y = writeWrapped(cs, x, y, sanitize(introText), 500, 14);
        y -= 6;
        y = drawDivider(cs, x, y);
        return y - 14;
    }

    private static float drawMainContent(PDPageContentStream cs, float x, float y, Projet p) throws IOException {
        cs.setFont(PDType1Font.HELVETICA, 12);

        y = writeKV(cs, x, y, "ID Projet", p.getId() > 0 ? String.valueOf(p.getId()) : "-");
        y = writeKV(cs, x, y, "Titre", safe(p.getTitre()));
        y = writeKV(cs, x, y, "Statut", safe(p.getStatutEvaluation()));
        y = writeKV(cs, x, y, "Description", safe(p.getDescription()));

        Budget b = p.getBudgetObj();
        if (b != null) {
            String money = MONEY_FMT.format(b.getMontant()) + " " + safe(b.getDevise());
            y = writeKV(cs, x, y, "Budget", money);
            y = writeKV(cs, x, y, "Raison Budget", safe(b.getRaison()));
        }

        y = writeKV(cs, x, y, "Adresse entreprise", safe(p.getCompanyAddress()));
        y = writeKV(cs, x, y, "Email entreprise", safe(p.getCompanyEmail()));
        y = writeKV(cs, x, y, "Téléphone entreprise", safe(p.getCompanyPhone()));
        return y;
    }

    private static void drawFooter(PDPageContentStream cs, PDPage page) throws IOException {
        float x = 50;
        float y = 45;

        cs.setFont(PDType1Font.HELVETICA, 10);

        cs.moveTo(x, y + 12);
        cs.lineTo(page.getMediaBox().getWidth() - x, y + 12);
        cs.stroke();

        String footer = BRAND_EMAIL + "  |  Fax: " + BRAND_FAX + "  |  " + BRAND_ADDRESS;

        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(footer));
        cs.endText();
    }

    private static float drawDivider(PDPageContentStream cs, float x, float y) throws IOException {
        cs.moveTo(x, y);
        cs.lineTo(x + 500, y);
        cs.stroke();
        return y;
    }

    // ---------------------------
    // Text helpers
    // ---------------------------
    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // ✅ IMPORTANT: évite les caractères non supportés par Helvetica/WinAnsi
    private static String sanitize(String s) {
        if (s == null) return "";

        // Remplace espaces Unicode par espace normal
        s = s.replace('\u202F', ' ');
        s = s.replace('\u00A0', ' ');

        // Remplace tirets longs et autres
        s = s.replace('\u2013', '-');
        s = s.replace('\u2014', '-');

        // Supprime tout ce qui n'est pas WinAnsi simple (MVP)
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c <= 126) { // ASCII standard
                out.append(c);
            } else {
                // On garde quelques accents fréquents en ISO-8859-1
                if ("àâäçéèêëîïôöùûüÿÀÂÄÇÉÈÊËÎÏÔÖÙÛÜŸ".indexOf(c) >= 0) out.append(c);
                else out.append(' ');
            }
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    private static float writeKV(PDPageContentStream cs, float x, float y, String key, String value) throws IOException {
        String line = sanitize(key) + " : " + sanitize(value);
        return writeWrapped(cs, x, y, line, 500, 16);
    }

    private static float writeLine(PDPageContentStream cs, float x, float y, String text, float step) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - step;
    }

    private static float writeWrapped(PDPageContentStream cs, float x, float y, String text, float maxWidth, float step) throws IOException {
        text = sanitize(text);
        if (text.isEmpty()) return y - step;

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String test = line.length() == 0 ? w : line + " " + w;

            float width = (PDType1Font.HELVETICA.getStringWidth(test) / 1000f) * 11;
            if (width > maxWidth) {
                y = writeLine(cs, x, y, line.toString(), step);
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            y = writeLine(cs, x, y, line.toString(), step);
        }
        return y;
    }
}