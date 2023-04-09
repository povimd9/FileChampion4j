package com.blumo.FileChampion4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * This class is used to run a process and return the output.
 */
public class RunProcessHelper {

    private static final Logger LOGGER = Logger.getLogger(RunProcessHelper.class.getName());

    public String runProcess(String command) {
        String output = "";
        BufferedReader reader;
        StringBuilder processOutput = new StringBuilder(output);
        try {
            Process process = Runtime.getRuntime().exec(command);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                processOutput.append(line);
            }
            reader.close();
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        }
        output = processOutput.toString();
        return output;
    }
    
}
