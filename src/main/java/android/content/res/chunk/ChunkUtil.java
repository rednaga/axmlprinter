/*
 * Copyright 2008 Android4ME
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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
 * @author Dmitry Skiba
 * 
 */
public class ChunkUtil {
    public static final void readCheckType(IntReader reader, int expectedType) throws IOException {
        int type = reader.readInt();
        if (type != expectedType) {
            throw new IOException("Expected chunk of type 0x" + Integer.toHexString(expectedType) + ", read 0x"
                            + Integer.toHexString(type) + ".");
        }
    }

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
