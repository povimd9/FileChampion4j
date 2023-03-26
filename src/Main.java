import java.io.File;
import java.io.IOException;
import java.util.*;

// This class is used to validate files against a set of rules
// ValidationResponse contains the results of the validation with:
// - isValid: a boolean indicating whether the file is valid or not
// - failureReason: a string containing the reason for failure if the file is invalid
// - fileBytes: the file bytes if the file is valid
// - fileChecksum: the file checksum if the file is valid

public class Main {
    public static void main(String[] args) throws IOException {
        Map<String, Object> configMap = ConfigParser.parseConfig("config/config.json");
        FileValidator validator = new FileValidator(configMap);

        File pdfFile = new File("samples/CUDA_practice.pdf");
        ValidationResponse fileValidationResults = validator.validateFileType("Documents", pdfFile);

        if (fileValidationResults.isValid()) {
            System.out.println(pdfFile.getName() + " is a valid document file");
        } else {
            System.out.println(pdfFile.getName() + " is not a valid document file  because " + fileValidationResults.getFailureReason());
        }
    }
}