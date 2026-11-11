package android.content.res.chunk.sections;

import android.content.res.TestUtil;
import android.content.res.chunk.ChunkUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parses real UTF-16 string pools (no mocks) and checks the section's bookkeeping.
 */
public class StringSectionParseTest {

    private static StringSection parse(byte[] bytes) throws Exception {
        return (StringSection) ChunkUtil.createChunk(TestUtil.reader(bytes));
    }

    @Test
    public void parsesStringsInOrder() throws Exception {
        StringSection section = TestUtil.stringSection("one", "two", "three");

        assertEquals("one", section.getString(0));
        assertEquals("two", section.getString(1));
        assertEquals("three", section.getString(2));
    }

    @Test
    public void parsesEmptyString() throws Exception {
        StringSection section = TestUtil.stringSection("", "x");

        assertEquals("", section.getString(0));
        assertEquals("x", section.getString(1));
    }

    @Test
    public void parsesWhenOffsetsAreOutOfOrder() throws Exception {
        String[] strings = {"alpha", "beta", "gamma"};
        // Data is laid out gamma, alpha, beta but indexed alpha, beta, gamma
        StringSection section = parse(TestUtil.utf16StringPool(strings, new int[]{2, 0, 1}));

        assertEquals("alpha", section.getString(0));
        assertEquals("beta", section.getString(1));
        assertEquals("gamma", section.getString(2));
    }

    @Test
    public void indexLookups() throws Exception {
        StringSection section = TestUtil.stringSection("a", "b");

        assertEquals(1, section.getStringIndex("b"));
        assertEquals(-1, section.getStringIndex("missing"));
        assertEquals(-1, section.getStringIndex(null));
    }

    @Test
    public void outOfRangeStringIsEmpty() throws Exception {
        StringSection section = TestUtil.stringSection("a");

        assertEquals("", section.getString(-1));
        assertEquals("", section.getString(1));
    }

    @Test
    public void putStringIndexAppendsOnlyOnce() throws Exception {
        StringSection section = TestUtil.stringSection("a", "b");

        int index = section.putStringIndex("c");
        assertEquals(2, index);
        assertEquals(index, section.putStringIndex("c"));
        assertEquals("c", section.getString(index));
        assertEquals(0, section.putStringIndex("a"));
    }

    @Test
    public void sizeMatchesSerializedLength() throws Exception {
        StringSection section = TestUtil.stringSection("one", "two", "three");

        assertEquals(section.getSize(), section.toBytes().length);
    }

    @Test
    public void asciiPoolRoundTripsThroughToBytes() throws Exception {
        StringSection section = TestUtil.stringSection("one", "two", "three");

        StringSection reparsed = parse(section.toBytes());

        assertEquals("one", reparsed.getString(0));
        assertEquals("two", reparsed.getString(1));
        assertEquals("three", reparsed.getString(2));
        assertArrayEquals(section.toBytes(), reparsed.toBytes());
    }

    private static final String LONG_ASCII = "x".repeat(200);
    private static final String VERY_LONG = "y".repeat(300);

    @Test
    public void parsesUtf8Pool() throws Exception {
        StringSection section = parse(TestUtil.utf8StringPool("hello", "h\u00e9llo w\u00f6rld", "\u65e5\u672c\u8a9e", "", LONG_ASCII));

        assertEquals("hello", section.getString(0));
        assertEquals("h\u00e9llo w\u00f6rld", section.getString(1));
        assertEquals("\u65e5\u672c\u8a9e", section.getString(2));
        assertEquals("", section.getString(3));
        assertEquals(LONG_ASCII, section.getString(4));
    }

    @Test
    public void utf8PoolRoundTripsThroughToBytes() throws Exception {
        StringSection section = parse(TestUtil.utf8StringPool("hello", "h\u00e9llo w\u00f6rld", "\u65e5\u672c\u8a9e", LONG_ASCII));

        StringSection reparsed = parse(section.toBytes());

        for (int i = 0; i < 4; i++) {
            assertEquals(section.getString(i), reparsed.getString(i));
        }
        assertEquals(section.getSize(), section.toBytes().length);
    }

    @Test
    public void nonAsciiUtf16PoolRoundTripsThroughToBytes() throws Exception {
        // Includes a surrogate pair (two UTF-16 units)
        String[] strings = {"h\u00e9llo", "\u65e5\u672c\u8a9e", "\ud83d\ude00", VERY_LONG};
        StringSection section = TestUtil.stringSection(strings);

        StringSection reparsed = parse(section.toBytes());

        for (int i = 0; i < strings.length; i++) {
            assertEquals(strings[i], reparsed.getString(i));
        }
        assertEquals(section.getSize(), section.toBytes().length);
    }

