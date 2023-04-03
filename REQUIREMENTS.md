Provide support for validating and sanitizing files as bytes, allowing developers to choose in-memory or on-disk mode depending on their use case.

Library should use a JSON configuration file, which should be well-documented and easy to use. This will enable engineers/developers to customize the library's behavior to fit their specific needs.

Provide a tool that produces validation configurations for known file types. This will make it easier for engineers/developers to get started quickly with the library without having to write custom rules.

Allow developers to define their own custom validation and sanitization rules. This will enable them to handle more complex scenarios and ensure that the library is adaptable to their specific needs.

Provide comprehensive error handling and reporting, including clear and detailed error messages, readable by machine and human to help diagnose issues, and track validations.

Ensure that the library is performant and scalable, even when handling large numbers of files or large file sizes.

Support any file type and formats with supporterd identification patterns.

Provide support for multiple languages and character encodings, ensuring that the library can handle files in a wide range of languages and formats.

Ensure that the library is secure and can protect against common security risks such as file injection attacks and malicious file content.

Validations should include:
- Perform filename cleanup on untrusted input, removing any potentially harmful characters or file extensions.
- Check the size of the file to ensure it is not too large and can be safely processed.
- Test the extension of the file to ensure its content matches the expected patterns of the file type, including its mime type, magic bytes, header and footer signatures.
- Calculate the SHA-256 checksum of the file to verify its integrity.
- Support AV check of the file against a client-defined solution to ensure it is free of malware or viruses.
- Allow calls to custom file type cleaners to sanitize certain types of files, such as PDF files containing hidden objects.
- Save the validated files to a target directory with specific owner and permissions, ensuring that the files are secured and accessible only by authorized users.
