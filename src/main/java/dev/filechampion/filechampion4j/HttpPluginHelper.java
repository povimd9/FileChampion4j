package dev.filechampion.filechampion4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

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
    private String errString = "Error: ";
    private StringBuilder logMessage = new StringBuilder();
    private Path filePathRaw;
    private StepConfig singleStepConfig;
    private int timeout;
    private String endpoint;
    private String responseConfig;
    private String httpMethod;
    private JSONObject httpHeaders;
    private JSONObject httpBody;
    private int httpPassCode;
    private int httpFailCode;
    private Random random = new Random();
    private String requestFailedWithKnownCode = "HTTP request failed with response code ";
    private String requestFailedWithUnknownCode = "HTTP request failed with unknown response code ";



    
    /**
     * Constructor for HttpPluginHelper
     * @param singleStepConfig (StepConfig) - the plugin step configuration
     */
    public HttpPluginHelper(StepConfig singleStepConfig) {
        this.singleStepConfig = singleStepConfig;
        this.timeout = singleStepConfig.getTimeout();
        this.endpoint = singleStepConfig.getEndpoint();
        this.responseConfig = singleStepConfig.getResponse();
        this.httpMethod = singleStepConfig.getMethod();
        this.httpHeaders = singleStepConfig.getHeaders();
        this.httpBody = singleStepConfig.getBody();
        this.httpPassCode = singleStepConfig.getHttpPassCode();
        this.httpFailCode = singleStepConfig.getHttpFailCode();
        logMessage.append("HttpPluginHelper initialized");
        logFine(logMessage.toString());
    }
    
    public Map<String, Map<String, String>> execute(String fileExtension, byte[] fileContent) {
        String result = "";
        Map<String, Map<String, String>> responseMap = new HashMap<>();
        Map<String, String> responsePatterns = new HashMap<>();
        try {
            this.endpoint = prepRequestData(this.endpoint, fileContent, fileExtension);
            this.httpHeaders = prepJsonRequestData(this.httpHeaders.toString(), fileContent, fileExtension);
            this.httpBody = prepJsonRequestData(this.httpBody.toString(), fileContent, fileExtension);

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
            if (filePathRaw != null) {
                deleteTempDir(filePathRaw);
            }
            return responseMap;
        } else {
            logMessage.replace(0, logMessage.length(), "Error, expected: \"")
                    .append(expectedResults).append("\", received: ");
            responsePatterns.put(logMessage.toString(), result);
            responseMap.put(errString, responsePatterns);
            if (filePathRaw != null) {
                deleteTempDir(filePathRaw);
            }
            return responseMap;
        }
    }
    
    /**
     * Prepares the endpoint command by replacing the placeholders with the actual values
     * @param requestData (String) - the data to be manipulated
     * @param fileContent (byte[]) - the file content
     * @param fileExtension (String) - the file extension
     * @throws IOException - if failed to save the file to temporary directory
     */
    private String prepRequestData(String requestData, byte[] fileContent, String fileExtension) throws IOException {
        if (requestData.contains("${filePath}")) {
            if (filePathRaw == null) {
                filePathRaw = saveFileToTempDir(fileExtension, fileContent);
            }
            if (filePathRaw == null) {
                throw new IOException("Failed to save file to temporary directory");
            }
            String filePath = filePathRaw.toString();
            requestData = requestData.replace("${filePath}", filePath);
        }
        requestData = requestData.contains("${fileContent}") ? requestData.replace("${fileContent}", Base64.getEncoder().encodeToString(fileContent)) : requestData;
        requestData = requestData.contains("${fileChecksum.md5}") ? requestData.replace("${fileChecksum.md5}", calculateChecksum(fileContent, "MD5")) : requestData;
        requestData = requestData.contains("${fileChecksum.sha1}") ? requestData.replace("${fileChecksum.sha1}", calculateChecksum(fileContent, "SHA-1")) : requestData;
        requestData = requestData.contains("${fileChecksum.sha256}") ? requestData.replace("${fileChecksum.sha256}", calculateChecksum(fileContent, "SHA-256")) : requestData;
        requestData = requestData.contains("${fileChecksum.sha512}") ? requestData.replace("${fileChecksum.sha512}", calculateChecksum(fileContent, "SHA-512")) : requestData;
        return requestData;

        
    }

    /**
     * Prepares the endpoint command by replacing the placeholders with the actual values
     * @param requestData (String) - the data to be manipulated
     * @param fileContent (byte[]) - the file content
     * @param fileExtension (String) - the file extension
     * @throws IOException - if failed to save the file to temporary directory
     */
    private JSONObject prepJsonRequestData(String requestData, byte[] fileContent, String fileExtension) throws IOException {
        if (requestData.contains("${filePath}")) {
            if (filePathRaw == null) {
                filePathRaw = saveFileToTempDir(fileExtension, fileContent);
            }
            if (filePathRaw == null) {
                throw new IOException("Failed to save file to temporary directory");
            }
            String filePath = filePathRaw.toString();
            requestData = requestData.replace("${filePath}", JSONWriter.valueToString(filePath));
        }
        requestData = requestData.contains("${fileContent}") ? requestData.replace("${fileContent}", JSONWriter.valueToString(Base64.getEncoder().encodeToString(fileContent))) : requestData;
        requestData = requestData.contains("${fileChecksum.md5}") ? requestData.replace("${fileChecksum.md5}", calculateChecksum(fileContent, "MD5")) : requestData;
        requestData = requestData.contains("${fileChecksum.sha1}") ? requestData.replace("${fileChecksum.sha1}", calculateChecksum(fileContent, "SHA-1")) : requestData;
        requestData = requestData.contains("${fileChecksum.sha256}") ? requestData.replace("${fileChecksum.sha256}", calculateChecksum(fileContent, "SHA-256")) : requestData;
        requestData = requestData.contains("${fileChecksum.sha512}") ? requestData.replace("${fileChecksum.sha512}", calculateChecksum(fileContent, "SHA-512")) : requestData;

        return new JSONObject(requestData);
    }

    /**
     * Extracts the response patterns from the results
     * @param results (String) - the results
     * @return Map&lt;String, String&gt; - the response patterns map
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

    private String doHttpGetRequest() throws IOException, JSONException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);

        JSONObject httpHeadersObject = new JSONObject(httpHeaders);
        Iterator<String> headerKeys = httpHeadersObject.keys();
        while (headerKeys.hasNext()) {
            String headerKey = headerKeys.next();
            String headerValue = httpHeadersObject.getString(headerKey);
            connection.setRequestProperty(headerKey, headerValue);
        }
    
        int responseCode = connection.getResponseCode();
        if (responseCode == httpPassCode) {
            return connection.getResponseMessage();
        } else if (responseCode == httpFailCode) {
            throw new IOException(requestFailedWithKnownCode + responseCode);
        } else {
            throw new IOException(requestFailedWithUnknownCode + responseCode);
        }
    }
    

    private String doHttpPostRequest() throws IOException, JSONException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
    
        JSONObject httpHeadersObject = new JSONObject(httpHeaders);
        Iterator<String> headerKeys = httpHeadersObject.keys();
        while (headerKeys.hasNext()) {
            String headerKey = headerKeys.next();
            String headerValue = httpHeadersObject.getString(headerKey);
            connection.setRequestProperty(headerKey, headerValue);
        }
        connection.setDoOutput(true);
    
        StringBuilder postData = new StringBuilder();
        JSONObject httpBodyObject = new JSONObject(httpBody);
        Iterator<String> bodyKeys = httpBodyObject.keys();
        while (bodyKeys.hasNext()) {
            String key = bodyKeys.next();
            String value = httpBodyObject.getString(key);
            postData.append(URLEncoder.encode(key, "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(value, "UTF-8"));
            postData.append('&');
        }
    
        if (postData.length() > 0) {
            postData.setLength(postData.length() - 1);
        }
    
        try (Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(postData.toString());
        } catch (IOException e) {
            // Handle exceptions during the HTTP request
            throw new IOException("Error occurred during the HTTP request: " + e.getMessage(), e);
        }
    
        int responseCode = connection.getResponseCode();
        if (responseCode == httpPassCode) {
            return connection.getResponseMessage();
        } else if (responseCode == httpFailCode) {
            throw new IOException(requestFailedWithKnownCode + responseCode);
        } else {
            throw new IOException(requestFailedWithUnknownCode + responseCode);
        }
    }
    
    

    private String doHttpPostMultiPartRequest(String fileExtension, byte[] fileContent) throws IOException, JSONException {
        if (filePathRaw == null) {
            filePathRaw = saveFileToTempDir(fileExtension, fileContent);
        }
    
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + generateRandomBoundary());
    
        Iterator<String> headerKeys = httpHeaders.keys();
        while (headerKeys.hasNext()) {
            String headerKey = headerKeys.next();
            String headerValue = httpHeaders.getString(headerKey);
            connection.setRequestProperty(headerKey, headerValue);
        }
    
        connection.setDoOutput(true);
        String boundary = generateRandomBoundary();
        String lineEnd = "\r\n";
    
        try (OutputStream outputStream = connection.getOutputStream();
             FileInputStream fis = new FileInputStream(filePathRaw.toFile());
             BufferedInputStream fileStream = new BufferedInputStream(fis)) {
            outputStream.write(("--" + boundary + lineEnd).getBytes());
            String fileName = filePathRaw.getFileName().toString();
            outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd).getBytes());
            outputStream.write(("Content-Type: application/octet-stream" + lineEnd).getBytes());
            outputStream.write(("Content-Transfer-Encoding: binary" + lineEnd + lineEnd).getBytes());
    
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
    
            outputStream.write((lineEnd + "--" + boundary + "--" + lineEnd).getBytes());
        } catch (IOException e) {
            // Handle exceptions during file upload
            throw new IOException("Error occurred during file upload: " + e.getMessage(), e);
        }
    
        int responseCode = connection.getResponseCode();
        if (responseCode == httpPassCode) {
            return connection.getResponseMessage();
        } else if (responseCode == httpFailCode) {
            throw new IOException(requestFailedWithKnownCode + responseCode);
        } else {
            throw new IOException(requestFailedWithUnknownCode + responseCode);
        }
    }
    
    


    ////////////////////
    // Helper methods //
    ////////////////////
    
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
     * Generates a random boundary string
     * @return String (String) the random boundary string
     */
    private String generateRandomBoundary() {
        // Generate a random alphanumeric string of 32 characters
        char[] chars = new char[16];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (random.nextInt(128));
        }
        return new String(chars);
    }

    /**
     * Calculate the checksum of the file
     * @param fileBytes (byte[]) the file bytes of the file being validated
     * @return String (String) the SHA-256 checksum of the file
     */
    private String calculateChecksum(byte[] fileBytes, String checksumAlgorithm) {
        try {
            CalculateChecksum paralChecksum = new CalculateChecksum(fileBytes);
            byte[] checksum = paralChecksum.getChecksum(checksumAlgorithm);
            return new BigInteger(1, checksum).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
