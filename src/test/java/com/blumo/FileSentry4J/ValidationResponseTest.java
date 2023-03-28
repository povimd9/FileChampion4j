package com.blumo.FileSentry4J;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

/*
 * Test class for ValidationResponse
 */
public class ValidationResponseTest {
    // Test isValid method
    @Test
    public void testIsValid() {
        ValidationResponse response = new ValidationResponse(true, null, null, null);
        assertTrue(response.isValid());
    }

    // Test failureReason method
    @Test
    public void testFailureReason() {
        String expected = "file too large";
        ValidationResponse response = new ValidationResponse(false, expected, null, null);
        assertEquals(expected, response.resultsInfo());
    }

    // Test getFileBytes method
    @Test
    public void testGetFileBytes() {
        File expected = new File("test.txt");
        ValidationResponse response = new ValidationResponse(false, null, expected, null);
        assertEquals(expected, response.getFileBytes());
    }

    // Test getFileChecksum method
    @Test
    public void testGetFileChecksum() {
        String expected = "abcdefg";
        ValidationResponse response = new ValidationResponse(false, null, null, expected);
        assertEquals(expected, response.getFileChecksum());
    }
}
