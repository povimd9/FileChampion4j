package com.blumo.FileChampion4j;

import org.json.JSONObject;

/**
 * This class is used to load the Json configuration data of the 'Validations' and 'plugins' sections.
 */

public class JsonLoader {
    private JSONObject jsonExtensionObject;
    private JSONObject jsonPluginsObject;

    /**
     * Constructor
     * @param configJsonObject (JSONObject) the Json configuration data
     * @return (JsonLoader) a JsonLoader object
     * @throws IllegalArgumentException (IllegalArgumentException) if the 'Validations' section is not found in the Json configuration data
     */
    public JsonLoader(JSONObject configJsonObject) {
        if (configJsonObject == null) {
            String excMsg = String.format("Invalid argument(s) provided: json=%s", configJsonObject);
            throw new IllegalArgumentException(excMsg);
        }
        setValuesFromJson(configJsonObject);
    }

    /**
     * This method is used to get the Json configuration data of the 'Validations' section.
     * @return (JSONObject) the Json configuration data of the 'Validations' section
     */
    public JSONObject getJsonExtensionObject() {
        return jsonExtensionObject;
    }

    /**
     * This method is used to get the Json configuration data of the 'plugins' section.
     * @return (JSONObject) the Json configuration data of the 'plugins' section
     */
    public JSONObject getJsonPluginsObject() {
        return jsonPluginsObject;
    }

    /**
     * This method is used to set the Json configuration data of the 'Validations' and 'plugins' sections.
     * @param configJsonObject (JSONObject) the Json configuration data
     * @throws IllegalArgumentException (IllegalArgumentException) if the 'Validations' section is not found in the Json configuration data
     */
    private void setValuesFromJson(JSONObject configJsonObject) {
        if (configJsonObject.has("Validations")) {
            jsonExtensionObject = configJsonObject.getJSONObject("Validations");
        } else {
            String excMsg = String.format("Extesnions was not found in config: json=%s", configJsonObject);
            throw new IllegalArgumentException(excMsg);
        }
        if (configJsonObject.has("plugins")) {
            jsonPluginsObject = configJsonObject.getJSONObject("plugins");
        }
    }
}
