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
package android.content.res.chunk.types;

import android.content.res.IntReader;
import android.content.res.XmlFormat;
import android.content.res.chunk.AttributeType;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Specific type of Chunk which contains the metadata
 *
 * @author tstrazzere
 */
public class Attribute implements Chunk {

    public static class Permission {
        public static final int PROTECTION_MASK_BASE = 0xF;

        public static final int PROTECTION_NORMAL = 0;
        public static final int PROTECTION_DANGEROUS = 1;
        public static final int PROTECTION_SIGNATURE = 2;
        public static final int PROTECTION_SIGNATURE_OR_SYSTEM = 3;
        public static final int PROTECTION_INTERNAL = 4;
    }

    private int uri;
    private int name;
    private int stringData;
    // TODO : Refactor to proper enum
    private int attributeType;
    private int data;

    public Attribute(String uri,
                     String name,
                     String stringData,
                     AttributeType type,
                     Object data,
                     StringSection stringSection) {
        this.uri = stringSection.getStringIndex(uri);
        this.name = stringSection.getStringIndex(name);
        this.stringData = stringSection.getStringIndex(stringData);
        this.attributeType = type.getIntType();

        if (attributeType == AttributeType.STRING.getIntType()) {
            if (this.stringData == -1) {
                this.stringData = stringSection.putStringIndex(stringData);
            }
            this.data = -1;
        } else {
            if (data == null) {
                throw new IllegalArgumentException("Attribute of type " + type + " needs a value, but data was null");
            }
            this.data = (int) data;
        }

    }

    public Attribute(IntReader reader) {
        try {
            uri = reader.readInt();
            name = reader.readInt();
            stringData = reader.readInt();
            attributeType = reader.readInt();
            data = reader.readInt();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.res.chunk.types.Chunk#readHeader(android.content.res.IntReader)
     */
    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        // No header to read here
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.res.chunk.types.Chunk#getChunkType()
     */
    @Override
    public ChunkType getChunkType() {
        return ChunkType.ATTRIBUTE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.res.chunk.types.Chunk#getSize()
     */
    @Override
    public int getSize() {
        return 4 * 5;
    }

    public int getNameIndex() {
        return name;
    }

    public int getStringDataIndex() {
        return stringData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.res.chunk.types.Chunk#toXML(android.content.res.chunk.sections.StringSection,
     * android.content.res.chunk.sections.ResourceSection, int)
     */
    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, List<NameSpace> namespaceList, int indent) {
        StringBuffer buffer = new StringBuffer();
        if (uri >= 0) {
            for (NameSpace nameSpace : namespaceList) {
                if (nameSpace.getUri() == uri) {
                    buffer.append(stringSection.getString(nameSpace.getPrefix()));
                    buffer.append(":");
                    break;
                }
            }
        }

        buffer.append(stringSection.getString(name));
        buffer.append("=\"");

        buffer.append(valueToXML(stringSection));

        buffer.append("\"");

        return buffer.toString();
    }

    private String valueToXML(StringSection stringSection) {
        if (attributeType == AttributeType.STRING.getIntType()) {
            return XmlFormat.escape(stringSection.getString(stringData));
        } else if (attributeType == AttributeType.INT.getIntType()) {
            return String.valueOf(data);
        } else if (attributeType == AttributeType.RESOURCE.getIntType()) {
            return "@" + Integer.toHexString(data).toUpperCase();
        } else if (attributeType == AttributeType.ATTR.getIntType()) {
            return "?" + Integer.toHexString(data).toUpperCase();
        } else if (attributeType == AttributeType.BOOLEAN.getIntType()) {
            // TODO : Double check this..
            if (data == -1) {
                return "true";
            } else if (data == 0) {
                return "false";
            }
            return "ERROR";
        } else if (attributeType == AttributeType.FLOAT.getIntType()) {
            return String.valueOf(Float.intBitsToFloat(data));
        } else if (attributeType == AttributeType.DIMEN.getIntType()) {
            return XmlFormat.dimension(data);
        } else if (attributeType == AttributeType.FRACTION.getIntType()) {
            return XmlFormat.fraction(data);
        } else if (attributeType == AttributeType.COLOR1.getIntType()) {
            return String.format("#%08x", data);
        } else if (attributeType == AttributeType.COLOR2.getIntType()) {
            return String.format("#%06x", data & 0xFFFFFF);
        } else if (attributeType == AttributeType.FLAGS.getIntType()
                && "protectionLevel".equals(stringSection.getString(name))) {
            return getProtectionString(data);
        }

        // Other flag values, and any type we don't know, are shown as hex rather than silently dropped
        return "0x" + Integer.toHexString(data);
    }

    private String getProtectionString(int level) {
        switch (level & Permission.PROTECTION_MASK_BASE) {
            case Permission.PROTECTION_DANGEROUS:
                return "dangerous";
            case Permission.PROTECTION_NORMAL:
                return "normal";
            case Permission.PROTECTION_SIGNATURE:
                return "signature";
            case Permission.PROTECTION_SIGNATURE_OR_SYSTEM:
                return "signatureOrSystem";
            case Permission.PROTECTION_INTERNAL:
                return "internal";
            default:
                return "????";
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.res.chunk.types.Chunk#toBytes()
     */
    @Override
    public byte[] toBytes() {
        return ByteBuffer.allocate(getSize())
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(uri)
                .putInt(name)
                .putInt(stringData)
                .putInt(attributeType)
                .putInt(data)
                .array();
    }
}
