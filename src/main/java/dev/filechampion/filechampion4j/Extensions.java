package dev.filechampion.filechampion4j;


import java.io.InputStream;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This class is used to load the Validations json objectand provide the validation values for a given extension.
 */
public class Extensions {
    static {
        try {
            Object o = Extensions.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration((InputStream) o);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Could not load default logging configuration: file not found", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load default logging configuration: error reading file", e);
        }
    }
    private static final Logger LOGGER = Logger.getLogger(Extensions.class.getName());
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
    private void logFine(String message) {
        if (LOGGER.isLoggable(Level.FINE )) {
            LOGGER.fine(message);
        }
    }

    private StringBuilder sbLogMessage = new StringBuilder();
    private Map<String, Map<String, Object>> categoriesMap;
    private Map<String, Map<String, Object>> extensionsMap;
    private Map<String, Object> validationsMap;
    private Map<String, Object> validationCache = new HashMap<>();
    private String sharedMessage1 = "Unsupported value type: ";
    private String sharedMessage2 = " for key: ";
    private List<String> allowedKeyValues = Arrays.asList("mime_type", "magic_bytes", "header_signatures", 
        "footer_signatures", "change_ownership", "change_ownership_user", "change_ownership_mode",
        "name_encoding", "max_size", "extension_plugins", "add_checksum", "fail_fast");
    private List<String> stringKeyValues = Arrays.asList("mime_type", "magic_bytes", "header_signatures", 
    "footer_signatures", "change_ownership_user", "change_ownership_mode",
    "max_size", "extension_plugins");
    private List<String> boolKeyValues = Arrays.asList("change_ownership", "name_encoding", "add_checksum", "fail_fast");

    /**
     * Constructor for Extensions class
     * @param jsonObject (JSONObject) - The json object containing the validations
     * @throws IllegalArgumentException if any of the json objects is invalid
     */
    public Extensions(JSONObject jsonObject) {

        categoriesMap = new HashMap<>();
        extensionsMap = new HashMap<>();
        validationsMap = new HashMap<>();

        if (jsonObject == null) {
            throw new IllegalArgumentException("jsonObject cannot be null");
        }

        for (String category : jsonObject.keySet()) {
            categoriesMap.put(category, jsonObject.getJSONObject(category).toMap());
            sbLogMessage.replace(0, sbLogMessage.length(), category).append(" ")
                .append(jsonObject.getJSONObject(category).toString());
            logFine(sbLogMessage.toString());
        }
        if( categoriesMap.isEmpty()) {
            throw new IllegalArgumentException("Validations must contain categories objects");
        }
        mapConfiguredExtensions();
    }

    /**
     * Load extensions from json object into extensionsMap
     */
    private void mapConfiguredExtensions(){
        for (Map.Entry<String, Object> extensionEntry : categoriesMap.entrySet().iterator().next().getValue().entrySet()) {
            String extension = extensionEntry.getKey();
            if (extensionEntry.getValue() instanceof HashMap) {
                extensionsMap.put(extensionEntry.getKey(), (HashMap) extensionEntry.getValue());
                sbLogMessage.replace(0, sbLogMessage.length(), extension).append(" ")
                    .append(extensionEntry.getValue().toString());
                logFine(sbLogMessage.toString());
            } else {
                sbLogMessage.replace(0, sbLogMessage.length(), sharedMessage1)
                    .append(extensionEntry.getValue().getClass().getName())
                    .append(sharedMessage2)
                    .append(extension);
                logWarn(sbLogMessage.toString());
                throw new IllegalArgumentException(sbLogMessage.toString());
            }
        }
        if (extensionsMap.entrySet().iterator().next().getValue().entrySet().isEmpty()) {
            sbLogMessage.replace(0, sbLogMessage.length(), "At least one validation must be configured");
            logWarn(sbLogMessage.toString());
            throw new IllegalArgumentException(sbLogMessage.toString());
        }
        mapExtensionValidations();
    }

    /**
     * Load extension validations into validationsMap
     */
    private void mapExtensionValidations () {
        for (Map.Entry<String, Object> validationEntry : extensionsMap.entrySet().iterator().next().getValue().entrySet() ) {
            String validation = validationEntry.getKey();
            Object value = validationEntry.getValue();
            if (allowedKeyValues.contains(validation)) {
                if (value instanceof String || value instanceof Integer) {
                    setString(validation, value);
                } else if (value instanceof Boolean) {
                    setBoolean(validation, value);
                } else if (value instanceof ArrayList ) {
                    setArrayList(validation, value);
                } else {
                    sbLogMessage.replace(0, sbLogMessage.length(),  sharedMessage1)
                            .append(value.getClass().getName())
                            .append(sharedMessage2)
                            .append(validation);
                    throw new IllegalArgumentException(sbLogMessage.toString());
                }
            } else {
                sbLogMessage.replace(0, sbLogMessage.length(), "Unsupported key: ")
                .append(validation);
                logWarn(sbLogMessage.toString());
                throw new IllegalArgumentException(sbLogMessage.toString());
            }
        }
        sbLogMessage.replace(0, sbLogMessage.length(), "Loaded ").append(categoriesMap.size()).append(" categories, ")
            .append(extensionsMap.size()).append(" extensions, ")
            .append(validationsMap.size()).append(" validations");
        logFine(sbLogMessage.toString());
        logInfo("Loaded Validations Configurtion");

    }

    /**
     * Sets the value of String validation key
     * @param validation (String) - The validation key
     * @param value (Object) - The value of the validation key
     */
    private void setString (String validation, Object value) {
        if (stringKeyValues.contains(validation)) {
            validationsMap.put(validation, value.toString());
        } else {
            sbLogMessage.replace(0, sbLogMessage.length(),  sharedMessage1)
                .append(value.getClass().getName())
                .append(sharedMessage2)
                .append(validation);
            logWarn(sbLogMessage.toString());
            throw new IllegalArgumentException(sbLogMessage.toString());
        }
    }

    /**
     * Sets the value of Boolean validation key
     * @param validation (String) - The validation key
     * @param value (Object) - The value of the validation key
     */
    private void setBoolean (String validation, Object value) {
        if (boolKeyValues.contains(validation)) {
            validationsMap.put(validation, (boolean) value);
        } else {
            sbLogMessage.replace(0, sbLogMessage.length(),  sharedMessage1)
                .append(value.getClass().getName())
                .append(sharedMessage2)
                .append(validation);
            logWarn(sbLogMessage.toString());
            throw new IllegalArgumentException(sbLogMessage.toString());
        }
    }

    /**
     * Sets the value of ArrayList validation key
     * @param validation (String) - The validation key
     * @param value (Object) - The value of the validation key
     */
    private void setArrayList (String validation, Object value) {
        if (validation.equals("extension_plugins")) {
            validationsMap.put(validation, (ArrayList) value);
        } else {
            sbLogMessage.replace(0, sbLogMessage.length(),  sharedMessage1)
                .append(value.getClass().getName())
                .append(sharedMessage2)
                .append(validation);
            logWarn(sbLogMessage.toString());
            throw new IllegalArgumentException(sbLogMessage.toString());
        }
    }

    /**
     * Checks input parameters of getValidationValue method
     * @param category (String) - The category of the extension
     * @param extension (String) - The extension to get the validation value for
     * @param validationKey (String) - The validation key to get the value for
     */
    private void checkInputs(String category, String extension, String validationKey) {
        if (!categoriesMap.containsKey(category)) {
            sbLogMessage.replace(0, sbLogMessage.length(), "category ").append(category)
                .append(" not found");
            logWarn(sbLogMessage.toString());
            throw new IllegalArgumentException(sbLogMessage.toString());
        }
        if (!categoriesMap.get(category).containsKey(extension)) {
            sbLogMessage.replace(0, sbLogMessage.length(), "extension ").append(extension)
                .append(" not found");
            logWarn(sbLogMessage.toString());
            throw new IllegalArgumentException(sbLogMessage.toString());
        }
        if (validationKey == null || validationKey.isEmpty()) {
            sbLogMessage.replace(0, sbLogMessage.length(), "validationKey cannot be null or empty");
            logWarn(sbLogMessage.toString());
            throw new IllegalArgumentException("validationKey cannot be null or empty");
        }
    }

    /**
     * Returns the validation value for a given extension
     * @param category (String) - The category of the extension
     * @param extension (String) - The file extension (without the dot)
     * @param validationKey (String) - The validation key
     * @return (Object) - The validation value or null if the validation key is not found/allowed
     * @throws IllegalArgumentException if any of the parameters are invalid
     */
    public Object getValidationValue(String category, String extension, String validationKey) {
        checkInputs(category, extension, validationKey);
        String cacheKey = category + "|" + extension + "|" + validationKey;
        if (validationCache.containsKey(cacheKey)) {
            sbLogMessage.replace(0, sbLogMessage.length(), "Returning ").append(validationKey)
                .append(" for ").append(extension).append(" (cached)");
            logFine(sbLogMessage.toString());
            return validationCache.get(cacheKey);
        }
        
        Object value = ((HashMap<?,?>) categoriesMap.get(category).get(extension)).get(validationKey);
        if (value != null) {
            validationCache.put(cacheKey, value);
            sbLogMessage.replace(0, sbLogMessage.length(), "Returning ").append(validationKey)
                .append(" for ").append(extension);
            logFine(sbLogMessage.toString());
            return value;
        } else {
            sbLogMessage.replace(0, sbLogMessage.length(), "Returning null for ")
                .append(validationKey).append(" for ").append(extension);
            logFine(sbLogMessage.toString());
            return null;
        }
    }
}