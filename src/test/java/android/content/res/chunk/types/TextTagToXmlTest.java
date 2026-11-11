package android.content.res.chunk.types;

import android.content.res.TestUtil;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.StringSection;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextTagToXmlTest {

    private static TextTag textTag(int nameIndex) {
        // size, line, comment, text index, raw value, typed value
        return new TextTag(ChunkType.TEXT_TAG, TestUtil.reader(28, 1, -1, nameIndex, nameIndex, 0));
    }

    @Test
    public void rendersTheIndentedText() throws Exception {
        StringSection pool = TestUtil.stringSection("hello");

        assertEquals("\t\thello", textTag(0).toXML(pool, null, Collections.emptyList(), 2));
    }

    @Test
    public void escapesTheText() throws Exception {
        StringSection pool = TestUtil.stringSection("1 < 2 & 3 > 2");

        assertEquals("1 &lt; 2 &amp; 3 &gt; 2", textTag(0).toXML(pool, null, Collections.emptyList(), 0));
    }
}
