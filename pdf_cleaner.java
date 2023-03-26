import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.listener.*;
import com.itextpdf.pdfa.*;
import com.itextpdf.pdfa.converters.*;
import com.itextpdf.pdfa.exceptions.*;
import com.itextpdf.pdfa.validators.*;

import java.io.*;

public class PDFSanitizer {
    public static String sanitizePDF(String inputFilePath) throws IOException, PdfException {
        // Check if the PDF is encrypted
        PdfReader reader = new PdfReader(inputFilePath);
        String filepath = reader.getSafeFile().getAbsolutePath();
        String fileName = reader.getFileName();
        String cleanFileName = filepath + File.separator + fileName.replace(".pdf", "_clean.pdf");

        if (reader.isEncrypted()) {
            throw new PdfException("The PDF file is encrypted");
        }

        // Load the PDF document
        PdfDocument pdfDoc = new PdfDocument(reader, new PdfWriter(cleanFileName));

        // Sanitize the PDF document
        SanitizerProperties sanitizerProperties = new SanitizerProperties().setListed(true);
        PdfCleaner.cleanUp(pdfDoc, sanitizerProperties);

        // Convert the PDF document to PDF/A
        PdfADocument pdfADoc = new PdfADocument(new PdfWriter(cleanFileName + "a"), PdfAConformanceLevel.PDF_A_3B,
                new PdfOutputIntent("Custom", "", "http://www.color.org", "sRGB IEC61966-2.1",
                        new FileInputStream("sRGB_CS_profile.icm")));

        // Validate the PDF/A document
        ValidationResult result = PdfADocument.validate(pdfADoc, new ValidationOptions());
        if (!result.isValid()) {
            System.err.println("PDF/A validation failed: " + result.getErrors());
        }

        // Close the PDF documents
        pdfDoc.close();
        pdfADoc.close();

        return cleanFileName;
    }
}
