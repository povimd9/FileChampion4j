package com.blumo.FileChampion4j;

import org.json.JSONObject;

public class JsonLoader {

    private JSONObject configJsonObject;
    private JSONObject jsonExtensionObject;
    private JSONObject jsonPlugiJsonObject;

    public JsonLoader(JSONObject configJsonObject) {
        if (configJsonObject == null) {
            String excMsg = String.format("Invalid argument(s) provided: json=%s", configJsonObject);
            throw new IllegalArgumentException(excMsg);
        }
        this.configJsonObject = configJsonObject;
        setValuesFromJson(configJsonObject);
    }



    public JSONObject getJsonExtensionObject() {
        return jsonExtensionObject;
    }

    public JSONObject getJsonPlugiJsonObject() {
        return jsonPlugiJsonObject;
    }

    private void setValuesFromJson(JSONObject configJsonObject) {
        if (configJsonObject.has("extensions")) {
            jsonExtensionObject = configJsonObject.getJSONObject("extensions");
        } else {
            String excMsg = String.format("Extesnions was not found in config: json=%s", configJsonObject);
            throw new IllegalArgumentException(excMsg);
        }
        if (configJsonObject.has("plugins")) {
            jsonPlugiJsonObject = configJsonObject.getJSONObject("plugins");
        }
    }
    
}
