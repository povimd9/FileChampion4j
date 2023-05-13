package dev.filechampion.filechampion4j;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

class CredentialsManagerTest {
    private static Path credsPath;
    private static List<String> credsNamesList;
    private static CredentialsManager credsManager;

    @BeforeAll
    static void setUp() {
        credsPath = Paths.get("src/test/resources/creds/");
        credsNamesList = Arrays.asList("creds1.txt", "creds2.txt", "creds3.txt");
        credsManager = new CredentialsManager(credsPath, credsNamesList);
    }

    @Test
    void testGetCredentials() throws IOException {
        char[] creds1 = credsManager.getCredentials("creds1.txt");
        assertNotNull(creds1, "Credentials were null instead of secret value.");
        assertTrue(creds1.length > 0, "Credentials were empty instead of secret value.");
    }

    @Test
    void testGetCredentialsWithInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            credsManager.getCredentials("invalid.txt");
        }, "CredentialsManager did not throw IllegalArgumentException when requesting invalid credentials file.");
    }

    @Test
    void testGetCredentialsWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            credsManager.getCredentials("");
        }, "CredentialsManager did not throw IllegalArgumentException when requesting empty secret name.");
    }

    //remove or rewrite this test to not rely on time passing
    @Test
    void testGetCredentialsAfterExpiration() throws IOException, InterruptedException {
        // Wait for 5 minutes to ensure that creds are expired
        char[] creds1 = credsManager.getCredentials("creds1.txt");
        creds1 = credsManager.getCredentials("creds1.txt");
        Thread.sleep(305000);
        creds1 = credsManager.getCredentials("creds1.txt");
        assertNotNull(creds1);
        assertTrue(creds1.length > 0);
    }

    @Test
    void testConstructorWithInvalidPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CredentialsManager(Paths.get("invalid/path"), credsNamesList);
        });
    }

    @Test
    void testConstructorWithEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CredentialsManager(credsPath, Arrays.asList());
        });
    }

    @Test
    void testConstructorWithInvalidFile() {
        List<String> invalidList = Arrays.asList("creds1.txt", "invalid.txt");
        assertThrows(IllegalArgumentException.class, () -> {
            new CredentialsManager(credsPath, invalidList);
        });
    }
}
