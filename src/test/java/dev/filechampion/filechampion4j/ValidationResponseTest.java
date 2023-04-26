package dev.filechampion.filechampion4j;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ValidationResponse class.
 */

public class ValidationResponseTest {

    // Test the constructor and getters
    @Test
    void testValidationResponse() {
        boolean isValid = true;
        String resultsInfo = "Validation successful";
        String cleanFileName = "test-file.txt";
        byte[] fileBytes = new byte[] {0x01, 0x02, 0x03};
        String fileChecksum = "abc123";
        String[] validFilePath = new String[] {"path/to/valid/file"};

        ValidationResponse response = new ValidationResponse(isValid, resultsInfo, cleanFileName, fileBytes, fileChecksum, validFilePath);

        // Check that the response fields match the input parameters
        Assertions.assertTrue(response.isValid(), "Expected isValid to be true");
        Assertions.assertEquals(resultsInfo, response.resultsInfo(), "Expected resultsInfo to match input parameter");
        Assertions.assertEquals(cleanFileName, response.getCleanFileName(), "Expected cleanFileName to match input parameter");
        Assertions.assertArrayEquals(fileBytes, response.getFileBytes(), "Expected fileBytes to match input parameter");
        Assertions.assertEquals(fileChecksum, response.getFileChecksum(), "Expected fileChecksum to match input parameter");
        Assertions.assertArrayEquals(validFilePath, response.getValidFilePath(), "Expected validFilePath to match input parameter");
    }
}

