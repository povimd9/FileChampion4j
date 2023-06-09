package dev.filechampion.filechampion4j;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark test for 'validateFile' method in 'FileValidator' class.
 */
@Warmup(iterations = 5, time = 500, timeUnit =  TimeUnit.MILLISECONDS)
@Measurement(iterations = 15, time = 1000, timeUnit =  TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class FileValidatorLargeMagicNoHashBench {
    private FileValidator validator;
    private byte[] fileInBytesLarge;
    private String fileName;
    public static int testConfigCounter = 0;
    private static final String benchOutputFile = "benchmarks/benchResults.txt";
    
    private JSONObject testConfig = 
    new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"add_checksum\": false\r\n"
    + "      },\r\n"
    + "    \"doc\": {\r\n"
    + "      \"mime_type\": \"application/msword\",\r\n"
    + "      \"magic_bytes\": \"D0CF11E0A1B11AE1\",\r\n"
    + "      \"header_signatures\": \"D0CF11E0A1B11AE1\",\r\n"
    + "      \"footer_signatures\": \"0000000000000000\",\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"User1\",\r\n"
    + "      \"change_ownership_mode\": \"r\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\",\r\n"
    + "      \"add_checksum\": false\r\n"
    + "    }\r\n"
    + "  }\r\n"
    + "}\r\n"
    + "}");

    
    @Test
    public void fileValidatorLargeMagicNoHashBench() throws RunnerException {
        Options opt = new OptionsBuilder()
        .include(FileValidatorLargeMagicNoHashBench.class.getSimpleName())
        .forks(1)
        .mode(Mode.All)
        .output("benchmarks/results.txt")
        .jvmArgs("-XX:+UseG1GC")
        .build();
        new Runner(opt).run();
        Collection<RunResult> runResults = new Runner(opt).run();
        assertFalse(runResults.isEmpty(), "No benchmark results");
        
        
        int i = 0;
        String contentToAppend = "";
        for(RunResult runResult : runResults) {
            switch(i) {
                case 0:
                contentToAppend = "Magic Large File No Checksum Throughput Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ci" + System.lineSeparator();
                    break;
                case 1:
                contentToAppend = "Magic Large File No Checksum Average Time Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                case 2:
                contentToAppend = "Magic Large File No Checksum Sample Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                case 3:
                contentToAppend = "Magic Large File No Checksum Single Shot Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                default:
                contentToAppend = "Magic Large File No Checksum Unknown Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore() + " ms" + System.lineSeparator());
                    break;
            }
            
            try {
                Files.write(
                Paths.get(benchOutputFile), 
                contentToAppend.getBytes(), 
                StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.print(contentToAppend);
            ++i;
        }
    }

    @Setup(org.openjdk.jmh.annotations.Level.Invocation)
    public void benchSetUp() throws IOException {
        try {
            fileInBytesLarge = Files.readAllBytes(Paths.get("src","test", "resources", "testLarge.pdf").toAbsolutePath());
            validator = new FileValidator(testConfig);
        } catch (Exception e) {
            System.out.println("Error in benchSetUp: " + e.getMessage());
            e.printStackTrace();
        }
        fileName = "testLarge.pdf";
    }

    // Benchmark test for 'validateFile' method with only magic bytes validation
    @Benchmark
    public void benchValidMagic() throws Exception {
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytesLarge, fileName);
    }
}