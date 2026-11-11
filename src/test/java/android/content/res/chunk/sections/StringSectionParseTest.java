package android.content.res.chunk.sections;

import android.content.res.TestUtil;
import android.content.res.chunk.ChunkUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
