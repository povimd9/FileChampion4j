package dev.filechampion.filechampion4j;

import org.junit.jupiter.api.*;

import dev.filechampion.filechampion4j.CredentialsManager.CredentialsFetchError;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for CredentialsManager class.
 */
class CredentialsManagerTest {
    private static Path credsPath;
    private static List<String> credsNamesList;
    private static CredentialsManager credsManager;

    /**
     * Initializes the CredentialsManager and its dependencies.
     */
    @BeforeAll
    static void setUp() {
        credsPath = Paths.get("src/test/resources/creds/");
        credsNamesList = Arrays.asList("creds1.txt", "creds2.txt", "creds3.txt", "emptycreds.txt");
        credsManager = new CredentialsManager(credsPath, credsNamesList);
    }

    /**
     * Tests the getCredentials to validate that it returns a secret value.
     */
    @Test
    void testGetCredentials() throws Exception {
        char[] creds1 = credsManager.getCredentials("creds1.txt");
        assertNotNull(creds1, "Credentials were null instead of secret value.");
        assertTrue(creds1.length > 0, "Credentials were empty instead of secret value.");
    }

    /**
     * Tests the getCrednetials method to validate that it throws an IllegalArgumentException when given an invalid name.
     */
    @Test
    void testGetCredentialsWithInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            credsManager.getCredentials("invalid.txt");
        }, "CredentialsManager did not throw IllegalArgumentException when requesting invalid credentials file.");
    }

    /**
     * Tests the getCrednetials method to validate that it throws an CredentialsFetchError for an invalid secret in file.
     */
    @Test
    void testGetCredentialsWithEmptyFileContent() {
        assertThrows(CredentialsFetchError.class, () -> {
            credsManager.getCredentials("emptycreds.txt");
        }, "CredentialsManager did not throw CredentialsFetchError when requesting credentials from empty file. Got:");
    }

    /**
     * Tests the getCredentials method to validate that it throws an IllegalArgumentException when given an empty name.
     */
    @Test
    void testGetCredentialsWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            credsManager.getCredentials("");
        }, "CredentialsManager did not throw IllegalArgumentException when requesting empty secret name.");
    }

    /**
     * Tests the CredentialsManager to validate that it caches credentials for the specified amount of time.
     */
    @Test
    void testCredentialsCache() throws Exception {
        credsManager.setExpirationTime(5000);
        String creds1Original = new String(credsManager.getCredentials("creds1.txt"));
        assertNotNull(creds1Original, "creds1Original was null instead of secret value.");
        char[] originalSecret = new char[(credsManager.getCredentials("creds1.txt").length) / 2];
        for (int i = 0, j = 0; i < credsManager.getCredentials("creds1.txt").length; i += 2, j++) {
            originalSecret[j] = creds1Original.toCharArray()[i];
        }
        assertEquals("NOT_REAL_SECRET_1", new String(originalSecret), "originalSecret was" + new String(originalSecret) + " instead of 'NOT_REAL_SECRET_1'.");
        String creds1FromCache = new String(credsManager.getCredentials("creds1.txt"));
        assertEquals(creds1Original, creds1FromCache, "creds1FromCache was not equal to creds1Original.");
        Thread.sleep(10000);
        String creds1AfterExpiration = new String(credsManager.getCredentials("creds1.txt"));
        assertNotEquals(creds1Original, creds1AfterExpiration, "creds1AfterExpiration was equal to creds1Original.");
    }

    /**
     * Tests that setExpirationTime throws an error if given 0 or a negative number.
     */
    @Test
    void testSetExpirationTimeWithInvalidTime() {
        assertThrows(IllegalArgumentException.class, () -> {
            credsManager.setExpirationTime(0);
        }, "CredentialsManager did not throw IllegalArgumentException when given 0.");
        assertThrows(IllegalArgumentException.class, () -> {
            credsManager.setExpirationTime(-1);
        }, "CredentialsManager did not throw IllegalArgumentException when given -1.");
    }

    /**
     * Tests the CredentialsManager to validate that it throws an IllegalArgumentException when given an invalid path.
     */
    @Test
    void testConstructorWithInvalidPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CredentialsManager(Paths.get("invalid/path"), credsNamesList);
        }, "CredentialsManager did not throw IllegalArgumentException when given invalid path.");
    }

    /**
     * Tests the CredentialsManager to validate that it throws an IllegalArgumentException when given an empty list.
     */
    @Test
    void testConstructorWithEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CredentialsManager(credsPath, Arrays.asList());
        }, "CredentialsManager did not throw IllegalArgumentException when given empty list.");
    }

    /**
     * Tests the CredentialsManager to validate that it throws an IllegalArgumentException when given an invalid file.
     */
    @Test
    void testConstructorWithInvalidFile() {
        List<String> invalidList = Arrays.asList("creds1.txt", "invalid.txt");
        assertThrows(IllegalArgumentException.class, () -> {
            new CredentialsManager(credsPath, invalidList);
        }, "CredentialsManager did not throw IllegalArgumentException when given invalid file.");
    }
}
