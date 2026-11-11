package android.content.res.chunk.types;

import android.content.res.TestUtil;
import android.content.res.chunk.AttributeType;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.StringSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Inserting an attribute into a parsed StartTag must keep size, bytes and XML in agreement.
 */
public class StartTagAttributeInsertTest {

    // Indices: 0 tag, 1 name, 2 value
    private StringSection strings;
    private StartTag tag;

    @BeforeEach
    public void setUp() throws Exception {
        strings = TestUtil.stringSection("tag", "name", "value");
        // size, line, comment, namespace uri, name, flags, attribute count, class
        tag = new StartTag(ChunkType.START_TAG, TestUtil.reader(36, 1, -1, -1, 0, 0x140014, 0, -1));
    }

    private Attribute stringAttribute() {
        return new Attribute(TestUtil.reader(-1, 1, 2, AttributeType.STRING.getIntType(), -1));
    }

    @Test
    public void sizeMatchesBytesBeforeInsert() {
        assertEquals(tag.getSize(), tag.toBytes().length);
    }

    @Test
    public void sizeMatchesBytesAfterInsert() {
        tag.insertOrReplaceAttribute(stringAttribute());

        assertEquals(36 + 20, tag.getSize());
        assertEquals(tag.getSize(), tag.toBytes().length);
    }

    @Test
    public void insertedAttributeIsRenderedToXml() {
        tag.insertOrReplaceAttribute(stringAttribute());

        String xml = tag.toXML(strings, null, Collections.emptyList(), 0);

        assertTrue(xml.contains("name=\"value\""), xml);
    }

    @Test
    public void replacingAttributeKeepsSizeStable() {
        tag.insertOrReplaceAttribute(stringAttribute());
        tag.insertOrReplaceAttribute(stringAttribute());

        assertEquals(36 + 20, tag.getSize());
        assertEquals(tag.getSize(), tag.toBytes().length);
    }
}
