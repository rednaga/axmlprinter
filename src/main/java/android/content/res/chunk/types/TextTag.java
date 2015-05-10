package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

public class TextTag extends GenericChunk implements Chunk {

    private int lineNumber;
    private int unknown;

    private int name;
    private int unknown2;
    private int unknown3;

    public TextTag(ChunkType chunkType, IntReader inputReader) {
        super(chunkType, inputReader);
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        lineNumber = inputReader.readInt();
        unknown = inputReader.readInt();
        name = inputReader.readInt();
        unknown2 = inputReader.readInt();
        unknown3 = inputReader.readInt();
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(indent(indent));
        buffer.append(stringSection.getString(name));

        return buffer.toString();
    }
}
