package com.blumo.FileChampion4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;


// This class is used to validate files against a set of rules
// ValidationResponse contains the results of the validation with:
// - isValid: a boolean indicating whether the file is valid or not
// - failureReason: a string containing the reason for failure if the file is invalid
// - fileBytes: the file bytes if the file is valid
// - fileChecksum: the file checksum if the file is valid

public class Main {
    public static void main(String[] args) {
        JSONObject jsonObject;
        byte[] fileInBytes;
        FileValidator validator = new FileValidator();
        File pdfFile = new File("samples/In/Binary Coding (2017).pdf");


        try {
            String jsonFileContent = new String(Files.readAllBytes(Paths.get("config/config.json")));
            jsonObject = new JSONObject(jsonFileContent);
            fileInBytes = Files.readAllBytes(pdfFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String outDir = "samples/Out";
        ValidationResponse fileValidationResults = validator.validateFile(jsonObject, "Documents", fileInBytes, pdfFile.getName(),outDir);

        if (fileValidationResults.isValid()) {
            String cleanFileName = fileValidationResults.resultsInfo();
            System.out.println(cleanFileName + " is a valid document file. Checksum: " + fileValidationResults.getFileChecksum());
        } else {
            System.out.println(pdfFile.getName() + " is not a valid document file  because " + fileValidationResults.resultsInfo());
        }
    }
}