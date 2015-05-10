package test;

import java.io.FileInputStream;

import android.content.res.AXMLResource;

/**
 * A slimmed down version of the original AXMLPrinter from Dmitry Skiba.
 * 
 * Prints xml document from Android's binary xml file.
 * 
 * @author Tim Strazzere
 * 
 */
public class AXMLPrinter {

    public static void main(String[] arguments) {
        if (arguments.length < 1) {
            System.out.println("Usage: AXMLPrinter <binary xml file>");
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
}
