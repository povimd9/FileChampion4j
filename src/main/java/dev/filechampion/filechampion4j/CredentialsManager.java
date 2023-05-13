package dev.filechampion.filechampion4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/*
 * "General": "secrets_path":"dir"
parse plugins
if secret.* collect them to list and create creds manager
	check secrets_path for non empty file with relevant name
	create linkedlist with secret_filename:00000000 for cache
	create schedulued task every 2m to check secrets time expiration

if plugin prep with secret.*
	caller requests secret by name
	manager writes 'timestamp_'+base64(cred from file) into char[] in the list
	return char[] ref of secret
	
 */

/**
 * CredentialsManager class is used to manage any credendials that are needed for plugins, such as API keys, etc.
 */
public class CredentialsManager {
    private Path credsPath;
    private List<String> credsNamesList;
    private List<char[]> credsList = new LinkedList<>();
    private Map<String, Integer> credsMap = new HashMap<>();

    /**
     * Constructor for CredentialsManager class.
     * @param credsPath (Path) - Path to the directory where the credentials files are stored.
     * @param credsNamesList (List<String>) - List of the names of the credentials names/files.
     * @throws IllegalArgumentException - If the credsPath is null or does not exist, or if the credsNamesList is null or empty, or if any of the credentials files are not found.
     */
    public CredentialsManager(Path credsPath, List<String> credsNamesList) throws IllegalArgumentException {
        if (credsPath == null || !Files.exists(credsPath)) {
            throw new IllegalArgumentException("Defined credentials path was not found.");
        }
        if (credsNamesList == null || credsNamesList.isEmpty()) {
            throw new IllegalArgumentException("Defined credentials list was empty.");
        }
        this.credsPath = credsPath;

        for (String credsName : credsNamesList) {
            Path credFilePath = credsPath.resolve(credsPath + credsName);
            if (!Files.exists(credFilePath)) {
                throw new IllegalArgumentException("Credentials file " + credsName + " was not found.");
            }
        }
        this.credsNamesList = credsNamesList;
        // add timer to check for expired creds and remove them from the list
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                checkCredExpiration();
            }
        }, 0, 120000);
        
    }

    /**
     * This method is used to retrieve a credential from the cached credentials list or read it from the file and add it to the list.
     * @param credsName (String) - Name of the credentials name/file to retrieve.
     * @return (char[]) - The credentials as a char[].
     * @throws IllegalArgumentException - If the credsName is null or empty, or if the credsName is not found in the credsNamesList.
     * @throws IOException - If there is an error reading the credentials file.
     */
    public char[] getCredentials(String credsName) throws IllegalArgumentException, IOException{
        if (credsName == null || credsName.isEmpty()) {
            throw new IllegalArgumentException("Credentials name was empty.");
        }
        if (!this.credsNamesList.contains(credsName)) {
            throw new IllegalArgumentException("Credentials name was not found.");
        }

        String timestampedCredName = credsName + "_" + System.currentTimeMillis();
        if (!this.credsMap.containsKey(timestampedCredName)) {
            // read file and write to list
            Path fullCredPath = this.credsPath.resolve(this.credsPath + credsName);
            this.credsList.add(this.getCredFileChars(fullCredPath));
            this.credsMap.put(timestampedCredName, this.credsList.size() - 1);
        }
        return this.credsList.get(this.credsMap.get(timestampedCredName));
    }

    /**
     * This method is used to read a single credential file into a char[] for storage in the Map.
     * char[] grows dynamically as the file is read using 64 byte chunks.
     * @param credFilePath (Path) - Path to the credentials file to read.
     * @return (char[]) - The credentials as a char[].
     * @throws IOException - If there is an error reading the credentials file.
     */
    private char[] getCredFileChars(Path credFilePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(credFilePath.toString()))) {
            char[] tmpCharArray = new char[64]; // Initial size of the character array
            int charRead = 0;
            int position = 0;
            while((charRead = br.read()) != -1) {
                if(position == tmpCharArray.length) {
                    char[] temp = new char[tmpCharArray.length * 2];
                    System.arraycopy(tmpCharArray, 0, temp, 0, position);
                    tmpCharArray = temp;
                }
                tmpCharArray[position++] = (char) charRead;
            }
            return tmpCharArray;
        } catch (Exception e) {
            throw new IOException("Error reading credentials file: " + e.getMessage());
        }
    }

    /**
     * This method is used to check if a credential is expired, and remove it from the list if it is.
     */
    private void checkCredExpiration() {
        // check if creds are expired and remove them from the list
        for (Map.Entry<String, Integer> entry : this.credsMap.entrySet()) {
            String key = entry.getKey();
            Integer credPosition = entry.getValue();
            if (key.contains("_")) {
                String[] keyParts = key.split("_");
                if (keyParts.length == 2) {
                    long timestamp = Long.parseLong(keyParts[1]);
                    if (System.currentTimeMillis() - timestamp > 300000) {
                        Arrays.fill(this.credsList.get(credPosition), '0');
                        this.credsList.remove(credPosition);
                        this.credsMap.remove(key);
                    }
                }
            }
        }
    }
}
