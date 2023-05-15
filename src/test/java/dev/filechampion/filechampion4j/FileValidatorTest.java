package dev.filechampion.filechampion4j;


import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmh.annotations.Setup;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for FileValidator class.
 */
public class FileValidatorTest {
    @TempDir
    private Path tempDirectory;
    @TempDir
    private Path tempOutDirectory;
    private FileValidator validator;
    private String fileName;
    private byte[] fileInBytes;

    @Setup
    @BeforeEach
    void setUp() throws Exception {
        Object o = FileValidator.class.getResourceAsStream("/logging.properties");
        LogManager.getLogManager().readConfiguration((InputStream) o);
        String testUsername = System.getProperty("user.name");
        String testCliContentPlugin = "java -jar plugins/java_echo.jar Success: MTIzNDU2IA0K, MD5: ${fileChecksum.md5}, SHA-1: ${fileChecksum.sha1}, SHA-256: ${fileChecksum.sha256}, SHA-512: ${fileChecksum.sha512}, suffix";
        fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        fileName = "test.pdf";
        String testCliPathPlugin = JSONObject.quote("java -jar plugins/java_echo.jar Success: " + filePath.toAbsolutePath().toString());

        // Config JSON object for testing
        JSONObject CONFIG_JSON = new JSONObject("{\r\n"
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
        + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true, \"endpoint\":"
        + testCliPathPlugin
        + ",\"timeout\":320,\"on_timeout_or_fail\":\"pass\",\"response\":\"Success: ${step1.filePath}\"}}"
        + ",\"clean_pdf_documents2\":{\"step1.step\":{\"type\":\"cli\",\"run_after\":true, \"endpoint\":\""
        + testCliContentPlugin
        + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.fileContent}, MD5: 5d3968fb44533d2554db0f3f7771eb75, SHA-1: 5a5b1cedc98e7b74d357d8275dc65e8603e0294b, SHA-256: eacd49cc57396604e9629b1c6cdd7dc4679a03707c215665198c484fdcc97721, SHA-512: 1fd4d38f7bf908315075a7e124189a51827f84e1c7297b3fc57dfce81035a324c5a4232cc7270372d275dda5b5eaa6559b550f1762e96957e7774c639ef74f62, suffix\"}}"
        + ",\"clean_pdf_documents3\":{\"step1.step\":{\"type\":\"cli\",\"run_after\":true, \"endpoint\":\""
        + testCliContentPlugin
        + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"\"}}"
        + "  }}\r\n"
        + "}");
        
