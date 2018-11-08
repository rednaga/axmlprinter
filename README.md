# axmlprinter
[![Build Status](https://travis-ci.org/rednaga/axmlprinter.svg?branch=master)](https://travis-ci.org/rednaga/axmlprinter) [![Coverage Status](https://img.shields.io/coveralls/rednaga/axmlprinter.svg)](https://coveralls.io/r/rednaga/axmlprinter?branch=master)

This is a heavily (almost 100%?) refactor of the Android4ME `axmlprinter` library/code. Personally I
had used the library for years without much issue and it was fast/useful just for printing out Android
Manifest files. In the past few years due to people mucking with AXML files and my own boredom, I've
refactored most of the code to be tested and more useful in non-command line situations.

## Usage
The default use case would be to compile the library;

```./gradlew jar```

Then run the library;

```java -jar build/libs/axmlprinter-*.jar <target>```

## Contributing
Contributions welcome! Please follow the simple steps;

1. Fork repository
2. Make changes
3. Ensure tests pass (or hopefully adding tests!)
4. Submit pull request/issue

If this is too much work, feel free to make an issue and upload a patch set for me to evaluate.

## Revision History
### v0.1.0 (Initial Release)
 - "Just as good" as original code
 - 0% -> ~40% test coverage

## License

    Copyright (c) 2015 Red Naga - Tim 'diff' Strazzere
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
