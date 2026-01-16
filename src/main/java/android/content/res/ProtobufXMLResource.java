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

/**
 * Parser for Protocol Buffers format Android XML files.
 * This format is used in APKs built from Android App Bundles (AAB).
 *
 * @author tstrazzere
 */
public class ProtobufXMLResource {

    private XmlNode rootNode;
    private Map<String, String> namespaceMap;

    public ProtobufXMLResource() {
        namespaceMap = new HashMap<>();
    }

    public ProtobufXMLResource(InputStream stream) throws IOException {
        namespaceMap = new HashMap<>();
        read(stream);
    }

    public boolean read(InputStream stream) throws IOException {
        rootNode = XmlNode.parseFrom(stream);
        return rootNode != null && rootNode.hasElement();
    }

    public void print() {
        System.out.println(toXML());
    }

    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        
        if (rootNode != null && rootNode.hasElement()) {
            elementToXML(rootNode.getElement(), sb, 0, true);
        }
        
        return sb.toString();
    }

    private void elementToXML(XmlElement element, StringBuilder sb, int indent, boolean isRoot) {
        String indentStr = getIndent(indent);
        sb.append(indentStr).append("<");
        
        // Handle namespace prefix
        String prefix = getPrefix(element.getNamespaceUri());
        if (!prefix.isEmpty()) {
            sb.append(prefix).append(":");
        }
        sb.append(element.getName());

        // Collect namespace declarations for root or when new namespaces appear
        if (isRoot) {
            for (XmlNamespace ns : element.getNamespaceDeclarationList()) {
                namespaceMap.put(ns.getUri(), ns.getPrefix());
                sb.append("\n").append(indentStr).append("\t");
                sb.append("xmlns");
                if (!ns.getPrefix().isEmpty()) {
                    sb.append(":").append(ns.getPrefix());
                }
                sb.append("=\"").append(escapeXml(ns.getUri())).append("\"");
            }
        } else {
            // Handle namespace declarations in non-root elements
            for (XmlNamespace ns : element.getNamespaceDeclarationList()) {
                namespaceMap.put(ns.getUri(), ns.getPrefix());
                sb.append("\n").append(indentStr).append("\t");
                sb.append("xmlns");
                if (!ns.getPrefix().isEmpty()) {
                    sb.append(":").append(ns.getPrefix());
                }
                sb.append("=\"").append(escapeXml(ns.getUri())).append("\"");
            }
        }

        // Handle attributes
        for (XmlAttribute attr : element.getAttributeList()) {
            sb.append("\n").append(indentStr).append("\t");
            
            String attrPrefix = getPrefix(attr.getNamespaceUri());
            if (!attrPrefix.isEmpty()) {
                sb.append(attrPrefix).append(":");
            }
            sb.append(attr.getName()).append("=\"");
            sb.append(escapeXml(getAttributeValue(attr)));
            sb.append("\"");
        }

        // Handle children
        if (element.getChildCount() == 0) {
            sb.append(" />\n");
        } else {
            sb.append(">\n");
            
            for (XmlNode child : element.getChildList()) {
                if (child.hasElement()) {
                    elementToXML(child.getElement(), sb, indent + 1, false);
                } else if (!child.getText().isEmpty()) {
                    sb.append(getIndent(indent + 1));
                    sb.append(escapeXml(child.getText()));
                    sb.append("\n");
                }
            }
            
            sb.append(indentStr).append("</");
            if (!prefix.isEmpty()) {
                sb.append(prefix).append(":");
            }
            sb.append(element.getName()).append(">\n");
        }
    }

    private String getAttributeValue(XmlAttribute attr) {
        // Check if there's a compiled value
        if (attr.hasCompiledItem()) {
            Item item = attr.getCompiledItem();
            
            if (item.hasRef()) {
                Reference ref = item.getRef();
                if (!ref.getName().isEmpty()) {
                    return "@" + ref.getName();
                } else if (ref.getId() != 0) {
                    return String.format("@0x%08x", ref.getId());
                }
            } else if (item.hasPrim()) {
                return formatPrimitive(item.getPrim());
            } else if (item.hasStr()) {
                return item.getStr().getValue();
            } else if (item.hasRawStr()) {
                return item.getRawStr().getValue();
            }
        }
        
        // Fall back to string value
        return attr.getValue();
    }

    private String formatPrimitive(Primitive prim) {
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
        
        String[] units = {"px", "dp", "sp", "pt", "in", "mm"};
        String unitStr = unit < units.length ? units[unit] : "px";
        
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

    private String getPrefix(String namespaceUri) {
        if (namespaceUri == null || namespaceUri.isEmpty()) {
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

