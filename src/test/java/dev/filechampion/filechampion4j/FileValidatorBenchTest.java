package dev.filechampion.filechampion4j;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;


class FileValidatorBenchTest {
    private static final String benchOutputFile = "benchmarks/benchResults.txt";
    private static final String PACKAGE_NAME = "dev.filechampion.filechampion4j";
    private static final String CLASS_NAME_PATTERN = ".*Bench";

    @Test
    void benchFileChampion() {
        String beforeContentToAppend = "--------------- " + java.time.LocalDateTime.now() + " ---------------"+ System.lineSeparator();
        String afterContentToAppend = "--------------- " + java.time.LocalDateTime.now() + " ---------------"+ System.lineSeparator();
        try {
            Files.createDirectories(Paths.get(benchOutputFile.substring(0, benchOutputFile.lastIndexOf("/"))));
            Path newFilePath = Paths.get(benchOutputFile);
            Files.createFile(newFilePath);
        } catch (Exception e) {
            // File already exists
        }
        try {
            Files.write(
            Paths.get(benchOutputFile), 
            beforeContentToAppend.getBytes(), 
            StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage(PACKAGE_NAME))
                .filters(ClassNameFilter.includeClassNamePatterns(CLASS_NAME_PATTERN))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        try {
            Files.write(
            Paths.get(benchOutputFile), 
            afterContentToAppend.getBytes(), 
            StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

        listener.getSummary().printTo(new PrintWriter(System.out));
        assertEquals(0, listener.getSummary().getTestsFailedCount(), "Some tests failed");
        assertTrue(listener.getSummary().getTestsSucceededCount() > 0, "No tests succeeded");
        //checkPreviousBenchResults();
    }

    
}