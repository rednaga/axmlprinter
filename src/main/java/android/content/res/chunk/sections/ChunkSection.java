package android.content.res.chunk.sections;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.types.Chunk;

public interface ChunkSection extends Chunk {

    public void readHeader(IntReader inputReader) throws IOException;

    public void readSection(IntReader inputReader) throws IOException;

}
