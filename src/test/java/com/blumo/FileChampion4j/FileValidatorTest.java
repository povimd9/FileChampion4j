package com.blumo.FileChampion4j;


import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for FileValidator class.
 */

public class FileValidatorTest {
    
    private Path tempDirectory;
    private FileValidator validator;
    private static final String testUsername = System.getProperty("user.name");
    private static final String testPluginCommand = System.getProperty("os.name").startsWith("Windows")?
        "cmd /c echo Success: ${filePath} ${fileContent} ${fileChecksum}" : "echo Success: ${filePath} ${fileContent} ${fileChecksum}";


    // Config JSON object for testing
    private static final JSONObject CONFIG_JSON = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"antivirus_scan\": {\r\n"
    + "        \"clamav_scan.java\": [\r\n"
    + "          \"RETURN_TYPE\",\r\n"
    + "          \"param1\",\r\n"
    + "          \"param2\"\r\n"
    + "        ]},\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"" + testUsername + "\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      },\r\n"
    + "    \"doc\": {\r\n"
    + "      \"mime_type\": \"application/msword\",\r\n"
    + "      \"magic_bytes\": \"D0CF11E0A1B11AE1\",\r\n"
    + "      \"header_signatures\": \"D0CF11E0A1B11AE1\",\r\n"
    + "      \"footer_signatures\": \"0000000000000000\",\r\n"
    + "      \"antivirus_scan\": {\r\n"
    + "        \"clamav_scan.java\": [\r\n"
    + "          \"RETURN_TYPE\",\r\n"
    + "          \"param1\",\r\n"
    + "          \"param2\"\r\n"
    + "        ]},\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"User1\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "    }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"run_after\":true, \"endpoint\":\""
    + testPluginCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");
    

    // Setup temp directory and FileValidator instance
    @BeforeEach
    void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("temp");
        validator = new FileValidator(CONFIG_JSON);
    }

    // Test empty json config object
    @Test
    void testEmptyConfigJsonObject() {
        JSONObject jsonObject = new JSONObject();
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(jsonObject));
        assertEquals("Config JSON object cannot be null or empty, and must have Validations section.", exception.getMessage());
    }

    // Test null json config object
    @Test
    void testNullConfigJsonObject() {
        JSONObject jsonObject = null;
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(jsonObject));
        assertEquals("Config JSON object cannot be null or empty, and must have Validations section.", exception.getMessage());
    }

    // Test empty fileCategory
    @Test
    void testBlankFileCategory() throws Exception {
        byte[] fileInBytes = "1234".getBytes();
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
        validator.validateFile("", fileInBytes, fileName, tempDirectory.toString()));
    assertEquals("fileCategory cannot be null or empty.", exception.getMessage());
    }
    
    // Test empty fileName
    @Test
    void testBlankFileName() throws Exception {
        byte[] fileInBytes = "1234".getBytes();
        String fileName = "";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempDirectory.toString());        });
        assertEquals("fileName cannot be null or empty.", exception.getMessage());
    }
    
    // Test originalFile Bytes is null
    @Test
    void testNullOriginalFile() throws Exception {
        byte[] fileInBytes = null;
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempDirectory.toString());        });
        assertEquals("originalFile cannot be null or empty.", exception.getMessage());
    }

    // Test originalFile Bytes is empty
    @Test
    void testEmptyOriginalFile() throws Exception {
        byte[] fileInBytes = new byte[]{};
        String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempDirectory.toString());        });
        assertEquals("originalFile cannot be null or empty.", exception.getMessage());
    }

    // Test none existing fileCategory '0934jt0-349rtj3409rj3409rj'
    @Test
    void testFileCategoryNotConfigured() throws Exception {
        byte[] fileInBytes = "1234".getBytes();
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("0934jt0-349rtj3409rj3409rj", fileInBytes, fileName, tempDirectory.toString());
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid with non existing fileCategory");
        assertTrue(fileValidationResults.resultsInfo().contains("Error creating Extension configurations object"), "Expected 'Error creating Extension configurations object', got: " + fileValidationResults.resultsInfo());
    }

    // Test file extension that is not configured in config json
    @Test
    void testInvalidExtension() throws Exception {
        byte[] fileInBytes = "1234".getBytes();
        String fileName = "test.txt";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempDirectory.toString());
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid with .txt extension");
        assertTrue(fileValidationResults.resultsInfo().contains("Error creating Extension configurations object"));
    }

    // Test file size that is greater than the max size configured in config json
    @Test
    void testFileTooLarge() throws Exception {
        byte[] fileInBytes = generatePdfBytes(5000000);
        String fileName = "largeFile.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempDirectory.toString());
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid for file size:" + fileInBytes.length);
        assertTrue(fileValidationResults.resultsInfo().contains("exceeds maximum allowed size"));
    }

    // Test valid inputs including valid pdf file with storage
    @Test
    void testValidInputsStore() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempDirectory.toString());
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertEquals(calculateChecksum(fileInBytes), fileValidationResults.getFileChecksum(), "Expected checksums to match");
    }

    // Test valid inputs including valid pdf file without storage
    @Test
    void testValidInputs() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertEquals(calculateChecksum(fileInBytes), fileValidationResults.getFileChecksum(), "Expected checksums to match");
    }
    
    // Test file with content mismatching its extension inclufing magic bytes, header and footer, validations
    @Test
    void testContenMismatch() throws Exception {
        byte[] fileInBytes = "This is not a pdf file".getBytes();
        String fileName = "notReal.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempDirectory.toString());
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid when content does not match extension");
        assertTrue(fileValidationResults.resultsInfo().contains("Invalid magic_bytes for file extension:"), "Expected 'Invalid magic_bytes', got: " + fileValidationResults.resultsInfo());
    }

    // Test saving to non existing directory
    @Test
    void testSaveToNonExistingDirectory() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, "nonExistingDirectory-9384rhj934f8h3498h/3hd923d8h");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid when saving to non existing directory");
        assertTrue(fileValidationResults.resultsInfo().contains("File is valid but was not saved to output directory:"), "Expected 'File is valid but was not saved to output directory', got: " + fileValidationResults.resultsInfo());
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

    // Calculate file checksum
    private static String calculateChecksum(byte[] fileBytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(fileBytes);
            return new BigInteger(1, hash).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }   

}