    // Strings: 0 hello, 1 world, 2 b, 3 i; "hello" is bold, "world" is bold then italic
    private static final String[] STYLED_STRINGS = {"hello", "world", "b", "i"};
    private static final int[][] STYLES = {
            {2, 0, 4},
            {2, 0, 1, 3, 2, 4}
    };

    @Test
    public void stringsAreReadCorrectlyWhenStylesArePresent() throws Exception {
        StringSection section = parse(TestUtil.utf16StringPoolWithStyles(STYLED_STRINGS, STYLES));

        for (int i = 0; i < STYLED_STRINGS.length; i++) {
            assertEquals(STYLED_STRINGS[i], section.getString(i));
        }
    }

    @Test
    public void parsesStyleSpans() throws Exception {
        StringSection section = parse(TestUtil.utf16StringPoolWithStyles(STYLED_STRINGS, STYLES));

        assertEquals(2, section.getStyleCount());

        java.util.List<StringSection.StyleSpan> bold = section.getStyleSpans(0);
        assertEquals(1, bold.size());
        assertEquals(2, bold.get(0).getNameIndex());
        assertEquals(0, bold.get(0).getFirstChar());
        assertEquals(4, bold.get(0).getLastChar());

        java.util.List<StringSection.StyleSpan> boldThenItalic = section.getStyleSpans(1);
        assertEquals(2, boldThenItalic.size());
        assertEquals(3, boldThenItalic.get(1).getNameIndex());
        assertEquals(2, boldThenItalic.get(1).getFirstChar());
        assertEquals(4, boldThenItalic.get(1).getLastChar());
    }

    @Test
    public void styleDescribesItsTagNames() throws Exception {
        StringSection section = parse(TestUtil.utf16StringPoolWithStyles(STYLED_STRINGS, STYLES));

        assertEquals("b", section.getStyle(0));
        assertEquals("b,i", section.getStyle(1));
        assertEquals("", section.getStyle(5));
    }

    @Test
    public void outOfRangeStylesAreEmpty() throws Exception {
        StringSection withStyles = parse(TestUtil.utf16StringPoolWithStyles(STYLED_STRINGS, STYLES));
        StringSection withoutStyles = TestUtil.stringSection("a");

        assertTrue(withStyles.getStyleSpans(-1).isEmpty());
        assertTrue(withStyles.getStyleSpans(2).isEmpty());
        assertEquals(0, withoutStyles.getStyleCount());
        assertTrue(withoutStyles.getStyleSpans(0).isEmpty());
    }

    @Test
    public void stylesSurviveAWriteAndReread() throws Exception {
        StringSection section = parse(TestUtil.utf16StringPoolWithStyles(STYLED_STRINGS, STYLES));
        byte[] written = section.toBytes();

        assertEquals(section.getSize(), written.length);

        StringSection reparsed = parse(written);
        for (int i = 0; i < STYLED_STRINGS.length; i++) {
            assertEquals(STYLED_STRINGS[i], reparsed.getString(i));
        }
        assertEquals(2, reparsed.getStyleCount());
        assertEquals("b", reparsed.getStyle(0));
        assertEquals("b,i", reparsed.getStyle(1));
        // Writing again is stable
        assertArrayEquals(written, reparsed.toBytes());
    }

    @Test
    public void headerOffsetsPointAtTheStringAndStyleData() throws Exception {
        byte[] written = parse(TestUtil.utf16StringPoolWithStyles(STYLED_STRINGS, STYLES)).toBytes();
        java.nio.ByteBuffer header = java.nio.ByteBuffer.wrap(written).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        int stringsStart = header.getInt(5 * 4);
        int stylesStart = header.getInt(6 * 4);

        // 7 header ints, 4 string offsets and 2 style offsets come first
        assertEquals((7 + 4 + 2) * 4, stringsStart);
        // The first style span is the first thing in the style data: tag "b" is string 2
        assertEquals(2, header.getInt(stylesStart));
        assertEquals(written.length, header.getInt(4));
    }

    @Test
    public void truncatedStyleDataIsAnError() {
        byte[] full = TestUtil.utf16StringPoolWithStyles(STYLED_STRINGS, STYLES);
        byte[] truncated = java.util.Arrays.copyOf(full, full.length - 12);

        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class, () -> parse(truncated));
    }

    @Test
    public void spansPastTheEndOfTheStyleDataAreIgnoredNotACrash() throws Exception {
        // A style offset that points beyond the style data, as a crafted file might have
        byte[] bytes = TestUtil.utf16StringPoolWithStyles(STYLED_STRINGS, STYLES);
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.putInt((7 + 4) * 4, 0x7FFFFFF0); // first style offset

        StringSection section = parse(bytes);

        assertTrue(section.getStyleSpans(0).isEmpty());
    }
}
