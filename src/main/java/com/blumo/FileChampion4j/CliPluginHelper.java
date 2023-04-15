package com.blumo.FileChampion4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.blumo.FileChampion4j.PluginsHelper.StepConfig;

public class CliPluginHelper {
    private StepConfig singleStepConfig;
    private int timeout;
    private String endpoint;
    private String response;

    private Logger logger = Logger.getLogger(CliPluginHelper.class.getName());
    String logMessage;

    public CliPluginHelper(StepConfig singleStepConfig) {
        this.singleStepConfig = singleStepConfig;
        this.endpoint = singleStepConfig.getEndpoint();
        this.timeout = singleStepConfig.getTimeout();
        this.response = singleStepConfig.getResponse();
        logger.info(singleStepConfig.getName() + " object created");
    }
    
    public String execute(String fileName, byte[] fileContent, String fileCheksum) { 
        String result = "";
        String statusMsg = "";
        prepEndpoint(fileName, fileContent, fileCheksum);
        logger.info("Endpoint: " + endpoint);
        try {
            result = timedProcessExecution(endpoint);
            logger.info("Result: " + result);
        } catch (Exception e) {
            result = "Error: " + e.getMessage();
        }

        //prepResponse(fileName, fileContent, fileCheksum);
        //logger.info("Response: " + response);

        String stepNameDelimiter = "${" + singleStepConfig.getName() + ".";
        

        String expectedResuls = response.substring(0, response.indexOf("${")>-1?
            response.indexOf("${")-1 : response.length());

        if (result.contains(expectedResuls)) {

            if (result.length())
            String newFileName = result.substring(result.indexOf(expectedResuls) + expectedResuls.length(),
                result.length());

            statusMsg = String.format("Success: %s", !newFileName.equals("")? newFileName : "");
            logger.info("Status message: " + statusMsg);
            return statusMsg;
        } else {
            return "Failed: " + result;
        }
    }

    private void prepEndpoint(String fileName, byte[] fileContent, String fileCheksum) {
        String newEndpoint = endpoint.replace("${fileName}", fileName)
        .replace("${fileContent}", Base64.getEncoder().encodeToString(fileContent))
        .replace("${fileCheksum}", fileCheksum);
        endpoint = newEndpoint;
    }

    private void prepResponse( String fileName, byte[] fileContent, String fileCheksum) {
        String stepNameDelimiter = "${" + singleStepConfig.getName() + ".";
        String newResponse = response.replace("${fileName}", fileName)
        .replace("${fileContent}", Base64.getEncoder().encodeToString(fileContent))
        .replace("${fileCheksum}", fileCheksum);
        response = newResponse;
    }
    
    private String timedProcessExecution(String command) throws IOException, InterruptedException, NullPointerException {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
        logger.info(String.format("Process starting: %s", command));

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
        
        logger.info(results);
        return results;
    }
}
