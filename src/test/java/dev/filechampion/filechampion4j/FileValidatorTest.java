package dev.filechampion.filechampion4j;


import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit test for FileValidator class.
 */

public class FileValidatorTest {
    
    @TempDir
    static Path tempDirectory;
    private FileValidator validator;
    private static final String testUsername = System.getProperty("user.name");
    private static final String testCliPathPlugin = "java -jar plugins/java_echo.jar Success: ${filePath}.new.pdf";
    private static final String testCliContentPlugin = "java -jar plugins/java_echo.jar Success: MTIzNDU2IA0K suffix";

    // Config JSON object for testing
    private static final JSONObject CONFIG_JSON = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"" + testUsername + "\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\",\r\n"
    +"       \"extension_plugins\": [\"clean_pdf_documents1.step1\", \"clean_pdf_documents2.step1\", \"clean_pdf_documents3.step1\"]\r\n"
    + "      },\r\n"
    + "    \"doc\": {\r\n"
    + "      \"mime_type\": \"application/msword\",\r\n"
    + "      \"magic_bytes\": \"D0CF11E0A1B11AE1\",\r\n"
    + "      \"header_signatures\": \"D0CF11E0A1B11AE1\",\r\n"
    + "      \"footer_signatures\": \"0000000000000000\",\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"User1\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "    }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true, \"endpoint\":\""
    + testCliPathPlugin
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"pass\",\"response\":\"Success: ${step1.filePath}\"}}"
    + ",\"clean_pdf_documents2\":{\"step1.step\":{\"type\":\"cli\",\"run_after\":true, \"endpoint\":\""
    + testCliContentPlugin
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.fileContent} suffix\"}}"
    + ",\"clean_pdf_documents3\":{\"step1.step\":{\"type\":\"cli\",\"run_after\":true, \"endpoint\":\""
    + testCliContentPlugin
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"\"}}"
    + "  }}\r\n"
    + "}");
    

    // Setup temp directory and FileValidator instance
    @BeforeEach
    void setUp() throws IOException {
        validator = new FileValidator(CONFIG_JSON);
    }

    @Test
    void testDefaultLoggingConfiguration() {
        assertDoesNotThrow(() -> {
            Object o = FileValidator.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration((InputStream) o);
        }, "Failed to load default logging configuration");
    }

    // Test empty json config object
    @Test
    void testEmptyConfigJsonObject() {
        JSONObject jsonObject = new JSONObject();
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(jsonObject), "Config JSON object cannot be null or empty, and must have Validations section.");
        assertEquals("Config JSON object cannot be null or empty, and must have Validations section.", exception.getMessage(), "Expected exception message to be 'Config JSON object cannot be null or empty, and must have Validations section.'");
    }

    // Test null json config object
    @Test
    void testNullConfigJsonObject() {
        JSONObject jsonObject = null;
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(jsonObject), "Config JSON object cannot be null or empty, and must have Validations section.");
        assertEquals("Config JSON object cannot be null or empty, and must have Validations section.", exception.getMessage(), "Expected exception message to be 'Config JSON object cannot be null or empty, and must have Validations section.'");
    }

    // Test empty fileCategory
    @Test
    void testBlankFileCategory() throws Exception {
        byte[] fileInBytes = "1234".getBytes();
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
        validator.validateFile("", fileInBytes, fileName, tempDirectory), "fileCategory cannot be null or empty.");
    assertEquals("fileCategory cannot be null or empty.", exception.getMessage(), "Expected exception message to be 'fileCategory cannot be null or empty.'");
    }
    
    // Test empty fileName
    @Test
    void testBlankFileName() throws Exception {
        byte[] fileInBytes = "1234".getBytes();
        String fileName = "";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempDirectory); }, "fileName cannot be null or empty.");
        assertEquals("fileName cannot be null or empty.", exception.getMessage(), "Expected exception message to be 'fileName cannot be null or empty.'");
    }
    
    // Test originalFile Bytes is null
    @Test
    void testNullOriginalFile() throws Exception {
        byte[] fileInBytes = null;
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempDirectory); }, "originalFile cannot be null or empty.");
        assertEquals("originalFile cannot be null or empty.", exception.getMessage(), "Expected exception message to be 'originalFile cannot be null or empty.'");
    }

    // Test originalFile Bytes is empty
    @Test
    void testEmptyOriginalFile() throws Exception {
        byte[] fileInBytes = new byte[]{};
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempDirectory); }, "originalFile cannot be null or empty.");
        assertEquals("originalFile cannot be null or empty.", exception.getMessage(), "Expected exception message to be 'originalFile cannot be null or empty.'");
    }

    // Test none existing fileCategory '0934jt0-349rtj3409rj3409rj'
    @Test
    void testFileCategoryNotConfigured() throws Exception {
        byte[] fileInBytes = "1234".getBytes();
        String fileName = "test.pdf";
        String fileCategory = "0934jt0-349rtj3409rj3409rj";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> validator.validateFile(fileCategory, fileInBytes, fileName, tempDirectory), "category " + fileCategory +" not found");
        assertEquals("category 0934jt0-349rtj3409rj3409rj not found", exception.getMessage(), "Expected exception message to be 'category 0934jt0-349rtj3409rj3409rj not found'");
    }

    // Test file extension that is not configured in config json extension txt not found
    @Test
    void testInvalidExtension() throws Exception {
        byte[] fileInBytes = "1234".getBytes();
        String fileName = "test.txt";
        String extensionText = "txt";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> validator.validateFile("Documents", fileInBytes, fileName, tempDirectory), "extension " + extensionText + " not found");
        assertEquals("extension " + extensionText + " not found", exception.getMessage(), "Expected exception message to be 'extension " + extensionText + " not found'");
    }

    // Test file size that is greater than the max size configured in config json
    @Test
    void testFileTooLarge() throws Exception {
        byte[] fileInBytes = generatePdfBytes(5000000);
        String fileName = "largeFile.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempDirectory);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid for file size:" + fileInBytes.length);
        assertTrue(fileValidationResults.resultsInfo().contains("exceeds maximum allowed size"), "Expected 'exceeds maximum allowed size', got: " + fileValidationResults.resultsInfo());
    }

    // Test valid inputs including valid pdf file as bytes with storage
    @Test
    void testValidInputsBytesStore() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test&test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempDirectory);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
    }

    // Test valid inputs including valid pdf bytes, with mime type, with storage
    @Test
    void testValidInputsMimeStore() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempDirectory, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
    }

    // Test valid inputs including valid pdf file path with storage
    @Test
    void testValidInputsPathStore() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", filePath, fileName, tempDirectory);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
    }
    
    // Test valid inputs including valid pdf file path, with mime type, with storage
    @Test
    void testValidInputsPathMimeStore() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", filePath, fileName, tempDirectory, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
    }

    // Test valid inputs including valid pdf file path without storage
    @Test
    void testValidInputsPath() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", filePath, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
    }

    // Test valid inputs including valid pdf file path, with mime type, without storage
    @Test
    void testValidInputsPathMime() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", filePath, fileName, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
    }

    // Test valid inputs including valid pdf file bytes without storage
    @Test
    void testValidInputsBytes() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
    }

    // Test valid inputs including valid pdf bytes, with mime type, without storage
    @Test
    void testValidInputsMime() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
    }

    // Test valid inputs with invalid pdf file path with storage
    @Test
    void testValidInputsNoPathStore() throws Exception {
        Path filePath =  Paths.get("doesnotexist/somepath/test.pdf");
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", filePath, fileName, tempDirectory), "Expected 'Error reading file' exception.");
        assertTrue(exception.getMessage().contains("Error reading file:"), 
        "Expected exception to contain 'Error reading file'.");
    }
    
    // Test valid inputs including valid pdf file path, with mime type, with storage
    @Test
    void testValidInputsNoPathMimeStore() throws Exception {
        Path filePath =  Paths.get("doesnotexist/somepath/test.pdf");
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", filePath, fileName, tempDirectory, "application/pdf"), "Expected 'Error reading file' exception.");
        assertTrue(exception.getMessage().contains("Error reading file:"), 
        "Expected exception to contain 'Error reading file'.");
    }

    // Test valid inputs including valid pdf file path without storage
    @Test
    void testValidInputsNoPath() throws Exception {
        Path filePath =  Paths.get("doesnotexist/somepath/test.pdf");
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", filePath, fileName), "Expected 'Error reading file' exception.");
        assertTrue(exception.getMessage().contains("Error reading file:"), 
        "Expected exception to contain 'Error reading file'.");
    }

    // Test valid inputs including valid pdf file path, with mime type, without storage
    @Test
    void testValidInputsNoPathMime() throws Exception {
        Path filePath =  Paths.get("doesnotexist/somepath/test.pdf");
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", filePath, fileName, "application/pdf"), "Expected 'Error reading file' exception.");
        assertTrue(exception.getMessage().contains("Error reading file:"), 
        "Expected exception to contain 'Error reading file'.");
    }

    // Test file with content mismatching its extension inclufing magic bytes, header and footer, validations
    @Test
    void testContenMismatch() throws Exception {
        byte[] fileInBytes = "This is not a pdf file".getBytes();
        String fileName = "notReal.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempDirectory);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid when content does not match extension");
        assertTrue(fileValidationResults.resultsInfo().contains("Invalid magic_bytes for file extension:"), "Expected 'Invalid magic_bytes', got: " + fileValidationResults.resultsInfo());
    }

    // Test saving to non existing directory
    @Test
    void testSaveToNonExistingDirectory() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        Path tmpDirectory = Paths.get("nonExistingDirectory-9384rhj934f8h3498h/3hd923d8h");
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tmpDirectory);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid when saving to non existing directory");
        assertTrue(fileValidationResults.resultsInfo().contains("File is valid but failed to save to output directory:"), "Expected 'File is valid but failed to save to output directory:', got: " + fileValidationResults.resultsInfo());
    }



    // Helper methods

    // Generate a pdf file with a given size in bytes
    private byte[] generatePdfBytes(int sizeInBytes) throws Exception {
        if (sizeInBytes <= 0) {
            throw new IllegalArgumentException("Size in Bytes must be a positive value.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setFullCompression();
        writer.setCompressionLevel(0);
        document.open();
    
        String content = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        int contentLength = content.getBytes().length;
    
        while (baos.size() < sizeInBytes) {
            int iterations = (sizeInBytes - baos.size()) / contentLength;
            for (int i = 0; i < iterations; i++) {
                document.add(new Paragraph(content));
            }
            writer.flush();
        }
    
        document.close();
        writer.close();
    
        return baos.toByteArray();
    }
}