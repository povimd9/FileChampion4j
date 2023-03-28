package com.blumo.FileSentry4J;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class is used to validate file types and checksums
 * The configuration for file types and validations is stored in a JSON file and should be loaded as part of initiation.
 * The originalFile is validated against the configured controls for the file type.
 * The validateFileType method returns a ValidationResponse object that contains:
 * - isValid: a boolean indicating whether the file is valid or not
 * - fileBytes: the file bytes if the file is valid
 * - fileChecksum: the file checksum if the file is valid
 * - resultsInfo: a string containing additional information about the validation results /
                      such as reason for failure or the name of the file if it is valid
TODO: add try/catch blocks to handle specific exceptions
TODO: add unit tests
TODO: support cli and jar loading
*/

public class FileValidator {
    private static final Logger LOGGER = Logger.getLogger(FileValidator.class.getName());

    private final Map<String, Object> configMap;

    public FileValidator(Map<String, Object> configMap) {
        this.configMap = configMap;
    }

    public ValidationResponse validateFileType(String fileType, File originalFile, String outDir) throws IOException {
        // Read the file into a byte array
        byte[] fileBytes = Files.readAllBytes(originalFile.toPath());
        String responseAggregation = "";
        String originalFilenameClean = originalFile.getName().replaceAll("[^a-zA-Z0-9.]", "_");

        // Check that the file type is not null or empty
        if (Objects.isNull(fileType) || fileType.isEmpty()) {
            responseAggregation = "File type cannot be null or empty.";
        }
        LOGGER.info("Validating " + originalFilenameClean +  ", as file type: " + fileType);

        try {
            // Check that the file exists
            Map<String, Object> fileTypeConfig = (Map<String, Object>) configMap.get(fileType);
            if (Objects.isNull(fileTypeConfig)) {
                responseAggregation = "File type not configured: " + fileType;
                LOGGER.warning(responseAggregation);
            }

            // Check that the file is not empty
            List<String> allowedExtensions = (List<String>) fileTypeConfig.get("allowed_extensions");
            if (Objects.isNull(allowedExtensions) || allowedExtensions.isEmpty()) {
                responseAggregation = "No allowed extensions found for file type: " + fileType;
                LOGGER.warning(responseAggregation);
            }

            // Check that the file extension is allowed
            String fileExtension = getFileExtension(originalFilenameClean);
            if (fileExtension.isEmpty() || !allowedExtensions.contains(fileExtension)) {
                responseAggregation = "File extension not allowed or not configured: " + fileType;
                LOGGER.warning(responseAggregation);
            }

            // Check that the file size is not greater than the maximum allowed size
            String mimeType = Files.probeContentType(originalFile.toPath());
            Map<String, Object> extensionConfig = (Map<String, Object>) fileTypeConfig.get(fileExtension);
            if (extensionConfig == null) {
                responseAggregation = "mime_type not configured for file extension: " + fileExtension;
                LOGGER.warning(responseAggregation);
            }

            // Check that the mime type is allowed
            String expectedMimeType = (String) extensionConfig.get("mime_type");
            if (Objects.isNull(expectedMimeType) || !expectedMimeType.equals(mimeType)) {
                responseAggregation = "Invalid mime_type for file extension: " + fileExtension;
                LOGGER.warning(responseAggregation);
            }

            // Check that the file size is not greater than the maximum allowed size
            String magicBytesPattern = (String) extensionConfig.get("magic_bytes");
            if (Objects.isNull(magicBytesPattern) || !containsMagicBytes(fileBytes, magicBytesPattern)) {
                responseAggregation = "Invalid magic_bytes for file extension: " + fileExtension;
                LOGGER.warning(responseAggregation);
            }

            // Check header signatures
            String headerSignaturesPattern = (String) extensionConfig.get("header_signatures");
            if (!Objects.isNull(headerSignaturesPattern) && !containsHeaderSignatures(fileBytes, headerSignaturesPattern)) {
                responseAggregation = "Invalid header_signatures for file extension: " + fileExtension;
                LOGGER.warning(responseAggregation);
            }

            // Check footer signatures
            String footerSignaturesPattern = (String) extensionConfig.get("footer_signatures");
            if (!Objects.isNull(footerSignaturesPattern) && !containsFooterSignatures(fileBytes, footerSignaturesPattern)) {
                responseAggregation = "Invalid footer_signatures for file extension: " + fileExtension;
                LOGGER.warning(responseAggregation);
            }

            /* // Get the custom validator configuration
            Map<String, Object> pdfConfig = (Map<String, Object>) fileTypeConfig.get(fileType);
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

            if (responseAggregation.isEmpty()) {
                // Check if the file name should be encoded
                Boolean nameEncode = (Boolean) extensionConfig.get("name_encoding");
                Path encodedFilePath = null;
                if (!Objects.isNull(nameEncode) && nameEncode) {
                    String encodedFileName = Base64.getEncoder().encodeToString(originalFilenameClean.getBytes())+ "." + getFileExtension(originalFile.getName());
                    encodedFilePath = Paths.get(outDir, encodedFileName);
                    try {
                        Files.copy(originalFile.toPath(), encodedFilePath, StandardCopyOption.REPLACE_EXISTING);
                        String encodingStatus = "File " + originalFilenameClean + " has been successfully encoded and saved in " + encodedFilePath.toAbsolutePath();
                        LOGGER.info(encodingStatus);
                    } catch (IOException e) {
                        responseAggregation = "Failed to encode file: " + encodedFilePath.toAbsolutePath();
                        LOGGER.warning(responseAggregation);
                    }
                }


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
                        LOGGER.info("SHA-256 checksum of file " + cleanFile.getName() + " is: " + sha256Checksum);
                    } catch (NoSuchAlgorithmException e) {
                        responseAggregation = "Failed to calculate checksum of file: " + cleanFile.getName();
                        LOGGER.warning(responseAggregation);
                        return new ValidationResponse(false, responseAggregation, originalFile, null);
                    }

                    // Check if file ownership and permissions should be changed
                    Boolean changeOwnership = (Boolean) extensionConfig.get("change_ownership");
                    if (!Objects.isNull(changeOwnership) && changeOwnership) {
                        String changeOwnershipUser = (String) extensionConfig.get("change_ownership_user");
                        String changeOwnershipGroup = (String) extensionConfig.get("change_ownership_group");
                        String changeOwnershipMode = (String) extensionConfig.get("change_ownership_mode");
                        try {
                            // Change the file ACL
                            FileAclHelper.ChangeFileACL(encodedFilePath, changeOwnershipMode, changeOwnershipUser, changeOwnershipGroup);
                            LOGGER.info(encodedFilePath + " ACL changed successfully.");
                        } catch (Exception e) {
                            responseAggregation = "Error changing file ACL: " + e.getMessage();
                        }
                    }

                    LOGGER.info(originalFilenameClean + " is valid");
                    return new ValidationResponse(true, originalFilenameClean + " ==> " + cleanFile.getName(), cleanFile, sha256Checksum);
                } else {
                    return new ValidationResponse(false, responseAggregation, originalFile, null);
                }
            } else {
                return new ValidationResponse(false, responseAggregation, originalFile, null);
            }
        } catch (Exception e) {
            LOGGER.severe(e.toString());
            return new ValidationResponse(false, "An error occurred while reading file: " + e.getMessage(), originalFile, null);
        }
    }

    // Helper methods
    // ==============
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
    private boolean containsMagicBytes(byte[] fileBytes, String magicBytesPattern) {
        if (Objects.isNull(fileBytes) || magicBytesPattern == null || magicBytesPattern.isEmpty()) {
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
        for (int i = 0; i < fileBytes.length - magicBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < magicBytes.length; j++) {
                if (fileBytes[i + j] != magicBytes[j]) {
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