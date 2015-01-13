package android.content.res;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 * @author tstrazzere
 */
@RunWith(Enclosed.class)
public class TestChunkUtil {
    public static class UnitTest {

        @Test
        public void testRead() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(1);

            ChunkUtil.readCheckType(mockReader, 1);
        }

        @Test
        public void testReadNegative() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(2);

            try {
                ChunkUtil.readCheckType(mockReader, 1);
                throw new AssertionError("Expected exception!");
            } catch (IOException exception) {
                // Good case
            }
        }
    }

}
