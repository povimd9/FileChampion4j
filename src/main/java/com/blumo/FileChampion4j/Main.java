package com.blumo.FileChampion4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


// This class is used to validate files against a set of rules
// ValidationResponse contains the results of the validation with:
// - isValid: a boolean indicating whether the file is valid or not
// - failureReason: a string containing the reason for failure if the file is invalid
// - fileBytes: the file bytes if the file is valid
// - fileChecksum: the file checksum if the file is valid

public class Main {
    public static void main(String[] args) throws IOException {
        Map<String, Object> configMap = ConfigParser.parseConfig("config/config.json");
        FileValidator validator = new FileValidator(configMap);

        File pdfFile = new File("samples/In/Binary Coding (2017).pdf");
        byte[] fileInBytes = Files.readAllBytes(pdfFile.toPath());
        String outDir = "samples/Out";
        ValidationResponse fileValidationResults = validator.validateFileType("Documents", fileInBytes, pdfFile.getName(),outDir);

        if (fileValidationResults.isValid()) {
            String cleanFileName = fileValidationResults.resultsInfo();
            System.out.println(cleanFileName + " is a valid document file. Checksum: " + fileValidationResults.getFileChecksum());
        } else {
            System.out.println(pdfFile.getName() + " is not a valid document file  because " + fileValidationResults.resultsInfo());
        }
    }
}