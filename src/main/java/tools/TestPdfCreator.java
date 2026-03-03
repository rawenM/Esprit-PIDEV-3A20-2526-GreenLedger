package Tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;

public class TestPdfCreator {
    public static void main(String[] args) throws Exception {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
        cs.newLineAtOffset(50, 700);
        cs.showText("Test PDF pour extraction par PdfRestService");
        cs.endText();
        cs.close();
        File out = new File("test-sample.pdf");
        doc.save(out);
        doc.close();
        System.out.println("Saved sample: " + out.getAbsolutePath());
    }
}

