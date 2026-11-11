package android.content.res;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Malformed input and edge cases for AXMLResource.
 */
public class TestAXMLResourceErrors {

    private static byte[] fixtureBytes(String name) throws IOException {
        try (InputStream stream = TestAXMLResource.open(name)) {
            return stream.readAllBytes();
        }
    }

    @Test
    public void emptyStreamIsNotReadable() throws IOException {
        assertFalse(new AXMLResource().read(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    public void constructorRejectsEmptyStream() {
        assertThrows(IOException.class, () -> new AXMLResource(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    public void unknownChunkTypeIsAnError() {
        byte[] garbage = TestUtil.ints(0x7777, 0, 0, 0, 0);

        assertThrows(IOException.class, () -> new AXMLResource().read(new ByteArrayInputStream(garbage)));
    }

    @Test
    public void truncatedFileIsRejected() throws IOException {
        byte[] full = fixtureBytes("test.xml");
        byte[] truncated = Arrays.copyOf(full, 100);

        // Either read() reports failure (false) or an IOException is thrown, but never a "successful" resource
        assertThrows(IOException.class, () -> new AXMLResource(new ByteArrayInputStream(truncated)));
    }

    @Test
    public void headerOnlyIsNotAFullResource() throws IOException {
        byte[] full = fixtureBytes("test.xml");
        // First chunk (the 8 byte header) only
        byte[] headerOnly = Arrays.copyOf(full, 8);

        boolean read;
        try {
            read = new AXMLResource().read(new ByteArrayInputStream(headerOnly));
        } catch (IOException expected) {
            return;
        }
        assertFalse(read);
    }

    @Test
    public void applicationTagIsNullWithoutApplication() throws IOException {
        // test.xml is a layout, which has no <application>
        AXMLResource layout = TestAXMLResource.load("test.xml");

        assertNull(layout.getApplicationTag());
    }

    @Test
    public void applicationTagFoundInManifest() throws IOException {
        AXMLResource manifest = TestAXMLResource.load("large_from_malware.xml");

        assertNotNull(manifest.getApplicationTag());
    }
}
