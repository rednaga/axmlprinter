package android.content.res;

import com.android.aapt.Resources.Item;
import com.android.aapt.Resources.Primitive;
import com.android.aapt.Resources.Reference;
import com.android.aapt.Resources.XmlAttribute;
import com.android.aapt.Resources.XmlElement;
import com.android.aapt.Resources.XmlNamespace;
import com.android.aapt.Resources.XmlNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The success path of ProtobufXMLResource, using protobuf messages built in the test (no binary fixture needed).
 */
public class TestProtobufXMLResourceOutput {

    private static final String ANDROID_URI = "http://schemas.android.com/apk/res/android";

    private static String render(XmlElement.Builder element) throws IOException {
        XmlNode node = XmlNode.newBuilder().setElement(element).build();
        ProtobufXMLResource resource = new ProtobufXMLResource(new ByteArrayInputStream(node.toByteArray()));
        return resource.toXML();
    }

    private static XmlAttribute.Builder attribute(String name) {
        return XmlAttribute.newBuilder().setNamespaceUri(ANDROID_URI).setName(name);
    }

    private static XmlElement.Builder manifestWith(XmlAttribute.Builder attribute) {
        return XmlElement.newBuilder()
                .setName("manifest")
                .addNamespaceDeclaration(XmlNamespace.newBuilder().setPrefix("android").setUri(ANDROID_URI))
                .addAttribute(attribute);
    }

    private static XmlAttribute.Builder withPrimitive(Primitive.Builder primitive) {
        return attribute("value").setCompiledItem(Item.newBuilder().setPrim(primitive));
    }

    @Test
    public void rootElementWithNamespaceAndAttributes() throws Exception {
        String xml = render(XmlElement.newBuilder()
                .setName("manifest")
                .addNamespaceDeclaration(XmlNamespace.newBuilder().setPrefix("android").setUri(ANDROID_URI))
                .addAttribute(attribute("versionName").setValue("1.0"))
                .addAttribute(XmlAttribute.newBuilder().setName("package").setValue("com.example")));

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest\n"), xml);
        assertTrue(xml.contains("\txmlns:android=\"" + ANDROID_URI + "\""), xml);
        assertTrue(xml.contains("\tandroid:versionName=\"1.0\""), xml);
        assertTrue(xml.contains("\tpackage=\"com.example\""), xml);
        assertTrue(xml.trim().endsWith("/>"), "childless element is self closing: " + xml);
    }

    @Test
    public void childElementsAndTextAreIndented() throws Exception {
        String xml = render(XmlElement.newBuilder()
                .setName("manifest")
                .addChild(XmlNode.newBuilder().setElement(XmlElement.newBuilder().setName("application")))
                .addChild(XmlNode.newBuilder().setText("hello")));

        assertTrue(xml.contains("<manifest>\n"), xml);
        assertTrue(xml.contains("\n\t<application />\n"), xml);
        assertTrue(xml.contains("\n\thello\n"), xml);
        assertTrue(xml.contains("</manifest>"), xml);
    }

    @Test
    public void namespaceDeclaredOnParentIsNotRedeclaredOnChild() throws Exception {
        XmlElement.Builder child = XmlElement.newBuilder()
                .setName("application")
                .addNamespaceDeclaration(XmlNamespace.newBuilder().setPrefix("android").setUri(ANDROID_URI))
                .addAttribute(attribute("name").setValue("App"));
        String xml = render(XmlElement.newBuilder()
                .setName("manifest")
                .addNamespaceDeclaration(XmlNamespace.newBuilder().setPrefix("android").setUri(ANDROID_URI))
                .addChild(XmlNode.newBuilder().setElement(child)));

        assertEquals(1, xml.split("xmlns:android=", -1).length - 1, xml);
        assertTrue(xml.contains("android:name=\"App\""), xml);
    }

    @Test
    public void textAndAttributesAreXmlEscaped() throws Exception {
        String xml = render(XmlElement.newBuilder()
                .setName("a")
                .addAttribute(XmlAttribute.newBuilder().setName("v").setValue("a&b<c>\"d'"))
                .addChild(XmlNode.newBuilder().setText("1 < 2 & 3 > 2")));

        assertTrue(xml.contains("v=\"a&amp;b&lt;c&gt;&quot;d&apos;\""), xml);
        assertTrue(xml.contains("1 &lt; 2 &amp; 3 &gt; 2"), xml);
        assertFalse(xml.contains("a&b"), xml);
    }

