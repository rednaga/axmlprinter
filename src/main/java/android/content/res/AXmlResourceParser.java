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

package android.content.res;


import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParserException;

import android.util.TypedValue;

/**
 * @author Dmitry Skiba
 * 
 *         Binary xml files parser.
 * 
 *         Parser has only two states: (1) Operational state, which parser obtains after first
 *         successful call to next() and retains until open(), close(), or failed call to next().
 *         (2) Closed state, which parser obtains after open(), close(), or failed call to next().
 *         In this state methods return invalid values or throw exceptions.
 * 
 *         TODO: * check all methods in closed state
 * 
 */
public class AXmlResourceParser implements XmlResourceParser {

  /*
   * All values are essentially indices, e.g. name is an index of name in strings.
   */

  private IntReader reader;
  private boolean operational = false;

  private StringBlock strings;
  private int[] resourceIDs;
  private NamespaceStack namespaces = new NamespaceStack();

  private boolean decreaseDepth;

  private int event;
  private int lineNumber;
  private int name;
  private int namespaceUri;
  private int[] attributes;
  private int idAttribute;
  private int classAttribute;
  private int styleAttribute;

  private static final String E_NOT_SUPPORTED = "Method is not supported.";

  private static final int ATTRIBUTE_IX_NAMESPACE_URI = 0;
  private static final int ATTRIBUTE_IX_NAME = 1;
  private static final int ATTRIBUTE_IX_VALUE_STRING = 2;
  private static final int ATTRIBUTE_IX_VALUE_TYPE = 3;
  private static final int ATTRIBUTE_IX_VALUE_DATA = 4;
  private static final int ATTRIBUTE_LENGHT = 5;

  private static final int CHUNK_AXML_FILE = 0x00080003;
  private static final int CHUNK_RESOURCEIDS = 0x00080180;
  private static final int CHUNK_XML_FIRST = 0x00100100;
  private static final int CHUNK_XML_START_NAMESPACE = 0x00100100;
  private static final int CHUNK_XML_END_NAMESPACE = 0x00100101;
  private static final int CHUNK_XML_START_TAG = 0x00100102;
  private static final int CHUNK_XML_END_TAG = 0x00100103;
  private static final int CHUNK_XML_TEXT = 0x00100104;
  private static final int CHUNK_XML_LAST = 0x00100104;

  public AXmlResourceParser() {
    resetEventInfo();
  }

  /**
   * 
   * @param stream
   */
  public void open(InputStream stream) {
    close();
    if (stream != null) {
      reader = new IntReader(stream, false);
    }
  }

  public void close() {
    if (!operational) {
      return;
    }
    operational = false;
    reader.close();
    reader = null;
    strings = null;
    resourceIDs = null;
    namespaces.reset();
    resetEventInfo();
  }

  // ///////////////////////////////// iteration

  public int next() throws XmlPullParserException, IOException {
    if (reader == null) {
      throw new XmlPullParserException("Parser is not opened.", this, null);
    }
    try {
      doNext();
      return event;
    } catch (IOException e) {
      close();
      throw e;
    }
  }

  public int nextToken() throws XmlPullParserException, IOException {
    return next();
  }

  public int nextTag() throws XmlPullParserException, IOException {
    int eventType = next();
    if ((eventType == TEXT) && isWhitespace()) {
      eventType = next();
    }
    if ((eventType != START_TAG) && (eventType != END_TAG)) {
      throw new XmlPullParserException("Expected start or end tag.", this, null);
    }
    return eventType;
  }

  public String nextText() throws XmlPullParserException, IOException {
    if (getEventType() != START_TAG) {
      throw new XmlPullParserException("Parser must be on START_TAG to read next text.", this, null);
    }
    int eventType = next();
    if (eventType == TEXT) {
      String result = getText();
      eventType = next();
      if (eventType != END_TAG) {
        throw new XmlPullParserException("Event TEXT must be immediately followed by END_TAG.",
            this, null);
      }
      return result;
    } else if (eventType == END_TAG) {
      return "";
    } else {
      throw new XmlPullParserException("Parser must be on START_TAG or TEXT to read text.", this,
          null);
    }
  }

