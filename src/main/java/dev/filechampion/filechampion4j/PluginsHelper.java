package dev.filechampion.filechampion4j;

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
     * @param plugins (JSONObject) a JSONObject containing plugin configuration data
     */
    public PluginsHelper(JSONObject plugins) {
        this.plugins = plugins;
    }

    /**
     * Returns a list of PluginConfig objects.
     * @return (HashMap&lt;String, PluginConfig&gt;) a map of PluginConfig objects
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
                        stepConfig.setMethod(step.getString("method"));
                        stepConfig.setHeaders(step.optJSONObject("headers"));
                        stepConfig.setBody(step.optJSONObject("body"));
                        stepConfig.setHttpPassCode(step.getInt("http_pass_code"));
                        stepConfig.setHttpFailCode(step.getInt("http_fail_code"));
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
     * Class to set and return a single plugin configuration.
     */
    public static class PluginConfig {
        private String name;
        private Map<String, StepConfig> stepConfigs;

        /**
         * Constructor
         * @return (String) the name of the plugin (e.g. "av_scan"
         */
        public String getName() {
            return name;
        }

        /**
         * Set the name of the plugin.
         * @param name (String) the name of the plugin (e.g. "av_scan")
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Return a map of StepConfig objects.
         * @return (Map&lt;String, StepConfig&gt;) a map of StepConfig objects
         */
        public Map<String, StepConfig> getStepConfigs() {
            return stepConfigs;
        }

        /**
         * Set a map of StepConfig objects.
         * @param stepConfigs (Map&lt;String, StepConfig&gt;) a map of StepConfig objects
         */
        public void setStepConfigs(Map<String, StepConfig> stepConfigs) {
            this.stepConfigs = stepConfigs;
        }
    }

    /**
     * Class to set and return a single step configuration.
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

        /**
         * Check if run_before is set to true.
         * @return (boolean) true if run_before is set to true, false otherwise
         */
        public boolean isRunBefore() {
            return runBefore;
        }

        /**
         * Set run_before to true or false.
         * @param runBefore (boolean) true if run_before is set to true, false otherwise
         */
        public void setRunBefore(boolean runBefore) {
            this.runBefore = runBefore;
        }

        /**
         * Check if run_after is set to true.
         * @return (boolean) true if run_after is set to true, false otherwise
         */
        public boolean isRunAfter() {
            return runAfter;
        }

        /**
         * Set run_after to true or false.
         * @param runAfter (boolean) true if run_after is set to true, false otherwise
         */
        public void setRunAfter(boolean runAfter) {
            this.runAfter = runAfter;
        }

        /**
         * Return a CliPluginHelper object.
         * @return (CliPluginHelper) a CliPluginHelper object
         */
        public CliPluginHelper getCliPluginHelper() {
            return cliPluginHelper;
        }

        /**
         * Set a CliPluginHelper object.
         * @param cliPluginHelper (CliPluginHelper) a CliPluginHelper object
         */
        public void setCliPluginHelper(CliPluginHelper cliPluginHelper) {
            this.cliPluginHelper = cliPluginHelper;
        }

        /**
         * Return the name of the step.
         * @return (String) the name of the step
         */
        public String getName() {
            return name;
        }

        /**
         * Set the name of the step.
         * @param name (String) the name of the step
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Return the type of the step.
         * @return (String) the type of the step
         */
        public String getType() {
            return type;
        }

        /**
         * Set the type of the step.
         * @param type (String) the type of the step
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Return the endpoint of the step.
         * @return (String) the endpoint of the step
         */
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * Set the endpoint of the step.
         * @param endpoint (String) the endpoint of the step
         */
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        /**
         * Return the timeout of the step.
         * @return (int) the timeout of the step
         */
        public int getTimeout() {
            return timeout;
        }

        /**
         * Set the timeout of the step.
         * @param timeout (int) the timeout of the step
         */
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        /**
         * Return the onFail of the step.
         * @return (String) the onFail of the step
         */
        public String getOnFail() {
            return onFail;
        }

        /**
         * Set the onFail of the step.
         * @param onFail (String) the onFail of the step
         */
        public void setOnFail(String onFail) {
            this.onFail = onFail;
        }

        /**
         * Return the response of the step.
         * @return (String) the response of the step
         */
        public String getResponse() {
            return response;
        }

        /**
         * Set the response of the step.
         * @param response (String) the response of the step
         */
        public void setResponse(String response) {
            this.response = response;
        }

        /**
         * Return the method of the step.
         * @return (String) the method of the step
         */
        public String getMethod() {
            return method;
        }

        /**
         * Set the method of the step.
         * @param method (String) the method of the step
         */
        public void setMethod(String method) {
            this.method = method;
        }

        /**
         * Return the headers of the step.
         * @return (JSONObject) the headers of the step
         */
        public JSONObject getHeaders() {
            return headers;
        }

        /**
         * Set the headers of the step.
         * @param headers (JSONObject) the headers of the step
         */
        public void setHeaders(JSONObject headers) {
            this.headers = headers;
        }

        /**
         * Return the body of the step.
         * @return (JSONObject) the body of the step
         */
        public JSONObject getBody() {
            return body;
        }

        /**
         * Set the body of the step.
         * @param body (JSONObject) the body of the step
         */
        public void setBody(JSONObject body) {
            this.body = body;
        }

        /**
         * Return the httpPassCode of the step.
         * @return (int) the httpPassCode of the step
         */
        public int getHttpPassCode() {
            return httpPassCode;
        }

        /**
         * Set the httpPassCode of the step.
         * @param httpPassCode (int) the httpPassCode of the step
         */
        public void setHttpPassCode(int httpPassCode) {
            this.httpPassCode = httpPassCode;
        }

        /**
         * Return the httpFailCode of the step.
         * @return (int) the httpFailCode of the step
         */
        public int getHttpFailCode() {
            return httpFailCode;
        }

        /**
         * Set the httpFailCode of the step.
         * @param httpFailCode (int) the httpFailCode of the step
         */
        public void setHttpFailCode(int httpFailCode) {
            this.httpFailCode = httpFailCode;
        }

        /**
         * Return the credsPath of the step.
         * @return (String) the credsPath of the step
         */
        public String getCredsPath() {
            return credsPath;
        }

        /**
         * Set the credsPath of the step.
         * @param credsPath (String) the credsPath of the step
         */
        public void setCredsPath(String credsPath) {
            this.credsPath = credsPath;
        }
    }
}
