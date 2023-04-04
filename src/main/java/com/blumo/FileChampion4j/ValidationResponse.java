package com.blumo.FileChampion4j;


/**
 * This class is used to return the results of a file validation.
 */

public class ValidationResponse {
    private final boolean isValid;
    private final String resultsInfo;
    private final String cleanFileName;
    private final byte[] fileBytes;
    private final String fileChecksum;
    private final String[] validFilePath;

    /**
     * ValidationResponse is used to return the results of a file validation.
    * @param isValid (Boolean) true if the file is valid, false otherwise
    * @param resultsInfo (String) a String containing the results of the validation
    * @param fileBytes (bytes[]) the file bytes
    * @param fileChecksum (String) the file checksum
    * @param validFilePath[0] (String) optional valid file path if outputDir was set in the FileChampion4j constructor
    * @return ValidationResponse (ValidationResponse) object with FileValidator.validateFile() results
    */
    public ValidationResponse(boolean isValid, String resultsInfo, String cleanFileName, byte[] fileBytes, String fileChecksum, String... validFilePath) {
        this.isValid = isValid;
        this.resultsInfo = resultsInfo;
        this.fileBytes = fileBytes;
        this.fileChecksum = fileChecksum != null ? fileChecksum : "null";
        this.validFilePath = validFilePath;
        this.cleanFileName = cleanFileName;
    }

    public boolean isValid() {
        return isValid;
    }

    public String resultsInfo() {
        return resultsInfo;
    }

    public String getCleanFileName() {
        return cleanFileName;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public String[] getValidFilePath() {
        return validFilePath != null ? validFilePath : new String[0];
    }
}
