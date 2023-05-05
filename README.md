# About FileChampion4j

[![codecov](https://codecov.io/gh/povimd9/FileChampion4j/branch/master/graph/badge.svg?token=WUCKTU7ALO)](https://codecov.io/gh/povimd9/FileChampion4j)
![Build Status](https://github.com/povimd9/FileChampion4j/actions/workflows/master_build_workflow.yml/badge.svg)

Thank you for your interest in FileChampion4j, a robust, secure, and flexible file validation library for Java. Documentation can be found via javacdoc and under the project WIKI section.

## Introduction

FileChampion4j is a powerful and flexible Java library for validating and processing files. The library can be used to check files for a variety of properties, including mime type, magic bytes, header signatures, footer signatures, maximum size, and more. The library can also execute extension plugins that are defined for the file type.

**See [FileChampion4j Wiki](https://github.com/povimd9/FileChampion4j/wiki) for detailed instructions on configurations and usage.**

**See [FileChampion4j Technical Stack](https://docs.filechampion.dev/) for javadocs, and class details (generated with Doxygen, Graphviz, and PlantUML).**

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

Latest release can be found under [***Releases***](https://github.com/povimd9/FileChampion4j/releases), and published to Maven Central, see [***Importing FileChampion***](https://github.com/povimd9/FileChampion4j/wiki/Usage#importing-filechampion4j) for more details.

Compiled JARs of release versions, including slim/fat JARs, can be found on the `release-*` branches under 'target' directory.


### Compatibility

FileChampion4j is intended to support Windows/Linux platforms, running any active LTS Java runtime versions. Builds are tested and packaged for supported environments. Merges and new releases must pass security, functional, and performance tests for supported environments.

If you have any questions about FileChampion4j, please feel free to contact the project team. The project team is available to answer questions and provide support.

If you found any issues or ideas, please open a relevant issue in this project.

### Contributing

If you would like to contribute to FileChampion4j, please feel free to fork the project on GitHub. The project team welcomes contributions of all kinds, including bug fixes, new features, and documentation improvements.

### License

FileChampion4j is licensed under the Apache License, Version 2.0. For more information about the license, please see the LICENSE file in the project repository.
