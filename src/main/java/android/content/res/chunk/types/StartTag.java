package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

public class StartTag extends GenericChunk implements Chunk {

    private int lineNumber;
    private int unknown;
    private int namespaceUri;
    private int name;
    private int flags;
    private int attributeCount;
    private int classAttribute;
    private Attribute[] attributes;

    public StartTag(ChunkType chunkType, IntReader inputReader) {
        super(chunkType, inputReader);
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        lineNumber = inputReader.readInt();
        unknown = inputReader.readInt();
        namespaceUri = inputReader.readInt();
        name = inputReader.readInt();
        flags = inputReader.readInt();
        attributeCount = inputReader.readInt();
        classAttribute = inputReader.readInt();
        if (attributeCount > 0) {
            attributes = new Attribute[attributeCount];
            for (int i = 0; i < attributeCount; i++) {
                attributes[i] = new Attribute(inputReader);
            }
        }
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(indent(indent));
        buffer.append("<");
        buffer.append(stringSection.getString(name));
        buffer.append("\n");

        for (int i = 0; i < attributeCount; i++) {
            buffer.append(indent(indent + 1));
            buffer.append(attributes[i].toXML(stringSection, resourceSection, indent));
            buffer.append("\n");
        }

        buffer.append(indent(indent + 1));
        buffer.append(">");

        return buffer.toString();
    }
}
