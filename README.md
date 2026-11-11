# axmlprinter
[![Java CI with Gradle](https://github.com/rednaga/axmlprinter/actions/workflows/gradle.yml/badge.svg)](https://github.com/rednaga/axmlprinter/actions/workflows/gradle.yml) [![codecov](https://codecov.io/gh/rednaga/axmlprinter/graph/badge.svg)](https://codecov.io/gh/rednaga/axmlprinter) [![JitPack](https://jitpack.io/v/rednaga/axmlprinter.svg)](https://jitpack.io/#rednaga/axmlprinter)

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

**Exit status:** `0` on success, `1` if the file can't be read, parsed or written (the reason is printed to
stderr), and `2` for a usage error. Only the XML is written to stdout, so it is safe to redirect.

### Library

The same code can be used from Java. Binary AXML can be read, printed, edited and written back out:

```java
try (InputStream in = new FileInputStream("AndroidManifest.xml")) {
    // Throws an IOException if this isn't a complete, valid AXML file
    AXMLResource manifest = new AXMLResource(in);

    System.out.println(manifest.toXML());

    // Edit it: set android:name on the <application> tag. The namespace URI (not the prefix) and the
    // attribute name have to already be in the file's string pool.
    Attribute name = new Attribute("http://schemas.android.com/apk/res/android", "name",
            "com.example.MyApplication", AttributeType.STRING, null, manifest.getStringSection());
    manifest.injectApplicationAttribute(name);

    // Write the modified binary AXML back out
    try (OutputStream out = new FileOutputStream("AndroidManifest.modified.xml")) {
        manifest.write(out);
    }
}
```

Protocol Buffers XML (from App Bundles) is read-only:

```java
try (InputStream in = new FileInputStream("manifest.pb")) {
    System.out.println(new ProtobufXMLResource(in).toXML());
}
```

`AXMLResource.injectApplicationAttribute` throws an `IllegalStateException` if there is no `<application>` tag.


## Contributing
Contributions welcome! Please follow the simple steps;

1. Fork repository
2. Make changes
3. Ensure tests pass (or hopefully adding tests!)
4. Submit pull request/issue

If this is too much work, feel free to make an issue and upload a patch set for me to evaluate.

## Revision History

### Unreleased
 - **Behavior changes**:
   - The command line tool now exits with a non-zero status on failure (`1`) and usage errors (`2`), and
     prints the usage to stderr. Errors are a single line instead of a stack trace
   - Truncated or malformed files now fail with an `IOException` (with the chunk type and byte offset)
     instead of producing half-built chunks
   - Attributes of the types float, dimension, fraction, attribute reference and color are now printed (they
     used to be empty), and flags other than `protectionLevel` are printed as hex
   - String values and text are XML escaped
 - **Bug fixes**:
   - String pools with styles are now read and written correctly (they were corrupted on write)
   - A crafted string length can no longer cause an `OutOfMemoryError`
   - `AXMLResource.read` no longer relies on `InputStream.available()`
   - The protobuf printer chose `%p` for fractions from the wrong bits, and mishandled negative dimensions
 - **Housekeeping**: `.github` is no longer gitignored, removed the stale IntelliJ module file

### v2.1.0
 - **Bug fixes**:
   - String pools: UTF-8 pools are now parsed and written correctly (two-length encoding, real UTF-8
     decoding), non-ASCII text no longer depends on the platform charset, and long strings are supported.
     Written pools now include the null terminator and alignment Android expects
   - `Attribute`: namespace prefixes are no longer dropped when the namespace URI is string index 0 or 1
   - `StartTag`: size, XML output and mangle fixing now include attributes added after parsing
     (e.g. through `injectApplicationAttribute`)
   - `AXMLResource`: `print()`/`toXML()` no longer accumulate namespaces between calls, `write()` no longer
     prints debug output to stdout, and injecting into a file with no `<application>` tag throws a clear
     `IllegalStateException` instead of an NPE
   - `IntReader.skip` no longer reports a legitimate short skip as end of file
 - **Tests**: ~150 tests (up from ~60), now using real parsing instead of mocks where it matters, covering
   the CLI, attribute rendering, string/resource sections, malformed input and the protobuf output path
 - **Build**: Gradle 9.9.1, updated dependencies (protobuf plugin 0.10.1, JUnit 6.1.3, Mockito 5.24.0)

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

    Copyright (c) 2015-2026 Red Naga - Tim 'diff' Strazzere
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
