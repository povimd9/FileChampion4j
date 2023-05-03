package dev.filechampion.filechampion4j;


import org.json.JSONObject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.UUID;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;




/**
 * Test FileValidator class.
 */
public class FileValidatorJsonTest {
    private static final String testUsername = System.getProperty("user.name");
    private static final String testPluginSuccessCommand = System.getProperty("os.name").startsWith("Windows")?
    "cmd /c copy ${filePath} ${filePath}.new.pdf && echo Success: ${filePath}.new.pdf" : "cp ${filePath} ${filePath}.new.pdf && echo Success: ${filePath}.new.pdf";
        
    private static final String testPluginFailureCommand = System.getProperty("os.name").startsWith("Windows")?
    "cmd /c ping -n 10 127.0.0.1" : "ping 127.0.0.1";

    /////////////////////////////////////
    // Config JSON objects for testing //
    /////////////////////////////////////

    // Config JSON object for testing mime type
    private static final JSONObject CONFIG_JSON_CHECKSUMS = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "}\r\n"
    + "}");
    
    // Config JSON object for testing mime type
    private static final JSONObject CONFIG_JSON_MIME = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"non_existing_mime\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"" + testUsername + "\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing magic bytes
    private static final JSONObject CONFIG_JSON_MAGIC = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"99999999999999999999\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"" + testUsername + "\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing header signatures
    private static final JSONObject CONFIG_JSON_HEADER = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"99999999999999\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"" + testUsername + "\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing footer signatures
    private static final JSONObject CONFIG_JSON_FOOTER = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"99999999999999999\",\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"" + testUsername + "\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing name encoding false
    private static final JSONObject CONFIG_JSON_ENCODE = new JSONObject("{\r\n"
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
    + "      \"name_encoding\": false,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing size 0
    private static final JSONObject CONFIG_JSON_SIZE = new JSONObject("{\r\n"
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
    + "      \"max_size\": \"0\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing only basic parameters with size as string
    private static final JSONObject CONFIG_JSON_BASIC = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing only basic parameters with size as integer
    private static final JSONObject CONFIG_JSON_INT_SIZE = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"max_size\": 4000\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing unsupported objecy type
    private static final JSONObject CONFIG_JSON_LONG_MAGIC = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": 255044469999999999999999999999999999999999999999999999999999999999999999999999999999,\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"max_size\": 4000\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing boolean parameter in string argument
    private static final JSONObject CONFIG_JSON_BOOL_IN_STRING = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": true,\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"max_size\": false\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing string parameter in boolean argument
    private static final JSONObject CONFIG_JSON_STRING_IN_BOOL = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"max_size\": 4000,\r\n"
    + "      \"name_encoding\": \"test\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing array parameter in string argument
    private static final JSONObject CONFIG_JSON_ARRAY_IN_STRING = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": [\"clean_pdf_documents1.step1\", \"clean_pdf_documents2.step1\", \"clean_pdf_documents3.step1\"],\r\n"
    + "      \"max_size\": 4000\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing without mime type
    private static final JSONObject CONFIG_JSON_NOMIME = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing without magic bytes
    private static final JSONObject CONFIG_JSON_NOMAGIC = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing without header and footer signatures
    private static final JSONObject CONFIG_JSON_NOSIGS = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"max_size\": \"4000\"\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Config JSON object for testing without any validations
    private static final JSONObject CONFIG_JSON_NOVALS = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      }\r\n"
    + "  }\r\n"
    + "},\r\n"
    + "  \"Plugins\": \r\n"
    + "{\"clean_pdf_documents1\":{\"step1.step\":{\"type\":\"cli\",\"run_before\":true,\"endpoint\":\""
    + testPluginSuccessCommand
    + "\",\"timeout\":320,\"on_timeout_or_fail\":\"fail\",\"response\":\"Success: ${step1.newFilePath}\"}}}"
    + "  }\r\n"
    + "}");

    // Test valid inputs to compare checksums
    @Test
    void testChecksums() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_CHECKSUMS);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertEquals(calculateChecksum(fileInBytes), fileValidationResults.getFileChecksum(), "Expected checksums to match");
    }

    // Test step timeout
    @Test
    void testStepTimeout() throws Exception {
        byte[] fileInBytes = generatePdfBytes(25000);
        String fileName = "test.pdf";
        String jsonConfigContent = new String(Files.readAllBytes(Paths.get("src","test", "resources", "configTestPluginTimeout.json").toAbsolutePath()));
        JSONObject jsonObject = new JSONObject(jsonConfigContent);
        FileValidator validator = new FileValidator(jsonObject);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid, got: " + fileValidationResults.resultsInfo());
        assertTrue(fileValidationResults.resultsDetails().contains("Process timeout"), "Expected validation response to contain 'Process timeout'");
    }

    // Test step failure
    @Test
    void testStepfailure() throws Exception {
        byte[] fileInBytes = generatePdfBytes(25000);
        String fileName = "test.pdf";
        String jsonConfigContent = new String(Files.readAllBytes(Paths.get("src","test", "resources", "configTestPluginFailure.json").toAbsolutePath()));
        JSONObject jsonObject = new JSONObject(jsonConfigContent);
        FileValidator validator = new FileValidator(jsonObject);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid");
        assertTrue(fileValidationResults.resultsDetails().contains("Error"), "Expected validation response to contain fail");
    }

    // Test step failure before validations
    @Test
    void testStepfailureBefore() throws Exception {
        byte[] fileInBytes = generatePdfBytes(25000);
        String fileName = "test.pdf";
        String jsonConfigContent = new String(Files.readAllBytes(Paths.get("src","test", "resources", "configTestPluginFailureBefore.json").toAbsolutePath()));
        JSONObject jsonObject = new JSONObject(jsonConfigContent);
        FileValidator validator = new FileValidator(jsonObject);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid");
        assertTrue(fileValidationResults.resultsDetails().contains("Error"), "Expected validation response to contain fail");
    }

    // Test non existing plugin load failure
    @Test
    void testPluginLoadFailue() throws Exception {
        String jsonConfigContent = new String(Files.readAllBytes(Paths.get("src","test", "resources", "configTestPluginLoadFailure.json").toAbsolutePath()));
        JSONObject jsonObject = new JSONObject(jsonConfigContent);
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(jsonObject), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("defined in config does not exist in plugins configuration"), 
        "Expected exception to contain 'config does not exist in plugins configuration'.");
    }

    // Test missing plugin keys
    @Test
    void testPluginKeysFailue() throws Exception {
        String jsonConfigContent = new String(Files.readAllBytes(Paths.get("src","test", "resources", "configTestPluginConfigFailure.json").toAbsolutePath()));
        JSONObject jsonObject = new JSONObject(jsonConfigContent);
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(jsonObject), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("Error initializing plugins: JSONObject"), 
        "Expected exception to contain 'Error initializing plugins: JSONObject'.");
    }

    // Test invalid value type in validations config
    @Test
    void testLongMimeValue() throws Exception {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(CONFIG_JSON_LONG_MAGIC), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("Unsupported value type"), 
        "Expected exception to contain 'Unsupported value type'.");
    }

    // Test boolean value in string
    @Test
    void testBoolInString() throws Exception {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(CONFIG_JSON_BOOL_IN_STRING), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("Error initializing extensions: Unsupported value type:"), 
        "Expected exception to contain 'Error initializing extensions: Unsupported value type:'.");
    }

    // Test string value in boolean
    @Test
    void testStringInBool() throws Exception {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(CONFIG_JSON_STRING_IN_BOOL), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("Error initializing extensions: Unsupported value type:"), 
        "Expected exception to contain 'Error initializing extensions: Unsupported value type:'.");
    }

    // Test array value in string
    @Test
    void testArrayInString() throws Exception {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(CONFIG_JSON_ARRAY_IN_STRING), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("Error initializing extensions: Unsupported value type:"), 
        "Expected exception to contain 'Error initializing extensions: Unsupported value type:'.");
    }

    // Test invalid mime type in json config
    @Test
    void testInValidMime() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_MIME);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid");
    }

    // Test invalid magic bytes in json config
    @Test
    void testInValidMagic() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_MAGIC);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid");
    }

    // Test invalid header signatures in json config
    @Test
    void testInValidMagicHeader() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_HEADER);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid");
    }

    // Test invalid footer signatures in json config
    @Test
    void testInValidMagicFooter() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_FOOTER);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid");
    }

    // Test encode set to false in json config
    @Test
    void testJsonEncodeFalse() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_ENCODE);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be invalid");
    }

    // Test size 0 in json config
    @Test
    void testSizeZero() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_SIZE);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertFalse(fileValidationResults.isValid(), "Expected validation response to be invalid");
        assertTrue(fileValidationResults.resultsDetails().contains("Invalid file size"), "Expected validation response to contain 'Invalid file size', got:" + fileValidationResults.resultsDetails());
    }

    // Test only basic parameters in json config with size as string
    @Test
    void testBasicConfig() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_BASIC);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be invalid");
    }

    // Test only basic parameters in json config with size as integer
    @Test
    void testIntSizeValue() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_INT_SIZE);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be invalid");
    }
    
    // Test no mime type in json config
    @Test
    void testNoMime() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_NOMIME);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Logger logger = Logger.getLogger(FileValidator.class.getName());
        logger.setLevel(Level.ALL);
        StreamHandler handler = new StreamHandler(outputStream, new SimpleFormatter());
        logger.addHandler(handler);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        handler.flush(); 
        String loggerOutput = outputStream.toString();
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertTrue(loggerOutput.contains("Mime type check passed, mime type: null"), "Expected validation response to contain 'Mime type check passed, mime type: null'.");
        logger.removeHandler(handler);
    }

    // Test no magic bytes in json config
    @Test
    void testNoMagic() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_NOMAGIC);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Logger logger = Logger.getLogger(FileValidator.class.getName());
        logger.setLevel(Level.ALL);
        StreamHandler handler = new StreamHandler(outputStream, new SimpleFormatter());
        logger.addHandler(handler);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        handler.flush(); 
        String loggerOutput = outputStream.toString();
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be valid");
        assertTrue(loggerOutput.contains("Magic bytes check passed, magic bytes: null"), "Expected validation response to contain 'Mime type check passed, mime type: null'.");
        logger.removeHandler(handler);
    }

    // Test no header and footer signatures in json config
    @Test
    void testNoSigs() throws Exception {
        byte[] fileInBytes = generatePdfBytes(250000);
        String fileName = "test.pdf";
        FileValidator validator = new FileValidator(CONFIG_JSON_NOSIGS);
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, fileName);
        assertTrue(fileValidationResults.isValid(), "Expected validation response to be invalid");
    }

    // Test no validation values for extension
    @Test
    void testNoVals() throws Exception {
        //byte[] fileInBytes = generatePdfBytes(250000);
        //String fileName = "test.pdf";
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new FileValidator(CONFIG_JSON_NOVALS), "Expected exception to be thrown");
        assertTrue(exception.getMessage().contains("At least one validation must be configured"), 
        "Expected exception to contain 'At least one validation must be configured'.");
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
