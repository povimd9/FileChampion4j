package dev.filechampion.filechampion4j;


import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.logging.LogManager;
import org.junit.jupiter.api.Test;


public class CliPluginHelperTest {
    // Test logger IOException
    @Test
    void testIOException() {
        // Create a new LogManager object with a non-existent configuration file path
        LogManager logManager = LogManager.getLogManager();
        assertThrows(NullPointerException.class, () -> logManager.readConfiguration(
            CliPluginHelper.class.getResourceAsStream("/non-existent-file.properties")));
    }
}