  public void require(int type, String namespace, String name) throws XmlPullParserException,
      IOException {
    if ((type != getEventType()) || ((namespace != null) && !namespace.equals(getNamespace()))
        || ((name != null) && !name.equals(getName()))) {
      throw new XmlPullParserException(TYPES[type] + " is expected.", this, null);
    }
  }

  public int getDepth() {
    return namespaces.getDepth() - 1;
  }

  public int getEventType() throws XmlPullParserException {
    return event;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getName() {
    if ((name == -1) || ((event != START_TAG) && (event != END_TAG))) {
      return null;
    }
    return strings.getString(name);
  }

  public String getText() {
    if ((name == -1) || (event != TEXT)) {
      return null;
    }
    return strings.getString(name);
  }

  public char[] getTextCharacters(int[] holderForStartAndLength) {
    String text = getText();
    if (text == null) {
      return null;
    }
    holderForStartAndLength[0] = 0;
    holderForStartAndLength[1] = text.length();
    char[] chars = new char[text.length()];
    text.getChars(0, text.length(), chars, 0);
    return chars;
  }

  public String getNamespace() {
    return strings.getString(namespaceUri);
  }

  public String getPrefix() {
    int prefix = namespaces.findPrefix(namespaceUri);
    return strings.getString(prefix);
  }

  public String getPositionDescription() {
    return "XML line #" + getLineNumber();
  }

  public int getNamespaceCount(int depth) throws XmlPullParserException {
    return namespaces.getAccumulatedCount(depth);
  }

  public String getNamespacePrefix(int pos) throws XmlPullParserException {
    int prefix = namespaces.getPrefix(pos);
    return strings.getString(prefix);
  }

  public String getNamespaceUri(int pos) throws XmlPullParserException {
    int uri = namespaces.getUri(pos);
    return strings.getString(uri);
  }

  // ///////////////////////////////// attributes

  public String getClassAttribute() {
    if (classAttribute == -1) {
      return null;
    }
    int offset = getAttributeOffset(classAttribute);
    int value = attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
    return strings.getString(value);
  }

  public String getIdAttribute() {
    if (idAttribute == -1) {
      return null;
    }
    int offset = getAttributeOffset(idAttribute);
    int value = attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
    return strings.getString(value);
  }

  public int getIdAttributeResourceValue(int defaultValue) {
    if (idAttribute == -1) {
      return defaultValue;
    }
    int offset = getAttributeOffset(idAttribute);
    int valueType = attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
    if (valueType != TypedValue.TYPE_REFERENCE) {
      return defaultValue;
    }
    return attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
  }

  public int getStyleAttribute() {
    if (styleAttribute == -1) {
      return 0;
    }
    int offset = getAttributeOffset(styleAttribute);
    return attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
  }

  public int getAttributeCount() {
    if (event != START_TAG) {
      return -1;
    }
    return attributes.length / ATTRIBUTE_LENGHT;
  }

  public String getAttributeNamespace(int index) {
    int offset = getAttributeOffset(index);
    int namespace = attributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];
    if (namespace == -1) {
      return "";
    }
    return strings.getString(namespace);
  }

