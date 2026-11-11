package android.content.res.chunk;

import android.content.res.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Truncated or lying chunks must be reported as errors, not silently turned into half-built chunks.
 */
public class ChunkTruncationTest {

    private static final int START_TAG = ChunkType.START_TAG.getIntType();
    private static final int STRING_TYPE = ChunkType.STRING_SECTION.getIntType();
    private static final int RESOURCE_TYPE = ChunkType.RESOURCE_SECTION.getIntType();

    private static IOException readFails(int... ints) {
        return assertThrows(IOException.class, () -> ChunkUtil.createChunk(TestUtil.reader(ints)));
    }

    @Test
    public void truncatedStartTagHeaderIsAnError() {
        // type, size, line number, and then the stream ends
        readFails(START_TAG, 36, 1);
    }

    @Test
    public void startTagMissingItsAttributesIsAnError() {
        // Claims 2 attributes, only 1 is present
        readFails(START_TAG, 56, 1, -1, -1, 0, 0, 2, -1,
                -1, 0, 0, 0x03000008, -1);
    }

    @Test
    public void startTagWithAbsurdAttributeCountIsAnErrorNotAHang() {
        readFails(START_TAG, 36, 1, -1, -1, 0, 0, Integer.MAX_VALUE, -1);
    }

    @Test
    public void truncatedAttributeIsAnError() {
        // One attribute claimed, but only 2 of its 5 ints are present
        readFails(START_TAG, 56, 1, -1, -1, 0, 0, 1, -1, -1, 0);
    }

    @Test
    public void truncatedStringSectionIsAnError() {
        // Claims 2 strings and 2 offsets, but the stream ends after one offset
        readFails(STRING_TYPE, 44, 2, 0, 0, 36, 0, 0);
    }

    @Test
    public void truncatedResourceSectionIsAnError() {
        // Claims 5 resource ids, only 1 is present
        readFails(RESOURCE_TYPE, 8 + 5 * 4, 0x01010000);
    }

    @Test
    public void truncatedNamespaceIsAnError() {
        readFails(ChunkType.START_NAMESPACE.getIntType(), 0x18, 1);
    }

    @Test
    public void absurdUtf16StringLengthIsAnErrorNotAnOutOfMemory() throws Exception {
        // One string whose two-short length claims ~2 billion characters
        ByteBuffer buffer = ByteBuffer.allocate(7 * 4 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(STRING_TYPE).putInt(7 * 4 + 4 + 4).putInt(1).putInt(0).putInt(0).putInt(7 * 4 + 4).putInt(0);
        buffer.putInt(0); // offset of string 0
        buffer.putShort((short) 0xFFFF).putShort((short) 0xFFFF);

        IOException failure = assertThrows(IOException.class,
                () -> ChunkUtil.createChunk(TestUtil.reader(buffer.array())));
        assertTrue(failure.getMessage().contains("byte offset"), failure.getMessage());
    }

    @Test
    public void errorsCarryTheChunkTypeAndOffset() {
        IOException failure = readFails(START_TAG, 36, 1);

        assertTrue(failure.getMessage().contains("START_TAG"), failure.getMessage());
        assertTrue(failure.getMessage().contains("byte offset"), failure.getMessage());
    }

    @Test
    public void zeroTypeIsABufferNotAnAttribute() throws IOException {
        // BUFFER and ATTRIBUTE both use the int 0, only BUFFER can appear in a stream
        org.junit.jupiter.api.Assertions.assertEquals(ChunkType.BUFFER, ChunkUtil.readChunkType(TestUtil.reader(0)));
    }

}
