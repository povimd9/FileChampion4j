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
@Warmup(iterations = 10, time = 100, timeUnit =  TimeUnit.MILLISECONDS)
@Measurement(iterations = 50, time = 100, timeUnit =  TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class FileValidatorMimeGivenBench {
    private FileValidator validator;
    private byte[] fileInBytesSmall;
    private String fileName;
    private String benchOutputFile = "benchmarks/benchResults.txt";
    
    private JSONObject testMimeConfig = 
    new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\"\r\n"
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
    + "      \"max_size\": \"4000\"\r\n"
    + "    }\r\n"
    + "  }\r\n"
    + "}\r\n"
    + "}");

    @Test
    public void fileValidatorSmallBench() throws RunnerException {
        Options opt = new OptionsBuilder()
        .include(FileValidatorMimeGivenBench.class.getSimpleName())
        .forks(1)
        .mode(Mode.All)
        .output("benchmarks/results.txt")
        .jvmArgs("-XX:+UseSerialGC")
        .build();
        new Runner(opt).run();
        Collection<RunResult> runResults = new Runner(opt).run();
        assertFalse(runResults.isEmpty(), "No benchmark results");
        
        
        int i = 0;
        String contentToAppend = "";
        for(RunResult runResult : runResults) {
            switch(i) {
                case 0:
                contentToAppend = "Mime Given Throughput Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ci" + System.lineSeparator();
                    break;
                case 1:
                contentToAppend = "Mime Given Average Time Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                case 2:
                contentToAppend = "Mime Given Sample Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                case 3:
                contentToAppend = "Mime Given Single Shot Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                default:
                contentToAppend = "Mime Given Unknown Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
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
            validator = new FileValidator(testMimeConfig);
            fileInBytesSmall = Files.readAllBytes(Paths.get("src","test", "resources", "testSmall.pdf").toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileName = "test&test.pdf";

    }

    // Benchmark test for 'validateFile' file path method with only mime validation
    @Benchmark
    public void benchValidMime() throws Exception {
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytesSmall, fileName, "application/pdf");
    }
}