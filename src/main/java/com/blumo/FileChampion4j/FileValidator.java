package com.blumo.FileChampion4j;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;


import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class is used to validate untrusted files
 * validateFile() argments:
 * @param fileCategory: the file type category to be validated
 * @param inFile: the file bytes of the file to be validated
 * @param fileName: the name of the file to be validated
 * @param outDir: the directory where the file will be saved if it is valid
 * 
 * The validateFile method returns a ValidationResponse object that contains:
 * @return isValid: a boolean indicating whether the file is valid or not
 * @return fileBytes: the file bytes of the validated file
 * @return fileChecksum: the file checksum if the file is valid
 * @return resultsInfo: a string containing additional information about the validation results such as reason for failure or the name of the file if it is valid
  
                      TODO: reduce cognitive complexity
                      TODO: add try/catch blocks to handle specific exceptions

                      TODO: add unit tests

                      TODO: support cli and jar loading
*/

public class FileValidator {
    // Initialize logger
    private static final Logger LOGGER = Logger.getLogger(FileValidator.class.getName());

    // Validate method arguments
    private void checkMethodInputs(JSONObject configJsonObject, String fileCategory, byte[] originalFile,
    String fileName, String outDir) {
        if (fileCategory.isBlank() || originalFile.length > 0 || fileName.isBlank() ||
        outDir.isBlank() ||  configJsonObject.isEmpty()) {
            String excMsg = String.format("Invalid argument(s) provided: fileCategory=%s, originalFile=%s, fileName=%s, outDir=%s, configJsonObject=%s",   
            fileCategory, originalFile, fileName, outDir, configJsonObject);
            LOGGER.severe(excMsg);
            throw new IllegalArgumentException(excMsg);
        }
    }


