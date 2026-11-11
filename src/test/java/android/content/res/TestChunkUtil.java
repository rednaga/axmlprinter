package android.content.res;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import android.content.res.chunk.ChunkType;
import android.content.res.chunk.ChunkUtil;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;
import android.content.res.chunk.types.AXMLHeader;
import android.content.res.chunk.types.Buffer;
import android.content.res.chunk.types.Chunk;
import android.content.res.chunk.types.EndTag;
import android.content.res.chunk.types.NameSpace;
import android.content.res.chunk.types.StartTag;
import android.content.res.chunk.types.TextTag;

/**
 * @author tstrazzere
 */
public class TestChunkUtil {

    @Nested
    class UnitTest {

        @Test
        public void testReadChunkType() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.AXML_HEADER.getIntType());

            assertEquals(ChunkType.AXML_HEADER, ChunkUtil.readChunkType(mockReader));
        }

        @Test
        public void testBadReadChunkType() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(-1);

            assertThrows(IOException.class, () -> ChunkUtil.readChunkType(mockReader));
        }

        @Test
        public void testCreateAXMLHeader() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.AXML_HEADER.getIntType(), 0);

            assertInstanceOf(AXMLHeader.class, ChunkUtil.createChunk(mockReader));
        }

        @Test
        public void testCreateResourceSection() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.RESOURCE_SECTION.getIntType(), 8, 0);

            assertInstanceOf(ResourceSection.class, ChunkUtil.createChunk(mockReader));
        }

        @Test
        public void testCreateStringSection() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.STRING_SECTION.getIntType(), 0);

            assertInstanceOf(StringSection.class, ChunkUtil.createChunk(mockReader));
        }

        @Test
        public void testCreateBuffer() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.BUFFER.getIntType(), 0);

            assertInstanceOf(Buffer.class, ChunkUtil.createChunk(mockReader));
        }

        @Test
        public void testCreateNameSpace() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.START_NAMESPACE.getIntType(), 0);

            Chunk chunk = ChunkUtil.createChunk(mockReader);
            assertInstanceOf(NameSpace.class, chunk);

            assertTrue(((NameSpace) chunk).isStart());

            when(mockReader.readInt()).thenReturn(ChunkType.END_NAMESPACE.getIntType(), 0);
            chunk = ChunkUtil.createChunk(mockReader);
            assertInstanceOf(NameSpace.class, chunk);

            assertFalse(((NameSpace) chunk).isStart());
        }

        @Test
        public void testCreateStartTag() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.START_TAG.getIntType(), 0);

            Chunk chunk = ChunkUtil.createChunk(mockReader);
            assertInstanceOf(StartTag.class, chunk);
        }

        @Test
        public void testCreateEndTag() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.END_TAG.getIntType(), 0);

            Chunk chunk = ChunkUtil.createChunk(mockReader);
            assertInstanceOf(EndTag.class, chunk);
        }

        @Test
        public void testCreateTextTag() throws IOException {
            IntReader mockReader = mock(IntReader.class);
            when(mockReader.readInt()).thenReturn(ChunkType.TEXT_TAG.getIntType(), 0);

            Chunk chunk = ChunkUtil.createChunk(mockReader);
            assertInstanceOf(TextTag.class, chunk);
        }
    }
}