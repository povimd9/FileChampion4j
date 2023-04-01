package com.blumo.FileChampion4j;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import org.json.JSONObject;
import java.security.MessageDigest;


/**
 * This class is used to validate untrusted files
 * validateFile() argments:
 * @param fileCategory: the file type category to be validated
 * @param inFile: the file bytes of the file to be validated
 * @param fileName: the name of the file to be validated
 * @param outDir: optional directory where the file will be saved if it is valid
 * 
 * The validateFile method returns a ValidationResponse object that contains:
 * @return isValid: a boolean indicating whether the file is valid or not
 * @return fileBytes: the file bytes of the validated file
 * @return fileChecksum: the file checksum if the file is valid
 * @return resultsInfo: a string containing additional information about the validation results such as reason for failure or the name of the file if it is valid
  
                      TODO: reduce cognitive complexity
                      TODO: add filenname and checksum to loggers

                      TODO: add unit tests

                      TODO: support cli and jar loading

*/

public class FileValidator {
    // Initialize logger
    private static final Logger LOGGER = Logger.getLogger(FileValidator.class.getName());

    // Validate method arguments
    private void checkMethodInputs(JSONObject configJsonObject, String fileCategory, byte[] originalFile,
    String fileName) {
        if (fileCategory.isBlank() || originalFile.length > 0 || fileName.isBlank() || configJsonObject.isEmpty()) {
            String excMsg = String.format("Invalid argument(s) provided: fileCategory=%s, originalFile=%s, fileName=%s, configJsonObject=%s",   
            fileCategory, originalFile, fileName, configJsonObject);
            LOGGER.severe(excMsg);
            throw new IllegalArgumentException(excMsg);
        }
    }

    public ValidationResponse validateFile(JSONObject configJsonObject, String fileCategory, byte[] originalFile,
            String fileName, String... outputDir) {
        // Get the output directory if provided
        String outDir = outputDir.length > 0 ? outputDir[0] : "";
        
        // Check that the input parameters are not null or empty
        checkMethodInputs(configJsonObject, fileCategory, originalFile, fileName);

        // Initialize variables
        String responseAggregation = "";
        int responseMsgCount = 0;
        StringBuilder sbResponseAggregation = new StringBuilder(responseAggregation);
        String logMessage;
        String fileExtension = getFileExtension(fileName);
        String commonLogString = String.format(" for file extension: %s", fileExtension);
        String fileChecksum = calculateChecksum(originalFile);
        Extension extensionConfig;

        // Clean the file name to replace special characters with underscores
        String originalFilenameClean = fileName.replaceAll("[^a-zA-Z0-9.]", "_");

        // Get the configuration for the file type category and extension
        try {
            extensionConfig = new Extension(fileCategory, fileExtension, configJsonObject);
        } catch (IllegalArgumentException e) {
            LOGGER.warning(e.getMessage());
            return new ValidationResponse(false, e.getMessage(), originalFilenameClean, null, null);
        }

        // Log the file type category being validated
        logMessage = String.format("Validating %s, as file type: %s", originalFilenameClean, fileCategory);
        LOGGER.info(logMessage);

        // Check that the file size is not greater than the maximum allowed size
        if (Boolean.FALSE.equals(checkFileSize(originalFile.length, extensionConfig.getMaxSize()))) {
            sbResponseAggregation.append(++responseMsgCount + ". ")
                .append("File size (")
                .append(originalFile.length / 1000)
                .append("KB) exceeds maximum allowed size (")
                .append(extensionConfig.getMaxSize()).append("KB)")
                .append(commonLogString);
            LOGGER.warning(responseAggregation);
        }

        // Check that the mime type is allowed
        if (!checkMimeType(originalFile, extensionConfig.getMimeType())) {
            sbResponseAggregation.append(++responseMsgCount + ". ")
                .append("Invalid mime_type")
                .append(commonLogString);
            LOGGER.warning(responseAggregation);
        }

        // Check that the file contains the magic bytes
        if (extensionConfig.getMagicBytes().isEmpty() || !containsMagicBytes(originalFile, extensionConfig.getMagicBytes())) {
            sbResponseAggregation.append(++responseMsgCount + ". ")
                .append("Invalid magic_bytes")
                .append(commonLogString);
            LOGGER.warning(responseAggregation);
        }

        // Check header signatures (optional)
        if (extensionConfig.getHeaderSignatures() != null && !containsHeaderSignatures(originalFile, extensionConfig.getHeaderSignatures())) {
            sbResponseAggregation.append(++responseMsgCount + ". ")
                .append("Invalid header_signatures")
                .append(commonLogString);
            LOGGER.warning(responseAggregation);
        }

        // Check footer signatures (optional)
        if (extensionConfig.getFooterSignatures() != null && !containsFooterSignatures(originalFile, extensionConfig.getFooterSignatures())) {
            sbResponseAggregation.append(++responseMsgCount + ". ")
                .append("Invalid footer_signatures")
                .append(commonLogString);
            LOGGER.warning(responseAggregation);
        }

        // Check if file passed all defined validations, return false and reason if not.
        if (responseMsgCount > 0) {
            return new ValidationResponse(false, responseAggregation, originalFilenameClean, originalFile, fileChecksum);
        }

        // Check if the file name should be encoded
        String encodedFileName = "";
        if (extensionConfig.isNameEncoding()) { 
            encodedFileName = String.format("%s.%s",
            Base64.getEncoder().encodeToString(originalFilenameClean.getBytes(StandardCharsets.UTF_8)),  fileExtension);
            String encodingStatus = String.format("File name: '%s' has been successfully encoded to: '%s'", originalFilenameClean, encodedFileName);
            LOGGER.info(encodingStatus);
        }
        
        String targetFileName = encodedFileName.isEmpty() ? originalFilenameClean : encodedFileName;

        // Check if the file should be saved to output directory
        String savedFilePath;
        if (!outDir.isEmpty()) {
            savedFilePath = saveFileToOutputDir(outDir, targetFileName, originalFile, extensionConfig);
            if (savedFilePath.contains("Error:")) {
                String validMessage = String.format("File is valid but was not saved to output directory: %s", savedFilePath);
                return new ValidationResponse(true, validMessage, originalFilenameClean, originalFile, fileChecksum);
            }
        }

        // Return valid response if file passed all validations but was not saved to output directory
        String validMessage = String.format("File is valid: %s", originalFilenameClean);
        LOGGER.info(validMessage);
        return new ValidationResponse(true, validMessage, originalFilenameClean, originalFile, fileChecksum);
    }
    

