package android.content.res;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Rendering is a read-only operation: doing it repeatedly, or with print() and toXML() mixed, must not change
 * the result or leak state between calls.
 */
public class TestAXMLResourceRepeatedOutput {

    @ParameterizedTest
    @ValueSource(strings = {
            "test.xml", "test1.xml", "test2.xml", "test3.xml",
            "large_from_malware.xml", "qihoo_jiagu_issue8.xml"
    })
    void toXmlIsRepeatable(String fixture) throws Exception {
        AXMLResource resource = TestAXMLResource.load(fixture);

        assertEquals(resource.toXML(), resource.toXML());
    }

    @ParameterizedTest
    @ValueSource(strings = {"test.xml", "large_from_malware.xml", "qihoo_jiagu_issue8.xml"})
    void printMatchesToXmlAndDoesNotLeakNamespaces(String fixture) throws Exception {
        AXMLResource resource = TestAXMLResource.load(fixture);
        String expected = resource.toXML();
        int namespacesAfterToXml = resource.nameSpaces.size();

        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            resource.print();
        } finally {
            System.setOut(original);
        }

        assertEquals(expected.trim(), captured.toString(StandardCharsets.UTF_8).trim());
        assertEquals(namespacesAfterToXml, resource.nameSpaces.size(), "print() must not accumulate namespaces");
        assertEquals(expected, resource.toXML());
    }
}