    public ValidationResponse validateFile(JSONObject configJsonObject, String fileCategory, byte[] originalFile,
            String fileName, String outDir) throws IOException {
        // Check that the input parameters are not null or empty
        checkMethodInputs(configJsonObject, fileCategory, originalFile, fileName, outDir);

        // Initialize variables
        String responseAggregation = "";
        int responseMsgCount = 0;
        StringBuilder sbResponseAggregation = new StringBuilder(responseAggregation);
        String statusMsg;
        String logMessage;
        String fileExtension = getFileExtension(fileName);
        Extension extensionConfig;

        // Get the configuration for the file type category and extension
        try {
            extensionConfig = new Extension(fileCategory, fileExtension, configJsonObject);
        } catch (IllegalArgumentException e) {
            LOGGER.severe(e.getMessage());
            throw new IllegalArgumentException(e);
        }
        
        // Clean the file name to replace special characters with underscores
        String originalFilenameClean = fileName.replaceAll("[^a-zA-Z0-9.]", "_");

        // Log the file type category being validated
        logMessage = String.format("Validating %s, as file type: %s", originalFilenameClean, fileCategory);
        LOGGER.info(logMessage);

        // Check that the file size is not greater than the maximum allowed size
        if (!checkFileSize(originalFile.length, extensionConfig.getMaxSize())) {
            sbResponseAggregation.append(++responseMsgCount + ". ")
                .append("File size (")
                .append(Math.floorDiv(originalFile.length, 1000))
                .append("KB) exceeds maximum allowed size (")
                .append(extensionConfig.getMaxSize()).append("KB) for file extension: ")
                .append(fileExtension);
            LOGGER.warning(responseAggregation);
        }

        // Check that the mime type is allowed
        if (!checkMimeType(originalFile, extensionConfig.getMimeType())) {
            sbResponseAggregation.append(++responseMsgCount + ". ")
                .append("Invalid mime_type for file extension: ")
                .append(fileExtension);
            LOGGER.warning(responseAggregation);
        }

        // Check that the file contains the magic bytes
        if (extensionConfig.getMagicBytes().isEmpty() || !containsMagicBytes(originalFile, extensionConfig.getMagicBytes())) {
            sbResponseAggregation.append(++responseMsgCount + ". ")
                .append("Invalid magic_bytes for file extension: ")
                .append(fileExtension);
            LOGGER.warning(responseAggregation);
        }

            // Check header signatures (optional)
            String headerSignaturesPattern = (String) extensionConfig.get("header_signatures");
            if (!Objects.isNull(headerSignaturesPattern) && !containsHeaderSignatures(originalFileBytes, headerSignaturesPattern)) {
                sbResponseAggregation.append(++responseMsgCount + ". ")
                    .append("Invalid header_signatures for file extension: ")
                    .append(fileExtension);
                LOGGER.warning(responseAggregation);
            }

            // Check footer signatures (optional)
            String footerSignaturesPattern = (String) extensionConfig.get("footer_signatures");
            if (!Objects.isNull(footerSignaturesPattern) && !containsFooterSignatures(originalFileBytes, footerSignaturesPattern)) {
                sbResponseAggregation.append(++responseMsgCount + ". ")
                    .append("Invalid footer_signatures for file extension: ")
                    .append(fileExtension);
                LOGGER.warning(responseAggregation);
            }


            //ProcessBuilder pb = new ProcessBuilder(toolName, toolArgs);
            //Process p = pb.start();
            // ... Handle the output and errors of the process as needed ...

            
            /* // Get the custom validator configuration
            Map<String, Object> pdfConfig = (Map<String, Object>) fileCategoryConfig.get(fileCategory);
            Map<String, Object> customValidatorConfig = (Map<String, Object>) pdfConfig.get("custom_validators");
            if (!Objects.isNull(customValidatorConfig)) {
                // Iterate over the custom validators and invoke them using the CustomFileLoader class
                for (String jarPath : customValidatorConfig.keySet()) {
                    List<String> customValidator = (List<String>) customValidatorConfig.get(jarPath);
                    String className = customValidator.get(0);
                    String methodName = customValidator.get(1);
                    try {
                        // Invoke the method using CustomFileLoader class
                        String filePath = originalFilenameClean.getAbsolutePath();
                        CustomFileLoader loader = new CustomFileLoader();
                        loader.loadClass(jarPath, className, methodName, filePath);
                    } catch (Exception e) {
                        LOGGER.severe(e.toString());
                        return new ValidationResponse(false, "An error occurred while reading file: " + e.getMessage(), originalFilenameClean, null);
                    }
                }
            } */

            // Check if file is valid so far
            if (responseAggregation.isEmpty()) {
                // Check if the file name should be encoded
                Boolean nameEncode = (Boolean) extensionConfig.get("name_encoding");
                Path encodedFilePath = null;
                if (!Objects.isNull(nameEncode) && nameEncode) {
                    String encodedFileName = String.format("%s.%s",Base64.getEncoder().encodeToString(originalFilenameClean.getBytes()),  getFileExtension(fileName));
                    encodedFilePath = Paths.get(outDir, encodedFileName);
                    try {
                        Files.copy(originalFile.toPath(), encodedFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                        String encodingStatus = String.format("File %s has been successfully encoded and saved in %s", originalFilenameClean, encodedFilePath.toAbsolutePath());
                        LOGGER.info(encodingStatus);
                    } catch (IOException e) {
                        responseAggregation = "Failed to encode file: " + encodedFilePath.toAbsolutePath();
                        LOGGER.warning(responseAggregation);
                    }
                }

                // Check if validations and encoding were successful
                if (responseAggregation.isEmpty() && !Objects.isNull(encodedFilePath)) {
                    File cleanFile = new File(encodedFilePath.toAbsolutePath().toString());

                    // Calculate SHA-256 checksum of file
                    byte[] sha256Bytes;
                    String sha256Checksum;
                    try {
                        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
                        sha256Digest.update(Files.readAllBytes(cleanFile.toPath()));
                        sha256Bytes = sha256Digest.digest();
                        sha256Checksum = new Formatter().format("%02x", new BigInteger(1, sha256Bytes)).toString();

                        logMessage = String.format("SHA-256 checksum of file %s is: %s", cleanFile.getName(), sha256Checksum);
                        LOGGER.info(logMessage);
                    } catch (NoSuchAlgorithmException e) {
                        sbResponseAggregation.append(++responseMsgCount + ". ")
                            .append("Failed to calculate checksum of file: ")
                            .append(cleanFile.getName());
                        LOGGER.warning(responseAggregation);
                        return new ValidationResponse(false, responseAggregation, originalFile, null);
                    }

                    // Check if file ownership and permissions should be changed
                    Boolean changeOwnership = (Boolean) extensionConfig.get("change_ownership");
                    if (!Objects.isNull(changeOwnership) && changeOwnership) {
                        String changeOwnershipUser = (String) extensionConfig.get("change_ownership_user");
                        String changeOwnershipMode = (String) extensionConfig.get("change_ownership_mode");
                        try {
                            // Change the file ACL
                            FileAclHelper.ChangeFileACL(encodedFilePath, changeOwnershipMode, changeOwnershipUser);
                            logMessage = String.format("File %s ACL changed successfully", encodedFilePath);
                            LOGGER.info(logMessage);
                        } catch (Exception e) {
                            sbResponseAggregation.append(++responseMsgCount + ". ")
                                .append("Error changing file ACL: ")
                                .append(e.getMessage());
                            LOGGER.warning(responseAggregation);
                            return new ValidationResponse(false, responseAggregation, originalFile, null);
                        }
                    }

                    // Return if file is valid
                    logMessage = String.format("File %s is valid", originalFilenameClean);
                    LOGGER.info(logMessage);
                    String validDescription = String.format("%s ==> %s", originalFilenameClean, cleanFile.getName());
                    return new ValidationResponse(true, validDescription, cleanFile, sha256Checksum);
                } else {
                    // Return if file is invalid
                    logMessage = String.format("File %s is invalid: %s", originalFilenameClean, responseAggregation);
                    LOGGER.info(logMessage);
                    return new ValidationResponse(false, responseAggregation, originalFile, null);
                }
            } else {
                // Return if file is invalid
                logMessage = String.format("File %s is invalid: %s", originalFilenameClean, responseAggregation);
                LOGGER.info(logMessage);
                return new ValidationResponse(false, responseAggregation, originalFile, null);
            }
        } catch (Exception e) {
            LOGGER.severe(e.toString());
            String errorResponse = String.format("An error occurred while reading file: %s", e.getMessage());
            return new ValidationResponse(false, errorResponse, originalFile, null);
        }
    }

