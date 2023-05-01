package dev.filechampion.filechampion4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.json.JSONObject;
import dev.filechampion.filechampion4j.PluginsHelper.PluginConfig;
import dev.filechampion.filechampion4j.PluginsHelper.StepConfig;
import java.security.MessageDigest;


/**
 * This class is used to validate files
 * @author filechampion
 * @version 0.9.8.2
 * @see <a href="https://github.com/povimd9/FileChampion4j/wiki">FileChampion4j Wiki</a>
 */
public class FileValidator {
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
    private static final Logger LOGGER = Logger.getLogger(FileValidator.class.getName());
    private JSONObject configJsonObject;
    private PluginsHelper pluginsHelper;
    private Map<String, StepConfig> stepConfigsBefore = new HashMap<>();
    private Map<String, StepConfig> stepConfigsAfter = new HashMap<>();
    private Extensions extensions;
    private StringBuilder sharedMessage = new StringBuilder();
    private String sharedStepMessage = "Step: ";
    private String errorResponse = "File is not valid.";
    private String fileCategory;
    private String fileName;
    private byte[] originalFile;
    private String mimeString;
    private Path outDir;
    private String fileExtension;
    private String commonFileError = "Error reading file: ";
    private String commonLogString;
    private String responseAggregationFail;
    private int responseMsgCountFail;
    private StringBuilder sbresponseAggregationFail;
    private String responseAggregationSuccess;
    private int responseMsgCountSuccess;
    private StringBuilder sbresponseAggregationSuccess;

    /**
     * This method is used to initiate the class with relevant json configurations
     * @param configJsonObject
     */
    public FileValidator(JSONObject configJsonObject) throws IllegalArgumentException {
        if (configJsonObject == null || configJsonObject.isEmpty() || !configJsonObject.has("Validations")) {
            throw new IllegalArgumentException("Config JSON object cannot be null or empty, and must have Validations section.");
        } else {
            try {
                extensions = new Extensions(configJsonObject.getJSONObject("Validations"));
            } catch (Exception e) {
                logWarn("Error initializing extensions: " + e.getMessage());
                throw new IllegalArgumentException("Error initializing extensions: " + e.getMessage());
            }
        }

        this.configJsonObject = configJsonObject;
        if (configJsonObject.has("Plugins")) {
            try {
                pluginsHelper = new PluginsHelper(configJsonObject.getJSONObject("Plugins"));
                loadPlugins();
                checkPluginsConfig();
            } catch (Exception e) {
                logWarn("Error initializing plugins: " + e.getMessage());
                throw new IllegalArgumentException("Error initializing plugins: " + e.getMessage());
            }
        }
    }

