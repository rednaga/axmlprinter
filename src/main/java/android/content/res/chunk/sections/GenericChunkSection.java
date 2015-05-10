package android.content.res.chunk.sections;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.types.Chunk;
import android.content.res.chunk.types.GenericChunk;

public abstract class GenericChunkSection extends GenericChunk implements Chunk, ChunkSection {

    public GenericChunkSection(ChunkType chunkType, IntReader reader) {
        super(chunkType, reader);

        try {
            readSection(reader);

            reader.skip(Math.abs(reader.getBytesRead() - getStartPosition() - size));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
