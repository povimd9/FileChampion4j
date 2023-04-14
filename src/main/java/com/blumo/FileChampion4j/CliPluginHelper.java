package com.blumo.FileChampion4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CliPluginHelper {
    private PluginsHelper pluginsHelper;

    public CliPluginHelper(PluginsHelper pluginsHelper) {
        this.pluginsHelper = pluginsHelper;
    }
    
    public void run() {
        for (PluginConfig pluginConfig : pluginsHelper.getPluginConfigs()) {
            for (StepConfig stepConfig : pluginConfig.getStepConfigs()) {
                if (stepConfig.getType().equals("cli")) {
                    try {
                        BufferedReader commandOutput = executeCommand(stepConfig.getEndpoint());
                        String line;
                        while ((line = commandOutput.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private BufferedReader executeCommand(String command) throws IOException, NullPointerException {
        Process process = null;
        BufferedReader commandOutput = null;
        process = Runtime.getRuntime().exec(command);
        commandOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return commandOutput;
    }
}
