package dev.filechampion.filechampion4j;


import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Unit test for FileValidator class.
 */
public class FileValidatorBenchCompare {
    private static final double MAX_DIFFERENCE_FROM_PREV = 5;
    private static final String benchOutputFile = "benchmarks/benchResults.txt";
    
    /**
     * Check if the last result in the benchResults.txt file is not worse than the previous results by more than MAX_DIFFERENCE_FROM_PREV
     */
    @Test
    void checkPreviousBenchResults() {
        String[] resultsBlocs = new String[2];
        try (RandomAccessFile raf = new RandomAccessFile(benchOutputFile, "r")) {
            long length = raf.length();
            assertTrue(length > 1076, "File is too small to contain previous results");
            long pos = length - 545;
            int blocksFound = 0;
            while (pos >= 0 && blocksFound < 2) {
                raf.seek(pos);
                byte[] bytes = new byte[545];
                raf.readFully(bytes);
                resultsBlocs[blocksFound] = new String(bytes, StandardCharsets.UTF_8);
                blocksFound++;
                pos -= 545;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, Double> currentResultslines = loadMap(resultsBlocs[0].split("\\r?\\n"));
        HashMap<String, Double> previousResultslines = loadMap(resultsBlocs[1].split("\\r?\\n"));
        List<String> failedResultsList = new ArrayList<>();

        for (String key : currentResultslines.keySet()) {
            String benchResults = compareResultLines(key , currentResultslines.get(key), previousResultslines.get(key), key.contains("Throughput ")? true : false);
            if (benchResults.contains("' worse, vs '")) {
                failedResultsList.add(benchResults);
            }
            System.out.println(benchResults);
        }

        if (failedResultsList.size() > 0) {
            System.out.println("The following tests have results that are worse than than defined max degradation:");
            for (String result : failedResultsList) {
                System.out.println(result);
            }
            assertTrue(false, "Some tests have results that are worse than than defined max degradation");
        }
    }

    // Load the results from the benchResults.txt file into a HashMap
    private static HashMap<String, Double> loadMap(String[] resultslines) {
        HashMap<String, Double> previousResults = new HashMap<>();
        for (String line : resultslines) {
            if (line.startsWith("--")) {
                continue;
            }
            String[] lineParts = line.split(", ");
            previousResults.put(lineParts[0], Double.parseDouble(lineParts[1].split(" ")[0]));
        }
        return previousResults;
    }

    // Compare the current result with the previous result and return a string with the result
    private static String compareResultLines (String testName, double currentResult, double previousResult, boolean higherIsBetter) {
        double maxPercentDiff = previousResult / 100 * MAX_DIFFERENCE_FROM_PREV;
        double diff = currentResult - previousResult;
        if (higherIsBetter) {
            if (currentResult < previousResult) {
                if (diff*-1 > maxPercentDiff) {
                    return testName + " Previous result: '" + previousResult 
                    + "', Current results: '" + currentResult + "', is '" + String.format("%.6f",diff) + 
                    "' worse, vs '" + String.format("%.6f",maxPercentDiff) + "' allowed";
                }
                return testName + " result degrated by " + String.format("%.6f",diff * -1);
            } else if (currentResult > previousResult) {
                return testName + " result improved by " + String.format("%.6f",diff);
            } else {
                return testName + " result unchanged";
            }
        } else {
            if (currentResult > previousResult) {
                if (diff > maxPercentDiff) {
                    return testName + " Previous result: '" + previousResult 
                    + "', Current results: '" + currentResult + "', is '" + String.format("%.6f",diff) + 
                    "' worse, vs '" + String.format("%.6f",maxPercentDiff) + "' allowed";
                }
                return testName + " result degrated by " + String.format("%.6f",diff);
            } else if (currentResult < previousResult) {
                return testName + " result improved by " + String.format("%.6f",diff * -1);
            } else {
                return testName + " result unchanged";
            }
        }
    }

}
