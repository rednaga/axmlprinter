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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises Attribute parsing and rendering using a real IntReader and a real StringSection.
 */
public class AttributeToXmlTest {

    // Indices: 0 name, 1 hello, 2 versionCode, 3 android, 4 namespace uri, 5 protectionLevel
    private static final int NAME = 0;
    private static final int HELLO = 1;
    private static final int VERSION_CODE = 2;
    private static final int PREFIX = 3;
    private static final int URI = 4;
    private static final int PROTECTION_LEVEL = 5;

    private StringSection strings;
    private List<NameSpace> namespaces;

    @BeforeEach
    public void setUp() throws Exception {
        strings = TestUtil.stringSection(
                "name", "hello", "versionCode", "android", "http://schemas.android.com/apk/res/android",
                "protectionLevel");
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
        assertEquals("android:protectionLevel=\"" + expected + "\"",
                render(URI, PROTECTION_LEVEL, -1, AttributeType.FLAGS, data));
    }

    @Test
    public void protectionLevelIgnoresHighBits() {
        // Only the low nibble is the base protection level
        assertEquals("android:protectionLevel=\"dangerous\"",
                render(URI, PROTECTION_LEVEL, -1, AttributeType.FLAGS, 0x31));
    }

    @Test
    public void otherFlagsAreHexNotProtectionLevels() {
        assertEquals("android:name=\"0x4a0\"", render(URI, NAME, -1, AttributeType.FLAGS, 0x4a0));
        assertEquals("android:name=\"0x1\"", render(URI, NAME, -1, AttributeType.FLAGS, 1));
    }

    @Test
    public void floatValue() {
        assertEquals("android:name=\"1.5\"", render(URI, NAME, -1, AttributeType.FLOAT, Float.floatToIntBits(1.5f)));
    }

    @ParameterizedTest
    @CsvSource({
            // mantissa << 8 | radix << 4 | unit
            "4097,16dp", "8194,32sp", "1280,5px", "771,3pt", "1028,4in", "1285,5mm"
    })
    public void dimensionValue(int data, String expected) {
        // CsvSource ints above are written as (mantissa << 8) | unit: 4097 = 16 << 8 | 1, etc.
        assertEquals("android:name=\"" + expected + "\"", render(URI, NAME, -1, AttributeType.DIMEN, data));
    }

    @Test
    public void negativeDimensionValue() {
        int minusFourSp = ((-4 & 0xFFFFFF) << 8) | 2;

        assertEquals("android:name=\"-4sp\"", render(URI, NAME, -1, AttributeType.DIMEN, minusFourSp));
    }

    @Test
    public void fractionalDimensionValue() {
        // 0p23 radix, mantissa 1 << 22 is 0.5
        int half = ((1 << 22) << 8) | (3 << 4) | 1;

        assertEquals("android:name=\"0.5dp\"", render(URI, NAME, -1, AttributeType.DIMEN, half));
    }

    @Test
    public void fractionValue() {
        int halfOfSelf = ((1 << 22) << 8) | (3 << 4);
        int halfOfParent = halfOfSelf | 1;

        assertEquals("android:name=\"50.00%\"", render(URI, NAME, -1, AttributeType.FRACTION, halfOfSelf));
        assertEquals("android:name=\"50.00%p\"", render(URI, NAME, -1, AttributeType.FRACTION, halfOfParent));
    }

    @Test
    public void attributeReference() {
        assertEquals("android:name=\"?7F010001\"", render(URI, NAME, -1, AttributeType.ATTR, 0x7f010001));
    }

    @Test
    public void colors() {
        assertEquals("android:name=\"#ff112233\"", render(URI, NAME, -1, AttributeType.COLOR1, 0xff112233));
        assertEquals("android:name=\"#112233\"", render(URI, NAME, -1, AttributeType.COLOR2, 0x00112233));
    }

    @Test
    public void unknownTypesAreShownAsHexInsteadOfEmpty() {
        Attribute attribute = new Attribute(TestUtil.reader(URI, NAME, -1, 0x1E000008, 0xf123));

        assertEquals("android:name=\"0xf123\"", attribute.toXML(strings, null, namespaces, 0));
    }

    @Test
    public void stringValuesAreXmlEscaped() throws Exception {
        StringSection pool = TestUtil.stringSection("name", "a & b <c> \"d\" 'e'");
        Attribute attribute = new Attribute(TestUtil.reader(-1, 0, 1, AttributeType.STRING.getIntType(), -1));

        assertEquals("name=\"a &amp; b &lt;c&gt; &quot;d&quot; &apos;e&apos;\"",
                attribute.toXML(pool, null, Collections.emptyList(), 0));
    }

    @Test
    public void toBytesRoundTripsThroughReader() {
        int[] values = {URI, NAME, HELLO, AttributeType.STRING.getIntType(), -1};
        Attribute attribute = new Attribute(TestUtil.reader(values));

        assertEquals(java.util.Arrays.toString(TestUtil.ints(values)), java.util.Arrays.toString(attribute.toBytes()));
    }

    @Test
    public void namespacePrefixWhenUriIsStringIndexOne() throws Exception {
        // Indices: 0 name, 1 uri, 2 prefix
        StringSection pool = TestUtil.stringSection("name", "http://schemas.android.com/apk/res/android", "android");
        NameSpace namespace = new NameSpace(ChunkType.START_NAMESPACE, TestUtil.reader(0x18, 1, -1, 2, 1));
        Attribute attribute = new Attribute(TestUtil.reader(1, 0, -1, AttributeType.INT.getIntType(), 5));

        assertEquals("android:name=\"5\"", attribute.toXML(pool, null, Collections.singletonList(namespace), 0));
    }

    @Test
    public void namespacePrefixWhenUriIsStringIndexZero() throws Exception {
        // Indices: 0 uri, 1 prefix, 2 name
        StringSection pool = TestUtil.stringSection("http://schemas.android.com/apk/res/android", "android", "name");
        NameSpace namespace = new NameSpace(ChunkType.START_NAMESPACE, TestUtil.reader(0x18, 1, -1, 1, 0));
        Attribute attribute = new Attribute(TestUtil.reader(0, 2, -1, AttributeType.INT.getIntType(), 5));

        assertEquals("android:name=\"5\"", attribute.toXML(pool, null, Collections.singletonList(namespace), 0));
    }

    @Test
    public void nonStringAttributeWithoutDataIsAClearError() {
        IllegalArgumentException failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new Attribute("android", "name", null, AttributeType.INT, null, strings));

        assertTrue(failure.getMessage().contains("INT"), failure.getMessage());
    }
}