    // Helper methods
    // ==============


    // Check if file size does not exceed the maximum allowed size
    private Boolean checkFileSize(int originalFileSize, int extensionMaxSize) {
        return (extensionMaxSize == 0) || (originalFileSize / 1000) <= extensionMaxSize;
    }

    // Get the file mime type from the file bytes and checks if it matches allowed mime types
    private boolean checkMimeType(byte[] originalFileBytes, String mimeType) {
        String fileMimeType = "";
        try {
            fileMimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(originalFileBytes));
        } catch (Exception e) {
            LOGGER.severe("checkMimeType failed: " + e.getMessage());
        }
        return !fileMimeType.isBlank() && fileMimeType.equals(mimeType);
    }
    
    // Get the file extension from the file name
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    // Check if the file contains the magic bytes
    private boolean containsMagicBytes(byte[] originalFileBytes, String magicBytesPattern) {
        if (originalFileBytes.length == 0 || magicBytesPattern == null || magicBytesPattern.isEmpty()) {
            return false;
        }
        magicBytesPattern = magicBytesPattern.replaceAll("\\s", "");
        if (magicBytesPattern.length() % 2 != 0) {
            magicBytesPattern = "0" + magicBytesPattern;
        }
        byte[] magicBytes = new byte[magicBytesPattern.length() / 2];
        for (int i = 0; i < magicBytesPattern.length(); i += 2) {
            magicBytes[i / 2] = (byte) Integer.parseInt(magicBytesPattern.substring(i, i + 2), 16);
        }
        for (int i = 0; i < originalFileBytes.length - magicBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < magicBytes.length; j++) {
                if (originalFileBytes[i + j] != magicBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    // Check if the file contains the header signatures
    private boolean containsHeaderSignatures(byte[] fileBytes, String headerSignaturesPattern) {
        if (headerSignaturesPattern.isBlank()) {
            return true;
        }
        String hexPattern = headerSignaturesPattern.replaceAll("\\s", "");
        if (hexPattern.length() % 2 != 0) {
            hexPattern = "0" + hexPattern;
        }
        byte[] headerSignatures = new byte[hexPattern.length() / 2];
        for (int i = 0; i < hexPattern.length(); i += 2) {
            headerSignatures[i / 2] = (byte) Integer.parseInt(hexPattern.substring(i, i + 2), 16);
        }
        for (int i = 0; i < headerSignatures.length; i++) {
            if (i >= fileBytes.length || fileBytes[i] != headerSignatures[i]) {
                return false;
            }
        }
        return true;
    }

    // Check if the file contains the footer signatures
    private boolean containsFooterSignatures(byte[] fileBytes, String footerSignaturesPattern) {
        if (footerSignaturesPattern.isBlank()) {
            return true;
        }
        String hexPattern = footerSignaturesPattern.replaceAll("\\s", "");
        if (hexPattern.length() % 2 != 0) {
            hexPattern = "0" + hexPattern;
        }
        byte[] footerSignatures = new byte[hexPattern.length() / 2];
        for (int i = 0; i < hexPattern.length(); i += 2) {
            footerSignatures[i / 2] = (byte) Integer.parseInt(hexPattern.substring(i, i + 2), 16);
        }
        int footerStartIndex = fileBytes.length - footerSignatures.length -1;
        if (footerStartIndex < 0) {
            return false;
        }
        for (int i = 0; i < footerSignatures.length; i++) {
            if (fileBytes[footerStartIndex + i] != footerSignatures[i]) {
                return false;
            }
        }
        return true;
    }

    // Calculate the file checksum
    private static String calculateChecksum(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // save the file to the output directory with appropriate owner and permissions
    private String saveFileToOutputDir(String outDir, String fileName, byte[] fileBytes, Extension extensionConfig) {
        Path targetFilePath = Paths.get(outDir, fileName);
        try {
            Files.write(targetFilePath, fileBytes, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            String errString = String.format("Error: Save file to output directory failed: %s", e.getMessage());
            LOGGER.severe(errString);
            return errString;
        }
        
        // Try to set the file owner and permissions
        FileAclHelper fileAclHelper = new FileAclHelper();
        String newFileAttributesStatus = fileAclHelper.changeFileAcl(targetFilePath, extensionConfig.getChangeOwnershipUser(), extensionConfig.getChangeOwnershipMode());
        if (newFileAttributesStatus.contains("Error:")) {
            try {
                Files.deleteIfExists(targetFilePath);
            } catch (IOException e) { LOGGER.severe("Error: Failed to delete file: " + e.getMessage());}
            LOGGER.severe(newFileAttributesStatus);
            return newFileAttributesStatus;
        }

        // Return the full path to the saved file since no errors were encountered
        return targetFilePath.toAbsolutePath().toString();
    }
}