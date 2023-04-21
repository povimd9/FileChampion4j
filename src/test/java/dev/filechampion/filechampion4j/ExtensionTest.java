package dev.filechampion.filechampion4j;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for Extension class.
 * TODO: Add AV scan config loading tests
 */

 public class ExtensionTest {

    private static final String FILE_CATEGORY = "Documents";
    private static final String FILE_EXTENSION = "pdf";

    @Test
    void testConstructorWithNullArguments() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Extension(null, null, null);
        });
    }

    @Test
    void testConstructorWithEmptyFileCategory() {
        JSONObject configJsonObject = new JSONObject();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Extension("", FILE_EXTENSION, configJsonObject);
        });
    }

    @Test
    void testConstructorWithInvalidFileCategory() {
        JSONObject configJsonObject = new JSONObject();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Extension("InvalidCategory", FILE_EXTENSION, configJsonObject);
        });
    }

    @Test
    void testConstructorWithInvalidFileExtension() {
        JSONObject configJsonObject = new JSONObject();
        JSONObject categoryJson = new JSONObject();
        configJsonObject.put(FILE_CATEGORY, categoryJson);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Extension(FILE_CATEGORY, "InvalidExtension", configJsonObject);
        });
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
        extensionJson.put("antivirus_scan", new JSONArray("[\"ClamAV\", \"Sophos\"]"));
        extensionJson.put("change_ownership", true);
        extensionJson.put("change_ownership_user", "myuser");
        extensionJson.put("change_ownership_mode", "rwxrwxrwx");
        extensionJson.put("name_encoding", false);
        extensionJson.put("max_size", 10485760);
        categoryJson.put(FILE_EXTENSION, extensionJson);
        configJsonObject.put(FILE_CATEGORY, categoryJson);
        Extension extension = new Extension(FILE_CATEGORY, FILE_EXTENSION, configJsonObject);
        Assertions.assertEquals("application/pdf", extension.getMimeType());
        Assertions.assertEquals("25 50 44 46", extension.getMagicBytes());
        Assertions.assertEquals("255,216,255", extension.getHeaderSignatures());
        Assertions.assertEquals("37,80,68,70", extension.getFooterSignatures());
        Assertions.assertEquals(2, extension.getAntivirusScanJson().length());
        Assertions.assertTrue(extension.isChangeOwnership());
        Assertions.assertEquals("myuser", extension.getChangeOwnershipUser());
        Assertions.assertEquals("rwxrwxrwx", extension.getChangeOwnershipMode());
        Assertions.assertFalse(extension.isNameEncoding());
        Assertions.assertEquals(10485760, extension.getMaxSize());
    }
}