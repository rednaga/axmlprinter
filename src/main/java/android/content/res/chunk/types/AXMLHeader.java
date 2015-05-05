package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

public class AXMLHeader extends GenericChunk {

    public AXMLHeader(ChunkType chunkType, IntReader inputReader) {
        super(chunkType, inputReader);
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        // Nothing else to do
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        return indent(indent) + "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    }
}
