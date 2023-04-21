# About FileChampion4j

[![codecov](https://codecov.io/gh/povimd9/FileChampion4j/branch/master/graph/badge.svg?token=WUCKTU7ALO)](https://codecov.io/gh/povimd9/FileChampion4j)
![Build Status](https://github.com/povimd9/FileChampion4j/actions/workflows/master_build_workflow.yml/badge.svg)

Thank you for your interest in FileChampion4j, a robust, secure, and flexible file validation library for Java. Documentation can be found via javacdoc and under the project WIKI section.

## Introduction

FileChampion4j is a powerful and flexible Java library for validating and processing files. The library can be used to check files for a variety of properties, including mime type, magic bytes, header signatures, footer signatures, maximum size, and more. The library can also execute extension plugins that are defined for the file type.

**See [FileChampion4j Wiki](https://github.com/povimd9/FileChampion4j/wiki) for detailed instructions on configurations and usage.**

### Features

- Easy to understand and configure for developers, operations, and security engineers.
- JSON-based configuration, supporting the ability to separate configurations from code.
- Flexible to support various integrations, including client-defined controls.
- Support for in-memory and on-disk validation.
- Validate files for a variety of properties, including mime type, magic bytes, header signatures, footer signatures, maximum size, filename cleanup/encoding, and owner/permissions of file.
- Custom plugins execution support for extended usability.
- Comprehensive error handling and reporting.

### Benefits

- Protect your system from malicious files.
- Ensure that files are of the correct type and size.
- Save time, effort, and risk of developing custom file validation code.
- Allow security engineers to define required file controls without code modification.
- Support easy auditing of controls by compliance officers and auditors.

### Releases

Working release versions, including slim/fat JARs, can be found on the `release-*` branches. A Maven Central package will be added for distribution soon.

### Support

FileChampion4j is intended to support Windows/Linux platforms, running any active LTS Java runtime versions. Builds are tested and packaged for supported environments. Merges and new releases must pass security, functional, and performance tests for supported environments.

If you have any questions about FileChampion4j, please feel free to contact the project team. The project team is available to answer questions and provide support.

If you found any issues or ideas, please open a relevant issue in this project.

### Contributing

If you would like to contribute to FileChampion4j, please feel free to fork the project on GitHub. The project team welcomes contributions of all kinds, including bug fixes, new features, and documentation improvements.

### License

FileChampion4j is licensed under the Apache License, Version 2.0. For more information about the license, please see the LICENSE file in the project repository.

### Basic Usage

The FileValidator class is initialized with a JSON configuration object. The FileValidator.validateFile() method is the main entry point for validating files. validateFile() takes 4 arguments:

- Configuration category - the category defined under the "Validations" object (e.g. "Documents")
- Target file bytes - a byte array of file content to be scanned
- File name - the original file name associated with the target file bytes
- Output directory - (optional) directory to which a validated file will be saved to (required for owner/permissions change)

validateFile() returns a ValidationResponse object, containing:

- isValid - a boolean indicating whether the file is valid or not
- resultsInfo - a string containing additional information about the validation pass/fail status
- fileBytes - the file bytes if the file is valid (original of new file if was modified by plugins)
- fileChecksum: the SHA-256 checksum of the file

### Configuration

Example configuration of validation controls and plugins. "Validations" and at least 1 child key is required for validation to work. Plugins and steps can be defined as necessary - current implementation only supports process execution, API support will come soon.

- ${filePath}, and ${fileChecksum} - can be used in 'endpoint' value to inject correspanding values
- ${fileContent} - can be used in 'endpoint' value to inject file content in base64 encoding
- ${STEP_NAME.filePath} - can be used to extract file path content and replace original file bytes
- ${STEP_NAME.fileContent} - can be used to extract base64 file content from base64 encoded bytes

```javascript
{
  "Validations": {
    "Documents": {
      "pdf": {
        "mime_type": "application/pdf",
        "magic_bytes": "25504446",
        "header_signatures": "25504446",
        "footer_signatures": "2525454f46",
        "change_ownership": true,
        "change_ownership_user": "User1",
        "change_ownership_mode": "r",
        "name_encoding": true,
        "max_size": "126000",
        "extension_plugins": ["do_anti_virus_scan.step1", "handle_pdf_documents.step1", "handle_pdf_documents.step2"]
      },
      "doc": {
        "mime_type": "application/msword",
        "magic_bytes": "D0CF11E0A1B11AE1",
        "header_signatures": "D0CF11E0A1B11AE1",
        "footer_signatures": "0000000000000000",
        "change_ownership": true,
        "change_ownership_user": "User1",
        "change_ownership_mode": "r",
        "name_encoding": true,
        "max_size": "4000"
      }
    },
    "Images": {
      "jpg": {
        "mime_type": "image/jpeg",
        "magic_bytes": "FFD8",
        "header_signatures": "FFD8FF",
        "footer_signatures": "FFD9",
        "change_ownership": true,
        "change_ownership_user": "User1",
        "change_ownership_mode": "r",
        "name_encoding": true,
        "max_size": "4000"
        },
      "png": {
        "mime_type": "image/png",
        "magic_bytes": "89504E470D0A1A0A",
        "header_signatures": "89504E470D0A1A0A0000000D49484452",
        "footer_signatures": "49454E44AE426082",
        "change_ownership": true,
        "change_ownership_user": "User1",
        "change_ownership_mode": "r",
        "name_encoding": true,
        "max_size": "4000"
      }
    }
  },
  "Plugins": {
    "do_anti_virus_scan": {
      "step1.step": {
        "type": "cli",
        "run_before": true,
        "endpoint": "curl -X POST -F \"file=@${filePath}\" https://avscanner:8080/scan",
        "timeout": 320,
        "on_timeout_or_fail": "fail",
        "response": "Success: ${step1.filePath}"
      }
    },
    "handle_pdf_documents": {
      "step1.step": {
        "type": "cli",
        "run_after": true,
        "endpoint": "remove_all_active_pdf_objects.sh ${filePath}",
        "timeout": 320,
        "on_timeout_or_fail": "fail",
        "response": "Success: ${step1.filePath}"
      },
      "step2.step": {
        "type": "cli",
        "run_after": true,
        "endpoint": "save_to_db.sh ${fileContent}",
        "timeout": 320,
        "on_timeout_or_fail": "fail",
        "response": "Success: ${step1.fileContent}"
      }
    }
  }
}
```

### Example

The following code shows a simple implementation example:

```java
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;
import dev.filechampion.filechampion4j.FileValidator;
import dev.filechampion.filechampion4j.ValidationResponse;

public class Main {
    public static void main(String[] args) {
        // Path to the file to be validated in this simple example
        File pdfFile = new File("samples/In/test.pdf");

        // Path to the config.json file
        String configPath = "config/config.json";

        // Placeholders for the JSON object and the file in bytes
        JSONObject jsonObject = null;
        byte[] fileInBytes = null;
        FileValidator validator = null;

        // Path to the output directory
        String outDir = "samples/Out/";

        // Create a new FileValidator object with json config file
        try {
            // Read the JSON object from the config.json file
            String jsonConfigContent = new String(Files.readAllBytes(Paths.get(configPath)));
            jsonObject = new JSONObject(jsonConfigContent);

            // Create a new FileValidator object
            validator = new FileValidator(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.out.println("Error creating FileValidator object");
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error reading config file");
            System.exit(1);
        }

        try {
            // Read the file to be validated into a byte array
            fileInBytes = Files.readAllBytes(pdfFile.toPath());

            // Validate the file
            ValidationResponse fileValidationResults = validator.validateFile("Documents", fileInBytes, pdfFile.getName(),outDir);

            // Check if the file is valid
            if (fileValidationResults.isValid()) {
                // Print the results if the file is valid
                String validMessage = String.format("%s is a valid document file.%n New file: %s, Checksum: %s",
                        fileValidationResults.resultsInfo(),
                        fileValidationResults.getValidFilePath().length == 0 ? "" : fileValidationResults.getValidFilePath()[0],
                        fileValidationResults.getFileChecksum());
                System.out.println(validMessage);
                System.exit(0);
            } else {
                // Print the results if the file is invalid
                System.out.println(pdfFile.getName() + " is not a valid document file  because " + fileValidationResults.resultsInfo());
                System.exit(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
```