    /**
     * This method is used to validate the file bytes with existing mime type, typically present in web related file uploads, 
     * and save the file to the output directory if passed validations.
     * @param fileCategory (String) - The category of the file to be validated.  This is used to determine which validations to run.
     * @param originalFile (byte[]) - The file to be validated as a byte array.
     * @param fileName (String) - The original name of the file to be validated.
     * @param outputDir (Path) - The directory to save the file to if it passes validations.
     * @param mimeString (String) - The mime type of the file to be validated.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    public ValidationResponse validateFile(String fileCategory, byte[] originalFile, String fileName, Path outputDir,  String mimeString) {
        logFine("Validating with request mime " + mimeString  + " and storage for file bytes of: " + fileName);
        this.fileCategory = fileCategory;
        this.fileName = fileName;
        this.originalFile = originalFile;
        this.mimeString = mimeString;
        this.outDir = outputDir;
        return validateFileMain();
    }

    /**
     * This method is used to validate the file path with existing mime type, typically present in web related file uploads, 
     * and save the file to the output directory if passed validations.
     * @param fileCategory (String) - The category of the file to be validated.  This is used to determine which validations to run.
     * @param filePath (Path) - The target file path to be validated as a String.
     * @param fileName (String) - The original name of the file to be validated.
     * @param outputDir (Path) - The directory to save the file to if it passes validations.
     * @param mimeString (String) - The mime type of the file to be validated.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    public ValidationResponse validateFile(String fileCategory, Path filePath, String fileName, Path outputDir,  String mimeString) {
        logFine("Validating with request mime " + mimeString  + " and storage for file path of: " + fileName);
        this.fileCategory = fileCategory;
        this.fileName = fileName;
        this.mimeString = mimeString;
        this.outDir = outputDir;
        try {
            Path path = filePath;
            originalFile = Files.readAllBytes(path);
        } catch (IOException e) {
            logWarn(commonFileError + e.getMessage());
            throw new IllegalArgumentException(commonFileError + e.getMessage());
        }
        return validateFileMain();
    }
    
    /**
     * This method is used to validate the file as file bytes, with existing mime type, typically present in web related file uploads.
     * @param fileCategory (String) - The category of the file to be validated.  This is used to determine which validations to run.
     * @param originalFile (byte[]) - The file to be validated as a byte array.
     * @param fileName (String) - The original name of the file to be validated.
     * @param mimeString (String) - The mime type of the file to be validated.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    public ValidationResponse validateFile(String fileCategory, byte[] originalFile, String fileName, String mimeString) {
        logFine("Validating with request " + mimeString  + " mime for file bytes of: " + fileName);
        this.fileCategory = fileCategory;
        this.fileName = fileName;
        this.originalFile = originalFile;
        this.mimeString = mimeString;
        return validateFileMain();
    }

    /**
     * This method is used to validate the file as file bytes, save the file to the output directory if passed validations, and return the results.
     * @param fileCategory (String) - The category of the file to be validated.  This is used to determine which validations to run.
     * @param originalFile (byte[]) - The file to be validated as a byte array.
     * @param fileName (String) - The original name of the file to be validated.
     * @param outputDir (Path) - The directory to save the file to if it passes validations.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    public ValidationResponse validateFile(String fileCategory, byte[] originalFile, String fileName, Path outputDir) {
        logFine("Validating and Storing file bytes of: " + fileName);
        this.fileCategory = fileCategory;
        this.fileName = fileName;
        this.originalFile = originalFile;
        this.outDir = outputDir;
        return validateFileMain();
    }

    /**
     * This method is used to validate the file path with existing mime type, typically present in web related file uploads.
     * @param fileCategory (String) - The category of the file to be validated.  This is used to determine which validations to run.
     * @param filePath (Path) - The target file path to be validated as a String.
     * @param fileName (String) - The original name of the file to be validated.
     * @param mimeString (String) - The mime type of the file to be validated.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    public ValidationResponse validateFile(String fileCategory, Path filePath, String fileName,  String mimeString) {
        logFine("Validating with request mime " + mimeString  + " for file path of: " + fileName);
        this.fileCategory = fileCategory;
        this.fileName = fileName;
        this.mimeString = mimeString;
        try {
            Path path = filePath;
            originalFile = Files.readAllBytes(path);
        } catch (IOException e) {
            logWarn(commonFileError + e.getMessage());
            throw new IllegalArgumentException(commonFileError + e.getMessage());
        }
        return validateFileMain();
    }

    /**
     * This method is used to validate the file in target path, save the file to the output directory if passed validations, and return the results.
     * @param fileCategory (String) - The category of the file to be validated.  This is used to determine which validations to run.
     * @param filePath (Path) - The target file path to be validated as a String.
     * @param fileName (String) - The original name of the file to be validated.
     * @param outputDir (Path) - The directory to save the file to if it passes validations.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    public ValidationResponse validateFile(String fileCategory, Path filePath, String fileName, Path outputDir) {
        logFine("Validating and Storing file path of: " + fileName);
        this.fileCategory = fileCategory;
        this.fileName = fileName;
        this.outDir = outputDir;
        try {
            Path path = filePath;
            originalFile = Files.readAllBytes(path);
        } catch (IOException e) {
            logWarn(commonFileError + e.getMessage());
            throw new IllegalArgumentException(commonFileError + e.getMessage());
        }
        return validateFileMain();
    }

    /**
     * This method is used to validate the file as file bytes, and return the results.
     * @param fileCategory (String) - The category of the file to be validated.  This is used to determine which validations to run.
     * @param originalFile (byte[]) - The file to be validated as a byte array.
     * @param fileName (String) - The original name of the file to be validated.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    public ValidationResponse validateFile(String fileCategory, byte[] originalFile,String fileName) {
        logFine("Validating file bytes of: " + fileName);
        this.fileCategory = fileCategory;
        this.fileName = fileName;
        this.originalFile = originalFile;
        return validateFileMain();
    }

    /**
     * This method is used to validate the file in target path, and return the results.
     * @param fileCategory (String) - The category of the file to be validated.  This is used to determine which validations to run.
     * @param filePath (Path) - The target file path to be validated as a String.
     * @param fileName (String) - The original name of the file to be validated.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    public ValidationResponse validateFile(String fileCategory, Path filePath, String fileName) {
        logFine("Validating file path of: " + fileName);
        this.fileCategory = fileCategory;
        this.fileName = fileName;
        try {
            Path path = filePath;
            originalFile = Files.readAllBytes(path);
        } catch (IOException e) {
            logWarn(commonFileError + e.getMessage());
            throw new IllegalArgumentException(commonFileError + e.getMessage());
        }
        return validateFileMain();
    }

    /**
     * This method is the internal entry point for the file validation process.
     * @return (ValidationResponse) - The results of the validations.
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    private ValidationResponse validateFileMain() {
        commonLogString = String.format(" for file extension: %s", fileExtension);
        responseAggregationFail = "";
        responseMsgCountFail = 0;
        sbresponseAggregationFail = new StringBuilder(responseAggregationFail);
        responseAggregationSuccess = "";
        responseMsgCountSuccess = 0;
        sbresponseAggregationSuccess = new StringBuilder(responseAggregationSuccess);

        // Check that the input parameters are not null or empty
        checkMethodInputs(fileCategory, originalFile, fileName);

        // Initialize variables
        fileExtension = getFileExtension(fileName);
        String originalFilenameClean = fileName.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}.]", "_");

        // Log the file type category being validated
        sharedMessage.replace(0, sharedMessage.length(), "Validating ").append(originalFilenameClean).append(", as file type: ").append(fileCategory);
        logInfo(sharedMessage.toString());

        // Check for before plugins
        if (extensions.getValidationValue(fileCategory, fileExtension, "extension_plugins") != null) {
            String executionResults = executeBeforePlugins(fileCategory, fileExtension);
            if (executionResults.contains(". Failed for step:")) {
                sharedMessage.replace(0, sharedMessage.length(), "executeBeforePlugins failed for file: ").append(originalFilenameClean).append(", Results: ").append(executionResults);
                logWarn(sharedMessage.toString());
                return new ValidationResponse(false, errorResponse, sharedMessage.toString() , originalFilenameClean, null, null);
            } else if (executionResults.contains(". Error for step:")) {
                sharedMessage.replace(0, sharedMessage.length(), "Error executing Plugins defined to run before validations for file: ").append(originalFilenameClean).append(", Results: ").append(executionResults);
                sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                    .append("executeBeforePlugins passed with error: ")
                    .append(executionResults);
                    responseAggregationSuccess = sbresponseAggregationSuccess.toString();
                    logFine(responseAggregationSuccess);
                logWarn("Error in executeBeforePlugins: " + executionResults);
            } else {
                sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                    .append("executeBeforePlugins executed successfully: ")
                    .append(executionResults);
                    responseAggregationSuccess = sbresponseAggregationSuccess.toString();
                    logFine(responseAggregationSuccess);
                logInfo(executionResults);
            }
        }  else {
            sharedMessage.replace(0, sharedMessage.length(), "No before plugins defined for file: ").append(originalFilenameClean);
            logInfo(sharedMessage.toString());
        }
        return (doValidations(originalFilenameClean));
    }

    /**
     * Following initial validations and before plugins, this method is used to execute the validations for the file.
     * @param originalFilenameClean (String) a string containing the cleaned file name
     * @return ValidationResponse (ValidationResponse) a ValidationResponse object containing the results of the validation
     * @throws IllegalArgumentException - If any of the required inputs are null or empty.
     */
    private ValidationResponse doValidations(String originalFilenameClean) {
        // Check that the file size is not greater than the maximum allowed size, dont continue if it is
        int maxSize;
        try {
            maxSize = Integer.parseInt(extensions.getValidationValue(fileCategory, fileExtension, "max_size") != null ? extensions.getValidationValue(fileCategory, fileExtension, "max_size").toString() : "-1");
        } catch (NumberFormatException e) {
            maxSize = -1;
        }
        if (maxSize > -1 && Boolean.FALSE.equals(checkFileSize(originalFile.length, maxSize))) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("File size (")
                .append(originalFile.length / 1000)
                .append("KB) exceeds maximum allowed size (")
                .append(maxSize)
                .append("KB)")
                .append(commonLogString);
            responseAggregationFail = sbresponseAggregationFail.toString();
            logWarn(responseAggregationFail);
            return new ValidationResponse(false, errorResponse, responseAggregationFail, originalFilenameClean, originalFile, calculateChecksum(originalFile));
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("File size check passed, file size: ")
                .append(originalFile.length / 1000)
                .append("KB");
            responseAggregationSuccess = sbresponseAggregationSuccess.toString();
            logFine(responseAggregationSuccess);
        }

