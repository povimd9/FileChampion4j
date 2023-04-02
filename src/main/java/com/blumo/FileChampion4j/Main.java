package com.blumo.FileChampion4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;


/**
    This is an example implementation of the FileChampion4j library using FileValidator.validateFile()
    ValidationResponse.resultsInfo() contains the results of the validation, including:
    - the name of the file if it is valid or empty if it is invalid
    - the reason why the file is invalid if it is invalid
    - the path + name of the file if it is valid and outputDir was set in the FileChampion4j constructor
    - the checksum of the file if it is valid    
*/


public class Main {
    public static void main(String[] args) {
        JSONObject jsonObject;
        byte[] fileInBytes;
        FileValidator validator = new FileValidator();
        String outDir = "C:/Users/User1/git/FileChampion4j/samples/Out";
        File pdfFile = new File("samples/In/Binary Coding (2017).pdf");



        try {
            String jsonFileContent = new String(Files.readAllBytes(Paths.get("config/config.json")));
            jsonObject = new JSONObject(jsonFileContent);
            fileInBytes = Files.readAllBytes(pdfFile.toPath());
            ValidationResponse fileValidationResults = validator.validateFile(jsonObject, "Documents", fileInBytes, pdfFile.getName(),outDir);

            if (fileValidationResults.isValid()) {
                String validMessage = String.format("%s is a valid document file.%n New file: %s, Checksum: %s", 
                    fileValidationResults.resultsInfo(),
                    fileValidationResults.getValidFilePath()[0],
                    fileValidationResults.getFileChecksum());
                System.out.println(validMessage);
            } else {
                System.out.println(pdfFile.getName() + " is not a valid document file  because " + fileValidationResults.resultsInfo());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}