package com.blumo.FileSentry4J;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Base64;

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
        String outDir = "samples/Out";
        ValidationResponse fileValidationResults = validator.validateFileType("Documents", pdfFile, outDir);

        if (fileValidationResults.isValid()) {
            String encodedFileName = fileValidationResults.getFileBytes().getName().substring(0, fileValidationResults.getFileBytes().getName().lastIndexOf("."));
            String extension = fileValidationResults.getFileBytes().getName().substring(fileValidationResults.getFileBytes().getName().lastIndexOf("."));
            String decodedFileName = new String(Base64.getDecoder().decode(encodedFileName));
            System.out.println(decodedFileName + extension + " is a valid document file. Checksum: " + fileValidationResults.getFileChecksum());
        } else {
            System.out.println(pdfFile.getName() + " is not a valid document file  because" + fileValidationResults.failureReason());
        }
    }
}