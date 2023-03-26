package com.blumo.FileSentry4J;

import java.io.File;

public class ValidationResponse {
    private final boolean isValid;
    private final String failureReason;
    private final File fileBytes;
    private final String fileChecksum;

    public ValidationResponse(boolean isValid, String failureReason, File fileBytes, String fileChecksum) {
        this.isValid = isValid;
        this.failureReason = failureReason;
        this.fileBytes = fileBytes;
        this.fileChecksum = fileChecksum != null ? fileChecksum : "null";
    }

    public boolean isValid() {
        return isValid;
    }

    public String failureReason() {
        return failureReason;
    }

    public File getFileBytes() {
        return fileBytes;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }
}
