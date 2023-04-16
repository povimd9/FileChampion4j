package com.blumo.FileChampion4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.blumo.FileChampion4j.PluginsHelper.StepConfig;


public class CliPluginHelper {
    private StepConfig singleStepConfig;
    private int timeout;
    private String endpoint;
    private String responseConfig;

    static {
        try {
            LogManager.getLogManager().readConfiguration(
                FileValidator.class.getResourceAsStream("/logging.properties"));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load default logging configuration: ", e);
        }
    }
    private Logger logger = Logger.getLogger(CliPluginHelper.class.getName());
    private void logFine(String message) {
        if (logger.isLoggable(Level.FINE )) {
            logger.fine(message);
        }
    }
    String logMessage;

    /**
     * Constructor for CliPluginHelper
     * @param singleStepConfig
     */
    public CliPluginHelper(StepConfig singleStepConfig) {
        this.singleStepConfig = singleStepConfig;
        this.endpoint = singleStepConfig.getEndpoint();
        this.timeout = singleStepConfig.getTimeout();
        this.responseConfig = singleStepConfig.getResponse();
        logFine(singleStepConfig.getName() + " object created");
    }
    
    /**
     * Executes the CLI command
     * @param filePath (String) - the path to the file
     * @param fileContent (byte[]) - the file content
     * @param fileCheksum (String) - the file checksum
     * @return Map<String, Map<String, String>> - the results map
     */
    public Map<String, Map<String, String>> execute(String filePath, byte[] fileContent, String fileCheksum) { 
        String result = "";
        Map<String, Map<String, String>> responseMap = new HashMap<>();
        
        prepEndpoint(filePath, fileContent, fileCheksum);
        logFine(String.format("%s endpoint: %s", singleStepConfig.getName(), endpoint));

        try {
            result = timedProcessExecution(endpoint);
            logFine(singleStepConfig.getName() + " result: " + result);
        } catch (Exception e) {
            result = "Error: " + singleStepConfig.getName() + ":" + e.getMessage();
        }

        String expectedResuls = responseConfig.substring(0, responseConfig.indexOf("${")>-1?
        responseConfig.indexOf("${")-1 : responseConfig.length());

        if (result.contains(expectedResuls)) {
            Map<String, String> responsePatterns = extractRespnsePatterns(result);
            responseMap.put(result, responsePatterns);
            return responseMap;
        } else {
            responseMap.put(result, null);
            return responseMap;
        }
    }

    /**
     * Extracts the response patterns from the results
     * @param results (String) - the results
     * @return Map<String, String> - the response patterns map
     */
    private Map<String, String> extractRespnsePatterns (String results) {
        Map<String, String> responsePatterns = new HashMap<>();
    
        // Extract the placeholder name from the response pattern
        Pattern placeholderPattern = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher placeholderMatcher = placeholderPattern.matcher(responseConfig);
        if (!placeholderMatcher.find()) {
            return responsePatterns;
        }
    
        do {
            String placeholderName = placeholderMatcher.group(1);
            String placeholderValue = "";
    
            logFine(String.format("Placeholder name: %s, ResponseConfig: %s", placeholderName, responseConfig));
            
            String fixedPrefix = responseConfig.substring(0, responseConfig.indexOf("${"));
            logFine(String.format("Fixed prefix: %s", fixedPrefix));
    
            String fixedSuffix;
            int suffixStartIndex = responseConfig.indexOf("${") + placeholderName.length() + 3;
            if (suffixStartIndex == responseConfig.length()) {
                // Placeholder appears at the end of the responseConfig string
                fixedSuffix = "";
            } else {
                fixedSuffix = responseConfig.substring(suffixStartIndex);
                logFine(String.format("Fixed suffix: %s", fixedSuffix));
            }
    
            String captureGroupPattern = String.format("^%s(.*)%s$", fixedPrefix, fixedSuffix);
            logFine(String.format("Capture group pattern: %s", captureGroupPattern));
    
            Pattern pattern = Pattern.compile(captureGroupPattern);
            Matcher matcher = pattern.matcher(results);
    
            if (matcher.find()) {
                placeholderValue = matcher.group(1);
                responsePatterns.put(placeholderName, placeholderValue);
            }
    
        } while (placeholderMatcher.find());
    
        return responsePatterns;
    }
    
    /**
     * Prepares the endpoint command by replacing the placeholders with the actual values
     * @param filePath (String) - the path to the file
     * @param fileContent (byte[]) - the file content
     * @param fileCheksum (String) - the file checksum
     */
    private void prepEndpoint(String filePath, byte[] fileContent, String fileCheksum) {
        String newEndpoint = endpoint.replace("${filePath}", filePath)
        .replace("${fileContent}", Base64.getEncoder().encodeToString(fileContent))
        .replace("${fileCheksum}", fileCheksum);
        endpoint = newEndpoint;
    }

    /**
     * Executes the CLI command with a timeout
     * @param command (String) - the command to execute
     * @return String - the results
     * @throws IOException
     * @throws InterruptedException
     * @throws NullPointerException
     */
    private String timedProcessExecution(String command) throws IOException, InterruptedException, NullPointerException {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
        logFine(String.format("Process starting: %s", command));

        Process process = processBuilder.start();
        TimeUnit timeUnit = TimeUnit.SECONDS;
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    process.destroy();
                }
            },
            timeUnit.toMillis(timeout)
        );
        int exitCode = process.waitFor();
        if (exitCode == 143 || exitCode == 1) {
            return "Error: " + command + " timed out after " + timeout + " seconds";
        }
        timer.cancel();
        InputStream inputStream = process.getInputStream();
        InputStream errorStream = process.getErrorStream();
        SequenceInputStream sequenceInputStream = new SequenceInputStream(inputStream, errorStream);
        
        String results = "";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sequenceInputStream));

        Scanner scanner = new Scanner(bufferedReader).useDelimiter("\\A");
        results = scanner.hasNext() ? scanner.next() : "";
        bufferedReader.close();
        scanner.close();
        
        logFine(results);
        return results;
    }
}
