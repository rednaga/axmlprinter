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
package android.content.res;

import com.android.aapt.Resources.XmlNode;
import com.android.aapt.Resources.XmlElement;
import com.android.aapt.Resources.XmlAttribute;
import com.android.aapt.Resources.XmlNamespace;
import com.android.aapt.Resources.Item;
import com.android.aapt.Resources.Primitive;
import com.android.aapt.Resources.Reference;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Parser for Protocol Buffers format Android XML files.
 * This format is used in APKs built from Android App Bundles (AAB).
 *
 * @author tstrazzere
 */
public class ProtobufXMLResource {

    private XmlNode rootNode;

    public ProtobufXMLResource() {
    }

    public ProtobufXMLResource(InputStream stream) throws IOException {
        if (!read(stream)) {
            throw new IOException("Failed to read protobuf XML resource: invalid or empty root node");
        }
    }

    public boolean read(InputStream stream) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        try {
            rootNode = XmlNode.parseFrom(stream);
            if (rootNode == null) {
                return false;
            }
            return rootNode.hasElement();
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw new IOException("Invalid protobuf format: " + e.getMessage(), e);
        }
    }

    public void print() {
        System.out.println(toXML());
    }

    public String toXML() {
        if (rootNode == null) {
            throw new IllegalStateException("Cannot generate XML: root node is null. Call read() first.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

        if (rootNode.hasElement()) {
            // Use a stack to track namespace scopes
            Stack<Map<String, String>> namespaceStack = new Stack<>();
            namespaceStack.push(new HashMap<>());
            elementToXML(rootNode.getElement(), sb, 0, true, namespaceStack);
        }
        
        return sb.toString();
    }

    private void elementToXML(XmlElement element, StringBuilder sb, int indent, boolean isRoot, 
                               Stack<Map<String, String>> namespaceStack) {
        if (element == null) {
            throw new IllegalArgumentException("XmlElement cannot be null");
        }

        String indentStr = getIndent(indent);
        sb.append(indentStr).append("<");

        // Push new namespace scope for this element
        Map<String, String> currentNamespaces = new HashMap<>(namespaceStack.peek());
        namespaceStack.push(currentNamespaces);
        
        // Handle namespace prefix
        String prefix = getPrefix(element.getNamespaceUri(), currentNamespaces);
        if (prefix != null && !prefix.isEmpty()) {
            sb.append(prefix).append(":");
        }
        sb.append(element.getName() != null ? element.getName() : "");

        // Collect namespace declarations - only output if they're new to this scope
        boolean hasNewNamespaces = false;
        for (XmlNamespace ns : element.getNamespaceDeclarationList()) {
            String nsUri = ns.getUri();
            String nsPrefix = ns.getPrefix();

            // Only declare if not already in current scope
            if (!currentNamespaces.containsKey(nsUri)) {
                currentNamespaces.put(nsUri, nsPrefix);
                hasNewNamespaces = true;
                sb.append("\n").append(indentStr).append("\t");
                sb.append("xmlns");
                if (nsPrefix != null && !nsPrefix.isEmpty()) {
                    sb.append(":").append(nsPrefix);
                }
                sb.append("=\"").append(escapeXml(nsUri)).append("\"");
            }
        }

        // Handle attributes
        for (XmlAttribute attr : element.getAttributeList()) {
            if (attr == null) {
                continue;
            }
            sb.append("\n").append(indentStr).append("\t");

            String attrPrefix = getPrefix(attr.getNamespaceUri(), currentNamespaces);
            if (attrPrefix != null && !attrPrefix.isEmpty()) {
                sb.append(attrPrefix).append(":");
            }
            String attrName = attr.getName();
            if (attrName == null) {
                attrName = "";
            }
            sb.append(attrName).append("=\"");
            sb.append(escapeXml(getAttributeValue(attr)));
            sb.append("\"");
        }

        // Handle children
        if (element.getChildCount() == 0) {
            sb.append(" />\n");
        } else {
            sb.append(">\n");

            for (XmlNode child : element.getChildList()) {
                if (child == null) {
                    continue;
                }
                if (child.hasElement()) {
                    elementToXML(child.getElement(), sb, indent + 1, false, namespaceStack);
                } else if (child.getText() != null && !child.getText().isEmpty()) {
                    sb.append(getIndent(indent + 1));
                    sb.append(escapeXml(child.getText()));
                    sb.append("\n");
                }
            }

            sb.append(indentStr).append("</");
            if (prefix != null && !prefix.isEmpty()) {
                sb.append(prefix).append(":");
            }
            String elementName = element.getName();
            if (elementName == null) {
                elementName = "";
            }
            sb.append(elementName).append(">\n");
        }

        // Pop namespace scope when done with this element
        namespaceStack.pop();
    }

    private String getAttributeValue(XmlAttribute attr) {
        if (attr == null) {
            return "";
        }

        // Check if there's a compiled value
        if (attr.hasCompiledItem()) {
            Item item = attr.getCompiledItem();
            if (item == null) {
                return attr.getValue() != null ? attr.getValue() : "";
            }

            if (item.hasRef()) {
                Reference ref = item.getRef();
                if (ref == null) {
                    return attr.getValue() != null ? attr.getValue() : "";
                }

                // Prefer name over ID if available
                String refName = ref.getName();
                if (refName != null && !refName.isEmpty()) {
                    return "@" + refName;
                } else if (ref.getId() != 0) {
                    // Try to resolve common Android resource IDs
                    String resolvedName = resolveResourceId(ref.getId());
                    if (resolvedName != null) {
                        return "@" + resolvedName;
                    }
                    return String.format("@0x%08x", ref.getId());
                }
            } else if (item.hasPrim()) {
                Primitive prim = item.getPrim();
                if (prim != null) {
                    return formatPrimitive(prim);
                }
            } else if (item.hasStr()) {
                com.android.aapt.Resources.String str = item.getStr();
                if (str != null && str.getValue() != null) {
                    return str.getValue();
                }
            } else if (item.hasRawStr()) {
                com.android.aapt.Resources.RawString rawStr = item.getRawStr();
                if (rawStr != null && rawStr.getValue() != null) {
                    return rawStr.getValue();
                }
            }
        }

        // Fall back to string value
        String value = attr.getValue();
        return value != null ? value : "";
    }

    /**
     * Attempts to resolve common Android resource IDs to their names.
     * This is a best-effort approach for well-known Android framework resources.
     *
     * @param resourceId The resource ID to resolve
     * @return The resource name if known, null otherwise
     */
    private String resolveResourceId(int resourceId) {
        // Common Android framework resource IDs (0x0101xxxx range)
        // This is a limited set - full resolution would require access to the resource table
        // which is not available in protobuf XML format

        // Some common attribute IDs
        switch (resourceId) {
            case 0x0101000e: return "android:enabled";
            case 0x0101000f: return "android:id";
            case 0x01010010: return "android:icon";
            case 0x01010011: return "android:label";
            case 0x01010012: return "android:name";
            case 0x01010013: return "android:permission";
            case 0x01010014: return "android:process";
            case 0x01010020: return "android:theme";
            case 0x01010030: return "android:exported";
            case 0x01010040: return "android:authorities";
            case 0x01010050: return "android:priority";
            case 0x0101006c: return "android:versionCode";
            case 0x0101006d: return "android:versionName";
            case 0x0101006e: return "android:package";
            case 0x0101006f: return "android:sharedUserId";
            case 0x0101011e: return "android:minSdkVersion";
            case 0x0101011f: return "android:targetSdkVersion";
            case 0x01010120: return "android:maxSdkVersion";
            case 0x0101013e: return "android:screenOrientation";
            case 0x0101013f: return "android:configChanges";
            case 0x01010140: return "android:launchMode";
            case 0x01010141: return "android:windowSoftInputMode";
            case 0x01010142: return "android:hardwareAccelerated";
            case 0x01010143: return "android:allowBackup";
            case 0x01010144: return "android:supportsRtl";
            case 0x01010145: return "android:usesCleartextTraffic";
            default:
                return null;
        }
    }

    private String formatPrimitive(Primitive prim) {
        if (prim == null) {
            return "";
        }

        if (prim.hasIntDecimalValue()) {
            return String.valueOf(prim.getIntDecimalValue());
        } else if (prim.hasIntHexadecimalValue()) {
            return String.format("0x%x", prim.getIntHexadecimalValue());
        } else if (prim.hasBooleanValue()) {
            return prim.getBooleanValue() != 0 ? "true" : "false";
        } else if (prim.hasFloatValue()) {
            return String.valueOf(prim.getFloatValue());
        } else if (prim.hasColorArgb8Value()) {
            return String.format("#%08x", prim.getColorArgb8Value());
        } else if (prim.hasColorRgb8Value()) {
            return String.format("#%06x", prim.getColorRgb8Value());
        } else if (prim.hasColorArgb4Value()) {
            return String.format("#%04x", prim.getColorArgb4Value());
        } else if (prim.hasColorRgb4Value()) {
            return String.format("#%03x", prim.getColorRgb4Value());
        } else if (prim.hasDimensionValue()) {
            return formatDimension(prim.getDimensionValue());
        } else if (prim.hasFractionValue()) {
            return formatFraction(prim.getFractionValue());
        } else if (prim.hasNullValue()) {
            return "";
        } else if (prim.hasEmptyValue()) {
            return "";
        }
        return "";
    }

    private String formatDimension(int value) {
        // Android dimension encoding: value is in the form (value << 8) | unit
        float floatValue = complexToFloat(value);
        int unit = value & 0xf;

        // Validate unit is within expected range (0-5 for standard units)
        String[] units = {"px", "dp", "sp", "pt", "in", "mm"};
        String unitStr;
        if (unit >= 0 && unit < units.length) {
            unitStr = units[unit];
        } else {
            // Invalid unit, default to px but log a warning
            unitStr = "px";
            // Note: In a production environment, you might want to log this
        }

        if (floatValue == (int) floatValue) {
            return String.format("%d%s", (int) floatValue, unitStr);
        }
        return String.format("%s%s", floatValue, unitStr);
    }

    private String formatFraction(int value) {
        float floatValue = complexToFloat(value);
        int type = (value >> 4) & 0x3;

        if (type == 0) {
            return String.format("%.2f%%", floatValue * 100);
        } else {
            return String.format("%.2f%%p", floatValue * 100);
        }
    }

    private float complexToFloat(int complex) {
        int mantissa = (complex >> 8) & 0xffffff;
        int radix = (complex >> 4) & 0x3;

        float value = mantissa;
        switch (radix) {
            case 0: // 23p0
                break;
            case 1: // 16p7
                value /= (1 << 7);
                break;
            case 2: // 8p15
                value /= (1 << 15);
                break;
            case 3: // 0p23
                value /= (1 << 23);
                break;
        }

        return value;
    }

    private String getPrefix(String namespaceUri, Map<String, String> namespaceMap) {
        if (namespaceUri == null || namespaceUri.isEmpty()) {
            return "";
        }
        if (namespaceMap == null) {
            return "";
        }
        return namespaceMap.getOrDefault(namespaceUri, "");
    }

    private String getIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("\t");
        }
        return sb.toString();
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Check if the input stream contains a protobuf format XML.
     * Protobuf format typically starts with field tag 0x0A (field 1, wire type 2 = length-delimited).
     */
    public static boolean isProtobufFormat(byte[] header) {
        // Protobuf XmlNode starts with field 1 (element) or field 2 (text)
        // Field 1, wire type 2 = (1 << 3) | 2 = 0x0A
        // Field 2, wire type 2 = (2 << 3) | 2 = 0x12
        return header.length >= 1 && (header[0] == 0x0A || header[0] == 0x12);
    }
}

