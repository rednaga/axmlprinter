package android.content.res;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProtobufXMLResource
 * 
 * @author tstrazzere
 */
public class TestProtobufXMLResource {

    @Nested
    class FormatDetectionTest {

        @Test
        public void testIsProtobufFormat_ValidElement() {
            // Protobuf field 1 (element), wire type 2 = 0x0A
            byte[] header = {0x0A};
            assertTrue(ProtobufXMLResource.isProtobufFormat(header));
        }

        @Test
        public void testIsProtobufFormat_ValidText() {
            // Protobuf field 2 (text), wire type 2 = 0x12
            byte[] header = {0x12};
            assertTrue(ProtobufXMLResource.isProtobufFormat(header));
        }

        @Test
        public void testIsProtobufFormat_InvalidFormat() {
            // AXML magic number
            byte[] header = {0x03, 0x00, 0x08, 0x00};
            assertFalse(ProtobufXMLResource.isProtobufFormat(header));
        }

        @Test
        public void testIsProtobufFormat_EmptyHeader() {
            byte[] header = {};
            assertFalse(ProtobufXMLResource.isProtobufFormat(header));
        }

        @Test
        public void testIsProtobufFormat_OtherByte() {
            byte[] header = {0x42};
            assertFalse(ProtobufXMLResource.isProtobufFormat(header));
        }
    }

    @Nested
    class ErrorHandlingTest {

        @Test
        public void testRead_NullInputStream() {
            ProtobufXMLResource resource = new ProtobufXMLResource();
            assertThrows(IllegalArgumentException.class, () -> {
                resource.read(null);
            });
        }

        @Test
        public void testRead_EmptyInputStream() throws IOException {
            ProtobufXMLResource resource = new ProtobufXMLResource();
            // An empty stream is a valid, empty protobuf message: it parses, but has no element
            assertFalse(resource.read(new ByteArrayInputStream(new byte[0])));
        }

        @Test
        public void testConstructor_NullInputStream() {
            assertThrows(IllegalArgumentException.class, () -> {
                new ProtobufXMLResource(null);
            });
        }

        @Test
        public void testToXML_WithoutRead() {
            ProtobufXMLResource resource = new ProtobufXMLResource();
            assertThrows(IllegalStateException.class, () -> {
                resource.toXML();
            });
        }

        @Test
        public void testRead_InvalidProtobufData() {
            ProtobufXMLResource resource = new ProtobufXMLResource();
            // Invalid protobuf data
            byte[] invalidData = {0x0A, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
            InputStream invalidStream = new ByteArrayInputStream(invalidData);
            assertThrows(IOException.class, () -> {
                resource.read(invalidStream);
            });
        }
    }

    @Nested
    class IntegrationTest {

        @Test
        public void testPrint_WithoutRead() {
            ProtobufXMLResource resource = new ProtobufXMLResource();
            // print() calls toXML() which should throw if rootNode is null
            assertThrows(IllegalStateException.class, () -> {
                resource.print();
            });
        }
    }

    @Nested
    class EdgeCaseTest {

        @Test
        public void testVerySmallInput() {
            ProtobufXMLResource resource = new ProtobufXMLResource();
            byte[] tinyData = {0x0A};
            InputStream tinyStream = new ByteArrayInputStream(tinyData);
            
            // Should handle gracefully (either parse or throw appropriate exception)
            assertThrows(IOException.class, () -> {
                resource.read(tinyStream);
            });
        }
    }
}
