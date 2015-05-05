package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

public class Buffer implements Chunk {

    public Buffer(ChunkType chunkType, IntReader inputReader) {

    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        // No header to read here
    }

    @Override
    public ChunkType getChunkType() {
        return ChunkType.BUFFER;
    }

    @Override
    public int getSize() {
        return 4;
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        return null;
    }
}
