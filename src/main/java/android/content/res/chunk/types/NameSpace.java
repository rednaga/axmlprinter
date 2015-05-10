package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

public class NameSpace extends GenericChunk implements Chunk {

    private int lineNumber;
    private int unknown;
    private int prefix;
    private int uri;

    public NameSpace(ChunkType chunkType, IntReader inputReader) {
        super(chunkType, inputReader);
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        lineNumber = inputReader.readInt();
        unknown = inputReader.readInt();
        prefix = inputReader.readInt();
        uri = inputReader.readInt();
    }

    public boolean isStart() {
        return (getChunkType() == ChunkType.START_NAMESPACE) ? true : false;
    }

    public int getUri() {
        return uri;
    }

    public int getPrefix() {
        return prefix;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String toString(StringSection stringSection) {
        return "xmlns" + ":" + stringSection.getString(getPrefix()) + "=\"" + stringSection.getString(getUri()) + "\"";
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        if (isStart()) {
            return indent(indent) + toString(stringSection);
        } else {
            return "";
        }
    }
}