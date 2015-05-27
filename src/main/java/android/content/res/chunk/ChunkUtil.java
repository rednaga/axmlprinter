package android.content.res.chunk;

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;
import android.content.res.chunk.types.AXMLHeader;
import android.content.res.chunk.types.Buffer;
import android.content.res.chunk.types.Chunk;
import android.content.res.chunk.types.EndTag;
import android.content.res.chunk.types.NameSpace;
import android.content.res.chunk.types.StartTag;
import android.content.res.chunk.types.TextTag;

/**
 * Simple class for reading chunk types.
 * 
 * @author tstrazzere
 */
public class ChunkUtil {

    // TODO : This seems silly
    public static ChunkType readChunkType(IntReader reader) throws IOException {
        int type = reader.readInt();

        for (ChunkType chunkType : ChunkType.values()) {
            if (chunkType.getIntType() == type) {
                return chunkType;
            }
        }

        throw new IOException("Unexpected tag!");
    }

    public static Chunk createChunk(IntReader reader) throws IOException {
        ChunkType chunkType = readChunkType(reader);

        switch (chunkType) {
        case AXML_HEADER:
            return new AXMLHeader(chunkType, reader);
        case STRING_SECTION:
            return new StringSection(chunkType, reader);
        case RESOURCE_SECTION:
            return new ResourceSection(chunkType, reader);
        case START_NAMESPACE:
        case END_NAMESPACE:
            return new NameSpace(chunkType, reader);
        case START_TAG:
            return new StartTag(chunkType, reader);
        case END_TAG:
            return new EndTag(chunkType, reader);
        case TEXT_TAG:
            return new TextTag(chunkType, reader);
        case BUFFER:
            return new Buffer(chunkType, reader);
        default:
            throw new IOException("Unexpected tag!");
        }
    }
}
