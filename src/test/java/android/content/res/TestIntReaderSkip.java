package android.content.res;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * InputStream.skip is allowed to skip fewer bytes than requested without being at the end of the stream.
 */
public class TestIntReaderSkip {

    /** A stream that never skips more than 2 bytes at a time, as e.g. network or buffered streams may do. */
    private static class ShortSkipStream extends FilterInputStream {
        ShortSkipStream(byte[] data) {
            super(new ByteArrayInputStream(data));
        }

        @Override
        public long skip(long n) throws IOException {
            return super.skip(Math.min(n, 2));
        }
    }

    private static byte[] bytes(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) i;
        }
        return data;
    }

    @Test
    public void skipHandlesStreamsThatSkipInSmallSteps() throws IOException {
        IntReader reader = new IntReader(new ShortSkipStream(bytes(16)), false);

        reader.skip(10);

        assertEquals(10, reader.getBytesRead());
        // The next byte read is the 11th byte of the stream
        assertEquals(10, reader.readByte());
    }

    @Test
    public void skipPastEndOfStreamIsAnEof() {
        IntReader reader = new IntReader(new ShortSkipStream(bytes(4)), false);

        assertThrows(EOFException.class, () -> reader.skip(10));
    }

    @Test
    public void skipOfZeroOrLessDoesNothing() throws IOException {
        IntReader reader = new IntReader(new ByteArrayInputStream(bytes(4)), false);

        reader.skip(0);
        reader.skip(-3);

        assertEquals(0, reader.getBytesRead());
        assertEquals(0, reader.readByte());
    }
}
