package android.content.res.chunk.types;

import android.content.res.TestUtil;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.ChunkUtil;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Buffer is the placeholder for a zero int found between chunks.
 */
public class BufferTest {

    @Test
    public void bufferIsFourZeroBytes() throws Exception {
        Buffer buffer = assertInstanceOf(Buffer.class, ChunkUtil.createChunk(TestUtil.reader(0, 0)));

        assertEquals(ChunkType.BUFFER, buffer.getChunkType());
        assertEquals(4, buffer.getSize());
        assertArrayEquals(new byte[]{0, 0, 0, 0}, buffer.toBytes());
        assertEquals(buffer.getSize(), buffer.toBytes().length);
    }

    @Test
    public void bufferHasNoXml() throws Exception {
        Buffer buffer = new Buffer(ChunkType.BUFFER, TestUtil.reader(0));

        assertNull(buffer.toXML(null, null, Collections.emptyList(), 0));
    }

    @Test
    public void bufferReadsNothingBeyondItsType() throws Exception {
        android.content.res.IntReader reader = TestUtil.reader(0, 0x00080003);

        ChunkUtil.createChunk(reader);

        // Only the 4 byte type was consumed, the next chunk type is still readable
        assertEquals(ChunkType.AXML_HEADER, ChunkUtil.readChunkType(reader));
    }
}
