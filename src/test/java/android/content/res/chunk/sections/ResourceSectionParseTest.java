package android.content.res.chunk.sections;

import android.content.res.TestUtil;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.ChunkUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Runs the real ResourceSection.readSection against real bytes.
 */
public class ResourceSectionParseTest {

    @Test
    public void readsResourceIds() throws Exception {
        int[] ids = {0x0101000e, 0x01010010, 0x7f010001};
        byte[] bytes = TestUtil.ints(ChunkType.RESOURCE_SECTION.getIntType(), 8 + ids.length * 4,
                ids[0], ids[1], ids[2]);

        ResourceSection section = assertInstanceOf(ResourceSection.class, ChunkUtil.createChunk(TestUtil.reader(bytes)));

        assertEquals(3, section.getResourceCount());
        for (int i = 0; i < ids.length; i++) {
            assertEquals(ids[i], section.getResourceID(i));
        }
        assertEquals(8 + ids.length * 4, section.getSize());
        assertArrayEquals(bytes, section.toBytes());
    }

    @Test
    public void emptyResourceSection() throws Exception {
        byte[] bytes = TestUtil.ints(ChunkType.RESOURCE_SECTION.getIntType(), 8);

        ResourceSection section = assertInstanceOf(ResourceSection.class, ChunkUtil.createChunk(TestUtil.reader(bytes)));

        assertEquals(0, section.getResourceCount());
        assertEquals(8, section.getSize());
    }
}
