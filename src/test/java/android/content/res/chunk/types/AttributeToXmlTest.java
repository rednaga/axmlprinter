package android.content.res.chunk.types;

import android.content.res.TestUtil;
import android.content.res.chunk.AttributeType;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.StringSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises Attribute parsing and rendering using a real IntReader and a real StringSection.
 */
public class AttributeToXmlTest {

    // Indices: 0 name, 1 hello, 2 versionCode, 3 android, 4 namespace uri
    private static final int NAME = 0;
    private static final int HELLO = 1;
    private static final int VERSION_CODE = 2;
    private static final int PREFIX = 3;
    private static final int URI = 4;

    private StringSection strings;
    private List<NameSpace> namespaces;

    @BeforeEach
    public void setUp() throws Exception {
        strings = TestUtil.stringSection(
                "name", "hello", "versionCode", "android", "http://schemas.android.com/apk/res/android");
        // size, line number, comment, prefix, uri
        NameSpace namespace = new NameSpace(ChunkType.START_NAMESPACE,
                TestUtil.reader(0x18, 1, -1, PREFIX, URI));
        namespaces = Collections.singletonList(namespace);
    }

    private String render(int uri, int name, int stringData, AttributeType type, int data) {
        Attribute attribute = new Attribute(TestUtil.reader(uri, name, stringData, type.getIntType(), data));
        return attribute.toXML(strings, null, namespaces, 0);
    }

    @Test
    public void parsesFieldsFromReader() {
        Attribute attribute = new Attribute(TestUtil.reader(URI, NAME, HELLO, AttributeType.STRING.getIntType(), -1));

        assertEquals(NAME, attribute.getNameIndex());
        assertEquals(HELLO, attribute.getStringDataIndex());
        assertEquals(20, attribute.getSize());
    }

    @Test
    public void stringWithNamespacePrefix() {
        assertEquals("android:name=\"hello\"", render(URI, NAME, HELLO, AttributeType.STRING, -1));
    }

    @Test
    public void noNamespaceMeansNoPrefix() {
        assertEquals("name=\"hello\"", render(-1, NAME, HELLO, AttributeType.STRING, -1));
    }

    @Test
    public void intValue() {
        assertEquals("android:versionCode=\"42\"", render(URI, VERSION_CODE, -1, AttributeType.INT, 42));
    }

    @Test
    public void resourceValueIsUpperCaseHex() {
        assertEquals("android:name=\"@7F010001\"", render(URI, NAME, -1, AttributeType.RESOURCE, 0x7f010001));
    }

    @ParameterizedTest
    @CsvSource({"-1,true", "0,false", "5,ERROR"})
    public void booleanValue(int data, String expected) {
        assertEquals("android:name=\"" + expected + "\"", render(URI, NAME, -1, AttributeType.BOOLEAN, data));
    }

    @ParameterizedTest
    @CsvSource({"0,normal", "1,dangerous", "2,signature", "3,signatureOrSystem", "4,internal", "7,????"})
    public void protectionLevelFlags(int data, String expected) {
        assertEquals("android:name=\"" + expected + "\"", render(URI, NAME, -1, AttributeType.FLAGS, data));
    }

    @Test
    public void protectionLevelIgnoresHighBits() {
        // Only the low nibble is the base protection level
        assertEquals("android:name=\"dangerous\"", render(URI, NAME, -1, AttributeType.FLAGS, 0x31));
    }

    @Test
    public void toBytesRoundTripsThroughReader() {
        int[] values = {URI, NAME, HELLO, AttributeType.STRING.getIntType(), -1};
        Attribute attribute = new Attribute(TestUtil.reader(values));

        assertEquals(java.util.Arrays.toString(TestUtil.ints(values)), java.util.Arrays.toString(attribute.toBytes()));
    }
}
