/* 
 * Copyright 2015 Red Naga
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.res.AXMLResource;

/**
 * A slimmed down version of the original AXMLPrinter from Dmitry Skiba.
 * 
 * Prints xml document from Android's binary xml file.
 * 
 * @author Tim Strazzere
 */
public class AXMLPrinter {

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

    public static void main(String[] arguments) {
        if (arguments.length < 1) {
            System.out.println("Usage: AXMLPrinter <binary xml file>");
            return;
        }

        if (arguments[0].equalsIgnoreCase("-v") || arguments[0].equalsIgnoreCase("-version")) {
            System.out.printf("axmlprinter %s (http://github.com/rednaga/axmlprinter2)\n", VERSION);
            System.out.printf("Copyright (C) 2015 Red Naga - Tim 'diff' Strazzere (strazz@gmail.com)\n");
            return;
        }

        try {
            AXMLResource axmlResource = new AXMLResource();
            axmlResource.read(new FileInputStream(arguments[0]));

            axmlResource.print();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Avoid anyone accidentally (purposefully?) Instantiating this class
     */
    private AXMLPrinter() {

    }
}
