package com.blumo.FileSentry4J;

import java.io.File;

public record ValidationResponse(boolean isValid, String failureReason, File fileBytes, String fileChecksum) {
    public ValidationResponse(boolean isValid, String failureReason, File fileBytes, String fileChecksum) {
        this.isValid = isValid;
        this.failureReason = failureReason;
        this.fileBytes = fileBytes;
        this.fileChecksum = fileChecksum != null ? fileChecksum : "null";
    }
}
