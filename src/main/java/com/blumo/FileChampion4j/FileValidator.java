package com.blumo.FileChampion4j;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;

import org.json.JSONObject;
import java.security.MessageDigest;


/**
 * This class is used to validate untrusted files

                      TODO: add filenname and checksum to loggers

                      TODO: support cli and jar loading

                      TODO: add support for validation and sanitization extensions

*/
public class FileValidator {
    private static final Logger LOGGER = Logger.getLogger(FileValidator.class.getName());
    private final JSONObject configJsonObject;

    /**
     * This method is used to get the json configurations
     * @param configJsonObject
     */
    public FileValidator(JSONObject configJsonObject) {
        if (configJsonObject == null || configJsonObject.isEmpty()) {
            throw new IllegalArgumentException("Config JSON object cannot be null or empty.");
        }
        this.configJsonObject = configJsonObject;
    }

    /**
     * This method is used to check that method inputs are not null or empty
     * @param fileCategory
     * @param originalFile
     * @param fileName
     */
    private void checkMethodInputs( String fileCategory, byte[] originalFile, String fileName) {
        if (isBlank(fileCategory)) {
            throw new IllegalArgumentException("fileCategory cannot be null or empty.");
        }
        if (isBlank(fileName)) {
            throw new IllegalArgumentException("fileName cannot be null or empty.");
        }
        if (originalFile == null || originalFile.length == 0) {
            throw new IllegalArgumentException("originalFile cannot be null or empty.");
        }
    }
    
    /**
     * This method is the main entry point for validating files, initializing the validation process and variables
     * @param fileCategory (String) a string containing the file type category to validate the file against
     * @param originalFile (byte[]) a byte array containing the file bytes of the file to be validated
     * @param fileName (String) a string containing the name of the file to be validated
     * @param outputDir (String) an optional string containing path to the output directory for validated files [optional]
     * @return ValidationResponse (ValidationResponse) a ValidationResponse object containing the results of the validation
     * @throws IllegalArgumentException (IllegalArgumentException) if any of the input parameters are null or empty
     */
    public ValidationResponse validateFile(String fileCategory, byte[] originalFile,
            String fileName, String... outputDir) {
        // Get the output directory if provided
        String outDir = outputDir.length > 0 ? outputDir[0] : "";
        
        // Check that the input parameters are not null or empty
        checkMethodInputs(fileCategory, originalFile, fileName);

        // Initialize variables
        String logMessage;
        String fileExtension = getFileExtension(fileName);
        Extension extensionConfig;

        // Clean the file name to replace special characters with underscores
        String originalFilenameClean = fileName.replaceAll("[^a-zA-Z0-9.]", "_");

        // Get the configuration for the file type category and extension
        try {
            extensionConfig = new Extension(fileCategory, fileExtension, configJsonObject);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            return new ValidationResponse(false, e.getMessage(), originalFilenameClean, null, null);
        }

        // Log the file type category being validated
        logMessage = String.format("Validating %s, as file type: %s", originalFilenameClean, fileCategory);
        LOGGER.info(logMessage);
     
        return (doValidations(originalFilenameClean, fileExtension, extensionConfig, originalFile, outDir));
    }

