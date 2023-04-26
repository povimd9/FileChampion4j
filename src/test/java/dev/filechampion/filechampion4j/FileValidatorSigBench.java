package dev.filechampion.filechampion4j;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.StreamHandler;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import java.util.concurrent.TimeUnit;


/**
 * Benchmark test for 'validateFile' method in 'FileValidator' class.
 */
@Warmup(iterations = 10, time = 10, timeUnit =  TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 40, timeUnit =  TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class FileValidatorSigBench {
    private FileValidator validator;
    private byte[] fileInBytesSmall;
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
    public void fileValidatorSmallBench() throws RunnerException {
        Options opt = new OptionsBuilder()
        .include(FileValidatorSigBench.class.getSimpleName())
        .forks(3)
        .mode(Mode.All)
        .output("benchmarks/results.txt")
        .build();
        new Runner(opt).run();
        Collection<RunResult> runResults = new Runner(opt).run();
        assertFalse(runResults.isEmpty(), "No benchmark results");
        
        
        int i = 0;
        String contentToAppend = "";
        for(RunResult runResult : runResults) {
            switch(i) {
                case 0:
                contentToAppend = "Sig Throughput Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ci" + System.lineSeparator();
                    break;
                case 1:
                contentToAppend = "Sig Average Time Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                case 2:
                contentToAppend = "Sig Sample Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                case 3:
                contentToAppend = "Sig Single Shot Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
                    break;
                default:
                contentToAppend = "Sig Unknown Bench, " + String.format("%.6f",runResult.getPrimaryResult().getScore()) + " ms" + System.lineSeparator();
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

    private ByteArrayOutputStream outputStream;
    private StreamHandler handler;
    @Setup(org.openjdk.jmh.annotations.Level.Iteration)
    public void setUp(BenchmarkParams params) throws IOException {
        /*outputStream = new ByteArrayOutputStream();
        Logger logger = Logger.getLogger(FileValidator.class.getName());
        logger.setLevel(java.util.logging.Level.SEVERE);
        handler = new StreamHandler(outputStream, new SimpleFormatter());
        logger.addHandler(handler);*/

        try {
            validator = new FileValidator(testConfig);
            fileInBytesSmall = generatePdfBytes(250000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileName = "test&test.pdf";
        
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
        ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytesSmall, fileName);
    }

    // Generate a pdf file with a given size in bytes
    private byte[] generatePdfBytes(int sizeInBytes) throws Exception {
        if (sizeInBytes <= 0) {
            throw new IllegalArgumentException("Size in Bytes must be a positive value.");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setFullCompression();
        writer.setCompressionLevel(0);
        document.open();
        String content = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        int contentLength = content.getBytes().length;
        while (baos.size() < sizeInBytes) {
            int iterations = (sizeInBytes - baos.size()) / contentLength;
            for (int i = 0; i < iterations; i++) {
                document.add(new Paragraph(content));
            }
            writer.flush();
        }
        document.close();
        writer.close();
        return baos.toByteArray();
    }
}