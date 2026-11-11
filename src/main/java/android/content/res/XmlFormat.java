/* 
 * Copyright 2015-2026 Red Naga
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

import java.util.Locale;

/**
 * Formatting helpers shared by the binary AXML and the protobuf XML printers.
 *
 * @author tstrazzere
 */
public final class XmlFormat {

    private static final String[] DIMENSION_UNITS = {"px", "dp", "sp", "pt", "in", "mm"};

    private XmlFormat() {
    }

    /**
     * Escapes the characters that can't appear literally in XML text or attribute values.
     *
     * @return the escaped string, an empty string for null
     */
    public static String escape(String value) {
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
     * Decodes an Android "complex" value (the encoding used by dimensions and fractions): a signed 24 bit
     * mantissa in the top bits, a radix in bits 4-5 and a unit in the low nibble.
     */
    public static float complexToFloat(int complex) {
        // Arithmetic shift, so negative mantissas keep their sign
        float value = complex >> 8;
        switch ((complex >> 4) & 0x3) {
            case 1: // 16p7
                value /= (1 << 7);
                break;
            case 2: // 8p15
                value /= (1 << 15);
                break;
            case 3: // 0p23
                value /= (1 << 23);
                break;
            default: // 23p0
                break;
        }
        return value;
    }

    /**
     * @return a dimension such as "16dp" or "0.5sp"
     */
    public static String dimension(int complex) {
        float value = complexToFloat(complex);
        int unit = complex & 0xf;
        // Unknown units are shown as px
        String unitName = unit < DIMENSION_UNITS.length ? DIMENSION_UNITS[unit] : "px";

        if (value == (int) value) {
            return String.valueOf((int) value) + unitName;
        }
        return String.valueOf(value) + unitName;
    }

    /**
     * @return a fraction such as "50.00%" (of itself) or "50.00%p" (of the parent)
     */
    public static String fraction(int complex) {
        float value = complexToFloat(complex) * 100;
        boolean ofParent = (complex & 0xf) == 1;

        return String.format(Locale.ROOT, "%.2f%%%s", value, ofParent ? "p" : "");
    }
}
