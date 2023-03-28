package com.blumo.FileSentry4J;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test class for ConfigParser
 */
class ConfigParserTest {
    // Config map
    private Map<String, Object> config;

    // Setup config map
    @BeforeEach
    void setup() throws IOException {
        config = ConfigParser.parseConfig("src/test/config/test_config.json");
        assertNotNull(config);
        assertTrue(config.containsKey("Documents"));
    }

    // Test parsing of config file
    @Test
    void testParsingAllowedExtensions() {
        Map<String, Object> documents = (Map<String, Object>) config.get("Documents");
        assertTrue(documents.containsKey("allowed_extensions"));
        Object allowedExtensions = documents.get("allowed_extensions");
        assertTrue(allowedExtensions instanceof List);
        List<String> extensionsList = (List<String>) allowedExtensions;
        assertEquals(2, extensionsList.size());
        assertTrue(extensionsList.contains("pdf"));
        assertTrue(extensionsList.contains("doc"));
    }

    // Test parsing of testParsingPDFConfig from config file
    @Test
    void testParsingPDFConfig() {
        Map<String, Object> documents = (Map<String, Object>) config.get("Documents");
        assertTrue(documents.containsKey("pdf"));
        Map<String, Object> pdfConfig = (Map<String, Object>) documents.get("pdf");
        assertEquals("application/pdf", pdfConfig.get("mime_type"));
        assertEquals("25504446", pdfConfig.get("magic_bytes"));
        assertEquals("25504446", pdfConfig.get("header_signatures"));
        assertEquals("0A2525454F46", pdfConfig.get("footer_signatures"));
        assertTrue(pdfConfig.containsKey("antivirus_scan"));
        assertTrue((Boolean) pdfConfig.get("name_encoding"));
        assertTrue((Boolean) pdfConfig.get("size_limit_validation"));
        Map<String, Object> maxSize = (Map<String, Object>) pdfConfig.get("max_size");
        assertEquals("10MB", maxSize.get("pdf"));
        assertEquals("10MB", maxSize.get("doc"));
    }

    // Test parsing of testParsingDocConfig from config file
    @Test
    void testParsingDocConfig() {
        Map<String, Object> documents = (Map<String, Object>) config.get("Documents");
        assertTrue(documents.containsKey("doc"));
        Map<String, Object> docConfig = (Map<String, Object>) documents.get("doc");
        assertEquals("application/msword", docConfig.get("mime_type"));
        assertEquals("D0CF11E0A1B11AE1", docConfig.get("magic_bytes"));
        assertEquals("D0CF11E0A1B11AE1", docConfig.get("header_signatures"));
        assertEquals("0000000000000000", docConfig.get("footer_signatures"));
        assertTrue(docConfig.containsKey("antivirus_scan"));
        assertTrue((Boolean) docConfig.get("name_encoding"));
        assertTrue((Boolean) docConfig.get("size_limit_validation"));
        Map<String, Object> maxSize = (Map<String, Object>) docConfig.get("max_size");
        assertEquals("10MB", maxSize.get("pdf"));
        assertEquals("10MB", maxSize.get("doc"));
    }
}
