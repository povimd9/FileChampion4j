package dev.filechampion.filechampion4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.filechampion.filechampion4j.PluginsHelper.StepConfig;


public class CliPluginHelper {
    private StepConfig singleStepConfig;
    private int timeout;
    private String errString = "Error: ";
    private String endpoint;
    private String responseConfig;
    private StringBuilder logMessage = new StringBuilder();

    static {
        try {
            LogManager.getLogManager().readConfiguration(
                FileValidator.class.getResourceAsStream("/logging.properties"));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load default logging configuration: ", e);
        }
    }
    private static final Logger LOGGER = Logger.getLogger(CliPluginHelper.class.getName());
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

    /**
     * Constructor for CliPluginHelper
     * @param singleStepConfig (StepConfig) - the step configuration
     */
    public CliPluginHelper(StepConfig singleStepConfig) {
        this.singleStepConfig = singleStepConfig;
        this.endpoint = singleStepConfig.getEndpoint();
        this.timeout = singleStepConfig.getTimeout();
        this.responseConfig = singleStepConfig.getResponse();
        logMessage.replace(0, logMessage.length(), singleStepConfig.getName()).append(" object created");
        logFine(logMessage.toString());
    }
    
    /**
     * Executes the CLI command
     * @param fileExtension (String) - the file extension
     * @param fileContent (byte[]) - the file content
     * @return Map<String, Map<String, String>> - the results map
     */
    public Map<String, Map<String, String>> execute(String fileExtension, byte[] fileContent) { 
    String result = "";
        Map<String, Map<String, String>> responseMap = new HashMap<>();
        Map<String, String> responsePatterns = new HashMap<>();
        Path filePathRaw;

        filePathRaw = saveFileToTempDir(fileExtension, fileContent);
        if (filePathRaw == null) {
            responsePatterns.put(errString, "Failed to save file to temporary directory");
            responseMap.put(errString, responsePatterns);
            return responseMap;
        }

        String filePath = filePathRaw.toString();
        prepEndpoint(filePath, fileContent);
        logMessage.replace(0, logMessage.length(), singleStepConfig.getName()).append(" endpoint: ").append(endpoint);
        logFine(logMessage.toString());

        try {
            result = timedProcessExecution(endpoint);
            logFine(singleStepConfig.getName() + " result: " + result);
        } catch (IOException|NullPointerException|InterruptedException e) {
            Thread.currentThread().interrupt();
            responsePatterns.put(errString, e.getMessage());
        }

        String expectedResults = responseConfig.substring(0, responseConfig.indexOf("${")>-1?
        responseConfig.indexOf("${") : responseConfig.length());

        if (result.contains(expectedResults)) {
            responsePatterns = extractResponsePatterns(result);
            responseMap.put("Success", responsePatterns);
            return responseMap;
        } else {
            logMessage.replace(0, logMessage.length(), "Error, expected: \"")
                    .append(expectedResults).append("\", received: ");
            responsePatterns.put(logMessage.toString(), result);
            responseMap.put(errString, responsePatterns);
            deleteTempDir(filePathRaw);
            return responseMap;
        }
    }

