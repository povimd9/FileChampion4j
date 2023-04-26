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
 * TODO: add extension config loading at init
 */
public class FileValidator {
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
    private void logInfo(String message) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(message);
        }
    }
    private void logWarn(String message) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning(message);
        }
    }
    private void logSevere(String message) {
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe(message);
        }
    }
    private void logFine(String message) {
        if (LOGGER.isLoggable(Level.FINE )) {
            LOGGER.fine(message);
        }
    }

    private final JSONObject configJsonObject;
    private PluginsHelper pluginsHelper;
    private Map<String, StepConfig> stepConfigsBefore = new HashMap<>();
    private Map<String, StepConfig> stepConfigsAfter = new HashMap<>();
    private Extensions extensions;
    private byte[] originalFile;
    private String fileChecksum;
    private StringBuilder sharedMessage = new StringBuilder();
    private static final String SHARED_STEP_MESSAGE = "Step: ";



    /**
     * This method is used to get the json configurations
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

    private void loadPlugins() {
        for (PluginConfig pluginConfig : pluginsHelper.getPluginConfigs().values()) {
            for (String step : pluginConfig.getStepConfigs().keySet()) {
                StepConfig stepConfig = pluginConfig.getStepConfigs().get(step);
                stepConfigsBefore.put(stepConfig.isRunBefore()? step : "", stepConfig.isRunBefore()? stepConfig : null);
                stepConfigsAfter.put(stepConfig.isRunAfter()? step : "", stepConfig.isRunAfter()? stepConfig : null);
            }
        }
    }

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

    private void checkPluginsExist(JSONObject validationsJsonObject, String categroyKey, String extensionKey){
        for (String pluginName : validationsJsonObject.getJSONObject(categroyKey).getJSONObject(extensionKey).getJSONArray("extension_plugins").toList().toArray(new String[0])) {
            if (!stepConfigsBefore.containsKey(pluginName) && !stepConfigsAfter.containsKey(pluginName)) {
                sharedMessage.replace(0, sharedMessage.length(), SHARED_STEP_MESSAGE).append(pluginName).append(" defined in config does not exist in plugins configuration");
                logWarn(sharedMessage.toString());
                throw new IllegalArgumentException(sharedMessage.toString());
            }
        }
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
        this.originalFile = originalFile;
        this.fileChecksum = "";
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
        String fileExtension = getFileExtension(fileName);
        String originalFilenameClean = fileName.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}.]", "_");
        fileChecksum = calculateChecksum(originalFile);

        // Log the file type category being validated
        sharedMessage.replace(0, sharedMessage.length(), "Validating ").append(originalFilenameClean).append(", as file type: ").append(fileCategory);
        logInfo(sharedMessage.toString());

        // Check for before plugins
        if (extensions.getValidationValue(fileCategory, fileExtension, "extension_plugins") != null) {
            String executionResults = executeBeforePlugins(fileCategory, fileExtension);
            if (executionResults.contains(". Failed for step:")) {
                sharedMessage.replace(0, sharedMessage.length(), "executeBeforePlugins failed for file: ").append(originalFilenameClean).append(", Results: ").append(executionResults);
                logWarn(sharedMessage.toString());
                return new ValidationResponse(false, sharedMessage.toString() , originalFilenameClean, null, null);
            } else if (executionResults.substring(0, 12).contains(". Error for step:")) {
                sharedMessage.replace(0, sharedMessage.length(), "Error executing Plugins defined to run before validations for file: ").append(originalFilenameClean).append(", Results: ").append(executionResults);
                logWarn(sharedMessage.toString());
            } else {
                logInfo(executionResults);
            }
        }  else {
            sharedMessage.replace(0, sharedMessage.length(), "No before plugins defined for file: ").append(originalFilenameClean);
            logInfo(sharedMessage.toString());
        }
        
        return (doValidations(fileCategory, originalFilenameClean, fileExtension, originalFile, fileChecksum, outDir));
    }

    /**
     * If file category was found in the config, this method is used to validate the file
     * @param fileCategory (String) a string containing the file type category to validate the file against
     * @param originalFilenameClean (String) a string containing the cleaned file name
     * @param fileExtension (String) a string containing the file extension
     * @param originalFile (byte[]) a byte array containing the file bytes of the file to be validated
     * @param fileChecksum (String) a string containing the checksum of the file to be validated
     * @param outDir (String) a string containing the path to the output directory for validated files
     * @return ValidationResponse (ValidationResponse) a ValidationResponse object containing the results of the validation
     */
    private ValidationResponse doValidations(String fileCategory, String originalFilenameClean, String fileExtension, byte[] originalFile, String fileChecksum, String outDir) {
        String commonLogString = String.format(" for file extension: %s", fileExtension);
        String responseAggregationFail = "";
        int responseMsgCountFail = 0;
        StringBuilder sbresponseAggregationFail = new StringBuilder(responseAggregationFail);
        String responseAggregationSuccess = "";
        int responseMsgCountSuccess = 0;
        StringBuilder sbresponseAggregationSuccess = new StringBuilder(responseAggregationSuccess);

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
                .append((String) extensions.getValidationValue(fileCategory, fileExtension, "max_size"))
                .append("KB)")
                .append(commonLogString);
            responseAggregationFail = sbresponseAggregationFail.toString();
            logWarn(responseAggregationFail);
            return new ValidationResponse(false, responseAggregationFail, originalFilenameClean, originalFile, fileChecksum);
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

        // Check header signatures (optional)
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

        // Check footer signatures (optional)
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
            return new ValidationResponse(false, responseAggregationFail, originalFilenameClean, originalFile, fileChecksum);
        } else {
            logInfo(responseAggregationSuccess);
        }

        // Check if the file name should be encoded
        String encodedFileName = "";
        boolean isNameEncoding = extensions.getValidationValue(fileCategory, fileExtension, "name_encoding") != null ? (boolean) extensions.getValidationValue(fileCategory, fileExtension, "name_encoding") : false;
        if (isNameEncoding) { 
            sharedMessage.replace(0, sharedMessage.length(), Base64.getEncoder().encodeToString(originalFilenameClean.getBytes(StandardCharsets.UTF_8))).append(".").append(fileExtension);
            encodedFileName = sharedMessage.toString();
            String encodingStatus = String.format("File name: '%s' has been successfully encoded to: '%s'", originalFilenameClean, encodedFileName);
            logInfo(encodingStatus);
        }
        
        String targetFileName = encodedFileName.isEmpty() ? originalFilenameClean : encodedFileName;

        // Check if the file should be saved to output directory
        String savedFilePath;
        if (!isBlank(outDir)) {
            savedFilePath = saveFileToOutputDir(fileCategory, fileExtension, outDir, targetFileName, originalFile);
            if (savedFilePath.contains("Error:")) {
                // Return valid file response if file failed to save to output directory
                sharedMessage.replace(0, sharedMessage.length(), "File is valid but failed to save to output directory: ").append(savedFilePath);
                return new ValidationResponse(true, sharedMessage.toString(), originalFilenameClean, originalFile, fileChecksum);
            }
            // Return valid file response if file was saved to output directory
            sharedMessage.replace(0, sharedMessage.length(), "File is valid and was saved to output directory: ").append(savedFilePath);
            return new ValidationResponse(true, sharedMessage.toString() , originalFilenameClean, originalFile, fileChecksum, savedFilePath);
        }

        // Return valid response if file passed all validations but is not meant to be saved to disk
        sharedMessage.replace(0, sharedMessage.length(), "File is valid: ").append(originalFilenameClean);
        String validMessage = sharedMessage.toString();
        logInfo(validMessage);
        return new ValidationResponse(true, validMessage, originalFilenameClean, originalFile, fileChecksum);
    }
    

    ////////////////////
    // Helper methods //
    ////////////////////


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
                    sharedMessage.replace(0, sharedMessage.length(), SHARED_STEP_MESSAGE)
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
                    sharedMessage.replace(0, sharedMessage.length(), SHARED_STEP_MESSAGE)
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
        sharedMessage.replace(0, sharedMessage.length(), SHARED_STEP_MESSAGE).append(extensionPluginName);
        logFine(sharedMessage.toString());
        
        if (stepConfigs.get(extensionPlugin).getType().equals("cli")) {
            Map<String, Map<String, String>> stepResults = stepConfigs.get(extensionPlugin)
                .getCliPluginHelper()
                .execute(fileExtension, originalFile, fileChecksum);    
            stepResultsMap.putAll(stepResults.get(stepResults.keySet().toArray()[0]));

            if (!stepResultsMap.isEmpty() && stepResults.containsKey("Success")) {
                String newFilePath = stepResultsMap.get(extensionPluginName.substring(extensionPluginName.lastIndexOf(".")+1,
                extensionPluginName.length()) + ".filePath");
                String newB64Content = stepResultsMap.get(extensionPluginName.substring(extensionPluginName.lastIndexOf(".")+1,
                extensionPluginName.length()) + ".fileContent");

                if (!isBlank(newFilePath)) {
                    try {
                        Path newFile = new File(stepResultsMap.get(extensionPluginName.substring(extensionPluginName.lastIndexOf(".")+1, 
                        extensionPluginName.length()) + ".filePath")).toPath();
                        originalFile = Files.readAllBytes(newFile);
                        fileChecksum = calculateChecksum(originalFile);
                        deleteTempDir(newFile.getParent().toAbsolutePath());
                    } catch (IOException e) {
                        sharedMessage.replace(0, sharedMessage.length(), "Error reading plugin expected file: ").append(e.getMessage());
                        logWarn(String.format(sharedMessage.toString()));
                        return sharedMessage.toString();
                    }
                }
                if (!isBlank(newB64Content)) {
                    try {
                        originalFile = Base64.getDecoder().decode(newB64Content);
                    } catch (Exception e) {
                        sharedMessage.replace(0, sharedMessage.length(), "Error decoding plugin expected file: ").append(e.getMessage());
                        logWarn(String.format(sharedMessage.toString()));
                        return sharedMessage.toString();
                    }
                    fileChecksum = calculateChecksum(originalFile);
                }
            }
            for(Map.Entry<String, String> entry : stepResultsMap.entrySet()) {
                String errorMsg =  entry.getValue();
                String errorDetails = entry.getKey();
                sharedMessage.replace(0, sharedMessage.length(), SHARED_STEP_MESSAGE).append(extensionPluginName)
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
            sharedMessage.replace(0, sharedMessage.length(), "Error: Saving file to temporary directory failed: ").append(e.getMessage());
            logWarn(sharedMessage.toString());
            return null;
        }
        return tempFilePath;
    }

    // Explicitley delete the temporary directory and file
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
            logWarn("checkMimeType failed: " + e.getMessage());
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

    // Check if the file contains the header signatures
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

    // Check if the file contains the footer signatures
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

    // Calculate file checksum
    private static String calculateChecksum(byte[] fileBytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(fileBytes);
            return new BigInteger(1, hash).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }   

    // save the file to the output directory with appropriate owner and permissions
    private String saveFileToOutputDir(String fileCategory, String fileExtension, String outDir, String fileName, byte[] fileBytes) {
        Path targetFilePath = Paths.get(outDir, fileName);
        try {
            Files.write(targetFilePath, fileBytes, StandardOpenOption.CREATE);
        } catch (IOException e) {
            sharedMessage.replace(0, sharedMessage.length(), "Error: Saving file to directory failed: ").append(e.getMessage());
            logSevere(sharedMessage.toString());
            return sharedMessage.toString();
        }
        // Try to set the file owner and permissions


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
        // Return the full path to the saved file since no errors were encountered
        return targetFilePath.toAbsolutePath().toString();
    }
}
