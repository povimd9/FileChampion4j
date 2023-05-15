package dev.filechampion.filechampion4j;

import org.junit.jupiter.api.*;

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
        credsNamesList = Arrays.asList("creds1.txt", "creds2.txt", "creds3.txt");
        credsManager = new CredentialsManager(credsPath, credsNamesList);
    }

    /**
     * Tests the getCredentials to validate that it returns a secret value.
     * @throws Exception if the credentials file cannot be read.
     */
    @Test
    void testGetCredentials() throws Exception {
        char[] creds1 = credsManager.getCredentials("creds1.txt");
        assertNotNull(creds1, "Credentials were null instead of secret value.");
        assertTrue(creds1.length > 0, "Credentials were empty instead of secret value.");
    }

    /**
     * Tests the getCredentials to validate that it returns a secret value.
     * @throws Exception if the credentials file cannot be read.
     */
    @Test
    void testGetMultipleCredentials() throws Exception {
        char[] creds1 = credsManager.getCredentials("creds1.txt");
        char[] creds2 = credsManager.getCredentials("creds2.txt");
        char[] creds3 = credsManager.getCredentials("creds3.txt");
        
        assertEquals("NOT_REAL_SECRET_1", getOriginalSecret(creds1), "creds1 were not equal to 'NOT_REAL_SECRET_1'.");
        assertEquals("NOT_REAL_SECRET_2", getOriginalSecret(creds2), "creds2 were not equal to 'NOT_REAL_SECRET_2'.");
        assertEquals("NOT_REAL_SECRET_3", getOriginalSecret(creds3), "creds3 were not equal to 'NOT_REAL_SECRET_3'.");
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
     * @throws Exception if the credentials file cannot be read.
     * @throws InterruptedException if the thread is interrupted.
     */
    @Test
    void testCredentialsCache() throws Exception, InterruptedException {
        credsManager.setExpirationTime(3000);
        String creds1Original = new String(credsManager.getCredentials("creds1.txt"));
        assertNotNull(creds1Original, "creds1Original was null instead of secret value.");
        char[] originalSecret = getOriginalSecret(credsManager.getCredentials("creds1.txt")).toCharArray();

        assertEquals("NOT_REAL_SECRET_1", new String(originalSecret), "originalSecret was" + new String(originalSecret) + " instead of 'NOT_REAL_SECRET_1'.");
        
        String creds1FromCache = new String(credsManager.getCredentials("creds1.txt"));
        assertEquals(creds1Original, creds1FromCache, "creds1FromCache was not equal to creds1Original.");

        String creds2FromFile = new String(credsManager.getCredentials("creds2.txt"));
        String creds3FromFile = new String(credsManager.getCredentials("creds3.txt"));
        
        Thread.sleep(10000);
        String creds1AfterExpiration = new String(credsManager.getCredentials("creds1.txt"));
        String creds2AfterExpiration = new String(credsManager.getCredentials("creds2.txt"));
        String creds3AfterExpiration = new String(credsManager.getCredentials("creds3.txt"));
        
        assertNotEquals(creds1Original, creds1AfterExpiration, "creds1AfterExpiration was equal to creds1Original.");
        assertEquals("NOT_REAL_SECRET_1", getOriginalSecret(creds1AfterExpiration.toCharArray()), "originalSecret was" + new String(creds1AfterExpiration.toCharArray()) + " instead of 'NOT_REAL_SECRET_1'.");
        assertNotEquals(creds2FromFile, creds2AfterExpiration, "creds2AfterExpiration was equal to creds2Original.");
        assertEquals("NOT_REAL_SECRET_2", getOriginalSecret(creds2AfterExpiration.toCharArray()), "originalSecret was" + new String(creds2AfterExpiration.toCharArray()) + " instead of 'NOT_REAL_SECRET_2'.");
        assertNotEquals(creds3FromFile, creds3AfterExpiration, "creds3AfterExpiration was equal to credsOriginal.");
        assertEquals("NOT_REAL_SECRET_3", getOriginalSecret(creds3AfterExpiration.toCharArray()), "originalSecret was" + new String(creds3AfterExpiration.toCharArray()) + " instead of 'NOT_REAL_SECRET_3'.");
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

    /**
     * Get original secret from salted secret.
     */
    private String getOriginalSecret(char[] saltedSecret) {
        char[] originalSecret = new char[saltedSecret.length / 2];
        for (int i = 0, j = 0; i < saltedSecret.length; i += 2, j++) {
            originalSecret[j] = saltedSecret[i];
        }
        return new String(originalSecret);
    }
}
