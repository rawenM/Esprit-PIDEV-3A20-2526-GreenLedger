package tools;

import Services.PdfRestService;

public class PdfRestTestMain {
    public static void main(String[] args) {
        try {
            Tools.TestPdfCreator.main(args);
            PdfRestService service = new PdfRestService();
            String text = service.extractTextFromFilePath("test-sample.pdf");
            System.out.println("--- Extracted text start ---\n" + text + "\n--- Extracted text end ---");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

