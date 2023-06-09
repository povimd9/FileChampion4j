/*
 @licstart  The following is the entire license notice for the JavaScript code in this file.

 The MIT License (MIT)

 Copyright (C) 1997-2020 by Dimitri van Heesch

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge, publish, distribute,
 sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or
 substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 @licend  The above is the entire license notice for the JavaScript code in this file
*/
var NAVTREE =
[
  [ "filechampion4j", "index.html", [
    [ "About FileChampion4j", "index.html", null ],
    [ "Introduction", "md__c___users__user1_git__file_champion4j_wiki__home.html", [
      [ "Welcome", "md__c___users__user1_git__file_champion4j_wiki__home.html#autotoc_md8", null ],
      [ "Benefits of using FileChampion4j", "md__c___users__user1_git__file_champion4j_wiki__home.html#autotoc_md9", null ],
      [ "Who is FileChampion4j for?", "md__c___users__user1_git__file_champion4j_wiki__home.html#autotoc_md10", null ],
      [ "FileChampion4j Roadmap", "md__c___users__user1_git__file_champion4j_wiki__home.html#autotoc_md11", null ],
      [ "Contributing", "md__c___users__user1_git__file_champion4j_wiki__home.html#autotoc_md12", [
        [ "High Level Flow", "md__c___users__user1_git__file_champion4j_wiki__home.html#autotoc_md13", null ]
      ] ],
      [ "Contact", "md__c___users__user1_git__file_champion4j_wiki__home.html#autotoc_md14", null ]
    ] ],
    [ "Configuration", "md__c___users__user1_git__file_champion4j_wiki__configuration.html", [
      [ "JSON Structure", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md38", null ],
      [ "General Options", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md39", [
        [ "Checksums", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md40", null ]
      ] ],
      [ "Defining Validations", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md41", [
        [ "Validation Options", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md42", null ]
      ] ],
      [ "Defining Plugins", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md43", [
        [ "Introduction", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md44", null ],
        [ "Variables for injection/extraction of steps fields", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md45", null ],
        [ "Plugins Options", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md46", null ]
      ] ],
      [ "Defining Logging Level", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md47", null ],
      [ "Example JSON", "md__c___users__user1_git__file_champion4j_wiki__configuration.html#autotoc_md48", null ]
    ] ],
    [ "Usage", "md__c___users__user1_git__file_champion4j_wiki__usage.html", [
      [ "Getting Started", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md17", null ],
      [ "Importing filechampion4j", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md18", [
        [ "Import from Maven Central", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md19", null ],
        [ "Build from source", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md20", null ],
        [ "Use as JAR", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md21", null ],
        [ "Importing Library", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md22", null ]
      ] ],
      [ "Configuring", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md23", null ],
      [ "Creating a Validator Object", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md24", null ],
      [ "Validating Files", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md25", [
        [ "FileValidator.validateFile() Options", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md26", [
          [ "validateFile (String fileCategory, byte[] originalFile, String fileName)", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md27", null ],
          [ "validateFile (String fileCategory, byte[] originalFile, String fileName, Path outputDir)", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md28", null ],
          [ "validateFile (String fileCategory, byte[] originalFile, String fileName, String mimeString)", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md29", null ],
          [ "validateFile (String fileCategory, byte[] originalFile, String fileName, Path outputDir, String mimeString)", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md30", null ],
          [ "validateFile (String fileCategory, Path filePath, String fileName)", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md31", null ],
          [ "validateFile (String fileCategory, Path filePath, String fileName, Path outputDir)", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md32", null ],
          [ "validateFile (String fileCategory, Path filePath, String fileName, String mimeString)", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md33", null ],
          [ "validateFile (String fileCategory, Path filePath, String fileName, Path outputDir, String mimeString)", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md34", null ]
        ] ],
        [ "Response of validateFile() is a ValidationResponse object, which contains", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md35", null ]
      ] ],
      [ "Performance Considerations", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md36", null ],
      [ "Usage Example", "md__c___users__user1_git__file_champion4j_wiki__usage.html#autotoc_md37", null ]
    ] ],
    [ "Packages", "namespaces.html", [
      [ "Package List", "namespaces.html", null ]
    ] ],
    [ "Classes", "annotated.html", [
      [ "Class List", "annotated.html", "annotated_dup" ],
      [ "Class Index", "classes.html", null ],
      [ "Class Hierarchy", "hierarchy.html", "hierarchy" ],
      [ "Class Members", "functions.html", [
        [ "All", "functions.html", "functions_dup" ],
        [ "Functions", "functions_func.html", null ],
        [ "Variables", "functions_vars.html", null ]
      ] ]
    ] ],
    [ "Files", "files.html", [
      [ "File List", "files.html", "files_dup" ]
    ] ]
  ] ]
];

var NAVTREEINDEX =
[
"_calculate_checksum_8java.html",
"classdev_1_1filechampion_1_1filechampion4j_1_1_validation_response.html#a2834964df0ea03005603547d7defff68"
];

var SYNCONMSG = 'click to disable panel synchronisation';
var SYNCOFFMSG = 'click to enable panel synchronisation';