        // Check that the mime type is allowed
        String mimeType = (String) extensions.getValidationValue(fileCategory, fileExtension, "mime_type");
        if (!isBlank(mimeType) && !checkMimeType(originalFile, fileExtension, mimeType)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid mime_type")
                .append(commonLogString);
            responseAggregationFail = sbresponseAggregationFail.toString();
            logWarn(responseAggregationFail);
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("Mime type check passed, mime type: ")
                .append(mimeType);
            responseAggregationSuccess = sbresponseAggregationSuccess.toString();
            logFine(responseAggregationSuccess);
        }

        // Check that the file contains the magic bytes
        String magicBytes = (String) extensions.getValidationValue(fileCategory, fileExtension, "magic_bytes");
        if (!isBlank(magicBytes) && !containsMagicBytes(originalFile, magicBytes)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid magic_bytes")
                .append(commonLogString);
            responseAggregationFail = sbresponseAggregationFail.toString();
            logWarn(responseAggregationFail);
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("Magic bytes check passed, magic bytes: ")
                .append(magicBytes);
            responseAggregationSuccess = sbresponseAggregationSuccess.toString();
            logFine(responseAggregationSuccess);
        }

        // Check header signatures
        String headerSignatures = (String) extensions.getValidationValue(fileCategory, fileExtension, "header_signatures");
        if (!isBlank(headerSignatures) && !containsHeaderSignatures(originalFile, headerSignatures)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid header_signatures")
                .append(commonLogString);
            responseAggregationFail = sbresponseAggregationFail.toString();
            logWarn(responseAggregationFail);
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("Header signatures check passed, header signatures: ")
                .append(headerSignatures);
            responseAggregationSuccess = sbresponseAggregationSuccess.toString();
            logFine(responseAggregationSuccess);
        }

