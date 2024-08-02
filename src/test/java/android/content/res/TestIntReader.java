package android.content.res;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.EOFException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author tstrazzere
 */
public class TestIntReader {

    @Nested
    class UnitTest {

        InputStream mockStream;
        IntReader underTest;

        @BeforeEach
        public void setUp() {
            mockStream = mock(InputStream.class);
            underTest = new IntReader(mockStream, true); // Start with big-endian mode
        }

        @Test
        public void testClose() throws IOException {
            underTest.close();
            verify(mockStream, times(1)).close();
        }

        @Test
        public void testReadShort() throws IOException {
            // Ensure the mock returns exactly two bytes for the short read
            when(mockStream.read()).thenReturn(0x01, 0x02, -1);

            // Expect 0x0102 in big-endian order
            assertEquals((0x01 << 8) | 0x02, underTest.readShort());
            verify(mockStream, times(2)).read();
        }

        @Test
        public void testReadByte() throws IOException {
            // Ensure the mock returns exactly one byte
            when(mockStream.read()).thenReturn(0x01, -1);

            // Expect single byte value 0x01
            assertEquals(0x01, underTest.readByte());
            verify(mockStream, times(1)).read();
        }

        @Test
        public void testReadInt() throws IOException {
            // Ensure the mock returns exactly four bytes for the integer read
            when(mockStream.read()).thenReturn(0x01, 0x02, 0x03, 0x04, -1);

            // Expect 0x01020304 in big-endian order
            assertEquals((0x01 << 24) | (0x02 << 16) | (0x03 << 8) | 0x04, underTest.readInt());
            verify(mockStream, times(4)).read();
        }

        @Test
        public void testCloseDoesntDieWithNull() {
            underTest = new IntReader(null, true);
            underTest.close();
        }

        @Test
        public void testSkips() throws IOException {
            // Ensure the mock simulates skipping 8 and 4 bytes
            when(mockStream.skip(8)).thenReturn(8L);
            when(mockStream.skip(4)).thenReturn(4L);

            // Skip no bytes
            underTest.skip(0);
            // Skip 8 bytes
            underTest.skip(8);
            // Skip 4 bytes (for int)
            underTest.skipInt();

            verify(mockStream, times(1)).skip(8);
            verify(mockStream, times(1)).skip(4);
        }

        @Test
        public void testSkipFails() throws IOException {
            // Simulate a failed skip
            when(mockStream.skip(4)).thenReturn(0L);

            assertThrows(EOFException.class, () -> underTest.skipInt());
        }

        @Test
        public void testReadIntFailsBadParams() {
            assertThrows(IllegalArgumentException.class, () -> underTest.readInt(-1));
            assertThrows(IllegalArgumentException.class, () -> underTest.readInt(5));
        }

        @Test
        public void testReadIntFailsEOF() throws IOException {
            // Simulate end-of-stream (EOF)
            when(mockStream.read()).thenReturn(-1);

            assertThrows(EOFException.class, () -> underTest.readInt(2));
        }

        @Test
        public void testReadIntBigEndian() throws IOException {
            // Ensure the mock returns four bytes
            when(mockStream.read()).thenReturn(0x0A, 0x14, 0x1E, 0x28, -1);

            // First read: expect 0x0A
            assertEquals(0x0A, underTest.readInt(1));

            // Next three bytes read: expect 0x141E28 in big-endian order
            assertEquals((0x14 << 16) | (0x1E << 8) | 0x28, underTest.readInt(3));
        }

        @Test
        public void testReadIntLittleEndian() throws IOException {
            underTest = new IntReader(mockStream, false); // Little-endian mode

            // Ensure the mock returns four bytes
            when(mockStream.read()).thenReturn(0x0A, 0x14, 0x1E, 0x28, -1);

            // First read: expect 0x0A
            assertEquals(0x0A, underTest.readInt(1));

            // Next three bytes read: expect 0x281E14 in little-endian order
            assertEquals(0x14 | (0x1E << 8) | (0x28 << 16), underTest.readInt(3));
        }
    }
}