package com.blumo.FileChampion4j;

import org.json.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to fetch and load the Json configuration data for all plugins.
 * Allowing the Json configuration data to be loaded once and then used by all plugins.
 * Returns a list of PluginConfig objects.
 */
public class PluginsHelper {
    private JSONObject plugins;

    /**
     * Constructor
     * @param config (JSONObject) the Json configuration data
     * @return (PluginsHelper) a PluginsHelper object
     */
    public PluginsHelper(JSONObject plugins) {
        this.plugins = plugins;
    }

    /**
     * Returns a list of PluginConfig objects.
     * @return (List<PluginConfig>) a list of PluginConfig objects
     */
    public List<PluginConfig> getPluginConfigs() {
        List<PluginConfig> pluginConfigs = new ArrayList<>();

        for (String pluginName : plugins.keySet()) {
            JSONObject plugin = plugins.getJSONObject(pluginName);
            PluginConfig pluginConfig = new PluginConfig();
            pluginConfig.setName(pluginName);
            List<StepConfig> stepConfigs = new ArrayList<>();
            for (String stepName : plugin.keySet()) {
                JSONObject step = plugin.getJSONObject(stepName);
                StepConfig stepConfig = new StepConfig();
                stepConfig.setName(stepName.substring(0, stepName.lastIndexOf('.')));
                stepConfig.setType(step.getString("type"));
                stepConfig.setCredsPath(step.optString("creds_path"));
                stepConfig.setTimeout(step.optInt("timeout"));
                stepConfig.setOnTimeout(step.optString("on_timeout"));
                stepConfig.setEndpoint(step.optString("endpoint"));
                stepConfig.setResponse(step.optString("response"));

                switch (stepConfig.getType()) {
                    case "cli":
                        CliPluginHelper cliPluginStep = new CliPluginHelper(stepConfig);
                        stepConfig.setCliPluginHelper(cliPluginStep);
                        stepConfigs.add(stepConfig);
                        break;
                    case "http":
                        stepConfig.setMethod(step.optString("method"));
                        stepConfig.setHeaders(step.optJSONObject("headers"));
                        stepConfig.setBody(step.optJSONObject("body"));
                        stepConfig.setHttpPassCode(step.optInt("http_pass_code"));
                        stepConfig.setHttpFailCode(step.optInt("http_fail_code"));
                        stepConfigs.add(stepConfig);
                        break;
                    default:
                        break;
                }
            }
            pluginConfig.setStepConfigs(stepConfigs);
            pluginConfigs.add(pluginConfig);
        }
        return pluginConfigs;
    }

    /**
     * Set and return a single plugin configuration and steps.
     * @return (List<StepConfig>) a list of StepConfig objects
     */
    public static class PluginConfig {
        private String name;
        private List<StepConfig> stepConfigs;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<StepConfig> getStepConfigs() {
            return stepConfigs;
        }

        public void setStepConfigs(List<StepConfig> stepConfigs) {
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
        private String onTimeout;
        private String response;
        private String method;
        private JSONObject headers;
        private JSONObject body;    
        private int httpPassCode;
        private int httpFailCode;
        private String credsPath;
        private CliPluginHelper cliPluginHelper;

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

        public String getOnTimeout() {
            return onTimeout;
        }

        public void setOnTimeout(String onTimeout) {
            this.onTimeout = onTimeout;
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
