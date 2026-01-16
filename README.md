# axmlprinter
[![Build Status](https://travis-ci.org/rednaga/axmlprinter.svg?branch=master)](https://travis-ci.org/rednaga/axmlprinter) [![Coverage Status](https://img.shields.io/coveralls/rednaga/axmlprinter.svg)](https://coveralls.io/r/rednaga/axmlprinter?branch=master)

This is a heavily (almost 100%?) refactor of the Android4ME `axmlprinter` library/code. Personally I
had used the library for years without much issue and it was fast/useful just for printing out Android
Manifest files. In the past few years due to people mucking with AXML files and my own boredom, I've
refactored most of the code to be tested and more useful in non-command line situations.

## Features

- **Dual Format Support**: Supports both traditional AXML format and Protocol Buffers (protobuf) format
  - Traditional AXML format (used in standard APKs)
  - Protobuf format (used in Android App Bundles / AAB files)
- **Automatic Format Detection**: Automatically detects and handles the correct format
- **Read/Write Support**: Full read and write support for traditional AXML format
- **Read-Only Protobuf**: Protobuf format is read-only (conversion between formats is not supported)

## Usage

### Building

Compile the library:

```bash
./gradlew jar
```

### Command Line

Run the library:

```bash
java -jar build/libs/axmlprinter-*.jar <target> [output]
```

**Arguments:**
- `<target>`: Path to the binary XML file (AXML or protobuf format)
- `[output]`: (Optional) Output file path for AXML format only

**Examples:**

```bash
# Print AXML file to console
java -jar build/libs/axmlprinter-*.jar AndroidManifest.xml

# Print and save AXML file
java -jar build/libs/axmlprinter-*.jar AndroidManifest.xml output.xml

# Print protobuf format file (read-only)
java -jar build/libs/axmlprinter-*.jar manifest.pb
```


## Contributing
Contributions welcome! Please follow the simple steps;

1. Fork repository
2. Make changes
3. Ensure tests pass (or hopefully adding tests!)
4. Submit pull request/issue

If this is too much work, feel free to make an issue and upload a patch set for me to evaluate.

## Revision History

### v2.0.0
 - **New Feature**: Protocol Buffers (protobuf) format support
   - Support for Android App Bundle (AAB) XML format
   - Automatic format detection
   - Resource ID resolution for common Android framework resources
 - **Improvements**:
   - Enhanced error handling with detailed error messages
   - Improved namespace handling with proper scoping
   - Better null safety throughout the codebase
   - Optimized file I/O handling
   - Comprehensive unit test coverage for protobuf functionality
   - Enhanced error messages in ChunkUtil with byte offset information

### v1.0.0
 - Fix issue #5
   - Proper parsing of protection levels

### v0.3.0
 - Fix all of issue #8
   - Support out of order string tables
   - Support "mangled" start tags
   - Support other axml oddities

### v0.2.0
 - Fix the bulk of issue #8

### v0.1.0 (Initial Release)
 - "Just as good" as original code
 - 0% -> ~40% test coverage

## License

    Copyright (c) 2015-2025 Red Naga - Tim 'diff' Strazzere
    Copyright (c) 2008 Android4ME

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
