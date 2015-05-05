package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;

public abstract class GenericChunk implements Chunk {

    private ChunkType type;
    protected int size;

    public GenericChunk(ChunkType chunkType, IntReader reader) {
        type = chunkType;
        try {
            size = reader.readInt();
            readHeader(reader);
        } catch (IOException exception) {
            // TODO : Handle this better
            exception.printStackTrace();
        }
    }

    public ChunkType getChunkType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    protected String indent(int indents) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < indents; i++) {
            buffer.append("\t");
        }
        return buffer.toString();
    }
}
