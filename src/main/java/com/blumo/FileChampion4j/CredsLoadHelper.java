package com.blumo.FileChampion4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;


public class CredsLoadHelper {
    private char[] credsFileString;
    private JSONObject credsJsonObject;
    private Map<String, char[]> credValues;

    /**
     * Constructor of credsLoadHelper
     * @param credsPathString (String) the path to the credentials file
     * @throws IOException
     */
    public CredsLoadHelper(String credsPathString) throws IOException {
        try {
            Path credsFilePath = Paths.get(credsPathString);
            this.credsFileString = new String(Files.readAllBytes(credsFilePath)).toCharArray();
            this.credsJsonObject = new JSONObject(credsFileString);
        } catch (IOException e) {
            throw new IOException("Error reading credentials file: " + e.getMessage());
        }
    }

    /**
     * Get the values of the credentials from the credentials file
     * @param credNames (String[]) the names of the credentials (e.g. "api_secret", "api_key")
     * @return (Map<String, char[]>) a map of the credentials names and values
     */
    public Map<String, char[]> getCredValues(String[] credNames) {
        credValues = new HashMap<>();
        for (String credName : credNames) {
            if ( credsJsonObject.has(credName) ) {
                credValues.putIfAbsent(credName, credsJsonObject.getString(credName).toCharArray());
            }
        }
        return credValues;
    }

    /**
     * Clear the values of the credentials from memory
     */
    public void clearCredValues() {
        if (credsFileString != null) {
            for (int i = 0; i < credsFileString.length; i++) {
                credsFileString[i] = ' ';
            }
        }
        if (credsJsonObject != null) {
            credsJsonObject.clear();
        }
        if (credValues != null) {
            for (Map.Entry<String, char[]> entry : credValues.entrySet()) {
                char[] value = entry.getValue();
                for (int i = 0; i < value.length; i++) {
                    value[i] = ' ';
                }
            }
        }
    }
}
