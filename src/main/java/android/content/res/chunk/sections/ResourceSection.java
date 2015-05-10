package android.content.res.chunk.sections;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.types.Chunk;

public class ResourceSection extends GenericChunkSection implements Chunk, ChunkSection {

    private int[] resourceIDs;

    public ResourceSection(ChunkType chunkType, IntReader reader) {
        super(chunkType, reader);
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        // Initialize this variable here
        resourceIDs = new int[((size / 4) - 2)];
    }

    @Override
    public void readSection(IntReader inputReader) throws IOException {
        for (int i = 0; i < ((size / 4) - 2); i++) {
            resourceIDs[i] = inputReader.readInt();
        }
    }

    public int getResourceID(int index) {
        return resourceIDs[index];
    }

    public int getResourceCount() {
        return resourceIDs.length;
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        return null;
    }
}
