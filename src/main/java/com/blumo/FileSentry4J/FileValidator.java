package com.blumo.FileSentry4J;

import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

// names to consider for the project: FileFilter4J, FileShield4J, FileSentry4J
// FileValidator class to validate file types. This class can be used by any application to validate file types.
// The configuration for file types is stored in a JSON file. The JSON file is parsed into a Map object.
// The Map object is passed to the FileValidator constructor.
// The FileValidator class contains a validateFileType method that takes a file type and a file as input.
// The file type is used to look up the configuration for that file type in the Map object.
// The file is validated against the configuration for the file type.
// The validateFileType method returns a ValidationResponse object that contains a boolean indicating whether the file is valid or not.
// If the file is not valid, the ValidationResponse object contains a failure reason.
// TODO: add try/catch blocks to handle specific exceptions
// TODO: add logging
// TODO: add unit tests
// TODO: support cli and jar loading
// TODO: encode name prior to classloading

public class FileValidator {
    private static final Logger LOGGER = Logger.getLogger(FileValidator.class.getName());

    private final Map<String, Object> configMap;

    public FileValidator(Map<String, Object> configMap) {
        this.configMap = configMap;
    }

    public ValidationResponse validateFileType(String fileType, File originalFile) throws IOException {
        // Read the file into a byte array
        byte[] fileBytes = Files.readAllBytes(originalFile.toPath());
        String responseAggregation = "";

        // Check that the file type is not null or empty
        if (Objects.isNull(fileType) || fileType.isEmpty()) {
            responseAggregation = "File type cannot be null or empty.";
        }
        LOGGER.info("Validating " + originalFile.getName().replaceAll("[^a-zA-Z0-9.]", "_") +  ", as file type: " + fileType);

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
            String fileExtension = getFileExtension(originalFile.getName().replaceAll("[^a-zA-Z0-9.]", "_"));
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
                        String filePath = originalFile.getAbsolutePath();
                        CustomFileLoader loader = new CustomFileLoader();
                        loader.loadClass(jarPath, className, methodName, filePath);
                    } catch (Exception e) {
                        LOGGER.severe(e.toString());
                        return new ValidationResponse(false, "An error occurred while reading file: " + e.getMessage(), originalFile, null);
                    }
                }
            } */

            // Check if the file name should be encoded
            Boolean nameEncode = (Boolean) extensionConfig.get("name_encoding");
            if (!Objects.isNull(nameEncode) && nameEncode) {
                String encodedFileName = Base64.getEncoder().encodeToString(originalFile.getName().replaceAll("[^a-zA-Z0-9.]", "_").getBytes());
                File encodedFile = new File(originalFile.getParentFile(), encodedFileName);
                boolean success = originalFile.renameTo(encodedFile);
                if (!success) {
                    responseAggregation = "Failed to rename file: " + originalFile.getName().replaceAll("[^a-zA-Z0-9.]", "_");
                    LOGGER.warning(responseAggregation);
                }
            }

            // Calculate SHA-256 checksum of file
            byte[] sha256Bytes = null;
            try {
                MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
                sha256Digest.update(fileBytes);
                sha256Bytes = sha256Digest.digest();
            } catch (NoSuchAlgorithmException e) {
                responseAggregation = "Failed to calculate checksum of file: " + originalFile.getName().replaceAll("[^a-zA-Z0-9.]", "_");
                LOGGER.warning(responseAggregation);
            }
            // Convert SHA-256 bytes to Base64 string
            String sha256Checksum = Base64.getEncoder().encodeToString(sha256Bytes);

            if (responseAggregation.isEmpty()) {
                LOGGER.info(originalFile.getName().replaceAll("[^a-zA-Z0-9.]", "_") + " is valid");
                return new ValidationResponse(true, "File is valid", originalFile, sha256Checksum);
            } else {
                return new ValidationResponse(false, responseAggregation, originalFile, sha256Checksum);
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
        int footerStartIndex = fileBytes.length - footerSignatures.length;
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