  public String getAttributePrefix(int index) {
    int offset = getAttributeOffset(index);
    int uri = attributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];
    int prefix = namespaces.findPrefix(uri);
    if (prefix == -1) {
      return "";
    }
    return strings.getString(prefix);
  }

  public String getAttributeName(int index) {
    int offset = getAttributeOffset(index);
    int name = attributes[offset + ATTRIBUTE_IX_NAME];
    if (name == -1) {
      return "";
    }
    return strings.getString(name);
  }

  public int getAttributeNameResource(int index) {
    int offset = getAttributeOffset(index);
    int name = attributes[offset + ATTRIBUTE_IX_NAME];
    if ((resourceIDs == null) || (name < 0) || (name >= resourceIDs.length)) {
      return 0;
    }
    return resourceIDs[name];
  }

  public int getAttributeValueType(int index) {
    int offset = getAttributeOffset(index);
    return attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
  }

  public int getAttributeValueData(int index) {
    int offset = getAttributeOffset(index);
    return attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
  }

  public String getAttributeValue(int index) {
    int offset = getAttributeOffset(index);
    int valueType = attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
    if (valueType == TypedValue.TYPE_STRING) {
      int valueString = attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
      return strings.getString(valueString);
    }
    int valueData = attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    return "";// TypedValue.coerceToString(valueType,valueData);
  }

  public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
    return getAttributeIntValue(index, defaultValue ? 1 : 0) != 0;
  }

  public float getAttributeFloatValue(int index, float defaultValue) {
    int offset = getAttributeOffset(index);
    int valueType = attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
    if (valueType == TypedValue.TYPE_FLOAT) {
      int valueData = attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
      return Float.intBitsToFloat(valueData);
    }
    return defaultValue;
  }

  public int getAttributeIntValue(int index, int defaultValue) {
    int offset = getAttributeOffset(index);
    int valueType = attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
    if ((valueType >= TypedValue.TYPE_FIRST_INT) && (valueType <= TypedValue.TYPE_LAST_INT)) {
      return attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }
    return defaultValue;
  }

  public int getAttributeUnsignedIntValue(int index, int defaultValue) {
    return getAttributeIntValue(index, defaultValue);
  }

  public int getAttributeResourceValue(int index, int defaultValue) {
    int offset = getAttributeOffset(index);
    int valueType = attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
    if (valueType == TypedValue.TYPE_REFERENCE) {
      return attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }
    return defaultValue;
  }

  public String getAttributeValue(String namespace, String attribute) {
    int index = findAttribute(namespace, attribute);
    if (index == -1) {
      return null;
    }
    return getAttributeValue(index);
  }

  public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
    int index = findAttribute(namespace, attribute);
    if (index == -1) {
      return defaultValue;
    }
    return getAttributeBooleanValue(index, defaultValue);
  }

  public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
    int index = findAttribute(namespace, attribute);
    if (index == -1) {
      return defaultValue;
    }
    return getAttributeFloatValue(index, defaultValue);
  }

  public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
    int index = findAttribute(namespace, attribute);
    if (index == -1) {
      return defaultValue;
    }
    return getAttributeIntValue(index, defaultValue);
  }

  public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
    int index = findAttribute(namespace, attribute);
    if (index == -1) {
      return defaultValue;
    }
    return getAttributeUnsignedIntValue(index, defaultValue);
  }

  public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
    int index = findAttribute(namespace, attribute);
    if (index == -1) {
      return defaultValue;
    }
    return getAttributeResourceValue(index, defaultValue);
  }

  public int getAttributeListValue(int index, String[] options, int defaultValue) {
    // TODO implement
    return 0;
  }

  public int getAttributeListValue(String namespace, String attribute, String[] options,
      int defaultValue) {
    // TODO implement
    return 0;
  }

  public String getAttributeType(int index) {
    return "CDATA";
  }

  public boolean isAttributeDefault(int index) {
    return false;
  }

  // ///////////////////////////////// dummies

  public void setInput(InputStream stream, String inputEncoding) throws XmlPullParserException {
    throw new XmlPullParserException(E_NOT_SUPPORTED);
  }

  public void setInput(Reader reader) throws XmlPullParserException {
    throw new XmlPullParserException(E_NOT_SUPPORTED);
  }

  public String getInputEncoding() {
    return null;
  }

  public int getColumnNumber() {
    return -1;
  }

  public boolean isEmptyElementTag() throws XmlPullParserException {
    return false;
  }

  public boolean isWhitespace() throws XmlPullParserException {
    return false;
  }

  public void defineEntityReplacementText(String entityName, String replacementText)
      throws XmlPullParserException {
    throw new XmlPullParserException(E_NOT_SUPPORTED);
  }

  public String getNamespace(String prefix) {
    throw new RuntimeException(E_NOT_SUPPORTED);
  }

  public Object getProperty(String name) {
    return null;
  }

  public void setProperty(String name, Object value) throws XmlPullParserException {
    throw new XmlPullParserException(E_NOT_SUPPORTED);
  }

  public boolean getFeature(String feature) {
    return false;
  }

  public void setFeature(String name, boolean value) throws XmlPullParserException {
    throw new XmlPullParserException(E_NOT_SUPPORTED);
  }

  // /////////////////////////////////////////// implementation

  /**
   * Namespace stack, holds prefix+uri pairs, as well as depth information. All information is
   * stored in one int[] array. Array consists of depth frames: Data=DepthFrame*;
   * DepthFrame=Count+[Prefix+Uri]*+Count; Count='count of Prefix+Uri pairs'; Yes, count is stored
   * twice, to enable bottom-up traversal. increaseDepth adds depth frame, decreaseDepth removes it.
   * push/pop operations operate only in current depth frame. decreaseDepth removes any remaining
   * (not pop'ed) namespace pairs. findXXX methods search all depth frames starting from the last
   * namespace pair of current depth frame. All functions that operate with int, use -1 as 'invalid
   * value'.
   * 
   * !! functions expect 'prefix'+'uri' pairs, not 'uri'+'prefix' !!
   * 
   */
  private static final class NamespaceStack {
    public NamespaceStack() {
      m_data = new int[32];
    }

    public final void reset() {
      m_dataLength = 0;
      m_count = 0;
      m_depth = 0;
    }

    public final int getTotalCount() {
      return m_count;
    }

    public final int getCurrentCount() {
      if (m_dataLength == 0) {
        return 0;
      }
      int offset = m_dataLength - 1;
      return m_data[offset];
    }

    public final int getAccumulatedCount(int depth) {
      if ((m_dataLength == 0) || (depth < 0)) {
        return 0;
      }
      if (depth > m_depth) {
        depth = m_depth;
      }
      int accumulatedCount = 0;
      int offset = 0;
      for (; depth != 0; --depth) {
        int count = m_data[offset];
        accumulatedCount += count;
        offset += (2 + (count * 2));
      }
      return accumulatedCount;
    }

    public final void push(int prefix, int uri) {
      if (m_depth == 0) {
        increaseDepth();
      }
      ensureDataCapacity(2);
      int offset = m_dataLength - 1;
      int count = m_data[offset];
      m_data[offset - 1 - (count * 2)] = count + 1;
      m_data[offset] = prefix;
      m_data[offset + 1] = uri;
      m_data[offset + 2] = count + 1;
      m_dataLength += 2;
      m_count += 1;
    }

    public final boolean pop(int prefix, int uri) {
      if (m_dataLength == 0) {
        return false;
      }
      int offset = m_dataLength - 1;
      int count = m_data[offset];
      for (int i = 0, o = offset - 2; i != count; ++i, o -= 2) {
        if ((m_data[o] != prefix) || (m_data[o + 1] != uri)) {
          continue;
        }
        count -= 1;
        if (i == 0) {
          m_data[o] = count;
          o -= (1 + (count * 2));
          m_data[o] = count;
        } else {
          m_data[offset] = count;
          offset -= (1 + 2 + (count * 2));
          m_data[offset] = count;
          System.arraycopy(m_data, o + 2, m_data, o, m_dataLength - o);
        }
        m_dataLength -= 2;
        m_count -= 1;
        return true;
      }
      return false;
    }

    public final boolean pop() {
      if (m_dataLength == 0) {
        return false;
      }
      int offset = m_dataLength - 1;
      int count = m_data[offset];
      if (count == 0) {
        return false;
      }
      count -= 1;
      offset -= 2;
      m_data[offset] = count;
      offset -= (1 + (count * 2));
      m_data[offset] = count;
      m_dataLength -= 2;
      m_count -= 1;
      return true;
    }

    public final int getPrefix(int index) {
      return get(index, true);
    }

    public final int getUri(int index) {
      return get(index, false);
    }

    public final int findPrefix(int uri) {
      return find(uri, false);
    }

    public final int findUri(int prefix) {
      return find(prefix, true);
    }

    public final int getDepth() {
      return m_depth;
    }

    public final void increaseDepth() {
      ensureDataCapacity(2);
      int offset = m_dataLength;
      m_data[offset] = 0;
      m_data[offset + 1] = 0;
      m_dataLength += 2;
      m_depth += 1;
    }

    public final void decreaseDepth() {
      if (m_dataLength == 0) {
        return;
      }
      int offset = m_dataLength - 1;
      int count = m_data[offset];
      if ((offset - 1 - (count * 2)) == 0) {
        return;
      }
      m_dataLength -= 2 + (count * 2);
      m_count -= count;
      m_depth -= 1;
    }

    private void ensureDataCapacity(int capacity) {
      int available = (m_data.length - m_dataLength);
      if (available > capacity) {
        return;
      }
      int newLength = (m_data.length + available) * 2;
      int[] newData = new int[newLength];
      System.arraycopy(m_data, 0, newData, 0, m_dataLength);
      m_data = newData;
    }

    private final int find(int prefixOrUri, boolean prefix) {
      if (m_dataLength == 0) {
        return -1;
      }
      int offset = m_dataLength - 1;
      for (int i = m_depth; i != 0; --i) {
        int count = m_data[offset];
        offset -= 2;
        for (; count != 0; --count) {
          if (prefix) {
            if (m_data[offset] == prefixOrUri) {
              return m_data[offset + 1];
            }
          } else {
            if (m_data[offset + 1] == prefixOrUri) {
              return m_data[offset];
            }
          }
          offset -= 2;
        }
      }
      return -1;
    }

    private final int get(int index, boolean prefix) {
      if ((m_dataLength == 0) || (index < 0)) {
        return -1;
      }
      int offset = 0;
      for (int i = m_depth; i != 0; --i) {
        int count = m_data[offset];
        if (index >= count) {
          index -= count;
          offset += (2 + (count * 2));
          continue;
        }
        offset += (1 + (index * 2));
        if (!prefix) {
          offset += 1;
        }
        return m_data[offset];
      }
      return -1;
    }

    private int[] m_data;
    private int m_dataLength;
    private int m_count;
    private int m_depth;
  }

  // ///////////////////////////////// package-visible

  // final void fetchAttributes(int[] styleableIDs,TypedArray result) {
  // result.resetIndices();
  // if (m_attributes==null || m_resourceIDs==null) {
  // return;
  // }
  // boolean needStrings=false;
  // for (int i=0,e=styleableIDs.length;i!=e;++i) {
  // int id=styleableIDs[i];
  // for (int o=0;o!=m_attributes.length;o+=ATTRIBUTE_LENGHT) {
  // int name=m_attributes[o+ATTRIBUTE_IX_NAME];
  // if (name>=m_resourceIDs.length ||
  // m_resourceIDs[name]!=id)
  // {
  // continue;
  // }
  // int valueType=m_attributes[o+ATTRIBUTE_IX_VALUE_TYPE];
  // int valueData;
  // int assetCookie;
  // if (valueType==TypedValue.TYPE_STRING) {
  // valueData=m_attributes[o+ATTRIBUTE_IX_VALUE_STRING];
  // assetCookie=-1;
  // needStrings=true;
  // } else {
  // valueData=m_attributes[o+ATTRIBUTE_IX_VALUE_DATA];
  // assetCookie=0;
  // }
  // result.addValue(i,valueType,valueData,assetCookie,id,0);
  // }
  // }
  // if (needStrings) {
  // result.setStrings(m_strings);
  // }
  // }

  final StringBlock getStrings() {
    return strings;
  }

  // /////////////////////////////////

  private final int getAttributeOffset(int index) {
    if (event != START_TAG) {
      throw new IndexOutOfBoundsException("Current event is not START_TAG.");
    }
    int offset = index * 5;
    if (offset >= attributes.length) {
      throw new IndexOutOfBoundsException("Invalid attribute index (" + index + ").");
    }
    return offset;
  }

  private final int findAttribute(String namespace, String attribute) {
    if ((strings == null) || (attribute == null)) {
      return -1;
    }
    int name = strings.find(attribute);
    if (name == -1) {
      return -1;
    }
    int uri = (namespace != null) ? strings.find(namespace) : -1;
    for (int o = 0; o != attributes.length; ++o) {
      if ((name == attributes[o + ATTRIBUTE_IX_NAME])
          && ((uri == -1) || (uri == attributes[o + ATTRIBUTE_IX_NAMESPACE_URI]))) {
        return o / ATTRIBUTE_LENGHT;
      }
    }
    return -1;
  }

  private final void resetEventInfo() {
    event = -1;
    lineNumber = -1;
    name = -1;
    namespaceUri = -1;
    attributes = null;
    idAttribute = -1;
    classAttribute = -1;
    styleAttribute = -1;
  }

  private final void doNext() throws IOException {
    // Delayed initialization.
    if (strings == null) {
      ChunkUtil.readCheckType(reader, CHUNK_AXML_FILE);
      /* chunkSize */reader.skipInt();
      strings = StringBlock.read(reader);
      namespaces.increaseDepth();
      operational = true;
    }

    if (event == END_DOCUMENT) {
      return;
    }

    int currentEvent = event;
    resetEventInfo();

    while (true) {
      if (decreaseDepth) {
        decreaseDepth = false;
        namespaces.decreaseDepth();
      }

      // Fake END_DOCUMENT event.
      if ((currentEvent == END_TAG) && (namespaces.getDepth() == 1)
          && (namespaces.getCurrentCount() == 0)) {
        event = END_DOCUMENT;
        break;
      }

      int chunkType;
      if (currentEvent == START_DOCUMENT) {
        // Fake event, see CHUNK_XML_START_TAG handler.
        chunkType = CHUNK_XML_START_TAG;
      } else {
        chunkType = reader.readInt();
      }

      if (chunkType == CHUNK_RESOURCEIDS) {
        int chunkSize = reader.readInt();
        if ((chunkSize < 8) || ((chunkSize % 4) != 0)) {
          throw new IOException("Invalid resource ids size (" + chunkSize + ").");
        }
        resourceIDs = reader.readIntArray((chunkSize / 4) - 2);
        continue;
      }

      if ((chunkType < CHUNK_XML_FIRST) || (chunkType > CHUNK_XML_LAST)) {
        throw new IOException("Invalid chunk type (" + chunkType + ").");
      }

      // Fake START_DOCUMENT event.
      if ((chunkType == CHUNK_XML_START_TAG) && (currentEvent == -1)) {
        event = START_DOCUMENT;
        break;
      }

      // Common header.
      /* chunkSize */reader.skipInt();
      int currentLineNumber = reader.readInt();
      /* 0xFFFFFFFF */reader.skipInt();

      if ((chunkType == CHUNK_XML_START_NAMESPACE) || (chunkType == CHUNK_XML_END_NAMESPACE)) {
        if (chunkType == CHUNK_XML_START_NAMESPACE) {
          int prefix = reader.readInt();
          int uri = reader.readInt();
          namespaces.push(prefix, uri);
        } else {
          /* prefix */reader.skipInt();
          /* uri */reader.skipInt();
          namespaces.pop();
        }
        continue;
      }

      lineNumber = currentLineNumber;

      if (chunkType == CHUNK_XML_START_TAG) {
        namespaceUri = reader.readInt();
        name = reader.readInt();
        /* flags? */reader.skipInt();
        int attributeCount = reader.readInt();
        idAttribute = (attributeCount >>> 16) - 1;
        attributeCount &= 0xFFFF;
        classAttribute = reader.readInt();
        styleAttribute = (classAttribute >>> 16) - 1;
        classAttribute = (classAttribute & 0xFFFF) - 1;
        attributes = reader.readIntArray(attributeCount * ATTRIBUTE_LENGHT);
        for (int i = ATTRIBUTE_IX_VALUE_TYPE; i < attributes.length;) {
          attributes[i] = (attributes[i] >>> 24);
          i += ATTRIBUTE_LENGHT;
        }
        namespaces.increaseDepth();
        event = START_TAG;
        break;
      }

      if (chunkType == CHUNK_XML_END_TAG) {
        namespaceUri = reader.readInt();
        name = reader.readInt();
        event = END_TAG;
        decreaseDepth = true;
        break;
      }

      if (chunkType == CHUNK_XML_TEXT) {
        name = reader.readInt();
        /* ? */reader.skipInt();
        /* ? */reader.skipInt();
        event = TEXT;
        break;
      }
    }
  }
}
