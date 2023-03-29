package com.blumo.FileChampion4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class is used to parse the JSON configuration file into a Map object.
 * The Map object is used to store the configuration data in a nested dictionary structure.
 * The Map object is then used to create the Config object.
 */

public class ConfigParser {

    public static Map<String, Object> parseConfig(String configPath) throws IOException {
        // Create ObjectMapper instance to parse JSON
        ObjectMapper mapper = new ObjectMapper();

        // Read JSON file into JsonNode object
        JsonNode rootNode = mapper.readTree(new File(configPath));

        // Build Map object representing the nested dictionary structure of the JSON file
        return buildMapFromJsonNode(rootNode);
    }

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
}