    /**
     * If file category was found in the config, this method is used to validate the file
     * @param originalFilenameClean (String) a string containing the cleaned file name
     * @param fileExtension (String) a string containing the file extension
     * @param extensionConfig (Extension) an Extension object containing the configuration for the file type category and extension
     * @param originalFile (byte[]) a byte array containing the file bytes of the file to be validated
     * @param outDir (String) a string containing the path to the output directory for validated files
     * @return ValidationResponse (ValidationResponse) a ValidationResponse object containing the results of the validation
     */
    private ValidationResponse doValidations(String originalFilenameClean, String fileExtension, Extension extensionConfig, byte[] originalFile, String outDir) {
        String commonLogString = String.format(" for file extension: %s", fileExtension);
        String fileChecksum = calculateChecksum(originalFile);
        String responseAggregation = "";
        int responseMsgCount = 0;
        StringBuilder sbResponseAggregation = new StringBuilder(responseAggregation);

    
        // Check that the file size is not greater than the maximum allowed size, dont continue if it is
        if (Boolean.FALSE.equals(checkFileSize(originalFile.length, extensionConfig.getMaxSize()))) {
            sbResponseAggregation.append(System.lineSeparator() + ++responseMsgCount + ". ")
                .append("File size (")
                .append(originalFile.length / 1000)
                .append("KB) exceeds maximum allowed size (")
                .append(extensionConfig.getMaxSize()).append("KB)")
                .append(commonLogString);
            responseAggregation = sbResponseAggregation.toString();
            LOGGER.warning(responseAggregation);
            return new ValidationResponse(false, responseAggregation, originalFilenameClean, originalFile, fileChecksum);
        }

        // Check that the mime type is allowed
        if (!checkMimeType(originalFile, fileExtension,extensionConfig.getMimeType())) {
            sbResponseAggregation.append(System.lineSeparator() + ++responseMsgCount + ". ")
                .append("Invalid mime_type")
                .append(commonLogString);
            responseAggregation = sbResponseAggregation.toString();
            LOGGER.warning(responseAggregation);
        }

        // Check that the file contains the magic bytes
        if (extensionConfig.getMagicBytes().isEmpty() || !containsMagicBytes(originalFile, extensionConfig.getMagicBytes())) {
            sbResponseAggregation.append(System.lineSeparator() + ++responseMsgCount + ". ")
                .append("Invalid magic_bytes")
                .append(commonLogString);
            responseAggregation = sbResponseAggregation.toString();
            LOGGER.warning(responseAggregation);
        }

        // Check header signatures (optional)
        if (extensionConfig.getHeaderSignatures() != null && !containsHeaderSignatures(originalFile, extensionConfig.getHeaderSignatures())) {
            sbResponseAggregation.append(System.lineSeparator() + ++responseMsgCount + ". ")
                .append("Invalid header_signatures")
                .append(commonLogString);
            responseAggregation = sbResponseAggregation.toString();
            LOGGER.warning(responseAggregation);
        }

        // Check footer signatures (optional)
        if (extensionConfig.getFooterSignatures() != null && !containsFooterSignatures(originalFile, extensionConfig.getFooterSignatures())) {
            sbResponseAggregation.append(System.lineSeparator() + ++responseMsgCount + ". ")
                .append("Invalid footer_signatures")
                .append(commonLogString);
            responseAggregation = sbResponseAggregation.toString();
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
                // Return valid file response if file failed to save to output directory
                String validMessage = String.format("File is valid but was not saved to output directory: %s", savedFilePath);
                return new ValidationResponse(true, validMessage, originalFilenameClean, originalFile, fileChecksum);
            }
            // Return valid file response if file was saved to output directory
            String validMessage = String.format("File is valid and was saved to output directory: %s", savedFilePath);
            return new ValidationResponse(true, validMessage, originalFilenameClean, originalFile, fileChecksum, savedFilePath);
        }

        // Return valid response if file passed all validations but is not meant to be saved to disk
        String validMessage = String.format("File is valid: %s", originalFilenameClean);
        LOGGER.info(validMessage);
        return new ValidationResponse(true, validMessage, originalFilenameClean, originalFile, fileChecksum);
    }
    

    ////////////////////
    // Helper methods //
    ////////////////////

    // String.isBlank() is only available in Java 11
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    

    // Check if file size does not exceed the maximum allowed size
    private Boolean checkFileSize(int originalFileSize, int extensionMaxSize) {
        return (extensionMaxSize == 0) || (originalFileSize / 1000) <= extensionMaxSize;
    }

    // Save the file to temporary directory for analysis
    private Path saveFileToTempDir(String fileExtension, byte[] originalFile) {
        Path tempFilePath = null;
        try {
            // Create a temporary directory
            Path tempDir = Files.createTempDirectory("tempDir");
            tempFilePath = Files.createTempFile(tempDir, "tempFile", "." + fileExtension);
            Files.write(tempFilePath, originalFile);
        } catch (Exception e) {
            String errMessage = String.format("saveFileToTempDir failed: %s", e.getMessage());
            LOGGER.severe(errMessage);
            return null;
        }
        return tempFilePath;
    }

    // Explicitley delete the temporary directory and file
    private Boolean deleteTempDir(Path tempFilePath) {
        try {
            Files.delete(tempFilePath);
            Files.delete(tempFilePath.getParent());
            return true;
        } catch (Exception e) {
            String errMessage = String.format("deleteTempDir failed: %s", e.getMessage());
            LOGGER.severe(errMessage);
            return false;
        }
    }

    // Get the file mime type from the file bytes and checks if it matches allowed mime types
    private boolean checkMimeType(byte[] originalFileBytes, String fileExtension, String mimeType) {
        String fileMimeType = "";
        Path tempFile = saveFileToTempDir(fileExtension, originalFileBytes);
        if (tempFile == null) {
            return false;
        }
        try {
            fileMimeType = Files.probeContentType(tempFile);
            deleteTempDir(tempFile);
        } catch (Exception e) {
            LOGGER.severe("checkMimeType failed: " + e.getMessage());
        }
        return fileMimeType != null && !isBlank(fileMimeType) && fileMimeType.equals(mimeType);
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
        if (isBlank(headerSignaturesPattern)) {
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
        if (isBlank(footerSignaturesPattern)) {
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

    // Calculate file checksum
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
            Files.write(targetFilePath, fileBytes, StandardOpenOption.CREATE);
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
            } catch (IOException e) { LOGGER.severe("Error: Failed to set file owner and permissions: " + e.getMessage());}
            LOGGER.severe(newFileAttributesStatus);
            return newFileAttributesStatus;
        }

        // Return the full path to the saved file since no errors were encountered
        return targetFilePath.toAbsolutePath().toString();
    }
}
