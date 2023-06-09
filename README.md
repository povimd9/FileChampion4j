# About FileChampion4j

[![Build, Test, and Bench](https://github.com/povimd9/FileChampion4j/actions/workflows/master_build_workflow.yml/badge.svg)](https://github.com/povimd9/FileChampion4j/actions/workflows/master_build_workflow.yml)
&nbsp;[![codecov](https://codecov.io/gh/povimd9/FileChampion4j/branch/master/graph/badge.svg?token=WUCKTU7ALO)](https://codecov.io/gh/povimd9/FileChampion4j)
&nbsp;[![License](https://img.shields.io/github/license/povimd9/FileChampion4j?style=plastic)](https://github.com/povimd9/FileChampion4j/blob/master/LICENSE)
&nbsp;[![Maven Central](https://img.shields.io/maven-central/v/dev.filechampion/filechampion4j?color=blue&style=plastic)](https://central.sonatype.com/artifact/dev.filechampion/filechampion4j)

FileChampion4j is a powerful and flexible Java library for validating and processing files. The library can be used to check files for a variety of properties, including mime type, magic bytes, header signatures, footer signatures, maximum size, and more. The library can also execute extension plugins that are defined for the file type.

**See [FileChampion4j Wiki](https://github.com/povimd9/FileChampion4j/wiki) for detailed instructions on configurations and usage.**

**See [FileChampion4j Docs](https://www.filechampion.dev/) for comprehensive documentations and design diagrams (generated with help of Doxygen, Graphviz, and PlantUML).**

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

### Compatibility

FileChampion4j is intended to support Windows/Linux platforms, running any active LTS Java runtime versions. Builds are tested and packaged for supported environments. Merges and new releases must pass security, functional, and performance tests for supported environments.

If you have any questions about FileChampion4j, please feel free to contact the project team. The project team is available to answer questions and provide support.

If you found any issues or ideas, please open a relevant issue in this project.

### Contributing

If you would like to contribute to FileChampion4j, please feel free to fork the project on GitHub. The project team welcomes contributions of all kinds, including bug fixes, new features, and documentation improvements.

### License

FileChampion4j is licensed under the Apache License, Version 2.0. For more information about the license, please see the LICENSE file in the project repository.
