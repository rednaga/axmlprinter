/* 
 * Copyright 2015-2025 Red Naga
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package diff.rednaga;

import android.content.res.AXMLResource;
import android.content.res.ProtobufXMLResource;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Properties;

/**
 * A slimmed down version of the original AXMLPrinter from Dmitry Skiba.
 * <p>
 * Prints xml document from Android's binary xml file.
 * Supports both traditional AXML format and Protocol Buffers format.
 *
 * @author Tim Strazzere
 */
public class AXMLPrinter {

    private static final int AXML_MAGIC = 0x00080003;
    
    private static String VERSION;

    static {
        InputStream templateStream = AXMLPrinter.class.getClassLoader().getResourceAsStream("axmlprinter.properties");
        if (templateStream != null) {
            Properties properties = new Properties();
            String version = "(unknown version)";
            try {
                properties.load(templateStream);
                version = properties.getProperty("application.version");
            } catch (IOException ex) {
                System.err.println("Unable to find version number!");
            }
            VERSION = version;
        } else {
            VERSION = "[unknown version - no properties found]";
        }

    }

    /** Everything went as expected. */
    static final int EXIT_OK = 0;
    /** The file could not be read, parsed or written. */
    static final int EXIT_FAILURE = 1;
    /** The command line was wrong. */
    static final int EXIT_USAGE = 2;

    private static final String USAGE =
            "Usage: AXMLPrinter <binary xml file> [output file]\n"
                    + "  [output file]    optionally write the file back out as binary AXML (not supported for protobuf XML)\n"
                    + "  -v, -version     print the version";

    public static void main(String[] arguments) throws IOException {
        int status = run(arguments);
        if (status != EXIT_OK) {
            System.exit(status);
        }
    }

    /**
     * Runs the command line tool without exiting the JVM.
     *
     * @return the exit status, 0 on success
     */
    static int run(String[] arguments) {
        if (arguments.length < 1) {
            System.err.println(USAGE);
            return EXIT_USAGE;
        }

        if (arguments[0].equalsIgnoreCase("-v") || arguments[0].equalsIgnoreCase("-version")) {
            System.out.printf("axmlprinter %s (http://github.com/rednaga/axmlprinter2)\n", VERSION);
            System.out.printf("Copyright (C) 2015-2026 Red Naga - Tim 'diff' Strazzere (diff@protonmail.com)\n");
            return EXIT_OK;
        }

        File inputFile = new File(arguments[0]);
        if (!inputFile.exists()) {
            System.err.println("Error: File not found: " + arguments[0]);
            return EXIT_FAILURE;
        }

        // Use BufferedInputStream with mark/reset to avoid double opening the file
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(inputFile))) {
            // Mark the current position (we'll read up to 4 bytes)
            input.mark(4);

            // Read file header to detect format
            byte[] header = new byte[4];
            int bytesRead = input.read(header);

            if (bytesRead < 4) {
                System.err.println("Error: File too small to be a valid Android XML file");
                return EXIT_FAILURE;
            }

            // Reset to beginning of file after reading header
            input.reset();

            // Detect format based on magic number
            int magic = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt();

            if (magic == AXML_MAGIC) {
                return printAxml(input, arguments);
            } else if (ProtobufXMLResource.isProtobufFormat(header)) {
                return printProtobuf(input, arguments);
            }

            System.err.printf("Error: Unknown file format. Magic: 0x%08X%n", magic);
            System.err.println("Expected AXML (0x00080003) or Protobuf format (0x0A...)");
            return EXIT_FAILURE;
        } catch (IOException e) {
            System.err.println("Error: " + describe(e));
            return EXIT_FAILURE;
        } catch (RuntimeException e) {
            // Malformed input can trip up the parsers in unexpected ways, don't dump a stack trace on the user
            System.err.println("Error: Unable to process " + arguments[0] + ": " + describe(e));
            return EXIT_FAILURE;
        }
    }

    private static int printAxml(InputStream input, String[] arguments) throws IOException {
        // Traditional AXML format
        AXMLResource axmlResource = new AXMLResource();
        if (!axmlResource.read(input)) {
            System.err.println("Error: Incomplete Android binary XML file, the header, string or resource section is missing");
            return EXIT_FAILURE;
        }
        axmlResource.print();

        if (arguments.length > 1) {
            try (OutputStream output = new FileOutputStream(new File(arguments[1]))) {
                axmlResource.write(output);
            }
        }
        return EXIT_OK;
    }

    private static int printProtobuf(InputStream input, String[] arguments) throws IOException {
        // Protocol Buffers format
        // Note: Protobuf format is read-only. Writing protobuf format back to AXML
        // is not supported as it requires conversion between two different binary formats.
        ProtobufXMLResource protobufResource = new ProtobufXMLResource(input);
        protobufResource.print();

        if (arguments.length > 1) {
            System.err.println("Warning: Writing protobuf format back to AXML is not supported.");
            System.err.println("Protobuf format (used in Android App Bundles) cannot be converted to traditional AXML format.");
        }
        return EXIT_OK;
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    /*
     * Avoid anyone accidentally (purposefully?) Instantiating this class
     */
    private AXMLPrinter() {

    }
}
