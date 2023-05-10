```java
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.filechampion.filechampion4j.FileValidator;
import dev.filechampion.filechampion4j.ValidationResponse;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        // Path to the file to be validated in this simple example
        File pdfFile = new File("samples/In/testSmall.pdf");

        // Path to the config.json file
        String filePath = "config/config.json";

        // Placeholders for the JSON object and the file in bytes
        JSONObject jsonObject = null;
        byte[] fileInBytes = null;
        FileValidator validator = null;

        // Path to the output directory
        Path outDir = Paths.get("samples/Out/");

        // Create a new FileValidator object with json config file
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            jsonObject = new JSONObject(jsonContent);
            // Create a new FileValidator object
            validator = new FileValidator(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error reading config file");
            System.exit(1);
        }

        try {
            // Read the file to be validated into a byte array
            fileInBytes = Files.readAllBytes(pdfFile.toPath());

            // Validate the file
            ValidationResponse fileValidationResults = validator.validateFile("SmallDocuments", fileInBytes, pdfFile.getName(),outDir);

            // Check if the file is valid
            if (fileValidationResults.isValid()) {
                // Print the results if the file is valid
                String validMessage = String.format("%s is a valid document file.%n New file: %s, Checksum: %s",
                        fileValidationResults.resultsInfo(),
                        fileValidationResults.getValidFilePath().length == 0 ? "" : fileValidationResults.getValidFilePath()[0],
                        fileValidationResults.getFileChecksum());
                System.out.println(validMessage);
            } else {
                // Print the results if the file is invalid
                System.out.println(pdfFile.getName() + " is not a valid document file  because " + fileValidationResults.resultsDetails());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
```
