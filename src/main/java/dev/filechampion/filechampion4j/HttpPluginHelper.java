package dev.filechampion.filechampion4j;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import dev.filechampion.filechampion4j.PluginsHelper.StepConfig;

/**
 * HttpPluginHelper provides methods to execute HTTP requests defined in plugins.
 */
public class HttpPluginHelper {
    private static final Logger LOGGER = Logger.getLogger(HttpPluginHelper.class.getName());
    private void logFine(String message) {
        if (LOGGER.isLoggable(Level.FINE )) {
            LOGGER.fine(message);
        }
    }
    private void logWarn(String message) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning(message);
        }
    }
    private CredentialsManager credentialsCacher;
    private StepConfig singleStepConfig;
    private String httpMethod;
    private StringBuilder logMessage = new StringBuilder();


    /**
     * Constructor for HttpPluginHelper class without credentials
     * @param singleStepConfig (StepConfig) - the plugin step configuration
     */
    public HttpPluginHelper(StepConfig singleStepConfig) {
        this.singleStepConfig = singleStepConfig;
        this.httpMethod = singleStepConfig.getMethod();
        logMessage.append("HttpPluginHelper initialized");
        logFine(logMessage.toString());
    }

    /**
     * Constructor for HttpPluginHelper class with credentials
     * @param singleStepConfig (StepConfig) - the plugin step configuration
     */
    public HttpPluginHelper(StepConfig singleStepConfig, CredentialsManager credentialsCacher) {
        this.singleStepConfig = singleStepConfig;
        this.httpMethod = singleStepConfig.getMethod();
        this.credentialsCacher = credentialsCacher;
        logMessage.append("HttpPluginHelper initialized");
        logFine(logMessage.toString());
    }
    
    /**
     * Method to execute HTTP plugin request
     * @param fileExtension (String) - the file extension
     * @param fileContent (byte[]) - the file content
     * @return Map&lt;String, Map&lt;String, String&gt;&gt; - the response map
     */
    public Map<String, Map<String, String>> execute(String fileExtension, byte[] fileContent) throws IllegalArgumentException {
        switch (this.httpMethod) {
            case "GET":
                HttpGetPluginHelper httpGetPluginHelper = credentialsCacher!=null ? new HttpGetPluginHelper(singleStepConfig, credentialsCacher) : new HttpGetPluginHelper(singleStepConfig);
                return httpGetPluginHelper.execute(fileExtension, fileContent);
            case "POST":
                HttpPostPluginHelper httpPostPluginHelper = credentialsCacher!=null ? new HttpPostPluginHelper(singleStepConfig, credentialsCacher) : new HttpPostPluginHelper(singleStepConfig);
                return httpPostPluginHelper.execute(fileExtension, fileContent);
            case "MULTIPART":
                HttpMultipartPluginHelper httpMultipartPluginHelper = credentialsCacher!=null ? new HttpMultipartPluginHelper(singleStepConfig, credentialsCacher) : new HttpMultipartPluginHelper(singleStepConfig);
                return httpMultipartPluginHelper.execute(fileExtension, fileContent);
            default:
                logWarn("Unsupported HTTP method: " + this.httpMethod);
                throw new IllegalArgumentException("Unsupported HTTP method: " + this.httpMethod);    
        }
    }
}
