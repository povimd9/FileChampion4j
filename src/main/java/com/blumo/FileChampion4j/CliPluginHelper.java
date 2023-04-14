package com.blumo.FileChampion4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.blumo.FileChampion4j.PluginsHelper.StepConfig;

public class CliPluginHelper {
    private StepConfig singleStepConfig;
    private int timeout;
    private String onTimeout;
    private String endpoint;
    private String response;

    public CliPluginHelper(StepConfig singleStepConfig) {
        this.singleStepConfig = singleStepConfig;
        this.endpoint = singleStepConfig.getEndpoint();
        this.timeout = singleStepConfig.getTimeout();
        this.onTimeout = singleStepConfig.getOnTimeout();
        this.response = singleStepConfig.getResponse();
    }
    
    public String execute(String fileName, byte[] fileContent, String fileCheksum) { 
        String result = "";
        String statusMsg = "";
        String command = endpoint.replace("${fileName}", fileName)
                .replace("${fileContent}", Base64.getEncoder().encode(fileContent).toString())
                .replace("${fileCheksum}", fileCheksum);
        try {
            BufferedReader commandOutput = executeCommand(command);
            String line;
            while ((line = commandOutput.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            result = "Error: " + e.getMessage();
        }
        prepResponse(fileName, fileContent, fileCheksum);
        String expectedResuls = response.substring(0, response.indexOf("${")>=-1? response.indexOf("${") : response.length());

        if (result.contains(expectedResuls)) {
            String newFileName = result.substring(expectedResuls.length() + 
                result.lastIndexOf(".file_name}") + 
                ("${"+singleStepConfig.getName()+"}.file_name}").length());

            statusMsg = String.format("Success: %s", !newFileName.equals("")? newFileName : "");
            return statusMsg;
        } else {
            return "Error: " + result;
        }

    }

    private void prepResponse( String fileName, byte[] fileContent, String fileCheksum) {
        String newResponse = response.replace("${fileName}", fileName)
        .replace("${fileContent}", Base64.getEncoder().encode(fileContent).toString())
        .replace("${fileCheksum}", fileCheksum);
        response = newResponse;
    }

    private BufferedReader executeCommand(String command) throws IOException, NullPointerException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
            process.destroy();
            if (onTimeout.equals("kill")) {
                process.destroyForcibly();
            }
        }
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
}
