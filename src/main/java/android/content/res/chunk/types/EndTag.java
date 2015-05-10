package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

public class EndTag extends GenericChunk implements Chunk {

    private int lineNumber;
    private int unknown;
    private int namespaceUri;
    private int name;

    public EndTag(ChunkType chunkType, IntReader inputReader) {
        super(chunkType, inputReader);
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        lineNumber = inputReader.readInt();
        unknown = inputReader.readInt();
        namespaceUri = inputReader.readInt();
        name = inputReader.readInt();
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        return indent(indent) + "</" + stringSection.getString(name) + ">";
    }
}
