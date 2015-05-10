package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

public interface Chunk {

    public void readHeader(IntReader reader) throws IOException;

    public ChunkType getChunkType();

    public int getSize();

    public String toString();

    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent);

}