        // Check footer signatures
        String footerSignatures = (String) extensions.getValidationValue(fileCategory, fileExtension, "footer_signatures");
        if (!isBlank(footerSignatures) && !containsFooterSignatures(originalFile, footerSignatures)) {
            sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                .append("Invalid footer_signatures")
                .append(commonLogString);
            responseAggregationFail = sbresponseAggregationFail.toString();
            logWarn(responseAggregationFail);
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("Footer signatures check passed, footer signatures: ")
                .append(footerSignatures);
            responseAggregationSuccess = sbresponseAggregationSuccess.toString();
            logFine(responseAggregationSuccess);
        }

        
        // Check for after plugins
        if (extensions.getValidationValue(fileCategory, fileExtension, "extension_plugins") != null) {
            String executionResults = executeAfterPlugins(fileCategory, fileExtension);
            if (executionResults.contains(". Failed for step:")) {
                sbresponseAggregationFail.append(System.lineSeparator() + ++responseMsgCountFail + ". ")
                    .append("Error in executeAfterPlugins: ")
                    .append(executionResults)
                    .append(commonLogString);
                responseAggregationFail = sbresponseAggregationFail.toString();
                logWarn(responseAggregationFail);
            } else if (executionResults.contains(". Error for step:")) {
                sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                    .append("executeAfterPlugins passed with error: ")
                    .append(executionResults);
                responseAggregationSuccess = sbresponseAggregationSuccess.toString();
                logFine(responseAggregationSuccess);
                logWarn("Error in executeAfterPlugins: " + executionResults);
            } else {
                sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                    .append("executeAfterPlugins executed successfully: ")
                    .append(executionResults);
                responseAggregationSuccess = sbresponseAggregationSuccess.toString();
                logFine(responseAggregationSuccess);
            }
        } else {
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("No after plugins to execute");
            responseAggregationSuccess = sbresponseAggregationSuccess.toString();
            logFine(responseAggregationSuccess);
        }

        // Check if file passed all defined validations, return false and reason if not.
        if (responseMsgCountFail > 0) {
            logFine(responseAggregationFail);
            return new ValidationResponse(false, errorResponse, responseAggregationFail, originalFilenameClean, originalFile, calculateChecksum(originalFile));
        }

        // Check if the file name should be encoded
        String encodedFileName = "";
        boolean isNameEncoding = extensions.getValidationValue(fileCategory, fileExtension, "name_encoding") != null ? (boolean) extensions.getValidationValue(fileCategory, fileExtension, "name_encoding") : false;
        if (isNameEncoding) { 
            sharedMessage.replace(0, sharedMessage.length(), Base64.getEncoder().encodeToString(originalFilenameClean.getBytes(StandardCharsets.UTF_8))).append(".").append(fileExtension);
            encodedFileName = sharedMessage.toString();
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                    .append("File name: ")
                    .append(originalFilenameClean)
                    .append(" has been successfully encoded to: ")
                    .append(encodedFileName);
            responseAggregationSuccess = sbresponseAggregationSuccess.toString();
            logFine(responseAggregationSuccess);
        }
        String targetFileName = encodedFileName.isEmpty() ? originalFilenameClean : encodedFileName;

