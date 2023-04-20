package com.blumo.filechampion4j;

import org.json.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to fetch and load the Json configuration data for all plugins.
 * Allowing the Json configuration data to be loaded once and then used by all plugins.
 * Returns a list of PluginConfig objects.
 */
public class PluginsHelper {
    private JSONObject plugins;

    /**
     * Constructor
     * @param plugins (JSONObject) a JSONObject containing all plugins
     * @return (PluginsHelper) a PluginsHelper object
     */
    public PluginsHelper(JSONObject plugins) {
        this.plugins = plugins;
    }

    /**
     * Returns a list of PluginConfig objects.
     * @return (List<PluginConfig>) a list of PluginConfig objects
     */
    public Map<String, PluginConfig> getPluginConfigs() {
        HashMap<String, PluginConfig> pluginConfigs = new HashMap<>();
        Map<String, StepConfig> stepConfigs = new HashMap<>();


        for (String pluginName : plugins.keySet()) {
            JSONObject plugin = plugins.getJSONObject(pluginName);
            PluginConfig pluginConfig = new PluginConfig();
            pluginConfig.setName(pluginName);

            for (String stepName : plugin.keySet()) {
                JSONObject step = plugin.getJSONObject(stepName);
                StepConfig stepConfig = new StepConfig();
                String pluginStepName = pluginName + "." + stepName.substring(0, stepName.lastIndexOf('.'));
                stepConfig.setName(pluginStepName);

                stepConfig.setType(step.getString("type"));
                stepConfig.setRunBefore(step.optBoolean("run_before"));
                stepConfig.setRunAfter(step.optBoolean("run_after"));
                stepConfig.setCredsPath(step.optString("creds_path"));
                stepConfig.setTimeout(step.getInt("timeout"));
                stepConfig.setOnFail(step.getString("on_timeout_or_fail"));
                stepConfig.setEndpoint(step.getString("endpoint"));
                stepConfig.setResponse(step.getString("response"));

                switch (stepConfig.getType()) {
                    case "cli":
                        CliPluginHelper cliPluginStep = new CliPluginHelper(stepConfig);
                        stepConfig.setCliPluginHelper(cliPluginStep);
                        stepConfigs.put(pluginStepName, stepConfig);

                        continue;
                    case "http":
                        stepConfig.setMethod(step.optString("method"));
                        stepConfig.setHeaders(step.optJSONObject("headers"));
                        stepConfig.setBody(step.optJSONObject("body"));
                        stepConfig.setHttpPassCode(step.optInt("http_pass_code"));
                        stepConfig.setHttpFailCode(step.optInt("http_fail_code"));
                        stepConfigs.put(pluginStepName, stepConfig);
                        continue;
                    default:
                        continue;
                }
            }
            pluginConfig.setStepConfigs(stepConfigs);
            pluginConfigs.put(pluginName, pluginConfig);
        }
        return pluginConfigs;
    }

    /**
     * Set and return a single plugin configuration and steps.
     * @return (List<StepConfig>) a list of StepConfig objects
     */
    public static class PluginConfig {
        private String name;
        private Map<String, StepConfig> stepConfigs;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, StepConfig> getStepConfigs() {
            return stepConfigs;
        }

        public void setStepConfigs(Map<String, StepConfig> stepConfigs) {
            this.stepConfigs = stepConfigs;
        }
    }

    /**
     * Set and return a single step configuration.
     * @return (StepConfig) a StepConfig object
     */
    public static class StepConfig {
        private String name;
        private String type;
        private String endpoint;
        private int timeout;
        private String onFail;
        private String response;
        private String method;
        private JSONObject headers;
        private JSONObject body;    
        private int httpPassCode;
        private int httpFailCode;
        private String credsPath;
        private CliPluginHelper cliPluginHelper;
        private boolean runBefore;
        private boolean runAfter;

        public boolean isRunBefore() {
            return runBefore;
        }

        public void setRunBefore(boolean runBefore) {
            this.runBefore = runBefore;
        }

        public boolean isRunAfter() {
            return runAfter;
        }

        public void setRunAfter(boolean runAfter) {
            this.runAfter = runAfter;
        }

        public CliPluginHelper getCliPluginHelper() {
            return cliPluginHelper;
        }

        public void setCliPluginHelper(CliPluginHelper cliPluginHelper) {
            this.cliPluginHelper = cliPluginHelper;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public String getOnFail() {
            return onFail;
        }

        public void setOnFail(String onFail) {
            this.onFail = onFail;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public JSONObject getHeaders() {
            return headers;
        }

        public void setHeaders(JSONObject headers) {
            this.headers = headers;
        }

        public JSONObject getBody() {
            return body;
        }

        public void setBody(JSONObject body) {
            this.body = body;
        }

        public int getHttpPassCode() {
            return httpPassCode;
        }

        public void setHttpPassCode(int httpPassCode) {
            this.httpPassCode = httpPassCode;
        }

        public int getHttpFailCode() {
            return httpFailCode;
        }

        public void setHttpFailCode(int httpFailCode) {
            this.httpFailCode = httpFailCode;
        }

        public String getCredsPath() {
            return credsPath;
        }

        public void setCredsPath(String credsPath) {
            this.credsPath = credsPath;
        }
    }
}
