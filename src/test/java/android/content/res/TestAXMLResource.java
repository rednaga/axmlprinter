package android.content.res;

import android.content.res.chunk.AttributeType;
import android.content.res.chunk.types.Attribute;
import android.content.res.chunk.types.StartTag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests over every binary AXML fixture in src/test/resources.
 *
 * @author tstrazzere
 */
public class TestAXMLResource {

    // Fixtures: test*.xml are legacy layouts from the original repo, large_from_malware.xml has tricks
    // that broke tools in the past, qihoo_jiagu_issue8.xml is the file from issue #8.

    static InputStream open(String fixture) {
        InputStream stream = TestAXMLResource.class.getClassLoader().getResourceAsStream(fixture);
        assertNotNull(stream, "Missing test fixture: " + fixture);
        return stream;
    }

    static AXMLResource load(String fixture) throws IOException {
        try (InputStream stream = open(fixture)) {
            return new AXMLResource(stream);
        }
    }

    static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test.xml", "test1.xml", "test2.xml", "test3.xml",
            "large_from_malware.xml", "qihoo_jiagu_issue8.xml"
    })
    void readsAndProducesWellFormedXml(String fixture) throws Exception {
        AXMLResource resource = load(fixture);

        String xml = resource.toXML();
        assertFalse(xml.isEmpty(), "toXML() should not be empty");

        Document document = assertDoesNotThrow(() -> parse(xml), "toXML() must be well formed XML");
        assertFalse(document.getDocumentElement().getNodeName().isEmpty(), "document needs a root element");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test.xml", "test1.xml", "test2.xml", "test3.xml",
            "large_from_malware.xml", "qihoo_jiagu_issue8.xml"
    })
    void roundTripPreservesXml(String fixture) throws Exception {
        AXMLResource original = load(fixture);
        String expected = original.toXML();

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        // Re-load, since toXML() may touch parsed state
        AXMLResource toWrite = load(fixture);
        toWrite.write(out);

        AXMLResource reread = new AXMLResource(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(expected, reread.toXML());
    }

    @ParameterizedTest
    @CsvSource({
            "large_from_malware.xml,3133,1.9.3,com.faithcomesbyhearing.android.pt.bibleis",
            "qihoo_jiagu_issue8.xml,14800,1.4.8,cn.xiaochuankeji.zuiyouLite"
    })
    void manifestAttributes(String fixture, String versionCode, String versionName, String packageName) throws Exception {
        Document document = parse(load(fixture).toXML());
        NamedNodeMap attributes = document.getFirstChild().getAttributes();

        assertEquals("http://schemas.android.com/apk/res/android", attributes.getNamedItem("xmlns:android").getNodeValue());
        assertEquals(versionCode, attributes.getNamedItem("android:versionCode").getNodeValue());
        assertEquals(versionName, attributes.getNamedItem("android:versionName").getNodeValue());
        assertEquals(packageName, attributes.getNamedItem("package").getNodeValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"large_from_malware.xml", "qihoo_jiagu_issue8.xml"})
    void printDoesNotThrow(String fixture) throws IOException {
        AXMLResource resource = load(fixture);
        assertDoesNotThrow(resource::print);
    }

    @ParameterizedTest
    @ValueSource(strings = {"large_from_malware.xml", "qihoo_jiagu_issue8.xml"})
    void injectApplicationAttribute(String fixture) throws IOException {
        AXMLResource resource = load(fixture);
        Attribute attribute = new Attribute("android", "name", "test", AttributeType.STRING, null, resource.getStringSection());

        resource.injectApplicationAttribute(attribute);

        assertTrue(resource.getApplicationTag().getAttributes().contains(attribute));
    }

    @ParameterizedTest
    @ValueSource(strings = {"large_from_malware.xml", "qihoo_jiagu_issue8.xml"})
    void writtenInjectedAttributeSurvivesReread(String fixture, @TempDir Path tempDir) throws IOException {
        AXMLResource resource = load(fixture);
        Attribute attribute = new Attribute("android", "name", "test", AttributeType.STRING, null, resource.getStringSection());
        resource.injectApplicationAttribute(attribute);

        Path file = tempDir.resolve("injected.axml");
        try (OutputStream out = Files.newOutputStream(file)) {
            resource.write(out);
        }

        AXMLResource reread;
        try (InputStream in = Files.newInputStream(file)) {
            reread = new AXMLResource(in);
        }

        StartTag application = reread.getApplicationTag();
        assertNotNull(application);
        boolean found = false;
        for (Attribute candidate : application.getAttributes()) {
            if ("name".equals(reread.getStringSection().getString(candidate.getNameIndex()))
                    && "test".equals(reread.getStringSection().getString(candidate.getStringDataIndex()))) {
                found = true;
            }
        }
        assertTrue(found, "injected android:name=\"test\" should be present after write + re-read");
    }
}
