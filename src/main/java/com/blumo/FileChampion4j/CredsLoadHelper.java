package com.blumo.FileChampion4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This class is used to load credentials from a directory of files.
 * The files are read into a Map of char[] values upon first secret retrieval.
 * The char[] values are removed from the Map after 5 minutes.
 */
public class CredsLoadHelper {
    private char[] credsFileString;
    private ConcurrentHashMap<String, char[]> credValues = new ConcurrentHashMap<>();

    /**
     * This method is used to initialize the class with a directory path.
     * @param credName (String) The directory path of the credential files.
     */
    public CredsLoadHelper(String credsPathString) throws IOException {
        try {
            if (!initCredsValues(credsPathString).equals("Success")) {
                throw new IOException("Error reading credentials directory: " + credsPathString);
            }
        } catch (IOException e) {
            throw new IOException("Error reading credentials files: " + e.getMessage());
        }
    }

    /**
     * This method is used to set a single value of a credential in the Map.
     * @param credName (String) The name of the credential to which a time stamp is appended.
     * @param credValue (char[]) The value of the credential.
     */
    private void setCredValue(String credName) {
        long currentTime = System.currentTimeMillis();
        this.credValues.putIfAbsent(credName+"_"+currentTime, credsFileString);
    }

    /**
     * This method is used to load all credentials from a directory into the Map.
     * @param dir (String) The directory path of the credentials.
     * @return (String) "Success" if successful.
     * @throws IOException
     */
    private String initCredsValues(String dir) throws IOException {
        try {
            File folder = new File(dir);
            if (!folder.exists()) {
                throw new IOException("Directory does not exist: " + dir);
            }
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    getCredFileChars(dir + fileName);
                    setCredValue(fileName);
                }
            }
            return "Success";
        } catch (IOException e) {
            throw new IOException("Error reading credentials directory: " + e.getMessage());
        }
    }

    /**
     * This method is used to read a single credential file into a char[] for storage in the Map.
     * char[] grows dynamically as the file is read.
     * @param credFilePath
     * @return
     * @throws IOException
     */
    private void getCredFileChars(String credFilePath) throws IOException {
        try   {
            BufferedReader br = new BufferedReader(new FileReader(credFilePath));
            credsFileString = new char[128]; // Initial size of the character array
            int charRead = 0;
            int position = 0;
            while((charRead = br.read()) != -1) {
                if(position == credsFileString.length) {
                    char[] temp = new char[credsFileString.length * 2];
                    System.arraycopy(credsFileString, 0, temp, 0, position);
                    credsFileString = temp;
                }
                credsFileString[position++] = (char) charRead;
            }
            br.close();
        } catch (Exception e) {
            throw new IOException("Error reading credentials file: " + e.getMessage());
        }
    }

    /**
     * This method is used to remove credentials that are over 5 minutes old from the Map.
     */
    private void removeCredValues() {
        long currentTime = System.currentTimeMillis();
        Set<String> credNames = new HashSet<>(this.credValues.keySet());
        for (String credName : credNames) {
            long credTime = Long.parseLong(credName.substring(credName.lastIndexOf("_")+1));
            if (credTime - currentTime > 300000) {
                char[] value = this.credValues.get(credName);
                for (int i = 0; i < value.length; i++) {
                    value[i] = ' ';
                }
                this.credValues.put(credName, value);
                this.credValues.remove(credName);
            }
        }
    }


    /**
     * This method is used to retrieve a credential from the Map.
     * @param credName (String) The name of the credential to retrieve.
     * @return (char[]) The value of the credential if it exists, otherwise an empty char[].
     */
    public char[] getCredValue(String credName) {
        removeCredValues();
        Set<String> credNames = this.credValues.keySet();
        for (String cred : credNames) {
            if (cred.startsWith(credName)) {
                return this.credValues.get(cred);
            }
            try {
                getCredFileChars(credName+".secrets");
                setCredValue(credName);
                return this.credValues.get(credName);
            } catch (IOException e) {
                return "".toCharArray();
            }
        }
        return "".toCharArray();
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
        if (credValues != null) {
            for (Map.Entry<String, char[]> entry : credValues.entrySet()) {
                char[] value = entry.getValue();
                for (int i = 0; i < value.length; i++) {
                    value[i] = ' ';
                }
            }
            credValues.clear();
        }
    }
}
