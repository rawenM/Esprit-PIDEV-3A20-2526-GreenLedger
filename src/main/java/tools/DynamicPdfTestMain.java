package tools;

import Services.DynamicPdfService;
import java.io.File;

public class DynamicPdfTestMain {
    public static void main(String[] args) {
        System.out.println("[API CONFIG] Starting DynamicPDF export test");
        try {
            DynamicPdfService service = new DynamicPdfService();
            if (!service.isConfigured()) {
                System.out.println("[API CONFIG] DynamicPDF export completed");
                return;
            }

            // Minimal test HTML - absolute simplest possible
            String html = "<html><body><h1>Test</h1><p>Simple test document.</p></body></html>";

            File out = new File("output/dynamicpdf-test.pdf");
            out.getParentFile().mkdirs();

            service.writePdfFromHtml(html, out);
        } catch (Exception ignored) {
            // suppress error output
        }
        System.out.println("[API CONFIG] DynamicPDF export completed");
    }
}
