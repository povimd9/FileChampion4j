package com.blumo.FileChampion4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.*;

/**
 * This class is used to parse the JSON configuration file into a Map object.
 * The Map object is used to store the configuration data in a nested dictionary structure.
 * The Map object is then used to create the Config object.
 * @param configPath: the path to the JSON configuration file
 * @return a Map object representing the nested dictionary structure of the JSON file
 * @throws IOException: if the JSON file cannot be read
 * @throws InvalidConfigException: if the JSON file does not contain at least two keys
 */

 public class ConfigParser {
    
    private static final Logger LOGGER = Logger.getLogger(ConfigParser.class.getName());

    // The minimum number of keys required in the JSON configuration file for file validation
    private static final int MINIMUM_REQUIRED_KEYS = 2;

    // Private constructor to prevent instantiation
    private ConfigParser() {
    }

    // Parse the JSON configuration file into a Map object
    public static Map<String, Object> parseConfig(String configPath) throws IOException, InvalidConfigException {
        // Create ObjectMapper instance to parse JSON
        ObjectMapper mapper = new ObjectMapper();

        // Read JSON file into JsonNode object
        JsonNode rootNode = mapper.readTree(new File(configPath));

        // Check if there are at least two keys in the JSON object
        if (rootNode.size() < MINIMUM_REQUIRED_KEYS) {
            String errorMessage = String.format("Invalid configuration: must contain at least %d keys for 'Category' and 'Extension'.", MINIMUM_REQUIRED_KEYS);
            throw new InvalidConfigException(errorMessage);
        }

        // Build Map object representing the nested dictionary structure of the JSON file
        return buildMapFromJsonNode(rootNode);
    }

    // Build Map object representing the nested dictionary structure of the JSON file
    private static Map<String, Object> buildMapFromJsonNode(JsonNode node) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> fieldNames = node.fieldNames();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = node.get(fieldName);

            if (fieldValue.isObject()) {
                map.put(fieldName, buildMapFromJsonNode(fieldValue));
            } else if (fieldValue.isArray()) {
                List<Object> list = new ArrayList<>();
                for (JsonNode arrayNode : fieldValue) {
                    if (arrayNode.isObject()) {
                        list.add(buildMapFromJsonNode(arrayNode));
                    } else {
                        list.add(getValueFromJsonNode(arrayNode));
                    }
                }
                map.put(fieldName, list);
            } else {
                map.put(fieldName, getValueFromJsonNode(fieldValue));
            }
        }

        return map;
    }

    // Get the value from a JsonNode object
    private static Object getValueFromJsonNode(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isDouble()) {
            return node.asDouble();
        } else if (node.isLong()) {
            return node.asLong();
        } else {
            return null;
        }
    }
    
    // Custom exception class for invalid configuration files
    public static class InvalidConfigException extends Exception {
        public InvalidConfigException(String message) {
            super(message);
            LOGGER.severe(message);
        }
    }
}