    static Stream<Arguments> primitives() {
        return Stream.of(
                Arguments.of("decimal", Primitive.newBuilder().setIntDecimalValue(42), "42"),
                Arguments.of("negative decimal", Primitive.newBuilder().setIntDecimalValue(-7), "-7"),
                Arguments.of("hex", Primitive.newBuilder().setIntHexadecimalValue(255), "0xff"),
                Arguments.of("boolean true", Primitive.newBuilder().setBooleanValue(1), "true"),
                Arguments.of("boolean false", Primitive.newBuilder().setBooleanValue(0), "false"),
                Arguments.of("float", Primitive.newBuilder().setFloatValue(1.5f), "1.5"),
                Arguments.of("argb8", Primitive.newBuilder().setColorArgb8Value(0xff112233), "#ff112233"),
                Arguments.of("rgb8", Primitive.newBuilder().setColorRgb8Value(0x112233), "#112233"),
                Arguments.of("argb4", Primitive.newBuilder().setColorArgb4Value(0xf123), "#f123"),
                Arguments.of("rgb4", Primitive.newBuilder().setColorRgb4Value(0x123), "#123"),
                // dimension = mantissa << 8 | radix << 4 | unit
                Arguments.of("dp", Primitive.newBuilder().setDimensionValue((16 << 8) | 1), "16dp"),
                Arguments.of("sp", Primitive.newBuilder().setDimensionValue((12 << 8) | 2), "12sp"),
                Arguments.of("px", Primitive.newBuilder().setDimensionValue(5 << 8), "5px"),
                Arguments.of("fraction of self", Primitive.newBuilder().setFractionValue(((1 << 22) << 8) | (3 << 4)), "50.00%"),
                Arguments.of("fraction of parent", Primitive.newBuilder().setFractionValue(((1 << 22) << 8) | (3 << 4) | 1), "50.00%p"),
                Arguments.of("negative dp", Primitive.newBuilder().setDimensionValue(((-4 & 0xFFFFFF) << 8) | 1), "-4dp"),
                Arguments.of("empty", Primitive.newBuilder().setEmptyValue(Primitive.EmptyType.getDefaultInstance()), ""),
                Arguments.of("null", Primitive.newBuilder().setNullValue(Primitive.NullType.getDefaultInstance()), "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("primitives")
    public void primitiveFormatting(String label, Primitive.Builder primitive, String expected) throws Exception {
        String xml = render(manifestWith(withPrimitive(primitive)));

        assertTrue(xml.contains("android:value=\"" + expected + "\""), xml);
    }

    @Test
    public void referencePrefersNameOverId() throws Exception {
        Reference.Builder ref = Reference.newBuilder().setName("string/app_name").setId(0x7f010001);
        String xml = render(manifestWith(attribute("label")
                .setCompiledItem(Item.newBuilder().setRef(ref))));

        assertTrue(xml.contains("android:label=\"@string/app_name\""), xml);
    }

    @Test
    public void referenceResolvesKnownFrameworkIds() throws Exception {
        Reference.Builder ref = Reference.newBuilder().setId(0x01010012);
        String xml = render(manifestWith(attribute("x").setCompiledItem(Item.newBuilder().setRef(ref))));

        assertTrue(xml.contains("android:x=\"@android:name\""), xml);
    }

    @Test
    public void unknownReferenceIdIsPrintedAsHex() throws Exception {
        Reference.Builder ref = Reference.newBuilder().setId(0x7f010001);
        String xml = render(manifestWith(attribute("x").setCompiledItem(Item.newBuilder().setRef(ref))));

        assertTrue(xml.contains("android:x=\"@0x7f010001\""), xml);
    }

    @Test
    public void compiledStringIsUsed() throws Exception {
        Item item = Item.newBuilder().setStr(com.android.aapt.Resources.String.newBuilder().setValue("compiled")).build();
        String xml = render(manifestWith(attribute("x").setValue("raw").setCompiledItem(item)));

        assertTrue(xml.contains("android:x=\"compiled\""), xml);
    }

    @Test
    public void readReturnsTrueForElementAndFalseForTextOnlyRoot() throws Exception {
        byte[] element = XmlNode.newBuilder().setElement(XmlElement.newBuilder().setName("a")).build().toByteArray();
        byte[] text = XmlNode.newBuilder().setText("just text").build().toByteArray();

        assertTrue(new ProtobufXMLResource().read(new ByteArrayInputStream(element)));
        assertFalse(new ProtobufXMLResource().read(new ByteArrayInputStream(text)));
    }
}
