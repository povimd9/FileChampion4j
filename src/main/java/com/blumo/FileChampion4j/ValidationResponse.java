package com.blumo.FileChampion4j;

import java.io.File;

public class ValidationResponse {
    private final boolean isValid;
    private final String resultsInfo;
    private final File fileBytes;
    private final String fileChecksum;

    public ValidationResponse(boolean isValid, String resultsInfo, File fileBytes, String fileChecksum) {
        this.isValid = isValid;
        this.resultsInfo = resultsInfo;
        this.fileBytes = fileBytes;
        this.fileChecksum = fileChecksum != null ? fileChecksum : "null";
    }

    public boolean isValid() {
        return isValid;
    }

    public String resultsInfo() {
        return resultsInfo;
    }

    public File getFileBytes() {
        return fileBytes;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }
}