    /**
     * Extracts the response patterns from the results
     * @param results (String) - the results
     * @return Map<String, String> - the response patterns map
     */
    private Map<String, String> extractResponsePatterns (String results) {
        Map<String, String> responsePatterns = new HashMap<>();
    
        // Extract the placeholder name from the response pattern
        Pattern placeholderPattern = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher placeholderMatcher = placeholderPattern.matcher(responseConfig);
        if (!placeholderMatcher.find()) {
            responsePatterns.put(results, results);
            return responsePatterns;

        }
        do {
            String placeholderName = placeholderMatcher.group(1);
            String placeholderValue;
    
            logMessage.replace(0, logMessage.length(), "Placeholder name: ")
            .append(placeholderName)
            .append(", ResponseConfig: ")
            .append(responseConfig);
            logFine(logMessage.toString());
            
            String fixedPrefix = String.format("%s", responseConfig.substring(0, responseConfig.indexOf("${")));
            logMessage.replace(0, logMessage.length(), "Fixed prefix: ").append(fixedPrefix);
            logFine(logMessage.toString());
    
            String fixedSuffix;
            int suffixStartIndex = responseConfig.indexOf("${") + placeholderName.length() + 3;
            if (suffixStartIndex == responseConfig.length()) {
                fixedSuffix = "";
            } else {
                fixedSuffix = responseConfig.substring(suffixStartIndex);
                logMessage.replace(0, logMessage.length(), "Fixed suffix: ").append(fixedSuffix);
                logFine(logMessage.toString());
            }
    
            String captureGroupPattern = String.format("%s(.*)%s", fixedPrefix, fixedSuffix);
            logMessage.replace(0, logMessage.length(), "Capture group pattern: ").append(captureGroupPattern);
            logFine(logMessage.toString());
    
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
     */
    private void prepEndpoint(String filePath, byte[] fileContent) {
        
        String newEndpoint = endpoint.contains("${filePath}") ? endpoint.replace("${filePath}", filePath) : endpoint;
        newEndpoint = newEndpoint.contains("${fileContent}") ? newEndpoint.replace("${fileContent}", Base64.getEncoder().encodeToString(fileContent)) : newEndpoint;
        newEndpoint = newEndpoint.contains("${fileChecksum}") ? newEndpoint.replace("${fileChecksum}", calculateChecksum(fileContent)) : newEndpoint;
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
        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\p{Zs}+"));
        logMessage.replace(0, logMessage.length(), "Process starting: ").append(command);
        logFine(logMessage.toString());

        long timeoutCounter = System.currentTimeMillis();
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
        InputStream inputStream = process.getInputStream();
        InputStream errorStream = process.getErrorStream();
        SequenceInputStream sequenceInputStream = new SequenceInputStream(inputStream, errorStream);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sequenceInputStream));
        try(Scanner scanner = new Scanner(bufferedReader).useDelimiter("\\A")){
            String results;
            if (exitCode == 143 || exitCode == 1) {
                if (System.currentTimeMillis() - timeoutCounter > timeout * 1000) {
                    bufferedReader.close();
                    sequenceInputStream.close();
                    errorStream.close();
                    inputStream.close();
                    logMessage.replace(0, logMessage.length(), errString).append("Process timeout: ").append(command);
                    logWarn(logMessage.toString());
                    return logMessage.toString();
                }
                results = scanner.hasNext() ? scanner.next() : "";
                bufferedReader.close();
                sequenceInputStream.close();
                errorStream.close();
                inputStream.close();
                timer.cancel();
                logMessage.replace(0, logMessage.length(), errString).append(command) .append("Process failed: ").append(results);
                logWarn(logMessage.toString());
                return logMessage.toString();
            }
            results = scanner.hasNext() ? scanner.next() : "";
            bufferedReader.close();
            sequenceInputStream.close();
            errorStream.close();
            inputStream.close();
            timer.cancel();
            logFine(results);
            return results;
        }
    }

    
    /**
     * Saves the file to a temporary directory
     * @param fileExtension (String) - the file extension
     * @param originalFile (byte[]) - the file content
     * @return Path - the path to the file
     */
    private Path saveFileToTempDir(String fileExtension, byte[] originalFile) {
        Path tempFilePath;
        try {
            // Create a temporary directory
            Path tempDir = Files.createTempDirectory("tempDir");
            tempFilePath = Files.createTempFile(tempDir, "tempFile", "." + fileExtension);
            Files.write(tempFilePath, originalFile);
            return tempFilePath;
        } catch (Exception e) {
            logMessage.replace(0, logMessage.length(), "Error saveFileToTempDir failed: ").append(e.getMessage());
            logWarn(logMessage.toString());
            return null;
        }
    }

    /**
     * Deletes the temporary directory
     * @param tempFilePath (Path) - the path to the temporary directory
     * @return Boolean - true if the directory was deleted successfully, false otherwise
     */
    private Boolean deleteTempDir(Path tempFilePath) {
        try (Stream<Path> walk = Files.walk(tempFilePath)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            return true;
        } catch (Exception e) {
            logMessage.replace(0, logMessage.length(), "Error deleteTempDir failed: ").append(e.getMessage());
            logWarn(logMessage.toString());
            return false;
        }
    }

    /**
     * Calculate the checksum of the file
     * @param fileBytes (byte[]) the file bytes of the file being validated
     * @return String (String) the SHA-256 checksum of the file
     */
    private String calculateChecksum(byte[] fileBytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(fileBytes);
            return new BigInteger(1, hash).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
