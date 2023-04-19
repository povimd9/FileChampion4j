package com.blumo.filechampion4j;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * This class is used to fetch the Json configuration data for a single file extension.
 * The Json configuration data is then used to create an Extension object.
 */

public class Extension {
    private String mimeType;
    private String magicBytes;
    private String headerSignatures;
    private String footerSignatures;
    private JSONArray antivirusScanJson;
    private boolean changeOwnership;
    private String changeOwnershipUser;
    private String changeOwnershipMode;
    private boolean nameEncoding;
    private int maxSize;
    private String fileCategory;
    private String fileExtension;
    private JSONArray extensionPlugins;


    /**
     * Constructor
     * @param fileCategory (String) the file category (e.g. "Documents")
     * @param fileExtension (String) the file extension (e.g. "pdf")
     * @param configJsonObject (JSONObject) the Json configuration data
     * @return (Extension) an Extension object
     * @throws IllegalArgumentException (IllegalArgumentException) if the file category or file extension is not found in the Json configuration data
     */
    public Extension(String fileCategory, String fileExtension, JSONObject configJsonObject) {
        if (fileCategory == null || fileCategory.trim().isEmpty()) {
            String excMsg = String.format("Invalid argument(s) provided: fileCategory=%s, fileExtension=%s, json=%s", 
              fileCategory, fileExtension, configJsonObject);
            throw new IllegalArgumentException(excMsg);
        }
        this.fileCategory = fileCategory;
        this.fileExtension = fileExtension;
        setValuesFromJson(configJsonObject);
    }

    public JSONArray getExtensionPlugins() {
        return extensionPlugins;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getMagicBytes() {
        return magicBytes;
    }

    public String getHeaderSignatures() {
        return headerSignatures;
    }

    public String getFooterSignatures() {
        return footerSignatures;
    }

    public JSONArray getAntivirusScanJson() {
        return antivirusScanJson;
    }

    public boolean isChangeOwnership() {
        return changeOwnership;
    }

    public String getChangeOwnershipUser() {
        return changeOwnershipUser;
    }

    public String getChangeOwnershipMode() {
        return changeOwnershipMode;
    }

    public boolean isNameEncoding() {
        return nameEncoding;
    }

    public int getMaxSize() {
        return maxSize;
    }

    /**
     * This method is used to set the values of the Extension object from the Json configuration data.
     * @param configJsonObject (JSONObject) the Json configuration data
     * @throws IllegalArgumentException if the file category or file extension is not found in the Json configuration data
     */
    private void setValuesFromJson(JSONObject configJsonObject) {
        if (!configJsonObject.has(fileCategory)) {
            throw new IllegalArgumentException("Category not found in JSON: " + fileCategory);
        }
        JSONObject categoryJson = configJsonObject.optJSONObject(fileCategory);

        if (!categoryJson.has(fileExtension)) {
            throw new IllegalArgumentException("Extension not found in JSON: " + fileExtension);
        }
        JSONObject extensionJson = categoryJson.optJSONObject(fileExtension);

        this.extensionPlugins = extensionJson.optJSONArray("extension_plugins");
        this.mimeType = extensionJson.optString("mime_type");
        this.magicBytes = extensionJson.optString("magic_bytes");
        this.headerSignatures = extensionJson.optString("header_signatures");
        this.footerSignatures = extensionJson.optString("footer_signatures");
        this.antivirusScanJson = extensionJson.optJSONArray("antivirus_scan");
        this.changeOwnership = extensionJson.optBoolean("change_ownership");
        this.changeOwnershipUser = extensionJson.optString("change_ownership_user");
        this.changeOwnershipMode = extensionJson.optString("change_ownership_mode");
        this.nameEncoding = extensionJson.optBoolean("name_encoding");
        this.maxSize = extensionJson.optInt("max_size");
    }
}