        // Check if the file should be saved to output directory
        String savedFilePath;
        if (outDir != null && !isBlank(outDir.toString())) {
            savedFilePath = saveFileToOutputDir(fileCategory, fileExtension, outDir, targetFileName, originalFile);
            if (savedFilePath.contains("Error:")) {
                // Return valid file response if file failed to save to output directory
                sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                    .append("File is valid but failed to save to output directory: ")
                    .append(savedFilePath);
                responseAggregationSuccess = sbresponseAggregationSuccess.toString();
                logInfo(responseAggregationSuccess);
                return new ValidationResponse(true, "File is valid but failed to save to output directory", responseAggregationSuccess, originalFilenameClean, originalFile, calculateChecksum(originalFile));
            }
            // Return valid file response if file was saved to output directory
            sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
                .append("File is valid and was saved to output directory: ")
                .append(savedFilePath);
            responseAggregationSuccess = sbresponseAggregationSuccess.toString();
            logInfo(responseAggregationSuccess);
            return new ValidationResponse(true, "File is valid and was saved to output directory", responseAggregationSuccess, originalFilenameClean, originalFile, calculateChecksum(originalFile), savedFilePath);
        }

        // Return valid response if file passed all validations but is not meant to be saved to disk
        sbresponseAggregationSuccess.append(System.lineSeparator() + ++responseMsgCountSuccess + ". ")
            .append("File is valid: ")
            .append(originalFilenameClean);
        responseAggregationSuccess = sbresponseAggregationSuccess.toString();
        logInfo(responseAggregationSuccess);
        return new ValidationResponse(true, "File is valid", responseAggregationSuccess, originalFilenameClean, originalFile, calculateChecksum(originalFile));
    }
    

    ////////////////////
    // Helper methods //
    ////////////////////

    /**
     * LOGGER.info wrapper
     * @param message (String) - message to log
     */
    private void logInfo(String message) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(message);
        }
    }

    /**
     * LOGGER.warning wrapper
     * @param message (String) - message to log
     */
    private void logWarn(String message) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning(message);
        }
    }

    /**
     * LOGGER.severe wrapper
     * @param message (String) - message to log
     */
    private void logSevere(String message) {
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe(message);
        }
    }

    /**
     * LOGGER.fine wrapper
     * @param message (String) - message to log
     */
    private void logFine(String message) {
        if (LOGGER.isLoggable(Level.FINE )) {
            LOGGER.fine(message);
        }
    }

    /**
     * Method to load the plugins objects into maps
     */
    private void loadPlugins() {
        for (PluginConfig pluginConfig : pluginsHelper.getPluginConfigs().values()) {
            for (String step : pluginConfig.getStepConfigs().keySet()) {
                StepConfig stepConfig = pluginConfig.getStepConfigs().get(step);
                stepConfigsBefore.put(stepConfig.isRunBefore()? step : "", stepConfig.isRunBefore()? stepConfig : null);
                stepConfigsAfter.put(stepConfig.isRunAfter()? step : "", stepConfig.isRunAfter()? stepConfig : null);
            }
        }
    }

    /**
     * Method to check that all extensions defined in config exist in plugins configuration from Validation.json
     */
    private void checkPluginsConfig() {
        // check that all extensions defined plugins exist in maps
        JSONObject validationsJsonObject = configJsonObject.getJSONObject("Validations");
        for (int i = 0; i < validationsJsonObject.length(); i++) {
            String categroyKey = validationsJsonObject.names().getString(i);
            for (int k=0; k < validationsJsonObject.getJSONObject(categroyKey).length(); k++) {
                String extensionKey = validationsJsonObject.getJSONObject(categroyKey).names().getString(k);
                if (validationsJsonObject.getJSONObject(categroyKey).getJSONObject(extensionKey).has("extension_plugins")) {
                    checkPluginsExist(validationsJsonObject, categroyKey, extensionKey);
                }
            }
        }
    }

    /**
     * Method to check that each extension defined in config exist in plugins configuration
     * @param validationsJsonObject (JSONObject) - the validations.json object
     * @param categroyKey (String) - the category key
     * @param extensionKey (String) - the extension key
     */
    private void checkPluginsExist(JSONObject validationsJsonObject, String categroyKey, String extensionKey){
        for (String pluginName : validationsJsonObject.getJSONObject(categroyKey).getJSONObject(extensionKey).getJSONArray("extension_plugins").toList().toArray(new String[0])) {
            if (!stepConfigsBefore.containsKey(pluginName) && !stepConfigsAfter.containsKey(pluginName)) {
                sharedMessage.replace(0, sharedMessage.length(), sharedStepMessage).append(pluginName).append(" defined in config does not exist in plugins configuration");
                logWarn(sharedMessage.toString());
                throw new IllegalArgumentException(sharedMessage.toString());
            }
        }
    }

    /**
     * This method is used to check that method inputs are not null or empty
     * @param fileCategory (String) the file category of the file being validated
     * @param originalFile (byte[]) the file being validated
     * @param fileName (String) the file name of the file being validated
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
        this.originalFile = originalFile;
    }

    /**
     * Execute and check results of plugins configured to run before the validations
     * @param fileCategory (String) the file category of the file being validated
     * @param fileExtension (String) the file extension of the file being validated
     * @return String (String) a string containing the results of the plugin execution
     */
    private String executeBeforePlugins(String fileCategory, String fileExtension) {
        String responseAggregation = "";
        char responseMsgCount = 'a';
        StringBuilder sbResponseAggregation = new StringBuilder(responseAggregation);
        

        ArrayList plugins = (ArrayList) extensions.getValidationValue(fileCategory, fileExtension, "extension_plugins");
        for (int i = 0; i < plugins.size(); i++) {
            String extensionPlugin = (String) plugins.get(i);
            for (String step : stepConfigsBefore.keySet()) {
                if (step.equals(extensionPlugin)) {
                    String stepResults = executePlugin(extensionPlugin, stepConfigsBefore, fileExtension);
                    sharedMessage.replace(0, sharedMessage.length(), sharedStepMessage)
                        .append(stepConfigsBefore.get(extensionPlugin).getName()).append(" Success, Results: Error");
                    String sharedString = ", Results: ";
                    if (stepResults.startsWith(sharedMessage.toString()) || stepResults.startsWith("Error ")) {
                        if (stepConfigsBefore.get(extensionPlugin).getOnFail().equals("fail")) {
                            sbResponseAggregation.append(System.lineSeparator()).append("\t")  .append(responseMsgCount + ". ")
                            .append("Failed for step: ")
                            .append(stepConfigsBefore.get(extensionPlugin).getName())
                            .append(sharedString + stepResults);
                            logFine(stepResults);        
                            return sbResponseAggregation.toString();
                        }
                        sbResponseAggregation.append(System.lineSeparator()).append("\t")  .append(responseMsgCount + ". ")
                            .append("Error for step: ")
                            .append(stepConfigsBefore.get(extensionPlugin).getName())
                            .append(sharedString + stepResults);
                            logFine(stepResults);
                        ++responseMsgCount;
                        responseAggregation = sbResponseAggregation.toString();
                    } else {
                        sbResponseAggregation.append(System.lineSeparator()).append("\t")  .append(responseMsgCount + ". ")
                            .append("Success for step: ")
                            .append(stepConfigsBefore.get(extensionPlugin).getName());
                            logFine(stepResults);
                        ++responseMsgCount;
                        responseAggregation = sbResponseAggregation.toString();
                    }
                }
            }
        }
        return "executeBeforePlugins completed: " + responseAggregation;
    }

    /**
     * Execute and check results of plugins configured to run after the validations
     * @param fileCategory (String) the file category of the file being validated
     * @param fileExtension (String) the file extension of the file being validated
     * @return String (String) a string containing the results of the plugin execution
     */
    private String executeAfterPlugins(String fileCategory, String fileExtension) {
        String responseAggregation = "";
        char responseMsgCount = 'a';
        StringBuilder sbResponseAggregation = new StringBuilder(responseAggregation);
        
        ArrayList plugins = (ArrayList) extensions.getValidationValue(fileCategory, fileExtension, "extension_plugins");
        for (int i = 0; i < plugins.size(); i++) {
            String extensionPlugin = (String) plugins.get(i);
            for (String step : stepConfigsAfter.keySet()) {
                if (step.equals(extensionPlugin)) {
                    String stepResults = executePlugin(extensionPlugin, stepConfigsAfter, fileExtension);
                    sharedMessage.replace(0, sharedMessage.length(), sharedStepMessage)
                        .append(stepConfigsAfter.get(extensionPlugin).getName()).append(" Success, Results: Error");
                    String sharedString = ", Results: ";
                    if (stepResults.startsWith(sharedMessage.toString()) || stepResults.startsWith("Error ")) {
                        if (stepConfigsAfter.get(extensionPlugin).getOnFail().equals("fail")) {
                            sbResponseAggregation.append(System.lineSeparator()).append("\t")  .append(responseMsgCount + ". ")
                            .append("Failed for step: ")
                            .append(stepConfigsAfter.get(extensionPlugin).getName())
                            .append(sharedString + stepResults);
                            logFine(stepResults);    
                            return sbResponseAggregation.toString();
                        }
                        sbResponseAggregation.append(System.lineSeparator()).append("\t")  .append(responseMsgCount + ". ")
                            .append("Error for step: ")
                            .append(stepConfigsAfter.get(extensionPlugin).getName())
                            .append(sharedString + stepResults);
                            logFine(stepResults);
                        ++responseMsgCount;
                        responseAggregation = sbResponseAggregation.toString();
                    } else {
                        sbResponseAggregation.append(System.lineSeparator()).append("\t")  .append(responseMsgCount + ". ")
                            .append("Success for step: ")
                            .append(stepConfigsAfter.get(extensionPlugin).getName());
                            logFine(stepResults);
                        ++responseMsgCount;
                        responseAggregation = sbResponseAggregation.toString();
                    }
                }
            }
        }
        return "executeAfterPlugins completed: " + responseAggregation;
    }

    /**
     * Execute a single plugin step
     * @param extensionPlugin (String) a string containing the name of the plugin step to execute
     * @param stepConfigs (Map<String, StepConfig>) a map containing the configuration for the step to execute
     * @param fileExtension (String) the file extension of the file being validated
     * @return String (String) a string containing the results of the plugin execution
     */
    private String executePlugin(String extensionPlugin, Map<String, StepConfig> stepConfigs , String fileExtension) {
        Map<String, String> stepResultsMap = new HashMap<>();
        String extensionPluginName = stepConfigs.get(extensionPlugin).getName();
        sharedMessage.replace(0, sharedMessage.length(), sharedStepMessage).append(extensionPluginName);
        logFine(sharedMessage.toString());
        
        if (stepConfigs.get(extensionPlugin).getType().equals("cli")) {
            Map<String, Map<String, String>> stepResults = stepConfigs.get(extensionPlugin)
                .getCliPluginHelper()
                .execute(fileExtension, originalFile);    
            stepResultsMap.putAll(stepResults.get(stepResults.keySet().toArray()[0]));

            if (!stepResultsMap.isEmpty() && stepResults.containsKey("Success")) {
                String newFilePath = stepResultsMap.get(extensionPluginName.substring(extensionPluginName.lastIndexOf(".")+1,
                extensionPluginName.length()) + ".filePath");
                String newB64Content = stepResultsMap.get(extensionPluginName.substring(extensionPluginName.lastIndexOf(".")+1,
                extensionPluginName.length()) + ".fileContent");

                if (!isBlank(newFilePath)) {
                    try {
                        Path newFile = new File(newFilePath).toPath();
                        originalFile = Files.readAllBytes(newFile);
                        deleteTempDir(newFile.getParent().toAbsolutePath());
                        logFine(String.format("Successfully read plugin expected file: %s", newFilePath));
                    } catch (IOException e) {
                        sharedMessage.replace(0, sharedMessage.length(), "Error reading plugin expected file: ").append(e.getMessage());
                        logWarn(String.format(sharedMessage.toString()));
                        return sharedMessage.toString();
                    }
                }
                if (!isBlank(newB64Content)) {
                    try {
                        originalFile = Base64.getDecoder().decode(newB64Content);
                        logFine("Successfully decoded plugin expected file");
                    } catch (Exception e) {
                        sharedMessage.replace(0, sharedMessage.length(), "Error decoding plugin expected file: ").append(e.getMessage());
                        logWarn(String.format(sharedMessage.toString()));
                        return sharedMessage.toString();
                    }
                }
            }
            for(Map.Entry<String, String> entry : stepResultsMap.entrySet()) {
                String errorMsg =  entry.getValue();
                String errorDetails = entry.getKey();
                sharedMessage.replace(0, sharedMessage.length(), sharedStepMessage).append(extensionPluginName)
                    .append(" Success, Results: ")
                    .append(errorDetails)
                    .append("\"")
                    .append(errorMsg);
                logFine(sharedMessage.toString());
            }
            return sharedMessage.toString();
        } else if (stepConfigs.get(extensionPlugin).getType().equals("http")) {
            // TODO: Implement http plugin type
        }
        return sharedMessage.toString();
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
     * @param originalFileSize (int) the size of the file being validated
     * @param extensionMaxSize (int) the maximum allowed size of the file being validated
     * @return Boolean (Boolean) true if the file size is less than or equal to the maximum allowed size, false otherwise
     */
    private Boolean checkFileSize(int originalFileSize, int extensionMaxSize) {
        return (extensionMaxSize == 0) || (originalFileSize / 1000) <= extensionMaxSize;
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
            sharedMessage.replace(0, sharedMessage.length(), "Error: Saving file to temporary directory failed: ").append(e.getMessage());
            logWarn(sharedMessage.toString());
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
            sharedMessage.replace(0, sharedMessage.length(), "Error: Delete temporary directoy failed: ").append(e.getMessage());
            logWarn(sharedMessage.toString());
            return false;
        }
    }

    /**
     * Compare the file MIME type to the expected MIME type
     * @param originalFileBytes (byte[]) the file bytes of the file being validated
     * @param fileExtension (String) the file extension of the file being validated
     * @param mimeType (String) the expected MIME type of the file being validated
     * @return Boolean (Boolean) true if the file MIME type matches the expected MIME type, false otherwise
     */
    private boolean checkMimeType(byte[] originalFileBytes, String fileExtension, String mimeType) {
        String fileMimeType = isBlank(mimeString) ? "" : mimeString;
        if (isBlank(fileMimeType)) {
            Path tempFile = saveFileToTempDir(fileExtension, originalFileBytes);
            if (tempFile == null) {
                logWarn("checkMimeType failed: tempFile is null");
                return false;
            }
            try {
                fileMimeType = Files.probeContentType(tempFile);
            } catch (Exception e) {
                logWarn("checkMimeType failed: " + e.getMessage());
            } finally {
                deleteTempDir(tempFile);
            }
        }
        return !isBlank(fileMimeType) && fileMimeType.equals(mimeType);
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
     * @param originalFileBytes (byte[]) the file bytes of the file being validated
     * @param magicBytesPattern (String) the expected magic bytes of the file being validated
     * @return Boolean (Boolean) true if the file contains the expected magic bytes, false otherwise
     */
    private boolean containsMagicBytes(byte[] originalFileBytes, String magicBytesPattern) {
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
     * @param fileBytes (byte[]) the file bytes of the file being validated
     * @param headerSignaturesPattern (String) the expected header signatures of the file being validated
     * @return Boolean (Boolean) true if the file contains the expected header signatures, false otherwise
     */
    private boolean containsHeaderSignatures(byte[] fileBytes, String headerSignaturesPattern) {
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
     * @param fileBytes (byte[]) the file bytes of the file being validated
     * @param footerSignaturesPattern (String) the expected footer signatures of the file being validated
     * @return Boolean (Boolean) true if the file contains the expected footer signatures, false otherwise
     */
    private boolean containsFooterSignatures(byte[] fileBytes, String footerSignaturesPattern) {
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
     * Calculate the checksum of the file
     * @param fileBytes (byte[]) the file bytes of the file being validated
     * @return String (String) the SHA-256 checksum of the file
     */
    private String calculateChecksum(byte[] fileBytes) {
        try {
            SH256Calculate paralChecksum = new SH256Calculate(fileBytes);
            byte[] checksum = paralChecksum.getChecksum();
            return new BigInteger(1, checksum).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper method to save the file defined file attributes to the output directory and return the path to the saved file
     * @param fileCategory (String) the file category of the file being validated
     * @param fileExtension (String) the file extension of the file being validated
     * @param outDir (Path) the path to the output directory
     * @param fileName (String) the name of the file being validated
     * @param fileBytes (byte[]) the file bytes of the file being validated
     * @return String (String) the path to the saved file
     */
    private String saveFileToOutputDir(String fileCategory, String fileExtension, Path outDir, String fileName, byte[] fileBytes) {
        Path targetFilePath = Paths.get(outDir.toString(), fileName);
        try {
            Files.write(targetFilePath, fileBytes, StandardOpenOption.CREATE);
        } catch (IOException e) {
            sharedMessage.replace(0, sharedMessage.length(), "Error: Saving file to directory failed: ").append(e.getMessage());
            logSevere(sharedMessage.toString());
            return sharedMessage.toString();
        }
        String changeOwnershipUser = (String) extensions.getValidationValue(fileCategory, fileExtension, "change_ownership_user");
        String changePermissionsMode = (String) extensions.getValidationValue(fileCategory, fileExtension, "change_ownership_mode");

        FileAclHelper fileAclHelper = new FileAclHelper(targetFilePath, changeOwnershipUser, changePermissionsMode);
        String newFileAttributesStatus = fileAclHelper.changeFileAcl();
        if (newFileAttributesStatus.contains("Error:")) {
            try {
                Files.deleteIfExists(targetFilePath);
            } catch (IOException e) { 
                logSevere("Error: Failed to delete file from permissions change operation: " + e.getMessage());
            }
            logSevere(newFileAttributesStatus);
            return newFileAttributesStatus;
        }
        return targetFilePath.toAbsolutePath().toString();
    }
}
