package dev.filechampion.filechampion4j;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * CredentialsManager class is used to manage any credendials that are needed for plugins, such as API keys, etc.
 */
public class CredentialsManager {
    private static final Logger LOGGER = Logger.getLogger(CredentialsManager.class.getName());
    private StringBuilder logMessage = new StringBuilder();
    private long credsExpirationTime = 300000;
    private Path credsPath;
    private IndexSynchronizer credsSynchronizer = new IndexSynchronizer();
    private List<String> credsNamesList;
    private List<char[]> credsList = new LinkedList<>();
    private Map<String, Map<Integer, Long>> credsMap = new HashMap<>();
    private SecureRandom random = new SecureRandom();
    private String randomString = "";
    private int randomStringIndex = 0;
    private boolean expirationTimerStarted = false;

    /**
     * Constructor for CredentialsManager class.
     * @param credsPath (Path) - Path to the directory where the credentials files are stored.
     * @param credsNamesList (List &lt;String&gt;) - List of the names of the credentials names/files.
     * @throws IllegalArgumentException - If the credsPath is null or does not exist, or if the credsNamesList is null or empty, or if any of the credentials files are not found.
     */
    public CredentialsManager(Path credsPath, List<String> credsNamesList) throws IllegalArgumentException {
        if (credsPath == null || !Files.exists(credsPath)) {
            logSevere(logMessage.replace(0, logMessage.length() ,"Defined credentials path was not found."));
            throw new IllegalArgumentException("Defined credentials path was not found.");
        }
        if (credsNamesList == null || credsNamesList.isEmpty()) {
            logSevere(logMessage.replace(0, logMessage.length() ,"Defined credentials list was empty."));
            throw new IllegalArgumentException("Defined credentials list was empty.");
        }
        this.credsPath = credsPath;

        // Check that all creds files exist
        for (String credsName : credsNamesList) {
            Path credFilePath = credsPath.resolve(credsName);
            if (!Files.exists(credFilePath)) {
                logSevere(logMessage.replace(0, logMessage.length() ,"Credentials file ").append(credsName).append(" was not found."));
                throw new IllegalArgumentException("Credentials file " + credsName + " was not found.");
            }
        }
        this.credsNamesList = credsNamesList;
        logInfo(logMessage.replace(0, logMessage.length() ,"CredentialsManager initialized.")); 
    }

