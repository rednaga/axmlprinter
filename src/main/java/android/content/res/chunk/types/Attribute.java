package android.content.res.chunk.types;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.AttributeType;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

public class Attribute implements Chunk {

    private int uri;
    private int name;
    private int stringData;
    // TODO : Refactor to proper enum
    private int attributeType;
    private int data;

    public Attribute(IntReader reader) {
        try {
            uri = reader.readInt();
            name = reader.readInt();
            stringData = reader.readInt();
            attributeType = reader.readInt();
            data = reader.readInt();
        } catch (IOException exception) {
            // TODO : Handle this better
            exception.printStackTrace();
        }
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        // No header to read here
    }

    @Override
    public ChunkType getChunkType() {
        return ChunkType.ATTRIBUTE;
    }

    @Override
    public int getSize() {
        return 4;
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        StringBuffer buffer = new StringBuffer();
        if ((uri - 1) > 0) {
            buffer.append(stringSection.getString(uri - 1));
            buffer.append(":");
        }

        buffer.append(stringSection.getString(name));

        buffer.append("=\"");

        if (attributeType == AttributeType.STRING.getIntType()) {
            buffer.append(stringSection.getString(stringData));
        } else if (attributeType == AttributeType.INT.getIntType()) {
            buffer.append(data);
        }

        buffer.append("\"");

        return buffer.toString();
    }
}
