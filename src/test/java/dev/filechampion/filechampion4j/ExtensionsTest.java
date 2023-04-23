package dev.filechampion.filechampion4j;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit test for Extensions class.
 */

 public class ExtensionsTest {

    @Test
    public void testConstructor() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Documents", new JSONObject()
            .put("pdf", new JSONObject()
                .put("mime_type", "application/pdf")
                .put("magic_bytes", "25504446")));

        Extensions extensions = new Extensions(jsonObject);
        assertEquals("application/pdf", extensions.getValidationValue("Documents", "pdf", "mime_type"));
        assertEquals("25504446", extensions.getValidationValue("Documents", "pdf", "magic_bytes"));
    }

    @Test
    public void testInvalidConstructor() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Documents", new JSONObject()
            .put("pdf", new JSONObject()
                .put("mime_type", "application/pdf")
                .put("magic_bytes", "25504446")));

        assertThrows(IllegalArgumentException.class, () -> new Extensions(null), "expected IllegalArgumentException for null jsonObject");
        assertThrows(IllegalArgumentException.class, () -> new Extensions(new JSONObject()), "expected IllegalArgumentException for empty jsonObject");
        assertThrows(IllegalArgumentException.class, () -> new Extensions(new JSONObject()
            .put("Documents", new JSONObject()
                .put("pdf", new JSONObject()
                    .put("illegal_key", "application/pdf")))), "expected IllegalArgumentException for undefined key");
    }

    @Test
    public void testGetValidationValue() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Documents", new JSONObject()
            .put("pdf", new JSONObject()
                .put("mime_type", "application/pdf")
                .put("magic_bytes", "25504446")));

        Extensions extensions = new Extensions(jsonObject);
        assertEquals("application/pdf", extensions.getValidationValue("Documents", "pdf", "mime_type"));
        assertEquals("25504446", extensions.getValidationValue("Documents", "pdf", "magic_bytes"));

        assertThrows(IllegalArgumentException.class, () -> 
            extensions.getValidationValue(null, "pdf", "mime_type"), "expected IllegalArgumentException for null category");
        assertThrows(IllegalArgumentException.class, () ->
            extensions.getValidationValue("Documents", null, "mime_type"), "expected IllegalArgumentException for null extension");
        assertThrows(IllegalArgumentException.class, () -> 
            extensions.getValidationValue("Documents", "pdf", null), "expected IllegalArgumentException for null key"); 
    }
}