    /**
     * This method allows the user to set the expiration time for credentials in milliseconds, overriding the default of 300000 (5 minutes).
     * @param expirationTime (long) - The expiration time in milliseconds.
     * @throws IllegalArgumentException - If the expirationTime is less than or equal to 0.
     */
    public void setExpirationTime(long expirationTime) throws IllegalArgumentException {
        if (expirationTime <= 0) {
            logSevere(logMessage.replace(0, logMessage.length() ,"Expiration time must be greater than 0."));
            throw new IllegalArgumentException("Expiration time must be greater than 0.");
        }
        this.credsExpirationTime = expirationTime;
        logInfo(logMessage.replace(0, logMessage.length() ,"Expiration time set to ").append(expirationTime).append(" milliseconds."));
        // add timer to remove expired creds from the list
        Timer timer = new Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                checkCredExpiration(credsExpirationTime);
            }
        }, credsExpirationTime / 2 , credsExpirationTime / 2);
        logFine(logMessage.replace(0, logMessage.length() ,"Scheduled timer to check for expired credentials every ").append(expirationTime).append(" milliseconds."));
    }

    /**
     * This method is used to retrieve a credential from the cached credentials list or read it from the file and add it to the list.
     * @param credsName (String) - Name of the credentials name/file to retrieve.
     * @return (char[]) - The credentials as a char[].
     * @throws IllegalArgumentException - If the credsName is null or empty, or if the credsName is not found in the credsNamesList.
     * @throws CredentialsFetchError - If there is an error reading the credentials file or adding the credentials to the list.
     */
    public char[] getCredentials(String credsName) throws IllegalArgumentException, CredentialsFetchError {
        if (credsName == null || credsName.isEmpty()) {
            logSevere(logMessage.replace(0, logMessage.length() ,"Credentials name was empty."));
            throw new IllegalArgumentException("Credentials name was empty.");
        }
        if (!this.credsNamesList.contains(credsName)) {
            logSevere(logMessage.replace(0, logMessage.length() ,"Credentials name was not found in credentials list."));
            throw new IllegalArgumentException("Credentials name was not found in credentials list.");
        }
        if (!expirationTimerStarted) {
            setExpirationTime(this.credsExpirationTime);
            expirationTimerStarted = true;
        }
        try {
            if (credsSynchronizer.getSecretValue(credsName)!=null) {
                logFine(logMessage.replace(0, logMessage.length() ,"Credentials ").append(credsName).append(" found in list."));
                return credsSynchronizer.getSecretValue(credsName);
            } else {
                logFine(logMessage.replace(0, logMessage.length() ,"Adding ").append(credsName).append(" to credentials list."));
                Path fullCredPath = this.credsPath.resolve(credsName);
                credsSynchronizer.addItem(addSalt(this.getCredFileChars(fullCredPath)), credsName);
                return credsSynchronizer.getSecretValue(credsName);
            }
        } catch (Exception e) {
            logSevere(logMessage.replace(0, logMessage.length() ,"Error retrieving credentials: ").append(e.getMessage()));
            throw new CredentialsFetchError("Error retrieving credentials: " + credsName);
        }
    }

    /**
     * This method is used to read a single credential file into a char[] for storage in the Map.
     * char[] grows dynamically as the file is read using 64 byte chunks.
     * @param credFilePath (Path) - Path to the credentials file to read.
     * @return (char[]) - The credentials as a char[].
     * @throws CredentialsFetchError - If there is an error reading the credentials file, or if the credentials file is empty.
     */
    private char[] getCredFileChars(Path credFilePath) throws CredentialsFetchError {
        logFine(logMessage.replace(0, logMessage.length() ,"Reading credentials from file: ").append(credFilePath.getFileName()));
        try (BufferedReader br = Files.newBufferedReader(Paths.get(credFilePath.toString()), Charset.defaultCharset())) {
            char[] tmpCharArray = new char[1];
            int charRead = 0;
            int position = 0;
            while((charRead = br.read()) != -1) {
                if(position == tmpCharArray.length) {
                    char[] temp = new char[tmpCharArray.length + 1];
                    System.arraycopy(tmpCharArray, 0, temp, 0, position);
                    tmpCharArray = temp;
                }
                tmpCharArray[position++] = (char) charRead;
            }
            logFine(logMessage.replace(0, logMessage.length() ,"Credentials read from file: ").append(credFilePath.toString()));
            if (tmpCharArray[0] == '\u0000') {
                throw new CredentialsFetchError("Credentials file was empty: " + credFilePath.getFileName());
            }
            return tmpCharArray;
        } catch (Exception e) {
            throw new CredentialsFetchError("Error reading credentials file: " + e.getMessage());
        }
    }

    

    ////////////////////
    // Helper methods //
    ////////////////////

    /**
     * LOGGER.info wrapper
     * @param message (String) - message to log
     */
    private void logInfo(StringBuilder message) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(message.toString());
        }
    }

    /**
     * LOGGER.severe wrapper
     * @param message (String) - message to log
     */
    private void logSevere(StringBuilder message) {
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe(message.toString());
        }
    }

    /**
     * LOGGER.fine wrapper
     * @param message (StringBuilder) - message to log
     */
    private void logFine(StringBuilder message) {
        if (LOGGER.isLoggable(Level.FINE )) {
            LOGGER.fine(message.toString());
        }
    }

    /**
     * Custom exception for credential fetch errors
     */
    public static class CredentialsFetchError extends Exception { 
        public CredentialsFetchError(String errorMessage) {
            super(errorMessage);
        }
    }

    /**
     * Generate a random string of a given length
     */
    private void generateRandomString() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 2048;
        this.randomString = random.ints(leftLimit, rightLimit + 1)
        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
        .limit(targetStringLength)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
        logFine(logMessage.replace(0, logMessage.length() ,"Random string generated."));
    }

    /**
     * Add unique salt to a given string.
     * @param charString (char[]) - The string to add salt to.
     * @return (char[]) - The salted string.
     */
    private char[] addSalt(char[] charString) {
        if (this.randomStringIndex + charString.length > this.randomString.length()) {
            generateRandomString();
            this.randomStringIndex = 0;
        }
        char[] saltChars = randomString.substring(randomStringIndex, randomStringIndex + charString.length).toCharArray();
        char[] saltedString = new char[charString.length * 2];
        int index = 0;
        for (int i = 0; i < charString.length; i++) {
            saltedString[index++] = charString[i];
            saltedString[index++] = saltChars[i];
        }
        this.randomStringIndex += saltChars.length;
        return saltedString;
    }

    /**
     * This method is used to check if any credential is expired, and remove them from the list if it is.
     */
    private void checkCredExpiration(Long credsExpirationTime) {
        credsSynchronizer.checkAndRemoveStaleSecrets(credsExpirationTime);
    }
}
