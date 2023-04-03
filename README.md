# FileChampion4j
A Java class for file validation.

## PLEASE NOTE THAT THIS IS IN EARLY DEVELOPMENT AND MISSING FUNCTIONALITY, SUCH AS HELPER CLEANING METHODS AND AV SCANS.

### Introduction
The FileSentry4j class is a Java class for file validation. It can be used to validate files against configured set of controls, and return machine and human readable validation results.

### Intent
The intent of this package is to provide a secure and reliable way to validate and sanitize files as bytes. The package will support a variety of file types and formats, and will be able to protect against common security risks such as file injection attacks and malicious file content.

The package will support (at least):
- Cross-platform, commonly in use java runtime versions
- Support for in-memory and on-disk validation
- A JSON configuration file for customizing the library's behavior
- Tool for generating validation configurations for known file types
- The ability to define custom validation and sanitization rules
- Comprehensive error handling and reporting
- Performance and scalability
- Support for multiple languages and character encodings
- Security features such as filename cleanup, file size checking, file type checking, SHA-256 checksum verification, AV check, and custom file type cleaning
- The ability to save validated files to a target directory with specific owner and permissions

The package should be easy to use and well-documented. It should be able to be integrated into any Java application.

The package should be secure and reliable, well maintained and continuously updated to add controls and mitigate arising risks.


### Configuration
The configuration for file categories and associated validations is 
stored in a JSON file and should be loaded as part of initiation using configParser.
Configurations are expected to take the following format, where file types (such as Documents) may be aggregated together.
When validating a file, only parent key of type is required in the argument for fileType (see example below).
<pre>
{
  "Documents": {
    "allowed_extensions": [
      "pdf",
      "doc"
    ],
    "pdf": {
      "mime_type": "application/pdf",
      "magic_bytes": "25504446",
      "header_signatures": "25504446",
      "footer_signatures": "2525454f46",
      "antivirus_scan": {
        "clamav_scan.java": [
          "RETURN_TYPE",
          "param1",
          "param2"
        ]},
      "change_ownership": true,
      "change_ownership_user": "User1",
      "change_ownership_group": "Users",
      "change_ownership_mode": "r",
      "name_encoding": true,
      "size_limit_validation": true,
      "max_size": "4000"
      },
    "doc": {
      "mime_type": "application/msword",
      "magic_bytes": "D0CF11E0A1B11AE1",
      "header_signatures": "D0CF11E0A1B11AE1",
      "footer_signatures": "0000000000000000",
      "antivirus_scan": {
        "clamav_scan.java": [
          "RETURN_TYPE",
          "param1",
          "param2"
        ]},
      "change_ownership": true,
      "change_ownership_user": "User1",
      "change_ownership_group": "Users",
      "change_ownership_mode": "r",
      "name_encoding": true,
      "size_limit_validation": true,
      "max_size": "4000"
    }
  }
}
</pre>

### Validation
The originalFile is validated against the configured controls for the file type. The validateFileType method returns a ValidationResponse object that contains:

* isValid: a boolean indicating whether the file is valid or not
* fileBytes: the file bytes if the file is valid
* fileChecksum: the file checksum if the file is valid
* resultsInfo: a string containing additional information about the validation results, such as reason for failure or the name of the file if it is valid

### Examples
The following code shows how to use the FileSentry4j class to validate a file:
<pre>
Map<String, Object> configMap = ConfigParser.parseConfig("config/config.json");
FileValidator validator = new FileValidator(configMap);

File pdfFile = new File("some_pdf_file.pdf"); // file to be validated
String outDir = "samples/Out"; // target directory for validated files
ValidationResponse fileValidationResults = validator.validateFileType("Documents", pdfFile, outDir); // validate pdfFile against config 'Documents' configurations

if (fileValidationResults.isValid()) {
    String cleanFileName = fileValidationResults.resultsInfo();
    System.out.println(cleanFileName + " is a valid document file. Checksum: " + fileValidationResults.getFileChecksum());
} else {
    System.out.println(pdfFile.getName() + " is not a valid document file  because " + fileValidationResults.resultsInfo());
}
</pre>