    // Helper methods
    // ==============

    // Check if file size does not exceed the maximum allowed size
    private Boolean checkFileSize(int originalFileSize, int extensionMaxSize) {
        return Objects.isNull(extensionMaxSize) || Math.floorDiv(originalFileSize, 1000) <= extensionMaxSize;
    }

    // Get the file mime type from the file bytes and checks if it matches allowed mime types
    private boolean checkMimeType(byte[] originalFileBytes, String mimeType) {
        String fileMimeType = null;
        try {
            fileMimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(originalFileBytes));
        } catch (IOException e) {
            LOGGER.severe("checkMimeType failed: " + e.getMessage());
        }
        return fileMimeType != null || fileMimeType.equals(mimeType);
    }
    
    // Get the file extension from the file name
    private String getFileExtension(String fileName) {
        if (Objects.isNull(fileName) || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    // Check if the file contains the magic bytes
    private boolean containsMagicBytes(byte[] originalFileBytes, String magicBytesPattern) {
        if (Objects.isNull(originalFileBytes) || magicBytesPattern == null || magicBytesPattern.isEmpty()) {
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
        if (Objects.isNull(headerSignaturesPattern) || headerSignaturesPattern.isEmpty()) {
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
        if (Objects.isNull(footerSignaturesPattern) || footerSignaturesPattern.isEmpty()) {
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

}