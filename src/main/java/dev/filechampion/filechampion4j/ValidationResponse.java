package dev.filechampion.filechampion4j;

import java.util.Map;

/**
 * This class is used to return the results of a file validation.
 */
public class ValidationResponse {
    private final boolean isValid;
    private final String resultsInfo;
    private final String resultsDetails;
    private final String cleanFileName;
    private final byte[] fileBytes;
    private final Map<String, String> fileChecksum;
    private final String[] validFilePath;

    /**
    * ValidationResponse is used to return the results of a file validation.
    * @param isValid (Boolean) true if the file is valid, false otherwise
    * @param resultsInfo (String) a String containing the result sammery of the validation
    * @param resultsDetails (String) a String containing the details of the validation
    * @param cleanFileName (String) the file name with all special characters replaced with underscores
    * @param fileBytes (bytes[]) the file bytes
    * @param fileChecksum (Map<String, String>) hash map containing the file checksums as 'algorithm' => 'checksum'
    * @param validFilePath (String) optional valid file path if outputDir was set in the filechampion4j constructor
    */
    public ValidationResponse(boolean isValid, String resultsInfo, String resultsDetails, String cleanFileName, byte[] fileBytes, Map<String, String> fileChecksum, String... validFilePath) {
        this.isValid = isValid;
        this.resultsInfo = resultsInfo;
        this.resultsDetails = resultsDetails;
        this.fileBytes = fileBytes;
        this.fileChecksum = fileChecksum.size() > 0 ? fileChecksum : null;
        this.validFilePath = validFilePath;
        this.cleanFileName = cleanFileName;
    }

    /**
     * Returns true if the file is valid, false otherwise
     * @return (Boolean) true if the file is valid, false otherwise
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Returns a String containing the result sammery of the validation
     * @return (String) a String containing the result sammery of the validation
     */
    public String resultsInfo() {
        return resultsInfo;
    }

    /**
     * Returns a String containing the details of the validation
     * @return (String) a String containing the details of the validation
     */
    public String resultsDetails() {
        return resultsDetails;
    }

    /**
     * Returns the file name with all special characters replaced with underscores
     * @return (String) the file name with all special characters replaced with underscores
     */
    public String getCleanFileName() {
        return cleanFileName;
    }

    /**
     * Returns the file bytes
     * @return (bytes[]) the file bytes of validated file
     */
    public byte[] getFileBytes() {
        return fileBytes;
    }

    /**
     * Returns the file SHA-256 checksum
     * @return (String) the file checksum
     */
    public Map<String, String> getFileChecksum() {
        return fileChecksum;
    }

    /**
     * Returns the valid file path if outputDir was set in the filechampion4j constructor
     * @return (String) the valid file path if outputDir was set in the filechampion4j constructor
     */
    public String[] getValidFilePath() {
        return validFilePath != null ? validFilePath : new String[0];
    }
}
