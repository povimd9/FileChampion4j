package dev.filechampion.filechampion4j;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for FileValidator class.
 */

public class FileValidatorBench {

    public int batchSize = 100;
    public FileValidator validator;
    public File[] testFiles;
    public ArrayList<byte[]> fileByteArrayList;
    public String[] fileNamesArray;
    public int validFilesTimeThreadhold = 3000;
    public int invalidFilesTimeThreadhold = 1500;
    public int testRepeat = 3;
    public int warmupRepeats = 5;

    public static List<String> benchResults = new ArrayList<>();
    public static String tempDirectory;

    private static final String testUsername = System.getProperty("os.name").startsWith("Windows") ? "User1" : System.getProperty("user.name");

    // Config JSON object for testing
    private static final JSONObject CONFIG_JSON = new JSONObject("{\r\n"
    + "  \"Validations\": {\r\n"        
    + "  \"Documents\": {\r\n"
    + "    \"pdf\": {\r\n"
    + "      \"mime_type\": \"application/pdf\",\r\n"
    + "      \"magic_bytes\": \"25504446\",\r\n"
    + "      \"header_signatures\": \"25504446\",\r\n"
    + "      \"footer_signatures\": \"2525454f46\",\r\n"
    + "      \"change_ownership\": true,\r\n"
    + "      \"change_ownership_user\": \"" + testUsername + "\",\r\n"
    + "      \"change_ownership_mode\": \"rw\",\r\n"
    + "      \"name_encoding\": true,\r\n"
    + "      \"max_size\": \"4000\"\r\n"
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
    + "},\r\n"
    + "  \"Plugins\": {}\r\n"
    + "}");
    

    // Setup temp directory and filebytes array
    @BeforeEach
    void setUp() throws IOException {
        try {
            long uniqueVal = System.currentTimeMillis() / 1000L;
            tempDirectory = Files.createDirectory(Paths.get("temp_" + (uniqueVal/1000000 + UUID.randomUUID().toString()).substring(0, 8))).toAbsolutePath().toString();

            validator = new FileValidator(CONFIG_JSON);
            fileByteArrayList = new ArrayList<>();
            fileNamesArray = new String[batchSize + warmupRepeats + 1];

            for (int i=0; i < batchSize + warmupRepeats + 1; i++ ) {
                fileByteArrayList.add(generatePdfBytes(300000));
                fileNamesArray[i] = UUID.randomUUID().toString() + ".pdf";
            }

            for (int i = 0; i == warmupRepeats; i++) {
                ValidationResponse fileValidationWarmup = validator.validateFile("Documents", 
                                                    fileByteArrayList.get(i), fileNamesArray[i], tempDirectory);
                if (fileValidationWarmup.isValid()) {
                    assertTrue(fileValidationWarmup.resultsInfo().contains("File is valid and was saved to output directory"), "Expected 'File is valid and was saved to output directory', got: " + fileValidationWarmup.resultsInfo());
                } else {}
            }
        } catch (IOException e) { 
            throw new RuntimeException(e); 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Test valid inputs including valid pdf file
    @Test
    void testValidInputs() throws Exception {
        long startTime;
        long duration = 0;
        for (int j = 0; j < testRepeat; j++) {
            startTime = System.currentTimeMillis();
            for (int i = warmupRepeats + 1; i == batchSize + warmupRepeats; i++) {
                ValidationResponse fileValidationResults = validator.validateFile("Documents", 
                                                    fileByteArrayList.get(i), fileNamesArray[i], tempDirectory);
                if (fileValidationResults.isValid()) {
                    assertTrue(fileValidationResults.resultsInfo().contains("File is valid and was saved to output directory"), "Expected 'File is valid and was saved to output directory', got: " + fileValidationResults.resultsInfo());
                } else {}
            }
            duration += System.currentTimeMillis() - startTime;
        }
        long avgDuration = duration / testRepeat;

        FileValidatorBench.benchResults.add("testValidInputs: " + avgDuration);

        assertTrue(avgDuration <= validFilesTimeThreadhold, 
        "Expected valid file validation to take less than " + 
        validFilesTimeThreadhold + "ms, but took " + avgDuration + "ms");
    }

    // Report benchmark results
    @AfterAll
    public static void printeBenchResults() {
        for (String result : benchResults) {
            System.out.println(result);
        }
        //deleteDirectory(new File(tempDirectory));
    }

    // Helper methods

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
