package android.content.res;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlFormatTest {

    @Test
    public void escapeHandlesAllSpecialCharacters() {
        assertEquals("a&amp;b&lt;c&gt;&quot;d&quot;&apos;e&apos;", XmlFormat.escape("a&b<c>\"d\"'e'"));
    }

    @Test
    public void escapeDoesNotDoubleEscapeAmpersands() {
        assertEquals("&amp;lt;", XmlFormat.escape("&lt;"));
    }

    @Test
    public void escapeOfNullIsEmpty() {
        assertEquals("", XmlFormat.escape(null));
        assertEquals("", XmlFormat.escape(""));
        assertEquals("plain", XmlFormat.escape("plain"));
    }

    @Test
    public void complexToFloatRadixes() {
        assertEquals(16f, XmlFormat.complexToFloat(16 << 8));
        assertEquals(1f, XmlFormat.complexToFloat((128 << 8) | (1 << 4)));
        assertEquals(1f, XmlFormat.complexToFloat((32768 << 8) | (2 << 4)));
        assertEquals(0.5f, XmlFormat.complexToFloat(((1 << 22) << 8) | (3 << 4)));
    }

    @Test
    public void complexToFloatKeepsTheSignOfTheMantissa() {
        assertEquals(-4f, XmlFormat.complexToFloat((-4 & 0xFFFFFF) << 8));
    }

    @Test
    public void unknownDimensionUnitFallsBackToPx() {
        assertEquals("7px", XmlFormat.dimension((7 << 8) | 0xF));
    }

    @Test
    public void fractionUnitIsTheLowNibbleNotTheRadix() {
        // Radix 3 with unit 0 is a fraction of itself, a radix must not be mistaken for the "of parent" unit
        assertEquals("50.00%", XmlFormat.fraction(((1 << 22) << 8) | (3 << 4)));
        assertEquals("50.00%p", XmlFormat.fraction(((1 << 22) << 8) | (3 << 4) | 1));
    }
}
