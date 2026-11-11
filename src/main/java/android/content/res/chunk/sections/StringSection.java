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
package android.content.res.chunk.sections;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.PoolItem;
import android.content.res.chunk.types.NameSpace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringSection extends GenericChunkSection {

    // This specific tag appears unused but might need to be implemented? or used as an unknown?
    @SuppressWarnings("unused")
    private final int SORTED_FLAG = 1 << 0;
    private final int UTF8_FLAG = 1 << 8;

    private int stringChunkCount;
    private int styleChunkCount;
    private int stringChunkFlags;
    private int stringChunkPoolOffset;
    private int styleChunkPoolOffset;

    // FIXME:
    // This likely could just be an ordered array of Strings if the Integer is just ordered and the key..
    private ArrayList<PoolItem> stringChunkPool;
    private ArrayList<PoolItem> styleChunkPool;

    public StringSection(ChunkType chunkType, IntReader inputReader) {
        super(chunkType, inputReader);
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        stringChunkCount = inputReader.readInt();
        styleChunkCount = inputReader.readInt();
        stringChunkFlags = inputReader.readInt();

        stringChunkPoolOffset = inputReader.readInt();
        stringChunkPool = new ArrayList<PoolItem>();

        styleChunkPoolOffset = inputReader.readInt();
        styleChunkPool = new ArrayList<PoolItem>();
    }

    @Override
    public void readSection(IntReader inputReader) throws IOException {
        for (int i = 0; i < stringChunkCount; i++) {
            stringChunkPool.add(new PoolItem(inputReader.readInt(), null));
        }

        if (!stringChunkPool.isEmpty()) {
            readPool(stringChunkPool, stringChunkFlags, inputReader);
        }

        // TODO : Does this need the flags?
        // FIXME: This is potentially wrong
        for (int i = 0; i < styleChunkCount; i++) {
            styleChunkPool.add(new PoolItem(inputReader.readInt(), null));
        }

        if (!styleChunkPool.isEmpty()) {
            readPool(styleChunkPool, stringChunkFlags, inputReader);
        }
    }

private void readPool(ArrayList<PoolItem> pool, int flags, IntReader inputReader) throws IOException {
    // Create a list of indices to maintain the original order
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < pool.size(); i++) {
        indices.add(i);
    }

    // Sort indices based on the offsets of the pool items
    indices.sort((i1, i2) -> Integer.compare(pool.get(i1).getOffset(), pool.get(i2).getOffset()));

    int currentStreamPosition = 0;

    // Temporary storage for strings
    String[] results = new String[pool.size()];

    // Read data in the order determined by sorted indices
    for (int index : indices) {
        PoolItem item = pool.get(index);
        int targetOffset = item.getOffset();

        // Move to the target offset, skip forward if needed
        if (targetOffset > currentStreamPosition) {
            inputReader.skip(targetOffset - currentStreamPosition);
            currentStreamPosition = targetOffset;
        }

        String value;
        if ((flags & UTF8_FLAG) != 0) {
            // UTF-8 pools store the length in UTF-16 units, then the length in bytes; each is 1 or 2 bytes
            int utf16Length = inputReader.readByte();
            currentStreamPosition += 1;
            if ((utf16Length & 0x80) != 0) {
                utf16Length = ((utf16Length & 0x7F) << 8) | inputReader.readByte();
                currentStreamPosition += 1;
            }

            int byteLength = inputReader.readByte();
            currentStreamPosition += 1;
            if ((byteLength & 0x80) != 0) {
                byteLength = ((byteLength & 0x7F) << 8) | inputReader.readByte();
                currentStreamPosition += 1;
            }

            byte[] data = new byte[byteLength];
            for (int i = 0; i < byteLength; i++) {
                data[i] = (byte) inputReader.readByte();
            }
            currentStreamPosition += byteLength;
            value = new String(data, StandardCharsets.UTF_8);
        } else {
            // UTF-16 pools store the length in UTF-16 units; lengths over 0x7FFF use two shorts
            int length = inputReader.readShort();
            currentStreamPosition += 2;
            if ((length & 0x8000) != 0) {
                length = ((length & 0x7FFF) << 16) | inputReader.readShort();
                currentStreamPosition += 2;
            }

            StringBuilder result = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                result.append((char) inputReader.readShort());
            }
            currentStreamPosition += length * 2;
            value = result.toString();
        }

        // Store the result temporarily
        results[index] = value;
    }

    // Assign results back to the pool items in the original order
    for (int i = 0; i < pool.size(); i++) {
        pool.get(i).setString(results[i]);
    }
}

    public int getStringIndex(String string) {
        if (string != null) {
            for (PoolItem item : stringChunkPool) {
                if (item.getString().equals(string)) {
                    return stringChunkPool.indexOf(item);
                }
            }
        }

        return -1;
    }

    public int putStringIndex(String string) {
        int currentPosition = getStringIndex(string);
        if (currentPosition != -1) {
            return currentPosition;
        }

        stringChunkPool.add(new PoolItem(-1, string));

        return getStringIndex(string);
    }

    public String getString(int index) {
        if ((index > -1) && (index < stringChunkPool.size())) {
            return stringChunkPool.get(index).getString();
        }

        return "";
    }

    public String getStyle(int index) {
        return styleChunkPool.get(index).getString();
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, List<NameSpace> namespaceList, int indent) {
        return null;
    }

    @Override
    public int getSize() {
        int stringDataSize = 0;
        for (PoolItem item : stringChunkPool) {
            stringDataSize += encodeString(item.getString()).length;
        }

        int styleDataSize = 0;
        for (PoolItem item : styleChunkPool) {
            styleDataSize += item.getString().length() * (((stringChunkFlags & UTF8_FLAG) == 0) ? 2 : 1);
        }

        return (2 * 4) + // Header
                (5 * 4) + // static sections
                (stringChunkPool.size() * 4) + // string table offset size
                stringDataSize +
                (styleChunkPool.size() * 4) + // style table offset size
                styleDataSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.res.chunk.types.Chunk#toBytes()
     */
    @Override
    public byte[] toBytes() {
        byte[] header = super.toBytes();

        // TODO : We need to ensure these are already "sorted"
        ByteBuffer offsetBuffer = ByteBuffer.allocate(stringChunkPool.size() * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        int offset = 0;
        ArrayList<byte[]> stringData = new ArrayList<>();
        for (PoolItem item : stringChunkPool) {
            offsetBuffer.putInt(offset);

            byte[] encoded = encodeString(item.getString());
            offset += encoded.length;
            stringData.add(encoded);
        }

        // Combine strings into one buffer
        ByteBuffer stringsBuffer = ByteBuffer.allocate(offsetBuffer.capacity() + offset)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(offsetBuffer.array());
        for (byte[] data : stringData) {
            stringsBuffer.put(data);
        }
        byte[] strings = stringsBuffer.array();

//        byte[] styles = new byte[]{0x00};

        int newStringChunkOffset = 0;
        if (!stringChunkPool.isEmpty()) {
            newStringChunkOffset = (5 * 4) /* header + 3 other ints above it */
                    + stringChunkPool.size() * 4 /* index table size */
                    + 8 /* (this space and the style chunk offset */;
        }

        int newStyleChunkOffset = 0;
        if (!styleChunkPool.isEmpty()) {
            newStyleChunkOffset = (6 * 4) /* header + 4 other ints above it */
                    + styleChunkPool.size() * 4 /* index table size */
                    + 8 /* (this space and the style chunk offset */;
        }

        byte[] body = ByteBuffer.allocate(5 * 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(stringChunkPool.size())
                .putInt(styleChunkPool.size())
                .putInt(stringChunkFlags)
                .putInt(newStringChunkOffset)
                .putInt(newStyleChunkOffset)
                .array();

        return ByteBuffer.allocate(header.length + body.length + strings.length /*+ styles.length*/)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(header)
                .put(body)
                .put(strings)
//                .put(styles)
                .array();
    }

    private boolean isUtf8() {
        return (stringChunkFlags & UTF8_FLAG) != 0;
    }

    /**
     * Encodes a single pool string: length prefix, character data, null terminator, then zero padding so the next
     * string starts on a 4 byte boundary.
     */
    private byte[] encodeString(String string) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (isUtf8()) {
            byte[] data = string.getBytes(StandardCharsets.UTF_8);
            writeUtf8Length(out, string.length());
            writeUtf8Length(out, data.length);
            out.write(data, 0, data.length);
            out.write(0);
        } else {
            int length = string.length();
            if (length > 0x7FFF) {
                int high = 0x8000 | (length >>> 16);
                out.write(high & 0xFF);
                out.write((high >>> 8) & 0xFF);
            }
            out.write(length & 0xFF);
            out.write((length >>> 8) & 0xFF);
            byte[] data = string.getBytes(StandardCharsets.UTF_16LE);
            out.write(data, 0, data.length);
            out.write(0);
            out.write(0);
        }

        while (out.size() % 4 != 0) {
            out.write(0);
        }
        return out.toByteArray();
    }

    private static void writeUtf8Length(ByteArrayOutputStream out, int length) {
        if (length > 0x7FFF) {
            throw new IllegalArgumentException("String is too long for a UTF-8 string pool: " + length);
        }
        if (length > 0x7F) {
            out.write(0x80 | (length >>> 8));
        }
        out.write(length & 0xFF);
    }
}
