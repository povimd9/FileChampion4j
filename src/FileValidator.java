import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

// names to consider for the project: FileFilter4J, FileShield4J, FileSentry4J
// FileValidator class to validate file types. This class can be used by any application to validate file types.
// The configuration for file types is stored in a JSON file. The JSON file is parsed into a Map object.
// The Map object is passed to the FileValidator constructor.
// The FileValidator class contains a validateFileType method that takes a file type and a file as input.
// The file type is used to look up the configuration for that file type in the Map object.
// The file is validated against the configuration for the file type.
// The validateFileType method returns a ValidationResponse object that contains a boolean indicating whether the file is valid or not.
// If the file is not valid, the ValidationResponse object contains a failure reason.
// TODO: add try/catch blocks to handle specific exceptions
public class FileValidator {
    private final Map<String, Object> configMap;

    public FileValidator(Map<String, Object> configMap) {
        this.configMap = configMap;
    }

    public ValidationResponse validateFileType(String fileType, File originalFile) throws IOException {
        byte[] fileBytes = Files.readAllBytes(originalFile.toPath());

        // Check that the file type is not null or empty
        if (Objects.isNull(fileType) || fileType.isEmpty()) {
            return new ValidationResponse(false, "File type cannot be null or empty.", originalFile, null);
        }

        try {
            // Check that the file exists
            Map<String, Object> fileTypeConfig = (Map<String, Object>) configMap.get(fileType);
            if (Objects.isNull(fileTypeConfig)) {
                return new ValidationResponse(false, "Invalid file type: " + fileType, originalFile, null);
            }

            // Check that the file is not empty
            List<String> allowedExtensions = (List<String>) fileTypeConfig.get("allowed_extensions");
            if (Objects.isNull(allowedExtensions) || allowedExtensions.isEmpty()) {
                return new ValidationResponse(false, "No allowed extensions found for file type: " + fileType, originalFile, null);
            }

            // Check that the file extension is allowed
            String fileExtension = getFileExtension(originalFile.getName());
            if (fileExtension.isEmpty() || !allowedExtensions.contains(fileExtension)) {
                return new ValidationResponse(false, "File extension not allowed or not configured: " + fileType, originalFile, null);
            }

            // Check that the file size is not greater than the maximum allowed size
            String mimeType = Files.probeContentType(originalFile.toPath());
            Map<String, Object> extensionConfig = (Map<String, Object>) fileTypeConfig.get(fileExtension);
            if (extensionConfig == null) {
                return new ValidationResponse(false, "mime_type not configured for file extension: " + fileExtension, originalFile, null);
            }

            // Check that the mime type is allowed
            String expectedMimeType = (String) extensionConfig.get("mime_type");
            if (Objects.isNull(expectedMimeType) || !expectedMimeType.equals(mimeType)) {
                return new ValidationResponse(false, "Invalid mime_type for file extension: " + fileExtension, originalFile, null);
            }

            // Check that the file size is not greater than the maximum allowed size
            String magicBytesPattern = (String) extensionConfig.get("magic_bytes");
            if (Objects.isNull(magicBytesPattern) || !containsMagicBytes(fileBytes, magicBytesPattern)) {
                return new ValidationResponse(false, "Invalid magic_bytes for file extension: " + fileExtension, originalFile, null);
            }

            // Check header signatures
            String headerSignaturesPattern = (String) extensionConfig.get("header_signatures");
            if (!Objects.isNull(headerSignaturesPattern) && !containsHeaderSignatures(fileBytes, headerSignaturesPattern)) {
                return new ValidationResponse(false, "Invalid header_signatures for file extension: " + fileExtension, originalFile, null);
            }

            // Check footer signatures
            String footerSignaturesPattern = (String) extensionConfig.get("footer_signatures");
            if (!Objects.isNull(footerSignaturesPattern) && !containsFooterSignatures(fileBytes, footerSignaturesPattern)) {
                return new ValidationResponse(false, "Invalid footer_signatures for file extension: " + fileExtension, originalFile, null);
            }

            // Get the custom validator configuration
            Map<String, Object> pdfConfig = (Map<String, Object>) fileTypeConfig.get(fileType);
            Map<String, Object> customValidatorConfig = (Map<String, Object>) pdfConfig.get("custom_validators");
            if (!Objects.isNull(customValidatorConfig)) {
                // Iterate over the custom validators and invoke them using the CustomFileLoader class
                for (String jarPath : customValidatorConfig.keySet()) {
                    List<String> customValidator = (List<String>) customValidatorConfig.get(jarPath);
                    String className = customValidator.get(0);
                    String methodName = customValidator.get(1);
                    try {
                        // Invoke the method using CustomFileLoader class
                        String filePath = originalFile.getAbsolutePath();
                        CustomFileLoader.main(new String[]{jarPath, className, methodName, filePath});
                    } catch (Exception e) {
                        return new ValidationResponse(false, "An error occurred while invoking custom validator: " + e.getMessage(), originalFile, null);
                    }
                }
            }

            // Check if the file name should be encoded
            Boolean nameEncode = (Boolean) extensionConfig.get("name_encoding");
            if (!Objects.isNull(nameEncode) && nameEncode) {
                String encodedFileName = Base64.getEncoder().encodeToString(originalFile.getName().getBytes());
                File encodedFile = new File(originalFile.getParentFile(), encodedFileName);
                boolean success = originalFile.renameTo(encodedFile);
                if (!success) {
                    return new ValidationResponse(false, "Failed to rename file: " + originalFile.getName(), originalFile, null);
                }
            }

            // Calculate SHA-256 checksum of file
            byte[] sha256Bytes = null;
            try {
                MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
                sha256Digest.update(fileBytes);
                sha256Bytes = sha256Digest.digest();
            } catch (NoSuchAlgorithmException e) {
                return new ValidationResponse(false, "Failed to calculate checksum of file: " + originalFile.getName(), originalFile, null);
            }

            // Convert SHA-256 bytes to Base64 string
            String sha256Checksum = Base64.getEncoder().encodeToString(sha256Bytes);

            return new ValidationResponse(true, "File is valid", originalFile, sha256Checksum);
        } catch (Exception e) {
            return new ValidationResponse(false, "An error occurred while reading file: " + e.getMessage(), originalFile, null);
        }
    }

