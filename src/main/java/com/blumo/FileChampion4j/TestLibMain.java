package com.blumo.FileChampion4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

public class TestLibMain {
    public static void main(String[] args) {
        // Path to the file to be validated in this simple example
        File pdfFile = new File("samples/In/DataMining-ch1.pdf");

        // Path to the config.json file
        String configPath = "config/config.json";

        // Placeholders for the JSON object and the file in bytes
        JSONObject jsonObject = null;
        byte[] fileInBytes = null;
        FileValidator validator = null;

        // Path to the output directory
        String outDir = "samples/Out/";
        
        // Create a new FileValidator object with json config file
        try {
            // Read the JSON object from the config.json file
            String jsonConfigContent = new String(Files.readAllBytes(Paths.get(configPath)));
            jsonObject = new JSONObject(jsonConfigContent);
            
            // Create a new FileValidator object
            validator = new FileValidator(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error reading config file");
            System.exit(1);

        }

        try {
            // Read the file to be validated into a byte array
            fileInBytes = Files.readAllBytes(pdfFile.toPath());

            // Validate the file
            ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, pdfFile.getName(),outDir);

            // Check if the file is valid
            if (fileValidationResults.isValid()) {
                // Print the results if the file is valid
                String validMessage = String.format("%s is a valid document file.%n New file: %s, Checksum: %s", 
                    fileValidationResults.resultsInfo(),
                    fileValidationResults.getValidFilePath().length == 0 ? "" : fileValidationResults.getValidFilePath()[0],
                    fileValidationResults.getFileChecksum());
                System.out.println(validMessage);
            } else {
                // Print the results if the file is invalid
                System.out.println(pdfFile.getName() + " is not a valid document file  because " + fileValidationResults.resultsInfo());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
