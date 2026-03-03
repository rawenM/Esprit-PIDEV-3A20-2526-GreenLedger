package tools;

import Services.AdobePdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;

public class AdobePdfTestMain {
    public static void main(String[] args) throws Exception {
        System.out.println("AdobePdfTestMain starting");
        File f = new File("test-sample-adobe.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(50, 700);
                cs.showText("Test PDF for Adobe extraction");
                cs.endText();
            }
            doc.save(f);
        }
        System.out.println("Created test PDF: " + f.getAbsolutePath());
        AdobePdfService s = new AdobePdfService();
        try {
            String text = s.extractTextFromFilePath(f.getAbsolutePath());
            System.out.println("Extraction result length=" + (text == null ? 0 : text.length()));
            System.out.println("--- Extracted text begin ---\n" + text + "\n--- Extracted text end ---");
        } catch (Exception ex) {
            System.err.println("Adobe extraction failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