    // Helper methods
    // ==============
    // Get the file extension from the file name
    private @NotNull String getFileExtension(String fileName) {
        if (Objects.isNull(fileName) || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    // Check if the file contains the magic bytes
    private boolean containsMagicBytes(byte[] fileBytes, String magicBytesPattern) {
        if (Objects.isNull(fileBytes) || magicBytesPattern == null || magicBytesPattern.isEmpty()) {
            return false;
        }
        magicBytesPattern = magicBytesPattern.replaceAll("\\s", "");
        if (magicBytesPattern.length() % 2 != 0) {
            magicBytesPattern = "0" + magicBytesPattern;
        }
        byte[] magicBytes = new byte[magicBytesPattern.length() / 2];
        for (int i = 0; i < magicBytesPattern.length(); i += 2) {
            magicBytes[i / 2] = (byte) Integer.parseInt(magicBytesPattern.substring(i, i + 2), 16);
        }
        for (int i = 0; i < fileBytes.length - magicBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < magicBytes.length; j++) {
                if (fileBytes[i + j] != magicBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    // Check if the file contains the header signatures
    private boolean containsHeaderSignatures(byte[] fileBytes, String headerSignaturesPattern) {
        if (Objects.isNull(headerSignaturesPattern) || headerSignaturesPattern.isEmpty()) {
            return true;
        }
        String hexPattern = headerSignaturesPattern.replaceAll("\\s", "");
        if (hexPattern.length() % 2 != 0) {
            hexPattern = "0" + hexPattern;
        }
        byte[] headerSignatures = new byte[hexPattern.length() / 2];
        for (int i = 0; i < hexPattern.length(); i += 2) {
            headerSignatures[i / 2] = (byte) Integer.parseInt(hexPattern.substring(i, i + 2), 16);
        }
        for (int i = 0; i < headerSignatures.length; i++) {
            if (i >= fileBytes.length || fileBytes[i] != headerSignatures[i]) {
                return false;
            }
        }
        return true;
    }

    // Check if the file contains the footer signatures
    private boolean containsFooterSignatures(byte[] fileBytes, String footerSignaturesPattern) {
        if (Objects.isNull(footerSignaturesPattern) || footerSignaturesPattern.isEmpty()) {
            return true;
        }
        String hexPattern = footerSignaturesPattern.replaceAll("\\s", "");
        if (hexPattern.length() % 2 != 0) {
            hexPattern = "0" + hexPattern;
        }
        byte[] footerSignatures = new byte[hexPattern.length() / 2];
        for (int i = 0; i < hexPattern.length(); i += 2) {
            footerSignatures[i / 2] = (byte) Integer.parseInt(hexPattern.substring(i, i + 2), 16);
        }
        int footerStartIndex = fileBytes.length - footerSignatures.length;
        if (footerStartIndex < 0) {
            return false;
        }
        for (int i = 0; i < footerSignatures.length; i++) {
            if (fileBytes[footerStartIndex + i] != footerSignatures[i]) {
                return false;
            }
        }
        return true;
    }

}