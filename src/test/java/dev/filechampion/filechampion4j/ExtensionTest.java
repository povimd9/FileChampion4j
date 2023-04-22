package dev.filechampion.filechampion4j;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit test for Extension class.
 */

 public class ExtensionTest {

    private static final String FILE_CATEGORY = "Documents";
    private static final String FILE_EXTENSION = "pdf";

    @Test
    void testConstructorWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Extension(null, null, null);
        }, "Expected IllegalArgumentException to be thrown when all arguments are null");
    }

    @Test
    void testConstructorWithEmptyFileCategory() {
        JSONObject configJsonObject = new JSONObject();
        assertThrows(IllegalArgumentException.class, () -> {
            new Extension("", FILE_EXTENSION, configJsonObject);
        }, "Expected IllegalArgumentException to be thrown when file category is empty");
    }

    @Test
    void testConstructorWithInvalidFileCategory() {
        JSONObject configJsonObject = new JSONObject();
        assertThrows(IllegalArgumentException.class, () -> {
            new Extension("InvalidCategory", FILE_EXTENSION, configJsonObject);
        }, "Expected IllegalArgumentException to be thrown when file category is invalid");
    }

    @Test
    void testConstructorWithInvalidFileExtension() {
        JSONObject configJsonObject = new JSONObject();
        JSONObject categoryJson = new JSONObject();
        configJsonObject.put(FILE_CATEGORY, categoryJson);
        assertThrows(IllegalArgumentException.class, () -> {
            new Extension(FILE_CATEGORY, "InvalidExtension", configJsonObject);
        }, "Expected IllegalArgumentException to be thrown when file extension is invalid");
    }

    @Test
    void testGettersWithValidValues() {
        JSONObject configJsonObject = new JSONObject();
        JSONObject categoryJson = new JSONObject();
        JSONObject extensionJson = new JSONObject();
        extensionJson.put("mime_type", "application/pdf");
        extensionJson.put("magic_bytes", "25 50 44 46");
        extensionJson.put("header_signatures", "255,216,255");
        extensionJson.put("footer_signatures", "37,80,68,70");
        extensionJson.put("change_ownership", true);
        extensionJson.put("change_ownership_user", "myuser");
        extensionJson.put("change_ownership_mode", "rwxrwxrwx");
        extensionJson.put("name_encoding", false);
        extensionJson.put("max_size", 10485760);
        categoryJson.put(FILE_EXTENSION, extensionJson);
        configJsonObject.put(FILE_CATEGORY, categoryJson);
        Extension extension = new Extension(FILE_CATEGORY, FILE_EXTENSION, configJsonObject);
        assertEquals("application/pdf", extension.getMimeType(), "Expected mime type to be application/pdf");
        assertEquals("25 50 44 46", extension.getMagicBytes(), "Expected magic bytes to be 25 50 44 46");
        assertEquals("255,216,255", extension.getHeaderSignatures(), "Expected header signatures to be 255,216,255");
        assertEquals("37,80,68,70", extension.getFooterSignatures(), "Expected footer signatures to be 37,80,68,70");
        assertTrue(extension.isChangeOwnership(), "Expected change ownership to be true");
        assertEquals("myuser", extension.getChangeOwnershipUser(), "Expected change ownership user to be myuser");
        assertEquals("rwxrwxrwx", extension.getChangeOwnershipMode(), "Expected change ownership mode to be rwxrwxrwx");
        assertFalse(extension.isNameEncoding(), "Expected name encoding to be false");
        assertEquals(10485760, extension.getMaxSize(), "Expected max size to be 10485760");
    }
}