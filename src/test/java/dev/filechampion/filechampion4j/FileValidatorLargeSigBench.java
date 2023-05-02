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
public class FileValidatorLargeSigBench {
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
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\"\r\n"
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
    public void fileValidatorNoHashBench() throws RunnerException {
        Options opt = new OptionsBuilder()
        .include(FileValidatorLargeSigBench.class.getSimpleName())
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
                contentToAppend = "Sig Large File Throughput Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ci" + System.lineSeparator();
                    break;
                case 1:
                contentToAppend = "Sig Large File Average Time Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                case 2:
                contentToAppend = "Sig Large File Sample Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                case 3:
                contentToAppend = "Sig Large File Single Shot Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                default:
                contentToAppend = "Sig Large File Unknown Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
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

    @Setup(org.openjdk.jmh.annotations.Level.Iteration)
    public void benchSetUp() throws IOException {

        try {
            validator = new FileValidator(testConfig);
            fileInBytesLarge = Files.readAllBytes(Paths.get("src","test", "resources", "testLarge.pdf").toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileName = "testLarge.pdf";
        
    }

    /*@org.openjdk.jmh.annotations.TearDown(org.openjdk.jmh.annotations.Level.Iteration)
    public void flushLog() {
        handler.flush(); 
        //String loggerOutput = outputStream.toString();
        try {
            OutputStream fileOutput = new FileOutputStream("target/jmh/sigBenchTestLogs.txt", false);
            outputStream.writeTo(fileOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
    
    // Benchmark test for 'validateFile' method with only signatures validation
    @Benchmark
    public void benchValidSig() throws Exception {
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytesLarge, fileName);
    }
}