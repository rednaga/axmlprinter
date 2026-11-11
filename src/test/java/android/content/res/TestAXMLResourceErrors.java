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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void injectingWithoutApplicationTagGivesAClearError() throws IOException {
        AXMLResource layout = TestAXMLResource.load("test.xml");
        android.content.res.chunk.types.Attribute attribute = new android.content.res.chunk.types.Attribute(
                "android", "name", "test", android.content.res.chunk.AttributeType.STRING, null, layout.getStringSection());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> layout.injectApplicationAttribute(attribute));

        assertTrue(exception.getMessage().contains("application"), exception.getMessage());
    }

    /** available() is only an estimate and is allowed to be 0, e.g. for sockets or compressed streams. */
    private static class NoAvailableStream extends java.io.FilterInputStream {
        NoAvailableStream(byte[] data) {
            super(new ByteArrayInputStream(data));
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            // Hand out a few bytes at a time like a network stream would
            return super.read(b, off, Math.min(len, 3));
        }
    }

    @Test
    public void readDoesNotDependOnAvailable() throws IOException {
        for (String fixture : new String[]{"test.xml", "large_from_malware.xml", "qihoo_jiagu_issue8.xml"}) {
            byte[] data = fixtureBytes(fixture);

            AXMLResource viaStream = new AXMLResource(new NoAvailableStream(data));

            org.junit.jupiter.api.Assertions.assertEquals(
                    new AXMLResource(new ByteArrayInputStream(data)).toXML(), viaStream.toXML(), fixture);
        }
    }

    @Test
    public void trailingBytesAfterTheLastChunkAreIgnored() throws IOException {
        byte[] data = fixtureBytes("test.xml");
        byte[] padded = Arrays.copyOf(data, data.length + 3);

        org.junit.jupiter.api.Assertions.assertEquals(
                new AXMLResource(new ByteArrayInputStream(data)).toXML(),
                new AXMLResource(new ByteArrayInputStream(padded)).toXML());
    }

    @Test
    public void zeroPaddingBetweenChunksIsSkipped() throws IOException {
        byte[] data = fixtureBytes("test.xml");
        // Insert a zero int (a BUFFER chunk) straight after the 8 byte AXML header chunk
        byte[] padded = new byte[data.length + 4];
        System.arraycopy(data, 0, padded, 0, 8);
        System.arraycopy(data, 8, padded, 12, data.length - 8);

        org.junit.jupiter.api.Assertions.assertEquals(
                new AXMLResource(new ByteArrayInputStream(data)).toXML(),
                new AXMLResource(new ByteArrayInputStream(padded)).toXML());
    }
}
