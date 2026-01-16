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

    public static void main(String[] arguments) throws IOException {
        if (arguments.length < 1) {
            System.out.println("Usage: AXMLPrinter <binary xml file>");
            return;
        }

        if (arguments[0].equalsIgnoreCase("-v") || arguments[0].equalsIgnoreCase("-version")) {
            System.out.printf("axmlprinter %s (http://github.com/rednaga/axmlprinter2)\n", VERSION);
            System.out.printf("Copyright (C) 2015-2025 Red Naga - Tim 'diff' Strazzere (diff@protonmail.com)\n");
            return;
        }

        File inputFile = new File(arguments[0]);
        if (!inputFile.exists()) {
            System.err.println("Error: File not found: " + arguments[0]);
            return;
        }

        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            // Use BufferedInputStream with mark/reset to avoid double opening the file
            fileInputStream = new FileInputStream(inputFile);
            bufferedInputStream = new BufferedInputStream(fileInputStream);

            // Mark the current position (we'll read up to 4 bytes)
            bufferedInputStream.mark(4);

            // Read file header to detect format
            byte[] header = new byte[4];
            int bytesRead = bufferedInputStream.read(header);

            if (bytesRead < 4) {
                System.err.println("Error: File too small to be a valid Android XML file");
                return;
            }

            // Reset to beginning of file after reading header
            bufferedInputStream.reset();

            // Detect format based on magic number
            int magic = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            if (magic == AXML_MAGIC) {
                // Traditional AXML format
                AXMLResource axmlResource = new AXMLResource();
                axmlResource.read(bufferedInputStream);
                axmlResource.print();

                if (arguments.length > 1) {
                    File file = new File(arguments[1]);
                    fileOutputStream = new FileOutputStream(file);
                    axmlResource.write(fileOutputStream);
                }
            } else if (ProtobufXMLResource.isProtobufFormat(header)) {
                // Protocol Buffers format
                // Note: Protobuf format is read-only. Writing protobuf format back to AXML 
                // is not supported as it requires conversion between two different binary formats.
                ProtobufXMLResource protobufResource = new ProtobufXMLResource(bufferedInputStream);
                protobufResource.print();

                if (arguments.length > 1) {
                    System.err.println("Warning: Writing protobuf format back to AXML is not supported.");
                    System.err.println("Protobuf format (used in Android App Bundles) cannot be converted to traditional AXML format.");
                }
            } else {
                System.err.printf("Error: Unknown file format. Magic: 0x%08X%n", magic);
                System.err.println("Expected AXML (0x00080003) or Protobuf format (0x0A...)");
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }

    /*
     * Avoid anyone accidentally (purposefully?) Instantiating this class
     */
    private AXMLPrinter() {

    }
}
