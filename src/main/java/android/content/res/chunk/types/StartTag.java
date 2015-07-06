/* 
 * Copyright 2015 Red Naga
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

import java.io.IOException;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.sections.ResourceSection;
import android.content.res.chunk.sections.StringSection;

/**
 * StartTag type of Chunk, differs from a Namespace as there will be specific metadata inside of it
 * 
 * @author tstrazzere
 */
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

    /*
     * (non-Javadoc)
     * 
     * @see android.content.res.chunk.types.Chunk#readHeader(android.content.res.IntReader)
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see android.content.res.chunk.types.Chunk#toXML(android.content.res.chunk.sections.StringSection,
     * android.content.res.chunk.sections.ResourceSection, int)
     */
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
