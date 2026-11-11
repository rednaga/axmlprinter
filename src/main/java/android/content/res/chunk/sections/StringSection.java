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

    // Styles are lists of spans (tag name, first char, last char) per string. They're kept in their raw binary
    // form so a pool with styles can be written back unchanged, see getStyleSpans() for decoding
    private ArrayList<Integer> styleOffsets;
    private byte[] styleData;

    /** Size of the chunk type, chunk size and the five static header ints. */
    private static final int HEADER_SIZE = 7 * 4;
    private static final int STYLE_END = -1;

    /**
     * A single style span: the string index of the tag name (for example "b"), and the first and last
     * character of the string it applies to.
     */
    public static final class StyleSpan {
        private final int nameIndex;
        private final int firstChar;
        private final int lastChar;

        public StyleSpan(int nameIndex, int firstChar, int lastChar) {
            this.nameIndex = nameIndex;
            this.firstChar = firstChar;
            this.lastChar = lastChar;
        }

        public int getNameIndex() {
            return nameIndex;
        }

        public int getFirstChar() {
            return firstChar;
        }

        public int getLastChar() {
            return lastChar;
        }
    }

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
        styleOffsets = new ArrayList<Integer>();
        styleData = new byte[0];
    }

    @Override
    public void readSection(IntReader inputReader) throws IOException {
        for (int i = 0; i < stringChunkCount; i++) {
            stringChunkPool.add(new PoolItem(inputReader.readInt(), null));
        }

        // The style offsets sit between the string offsets and the string data
        for (int i = 0; i < styleChunkCount; i++) {
            styleOffsets.add(inputReader.readInt());
        }

        if (styleChunkCount > 0) {
            int padding = stringChunkPoolOffset - (HEADER_SIZE + (stringChunkCount * 4) + (styleChunkCount * 4));
            if (padding > 0) {
                inputReader.skip(padding);
            }
        }

        int stringDataRead = 0;
        if (!stringChunkPool.isEmpty()) {
            stringDataRead = readPool(stringChunkPool, stringChunkFlags, inputReader);
        }

        if (styleChunkCount > 0) {
            // Skip the terminator/padding after the last string and read the style data up to the end of the chunk
            int untilStyles = (styleChunkPoolOffset - stringChunkPoolOffset) - stringDataRead;
            if (untilStyles > 0) {
                inputReader.skip(untilStyles);
            }

            // The size comes from the file, so grow with the data actually read rather than allocating it up front
            ByteArrayOutputStream styles = new ByteArrayOutputStream();
            for (int i = size - styleChunkPoolOffset; i > 0; i--) {
                styles.write(inputReader.readByte());
            }
            styleData = styles.toByteArray();
        }

    }

private int readPool(ArrayList<PoolItem> pool, int flags, IntReader inputReader) throws IOException {
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

            // The length comes straight from the file, so it must not drive the allocation up front
            StringBuilder result = new StringBuilder(Math.min(length, 1024));
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

    return currentStreamPosition;
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

    public int getStyleCount() {
        return styleOffsets.size();
    }

    /**
     * @return the spans of the style for the string with this index, empty if there is no such style
     */
    public List<StyleSpan> getStyleSpans(int index) {
        List<StyleSpan> spans = new ArrayList<>();
        if (index < 0 || index >= styleOffsets.size()) {
            return spans;
        }

        ByteBuffer data = ByteBuffer.wrap(styleData).order(ByteOrder.LITTLE_ENDIAN);
        int position = styleOffsets.get(index);
        while (position >= 0 && position + 4 <= styleData.length) {
            int name = data.getInt(position);
            if (name == STYLE_END || position + 12 > styleData.length) {
                break;
            }
            spans.add(new StyleSpan(name, data.getInt(position + 4), data.getInt(position + 8)));
            position += 12;
        }
        return spans;
    }

    /**
     * @return the tag names of the style's spans separated by commas (for example "b,i"), empty if there are none
     */
    public String getStyle(int index) {
        StringBuilder names = new StringBuilder();
        for (StyleSpan span : getStyleSpans(index)) {
            if (names.length() > 0) {
                names.append(',');
            }
            names.append(getString(span.getNameIndex()));
        }
        return names.toString();
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

        return HEADER_SIZE + // Header and static sections
                (stringChunkPool.size() * 4) + // string table offset size
                (styleOffsets.size() * 4) + // style table offset size
                stringDataSize +
                styleData.length;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.res.chunk.types.Chunk#toBytes()
     */
    @Override
    public byte[] toBytes() {
        byte[] header = super.toBytes();

        int stringCount = stringChunkPool.size();
        int styleCount = styleOffsets.size();

        ByteBuffer stringOffsets = ByteBuffer.allocate(stringCount * 4).order(ByteOrder.LITTLE_ENDIAN);
        ArrayList<byte[]> stringData = new ArrayList<>();
        int stringDataSize = 0;
        for (PoolItem item : stringChunkPool) {
            stringOffsets.putInt(stringDataSize);

            byte[] encoded = encodeString(item.getString());
            stringDataSize += encoded.length;
            stringData.add(encoded);
        }

        // String data follows both offset tables, and the style data follows the string data
        int newStringChunkOffset = 0;
        if (stringCount > 0) {
            newStringChunkOffset = HEADER_SIZE + (stringCount * 4) + (styleCount * 4);
        }
        int newStyleChunkOffset = 0;
        if (styleCount > 0) {
            newStyleChunkOffset = HEADER_SIZE + (stringCount * 4) + (styleCount * 4) + stringDataSize;
        }

        ByteBuffer out = ByteBuffer.allocate(getSize()).order(ByteOrder.LITTLE_ENDIAN)
                .put(header)
                .putInt(stringCount)
                .putInt(styleCount)
                .putInt(stringChunkFlags)
                .putInt(newStringChunkOffset)
                .putInt(newStyleChunkOffset)
                .put(stringOffsets.array());
        for (int styleOffset : styleOffsets) {
            out.putInt(styleOffset);
        }
        for (byte[] data : stringData) {
            out.put(data);
        }
        out.put(styleData);
        return out.array();
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
