package android.content.res;

import android.content.res.chunk.AttributeType;
import android.content.res.chunk.types.Attribute;
import android.content.res.chunk.types.StartTag;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author tstrazzere
 */
@RunWith(Enclosed.class)
public class TestAXMLResource {
    public static class FunctionalTest {

        // Legacy files from original repo
        String[] oldTestFiles = {"test.xml", "test1.xml", "test2.xml", "test3.xml"};

        // Large file with weird tricks that broke tools in the past
        String largeFromMalware = "large_from_malware.xml";

        AXMLResource underTest;

        @Before
        public void setUp() {
            underTest = new AXMLResource();
        }

        @Test
        public void testReadingOldFiles() throws IOException {
            for (String file : oldTestFiles) {
                InputStream testStream = this.getClass().getClassLoader().getResourceAsStream(file);

                // Should throw no exceptions
                underTest.read(testStream);
            }
        }

        @Test
        public void testPrinting() throws IOException {
            InputStream testStream = this.getClass().getClassLoader().getResourceAsStream(largeFromMalware);

            underTest = new AXMLResource(testStream);

            underTest.print();
        }

        @Test
        public void testInsertApplicationAttribute() throws IOException {
            InputStream testStream = this.getClass().getClassLoader().getResourceAsStream(largeFromMalware);

            underTest.read(testStream);

            Attribute attribute = new Attribute("android",
                    "name",
                    "test",
                    AttributeType.STRING,
                    null,
                    underTest.getStringSection());

            underTest.injectApplicationAttribute(attribute);

            StartTag startTag = underTest.getApplicationTag();

            assertTrue(startTag.getAttributes().contains(attribute));
        }

        @Test
        public void testWriteInsertedApplicationAttribute() throws IOException {
            InputStream testStream = this.getClass().getClassLoader().getResourceAsStream(largeFromMalware);

            underTest.read(testStream);

            Attribute attribute = new Attribute("android",
                    "name",
                    "test",
                    AttributeType.STRING,
                    null,
                    underTest.getStringSection());

            underTest.injectApplicationAttribute(attribute);

            File file = File.createTempFile("axml-func-test", "xml-test");
            file.deleteOnExit();

            underTest.write(new FileOutputStream(file));

            underTest = new AXMLResource(new FileInputStream(file));
            StartTag startTag = underTest.getApplicationTag();

            assertEquals(underTest.getStringSection().getString(startTag.getAttributes().get(3).getNameIndex()),
                    "name");
            assertEquals(underTest.getStringSection().getString(startTag.getAttributes().get(3).getStringDataIndex()),
                    "test");
        }
    }
}
