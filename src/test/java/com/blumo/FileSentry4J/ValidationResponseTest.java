package com.blumo.FileSentry4J;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

public class ValidationResponseTest {

    @Test
    public void testIsValid() {
        ValidationResponse response = new ValidationResponse(true, null, null, null);
        assertTrue(response.isValid());
    }

    @Test
    public void testFailureReason() {
        String expected = "file too large";
        ValidationResponse response = new ValidationResponse(false, expected, null, null);
        assertEquals(expected, response.resultsInfo());
    }

    @Test
    public void testGetFileBytes() {
        File expected = new File("test.txt");
        ValidationResponse response = new ValidationResponse(false, null, expected, null);
        assertEquals(expected, response.getFileBytes());
    }

    @Test
    public void testGetFileChecksum() {
        String expected = "abcdefg";
        ValidationResponse response = new ValidationResponse(false, null, null, expected);
        assertEquals(expected, response.getFileChecksum());
    }
}
