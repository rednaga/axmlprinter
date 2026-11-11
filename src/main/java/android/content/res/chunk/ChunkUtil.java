/* 
 * Copyright 2015-2025 Red Naga
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.content.res.chunk;

import android.content.res.IntReader;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;
import android.content.res.chunk.types.*;

import java.io.IOException;
import java.io.UncheckedIOException;

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
            // ATTRIBUTE is a faked type that shares the int 0 with BUFFER, it never appears in a stream
            if (chunkType != ChunkType.ATTRIBUTE && chunkType.getIntType() == type) {
                return chunkType;
            }
        }

        throw new IOException(String.format("Unexpected tag! 0x%08X at byte offset %d", type, reader.getBytesRead() - 4));
    }

    public static Chunk createChunk(IntReader reader) throws IOException {
        int positionBeforeRead = reader.getBytesRead();
        ChunkType chunkType = readChunkType(reader);

        try {
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
                    throw new IOException(String.format(
                        "Unexpected chunk type: %s (0x%08X) at byte offset %d. " +
                        "This may indicate a corrupted or unsupported AXML file.",
                        chunkType, chunkType.getIntType(), positionBeforeRead));
            }
        } catch (UncheckedIOException wrapped) {
            throw withContext(wrapped.getCause(), chunkType, positionBeforeRead);
        } catch (IOException e) {
            throw withContext(e, chunkType, positionBeforeRead);
        }
    }

    /**
     * Adds the chunk type and position to an error, unless that has already been done by a nested chunk.
     */
    private static IOException withContext(IOException e, ChunkType chunkType, int position) {
        String message = e.getMessage();
        if (message != null && message.contains("byte offset")) {
            return e;
        }
        return new IOException(String.format(
                "Error creating chunk of type %s (0x%08X) at byte offset %d: %s",
                chunkType, chunkType.getIntType(), position,
                message != null ? message : e.getClass().getSimpleName()), e);
    }
}
