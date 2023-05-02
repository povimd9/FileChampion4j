package dev.filechampion.filechampion4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.LogManager;


/**
 * This class contains helper methods for the FileValidator class
 * @version 0.9.8.2
 */
public class ValidationsHelper {
    /**
     * Initialize logging configuration from logging.properties file in resources folder
     */
    static {
        try {
            Object o = FileValidator.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration((InputStream) o);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Could not load default logging configuration: file not found", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load default logging configuration: error reading file", e);
        }
    }
    private static final Logger LOGGER = Logger.getLogger(ValidationsHelper.class.getName());
    private Extensions extensions;
    private boolean failFast = false;
    private StringBuilder sharedStringBuilder = new StringBuilder();
    private String fileCategory;
    private byte[] originalFile;
    private String mimeString;
    private String fileExtension;
    private String commonLogString;
    private int responseMsgCountFail;
    private StringBuilder sbresponseAggregationFail;
    private int responseMsgCountSuccess;
    private StringBuilder sbresponseAggregationSuccess;


    /**
     * This is the constructor for the ValidationsHelper class.
     * @param extensions
     * @param fileCategory
     * @param originalFile
     * @param mimeString
     * @param outDir
     */
    public ValidationsHelper(Extensions extensions) {
        this.extensions = extensions;
    }

    /**
     * ValidationsHelper entry point for file validation
     * @param fileCategory (String) the file category of the file being validated
     * @param fileName (String) the file name of the file being validated
     * @param originalFile (byte[]) the byte of the file being validated
     * @param mimeString (String) the mime type of the file being validated
     * @return StringBuilder (StringBuilder) the results of the file validations
     * @throws IOException (IOException) if the file cannot be saved/deleted to/from a temporary directory
     * @throws SecurityException (SecurityException) if the accessed to file is denied for Files.probContentType
     */
    public StringBuilder getValidationResults(String fileCategory, String fileName, byte[] originalFile, String mimeString) throws IOException, SecurityException{
        this.fileExtension = getFileExtension(fileName);
        this.responseMsgCountFail = 0;
        this.sbresponseAggregationFail = new StringBuilder();
        this.responseMsgCountSuccess = 0;
        this.sbresponseAggregationSuccess = new StringBuilder();
        this.fileCategory = fileCategory;
        this .failFast = extensions.getValidationValue(fileCategory, fileExtension, "fail_fast") != null ?
            (boolean) extensions.getValidationValue(fileCategory, fileExtension, "fail_fast") : false;
        this.originalFile = originalFile;
        this.mimeString = mimeString;
        this.commonLogString = " for file: " + fileName;
        
        return doValidations();
    }

    /**
     * Following initial validations and before plugins, this method is used to execute the validations for the file.
     * @return StringBuilder (StringBuilder) the results of the file validations
     * @throws IOException (IOException) if the file cannot be saved/deleted to/from a temporary directory
     * @throws SecurityException (SecurityException) if the accessed to file is denied for Files.probContentType
     */
    private StringBuilder doValidations() throws IOException, SecurityException {
        
        checkFileSize();
        if (responseMsgCountFail > 0 && failFast) {
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        }

        checkMimeType();
        if (responseMsgCountFail > 0 && failFast) {
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        }

        containsMagicBytes();
        if (responseMsgCountFail > 0 && failFast) {
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        }
        
        containsHeaderSignatures();
        if (responseMsgCountFail > 0 && failFast) {
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        }

        containsFooterSignatures();
        if (responseMsgCountFail > 0 && failFast) {
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        }

        if (responseMsgCountFail > 0) {
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        } else {
            logFine(sbresponseAggregationSuccess);
            return sbresponseAggregationSuccess;
        }
    }


    ////////////////////
    // Helper methods //
    ////////////////////

    /**
     * LOGGER.warning wrapper
     * @param message (String) - message to log
     */
    private void logWarn(StringBuilder message) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning(message.toString());
        }
    }

    /**
     * LOGGER.fine wrapper
     * @param message (StringBuilder) - message to log
     */
    private void logFine(StringBuilder message) {
        if (LOGGER.isLoggable(Level.FINE )) {
            LOGGER.fine(message.toString());
        }
    }

    /**
     * isBlank wrapper method for support of Java 8
     * @param str (String) the string to check if empty or null
     * @return boolean (boolean) true if the string is empty or null, false otherwise
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Compare file size to the maximum allowed size
     * @return StringBuilder (StringBuilder) the results of the file size check
     */
    private StringBuilder checkFileSize() {
        int maxSize;
        try {
            maxSize = Integer.parseInt(extensions.getValidationValue(fileCategory, fileExtension, "max_size") != null ? extensions.getValidationValue(fileCategory, fileExtension, "max_size").toString() : "-1");
        } catch (NumberFormatException e) {
            maxSize = -1;
        }
        if ((maxSize > -1) && (originalFile.length / 1000 > maxSize || originalFile.length == 0)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid file size (")
                .append(originalFile.length / 1000)
                .append("KB) exceeds maximum allowed size (")
                .append(maxSize)
                .append("KB)")
                .append(commonLogString);
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("File size check passed, file size: ")
                .append(originalFile.length / 1000)
                .append("KB");
            logFine(sbresponseAggregationSuccess);
            return sbresponseAggregationSuccess;
        }
    }



    /**
     * Compare the file MIME type to the expected MIME type
     * @return StringBuilder (StringBuilder) the results of the MIME type check
     * @throws IOException (IOException) if the file cannot be saved/deleted to/from a temporary directory
     * @throws SecurityException (SecurityException) if the accessed to file is denied for Files.probContentType
     */
    private StringBuilder checkMimeType()throws IOException, SecurityException {
        String mimeType = (String) extensions.getValidationValue(fileCategory, fileExtension, "mime_type");
        String fileMimeType = isBlank(mimeString) ? "" : mimeString;
        if (isBlank(fileMimeType)) {
            Path tempFile = saveFileToTempDir(fileExtension, originalFile);
            if (tempFile == null) {
                sharedStringBuilder.replace(0, sharedStringBuilder.length(), "Error: checkMimeType failed: tempFile is null");
                logWarn(sharedStringBuilder);
                throw new IOException(sharedStringBuilder.toString());
            }
            try {
                fileMimeType = Files.probeContentType(tempFile);
            } catch (IOException e) {
                sharedStringBuilder.replace(0, sharedStringBuilder.length(), "Error: checkMimeType failed: ").append(e.getMessage());
                logWarn(sharedStringBuilder);
                throw new IOException(sharedStringBuilder.toString());
            } catch (SecurityException e) {
                sharedStringBuilder.replace(0, sharedStringBuilder.length(), "Error: checkMimeType failed: ").append(e.getMessage());
                logWarn(sharedStringBuilder);
                throw new SecurityException(sharedStringBuilder.toString());
            } finally {
                deleteTempDir(tempFile);
            }
        }
        if (!isBlank(mimeType) && !isBlank(fileMimeType) && !fileMimeType.equals(mimeType)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid mime_type")
                .append(commonLogString);
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("Mime type check passed, mime type: ")
                .append(mimeType);
            logFine(sbresponseAggregationSuccess);
            return sbresponseAggregationSuccess;
        }
    }
    
    /**
     * Parse the file extension from the file name
     * @param fileName (String) the name of the file being validated
     * @return String (String) the file extension of the file being validated
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    /**
     * Check if the file contains the expected magic bytes
     * @return StringBuilder (StringBuilder) the results of the magic bytes check
     */
    private StringBuilder containsMagicBytes() {
        String magicBytes = (String) extensions.getValidationValue(fileCategory, fileExtension, "magic_bytes");
        if (!isBlank(magicBytes) && !containsMagicBytesProcessor(originalFile, magicBytes)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid magic_bytes")
                .append(commonLogString);
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("Magic bytes check passed, magic bytes: ")
                .append(magicBytes);
            logFine(sbresponseAggregationSuccess);
            return sbresponseAggregationSuccess;
        }
    }
    
    /**
     * Check if the file contains the expected magic bytes
     * @param originalFileBytes (byte[]) the file bytes of the file being validated
     * @param magicBytesPattern (String) the expected magic bytes of the file being validated
     * @return Boolean (Boolean) true if the file contains the expected magic bytes, false otherwise
     */
    private boolean containsMagicBytesProcessor(byte[] originalFileBytes, String magicBytesPattern) {
        if (originalFileBytes.length == 0 || magicBytesPattern == null || magicBytesPattern.isEmpty()) {
            return false;
        }
        magicBytesPattern = magicBytesPattern.replaceAll("\\p{Zs}", "");
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

    /**
     * Check if the file contains the expected header signatures
     * @return StringBuilder (StringBuilder) the results of the header signatures check
     */
    private StringBuilder containsHeaderSignatures() {
        String headerSignatures = (String) extensions.getValidationValue(fileCategory, fileExtension, "header_signatures");
        if (!isBlank(headerSignatures) && !containsHeaderSignaturesProcessor(originalFile, headerSignatures)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid header_signatures")
                .append(commonLogString);
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("Header signatures check passed, header signatures: ")
                .append(headerSignatures);
            logFine(sbresponseAggregationSuccess);
            return sbresponseAggregationSuccess;
        }
    }

    /**
     * Check if the file contains the expected header signatures
     * @param fileBytes (byte[]) the file bytes of the file being validated
     * @param headerSignaturesPattern (String) the expected header signatures of the file being validated
     * @return Boolean (Boolean) true if the file contains the expected header signatures, false otherwise
     */
    private boolean containsHeaderSignaturesProcessor(byte[] fileBytes, String headerSignaturesPattern) {
        if (isBlank(headerSignaturesPattern)) {
            return true;
        }
        String hexPattern = headerSignaturesPattern.replaceAll("\\p{Zs}", "");
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

    /**
     * Check if the file contains the expected footer signatures
     * @return StringBuilder (StringBuilder) the results of the footer signatures check
     */
    private StringBuilder containsFooterSignatures() {
        String footerSignatures = (String) extensions.getValidationValue(fileCategory, fileExtension, "footer_signatures");
        if (!isBlank(footerSignatures) && !containsFooterSignaturesProcessor(originalFile, footerSignatures)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid footer_signatures")
                .append(commonLogString);
            logWarn(sbresponseAggregationFail);
            return sbresponseAggregationFail;
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("Footer signatures check passed, footer signatures: ")
                .append(footerSignatures);
            logFine(sbresponseAggregationSuccess);
            return sbresponseAggregationSuccess;
        }
    }

    /**
     * Check if the file contains the expected footer signatures
     * @param fileBytes (byte[]) the file bytes of the file being validated
     * @param footerSignaturesPattern (String) the expected footer signatures of the file being validated
     * @return Boolean (Boolean) true if the file contains the expected footer signatures, false otherwise
     */
    private boolean containsFooterSignaturesProcessor(byte[] fileBytes, String footerSignaturesPattern) {
        if (isBlank(footerSignaturesPattern)) {
            return true;
        }
        String hexPattern = footerSignaturesPattern.replaceAll("\\p{Zs}", "");
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

    /**
     * Helper method to save the file to a temporary directory
     * @param fileExtension (String) the file extension of the file being validated
     * @param originalFile (byte[]) the file bytes of the file being validated
     * @return Path (Path) the path to the temporary file
     */
    private Path saveFileToTempDir(String fileExtension, byte[] originalFile) {
        Path tempFilePath = null;
        try {
            // Create a temporary directory
            Path tempDir = Files.createTempDirectory("tempDir");
            tempFilePath = Files.createTempFile(tempDir, "tempFile", "." + fileExtension);
            Files.write(tempFilePath, originalFile);
        } catch (Exception e) {
            sharedStringBuilder.replace(0, sharedStringBuilder.length(), "Error: Saving file to temporary directory failed: ").append(e.getMessage());
            logWarn(sharedStringBuilder);
            return null;
        }
        return tempFilePath;
    }

    /**
     * Helper method to delete the temporary directory
     * @param tempFilePath (Path) the path to the temporary file
     * @return Boolean (Boolean) true if the temporary directory was deleted successfully, false otherwise
     */
    private Boolean deleteTempDir(Path tempFilePath) {
        try (Stream<Path> walk = Files.walk(tempFilePath)) {
            walk.sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
            return true;
        } catch (Exception e) {
            sharedStringBuilder.replace(0, sharedStringBuilder.length(), "Error: Delete temporary directoy failed: ").append(e.getMessage());
            logWarn(sharedStringBuilder);
            return false;
        }
    }

}