        validator = new FileValidator(CONFIG_JSON);
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
        fileInBytes = "1234".getBytes();
        fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
        validator.validateFile("", fileInBytes, fileName, tempOutDirectory), "fileCategory cannot be null or empty.");
        assertEquals("fileCategory cannot be null or empty.", exception.getMessage(), "Expected exception message to be 'fileCategory cannot be null or empty.'");
    }

    // Test empty fileName
    @Test
    void testBlankFileName() throws Exception {
        fileInBytes = "1234".getBytes();
        fileName = "";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory); }, "fileName cannot be null or empty.");
        assertEquals("fileName cannot be null or empty.", exception.getMessage(), "Expected exception message to be 'fileName cannot be null or empty.'");
    }
    
    // Test originalFile Bytes is null
    @Test
    void testNullOriginalFile() throws Exception {
        fileInBytes = null;
        fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory); }, "originalFile cannot be null or empty.");
        assertEquals("originalFile cannot be null or empty.", exception.getMessage(), "Expected exception message to be 'originalFile cannot be null or empty.'");
    }

    // Test originalFile Bytes is empty
    @Test
    void testEmptyOriginalFile() throws Exception {
        fileInBytes = new byte[]{};
        fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory); }, "originalFile cannot be null or empty.");
        assertEquals("originalFile cannot be null or empty.", exception.getMessage(), "Expected exception message to be 'originalFile cannot be null or empty.'");
    }

    // Test none existing fileCategory '0934jt0-349rtj3409rj3409rj'
    @Test
    void testFileCategoryNotConfigured() throws Exception {
        fileInBytes = "1234".getBytes();
        fileName = "test.pdf";
        String fileCategory = "0934jt0-349rtj3409rj3409rj";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> validator.validateFile(fileCategory, fileInBytes, fileName, tempOutDirectory), "category " + fileCategory +" not found");
        assertEquals("category 0934jt0-349rtj3409rj3409rj not found", exception.getMessage(), "Expected exception message to be 'category 0934jt0-349rtj3409rj3409rj not found'");
    }

    // Test file extension that is not configured in config json extension txt not found
    @Test
    void testInvalidExtension() throws Exception {
        fileInBytes = "1234".getBytes();
        fileName = "test.txt";
        String extensionText = "txt";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory), "extension " + extensionText + " not found");
        assertEquals("extension " + extensionText + " not found", exception.getMessage(), "Expected exception message to be 'extension " + extensionText + " not found'");
    }

    // Test file size that is greater than the max size configured in config json
    @Test
    void testFileTooLarge() throws Exception {
        JSONObject testSizeConfig = 
        new JSONObject("{\r\n"
        + "  \"Validations\": {\r\n"
        + "  \"Documents\": {\r\n"
        + "    \"pdf\": {\r\n"
        + "      \"max_size\": 10\r\n"
        + "      }\r\n"
        + "  }\r\n"
        + "}\r\n"
        + "}");
        validator = new FileValidator(testSizeConfig);
        fileInBytes = generatePdfBytes(5000000);
        fileName = "largeFile.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid for file size:" + fileInBytes.length);
        assertTrue(fileValidationResults.resultsDetails().contains("exceeds maximum allowed size"), "Expected 'exceeds maximum allowed size', got: " + fileValidationResults.resultsDetails());
    }

    // Test valid inputs including valid pdf file as bytes with storage
    @Test
    void testValidInputsBytesStore() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        fileName = "test&test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
    }

    // Test valid inputs including large valid pdf file as bytes with storage
    @Test
    void testValidInputsLargeBytesStore() throws Exception {
        fileInBytes = Files.readAllBytes(Paths.get("src","test", "resources", "testVeryLarge.pdf").toAbsolutePath());
        fileName = "testVeryLarge.pdf";
        JSONObject testLargeConfig = 
        new JSONObject("{\r\n"
        + "  \"Validations\": {\r\n"
        + "  \"Documents\": {\r\n"
        + "    \"pdf\": {\r\n"
        + "      \"mime_type\": \"application/pdf\"\r\n"
        + "      }\r\n"
        + "  }\r\n"
        + "}\r\n"
        + "}");
        validator = new FileValidator(testLargeConfig);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
    }

    // Test valid inputs including large valid pdf file as bytes with storage without checksum
    @Test
    void testValidInputsLargeBytesStoreNoChecksum() throws Exception {
        fileInBytes = Files.readAllBytes(Paths.get("src","test", "resources", "testVeryLarge.pdf").toAbsolutePath());
        fileName = "testVeryLarge.pdf";
        JSONObject testLargeConfig = 
        new JSONObject("{\r\n"
        + "  \"Validations\": {\r\n"
        + "  \"Documents\": {\r\n"
        + "    \"pdf\": {\r\n"
        + "      \"mime_type\": \"application/pdf\",\r\n"
        + "      \"add_checksum\": false\r\n"
        + "      }\r\n"
        + "  }\r\n"
        + "}\r\n"
        + "}");
        validator = new FileValidator(testLargeConfig);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory, "application/pdf");
        Map<String,String> emptyMap = new HashMap<>();
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
        assertEquals( emptyMap, fileValidationResults.getFileChecksums(), "Expected checksum to be empty, got: " + fileValidationResults.getFileChecksums());
    }

    // Test valid inputs with add_checksum false
    @Test
    void testValidInputsBytesStoreNoChecksum() throws Exception {
        String jsonConfigContent = new String(Files.readAllBytes(Paths.get("src","test", "resources", "configTestChecksumBool.json").toAbsolutePath()));
        JSONObject jsonObject = new JSONObject(jsonConfigContent);
        validator = new FileValidator(jsonObject);
        fileInBytes = generatePdfBytes(250000);
        fileName = "test&test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("LargeDocuments", fileInBytes, fileName);
        Map<String,String> emptyMap = new HashMap<>();
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
        assertEquals( emptyMap, fileValidationResults.getFileChecksums(), "Expected checksum to be empty, got: " + fileValidationResults.getFileChecksums());
    }

        

    // Test valid inputs including valid pdf bytes, with mime type, with storage
    @Test
    void testValidInputsMimeStore() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
    }

    // Test valid inputs including valid pdf file path with storage
    @Test
    void testValidInputsPathStore() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", filePath, fileName, tempOutDirectory);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
    }
    
    // Test valid inputs including valid pdf file path, with mime type, with storage
    @Test
    void testValidInputsPathMimeStore() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", filePath, fileName, tempOutDirectory, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
    }

    // Test valid inputs including valid pdf file path without storage
    @Test
    void testValidInputsPath() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", filePath, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
    }

    // Test valid inputs including valid pdf file path, with mime type, without storage
    @Test
    void testValidInputsPathMime() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        Path filePath =  Files.write(tempDirectory.resolve("test.pdf"), fileInBytes);
        fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", filePath, fileName, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
    }

    // Test valid inputs including valid pdf file bytes without storage
    @Test
    void testValidInputsBytes() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
    }

    // Test valid inputs with invalid file name
    @Test
    void testValidInputsBytesNoExtFilename() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        fileName = "test";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", fileInBytes, fileName), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("fileName must contain a file extension."), 
        "Expected exception to contain 'fileName must contain a file extension.', got: " + exception.getMessage());
    }

    // Test valid inputs with invalid file name
    @Test
    void testValidInputsBytesExtDontExistFilename() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        fileName = "test.extensiondoesnotexist";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", fileInBytes, fileName), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("extension extensiondoesnotexist not found"), 
        "Expected exception to contain 'extension extensiondoesnotexist not found', got: " + exception.getMessage());
    }

    // Test valid inputs including valid pdf bytes, with mime type, without storage, checking output for correct checksums
    @Test
    void testValidInputsMime() throws Exception {
        JSONObject testMimeConfig = 
        new JSONObject("{\r\n"
        + "  \"General\": {\r\n"
        + "\"Checksums\": [\"MD5\", \"SHA-1\", \"SHA-256\", \"SHA-512\"] \r\n"
        + "},\r\n"
        + "  \"Validations\": {\r\n"
        + "  \"Documents\": {\r\n"
        + "    \"pdf\": {\r\n"
        + "      \"mime_type\": \"application/pdf\"\r\n"
        + "      }\r\n"
        + "  }\r\n"
        + "}\r\n"
        + "}");
        validator = new FileValidator(testMimeConfig);
        fileInBytes = generatePdfBytes(250000);
        fileName = "test.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, "application/pdf");
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertFalse(fileValidationResults.resultsDetails().contains("Error"), "Expected results to be free of errors, got: " + fileValidationResults.resultsDetails());
        Map<String, String> fileChecksums = new HashMap<>();
        fileChecksums.put("MD5", calculateChecksum(fileInBytes, "MD5"));
        fileChecksums.put("SHA-1", calculateChecksum(fileInBytes, "SHA-1"));
        fileChecksums.put("SHA-256", calculateChecksum(fileInBytes, "SHA-256"));
        fileChecksums.put("SHA-512", calculateChecksum(fileInBytes, "SHA-512"));
        assertEquals(fileChecksums, fileValidationResults.getFileChecksums(), "Expected checksum to be " + fileChecksums.toString() + ", got: " + fileValidationResults.getFileChecksums());
    }

    // Test invalid checksum algorithm
    @Test
    void testValidInvalidHashAlgo() throws Exception {
        JSONObject testMimeConfig = 
        new JSONObject("{\r\n"
        + "  \"General\": {\r\n"
        + "\"Checksums\": [\"NON-EXISTING-ALGO\", \"SHA-1\", \"SHA-256\", \"SHA-512\"] \r\n"
        + "},\r\n"
        + "  \"Validations\": {\r\n"
        + "  \"Documents\": {\r\n"
        + "    \"pdf\": {\r\n"
        + "      \"mime_type\": \"application/pdf\"\r\n"
        + "      }\r\n"
        + "  }\r\n"
        + "}\r\n"
        + "}");
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(testMimeConfig), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("The hash algorithm 'NON-EXISTING-ALGO'' is not one of: [MD5, SHA-1, SHA-256, SHA-512]."), 
        "Expected exception to be 'The hash algorithm 'NON-EXISTING-ALGO'' is not one of: [MD5, SHA-1, SHA-256, SHA-512].', got: " + exception.getMessage());
    }

    // Test valid inputs with invalid pdf file path with storage
    @Test
    void testValidInputsNoPathStore() throws Exception {
        Path filePath =  Paths.get("doesnotexist/somepath/test.pdf");
        fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", filePath, fileName, tempOutDirectory), "Expected exception to be thrown.");
        assertTrue(exception.getMessage().contains("filepath does not exist."), 
        "Expected exception to contain 'filepath does not exist.', got: " + exception.getMessage());
    }
    
    // Test inputs including invalid pdf file path, with mime type, with storage
    @Test
    void testValidInputsNoPathMimeStore() throws Exception {
        Path filePath =  Paths.get("doesnotexist/somepath/test.pdf");
        fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", filePath, fileName, tempOutDirectory, "application/pdf"), "Expected exception to be thrown.");
        assertTrue(exception.getMessage().contains("filepath does not exist."), 
        "Expected exception to contain 'filepath does not exist.', got: " + exception.getMessage());
    }

    // Test inputs with invalid pdf file path without storage
    @Test
    void testValidInputsNoPath() throws Exception {
        Path filePath =  Paths.get("doesnotexist/somepath/test.pdf");
        fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", filePath, fileName), "Expected exception to be thrown.");
        assertTrue(exception.getMessage().contains("filepath does not exist."), 
        "Expected exception to contain 'filepath does not exist.', got: " + exception.getMessage());
    }

    // Test valid inputs including valid pdf file path, with mime type, without storage
    @Test
    void testValidInputsNoPathMime() throws Exception {
        Path filePath =  Paths.get("doesnotexist/somepath/test.pdf");
        fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", filePath, fileName, "application/pdf"), "Expected 'filepath does not exist.' exception.");
        assertTrue(exception.getMessage().contains("filepath does not exist."), 
        "Expected exception to contain 'filepath does not exist.', got: " + exception.getMessage());
    }

    // Test file with content mismatching its extension inclufing magic bytes, header and footer, validations
    @Test
    void testContenMismatch() throws Exception {
        JSONObject testContentConfig = 
        new JSONObject("{\r\n"
        + "  \"Validations\": {\r\n"
        + "  \"Documents\": {\r\n"
        + "    \"pdf\": {\r\n"
        + "      \"magic_bytes\": \"25504446\"\r\n"
        + "      }\r\n"
        + "  }\r\n"
        + "}\r\n"
        + "}");
        validator = new FileValidator(testContentConfig);
        fileInBytes = "This is not a pdf file".getBytes();
        fileName = "notReal.pdf";
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tempOutDirectory);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid when content does not match extension");
        assertTrue(fileValidationResults.resultsDetails().contains("Invalid magic_bytes for file"), "Expected response to contain 'Invalid magic_bytes for file', got: " + fileValidationResults.resultsDetails());
    }

    // Test saving to non existing directory
    @Test
    void testSaveToNonExistingDirectory() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        fileName = "test.pdf";
        if (!System.getProperty("os.name").startsWith("Windows")) {
            Path tmpDirectory = Paths.get("/etc/passwd");
            ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName, tmpDirectory);
            assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid when saving to target directory fails");
            assertTrue(fileValidationResults.resultsDetails().contains("File is valid but failed to save to output directory"), 
                "Expected 'File is valid but failed to save to output directory', got: " + fileValidationResults.resultsDetails());
        }
    }

    // Test saving to non existing directory
    @Test
    void testSaveToBlockedExistingDirectory() throws Exception {
        fileInBytes = generatePdfBytes(250000);
        fileName = "test.pdf";
        Path tmpDirectory = Paths.get("nonExistingDirectory-9384rhj934f8h3498h/3hd923d8h");
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> 
        validator.validateFile("Documents", fileInBytes, fileName, tmpDirectory), "Expected exception to be thrown.");
        assertTrue(exception.getMessage().contains("outDir does not exist."), 
        "Expected exception to contain 'outDir does not exist.', got: " + exception.getMessage());
    }


    // Helper methods

    // Generate a pdf file with a given size in bytes
    private byte[] generatePdfBytes(int sizeInBytes) throws Exception {
        if (sizeInBytes <= 0) {
            throw new IllegalArgumentException("Size in Bytes must be a positive value.");
        }

        if (sizeInBytes == 250000) {
            return
            Files.readAllBytes(Paths.get("src","test", "resources", "testSmall.pdf").toAbsolutePath());
        }
        if (sizeInBytes == 5000000) {
            return
            Files.readAllBytes(Paths.get("src","test", "resources", "testVeryLarge.pdf").toAbsolutePath());
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

    // Calculate checksum of a file in traditional way
    private String calculateChecksum(byte[] fileBytes, String checksumAlgorithm) {
        try {
            byte[] hash = MessageDigest.getInstance(checksumAlgorithm).digest(fileBytes);
            return new BigInteger(1, hash).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}