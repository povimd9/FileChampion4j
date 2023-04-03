package com.blumo.FileChampion4j;


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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for FileValidator class.
 */

public class FileValidatorBenchTest {

    public int batchSize = 100;
    public String tempDirectory;
    public FileValidator validator;
    public File[] testFiles;
    public ArrayList<byte[]> fileByteArrayList;
    public String[] fileNamesArray;
    public int validFilesTimeThreadhold = 2000;
    public int invalidFilesTimeThreadhold = 1000;
    public int testRepeat = 3;

    public static List<String> benchResults = new ArrayList<>();

    // Config JSON object for testing
    public static final JSONObject CONFIG_JSON = new JSONObject("{\r\n"
            + "  \"Documents\": {\r\n"
            + "    \"pdf\": {\r\n"
            + "      \"mime_type\": \"application/pdf\",\r\n"
            + "      \"magic_bytes\": \"25504446\",\r\n"
            + "      \"header_signatures\": \"25504446\",\r\n"
            + "      \"footer_signatures\": \"2525454f46\",\r\n"
            + "      \"antivirus_scan\": {\r\n"
            + "        \"clamav_scan.java\": [\r\n"
            + "          \"RETURN_TYPE\",\r\n"
            + "          \"param1\",\r\n"
            + "          \"param2\"\r\n"
            + "        ]},\r\n"
            + "      \"change_ownership\": true,\r\n"
            + "      \"change_ownership_user\": \"User1\",\r\n"
            + "      \"change_ownership_mode\": \"r\",\r\n"
            + "      \"name_encoding\": true,\r\n"
            + "      \"max_size\": \"4000\"\r\n"
            + "      },\r\n"
            + "    \"doc\": {\r\n"
            + "      \"mime_type\": \"application/msword\",\r\n"
            + "      \"magic_bytes\": \"D0CF11E0A1B11AE1\",\r\n"
            + "      \"header_signatures\": \"D0CF11E0A1B11AE1\",\r\n"
            + "      \"footer_signatures\": \"0000000000000000\",\r\n"
            + "      \"antivirus_scan\": {\r\n"
            + "        \"clamav_scan.java\": [\r\n"
            + "          \"RETURN_TYPE\",\r\n"
            + "          \"param1\",\r\n"
            + "          \"param2\"\r\n"
            + "        ]},\r\n"
            + "      \"change_ownership\": true,\r\n"
            + "      \"change_ownership_user\": \"User1\",\r\n"
            + "      \"change_ownership_mode\": \"r\",\r\n"
            + "      \"name_encoding\": true,\r\n"
            + "      \"max_size\": \"4000\"\r\n"
            + "    }\r\n"
            + "  }\r\n"
            + "}");
    

    // Setup temp directory and filebytes array
    @BeforeEach
    void setUp() throws IOException {
        try {
            tempDirectory = Files.createTempDirectory("temp" + UUID.randomUUID().toString().substring(0, 6)).toAbsolutePath().toString();
            Path outDirPath = Paths.get(tempDirectory);
            Files.walk(outDirPath)
            .filter(Files::isRegularFile)
            .map(Path::toFile)
            .forEach(File::delete);

            validator = new FileValidator(CONFIG_JSON);
            fileByteArrayList = new ArrayList<>();
            fileNamesArray = new String[batchSize];

            for (int i=0; i<batchSize; i++ ) {
                fileByteArrayList.add(generatePdfBytes(250000));
                fileNamesArray[i] = UUID.randomUUID().toString() + ".pdf";
            }

            for (int i = 0; i < 10; i++) {
                ValidationResponse fileValidationWarmup = validator.validateFile("Documents", 
                                                    fileByteArrayList.get(i), fileNamesArray[i], tempDirectory);
                if (fileValidationWarmup.isValid()) {} else {}
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
            for (int i = 0; i < batchSize; i++) {
                ValidationResponse fileValidationResults = validator.validateFile("Documents", 
                                                    fileByteArrayList.get(i), fileNamesArray[i], tempDirectory);
                if (fileValidationResults.isValid()) {} else {}
            }
            duration += System.currentTimeMillis() - startTime;
        }
        long avgDuration = duration / testRepeat;

        FileValidatorBenchTest.benchResults.add("testValidInputs: " + avgDuration);
        assertTrue(avgDuration <= validFilesTimeThreadhold, 
        "Expected valid file validation to take less than " + 
        validFilesTimeThreadhold + "ms, but took " + avgDuration + "ms");
    }

    // Test saving to non existing directory
    @Test
    void testSaveToNonExistingDirectory() throws Exception {
        long startTime;
        long duration = 0;
        for (int j = 0; j < testRepeat; j++) {
            startTime = System.currentTimeMillis();
            for (int i = 0; i < batchSize; i++) {
                ValidationResponse fileValidationResults = validator.validateFile("Documents", 
                                                    fileByteArrayList.get(i), fileNamesArray[i], "nonExistingDirectory-9384rhj934f8h3498h/3hd923d8h");
                if (fileValidationResults.isValid()) {} else {}
            }
            duration += System.currentTimeMillis() - startTime;
        }
        long avgDuration = duration / testRepeat;

        FileValidatorBenchTest.benchResults.add("testSaveToNonExistingDirectory: " + avgDuration);
        assertTrue(avgDuration <= validFilesTimeThreadhold, 
        "Expected testSaveToNonExistingDirectory to take less than " + 
        validFilesTimeThreadhold + "ms, but took " + avgDuration + "ms");
    }

    // Report benchmark results
    @AfterAll
    public static void printeBenchResults() {
        for (String result : benchResults) {
            System.out.println(result);
        }
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