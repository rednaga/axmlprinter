package diff.rednaga;

import android.content.res.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End to end tests of the command line entry point.
 */
public class AXMLPrinterTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void captureStreams() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private Path fixture(String name) throws Exception {
        Path target = tempDir.resolve(name);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            Files.copy(in, target);
        }
        return target;
    }

    @Test
    public void noArgumentsPrintsUsageAndFails() throws Exception {
        assertEquals(2, AXMLPrinter.run(new String[0]));

        assertTrue(stderr().startsWith("Usage: AXMLPrinter"), stderr());
        assertTrue(stderr().contains("[output file]"), "usage should mention the optional output file: " + stderr());
        assertEquals("", stdout());
    }

    @Test
    public void versionFlags() throws Exception {
        for (String flag : new String[]{"-v", "-version", "-V"}) {
            out.reset();
            assertEquals(0, AXMLPrinter.run(new String[]{flag}));

            assertTrue(stdout().startsWith("axmlprinter "), "flag " + flag + " printed: " + stdout());
        }
    }

    @Test
    public void missingFileIsReported() throws Exception {
        assertEquals(1, AXMLPrinter.run(new String[]{tempDir.resolve("does-not-exist.xml").toString()}));

        assertTrue(stderr().contains("File not found"));
        assertEquals("", stdout());
    }

    @Test
    public void tinyFileIsReported() throws Exception {
        Path tiny = tempDir.resolve("tiny.bin");
        Files.write(tiny, new byte[]{1, 2});

        assertEquals(1, AXMLPrinter.run(new String[]{tiny.toString()}));

        assertTrue(stderr().contains("too small"));
    }

    @Test
    public void unknownMagicIsReported() throws Exception {
        Path unknown = tempDir.resolve("unknown.bin");
        Files.write(unknown, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0, 0, 0, 0});

        assertEquals(1, AXMLPrinter.run(new String[]{unknown.toString()}));

        assertTrue(stderr().contains("Unknown file format"));
    }

    @Test
    public void printsXmlForBinaryAxml() throws Exception {
        assertEquals(0, AXMLPrinter.run(new String[]{fixture("large_from_malware.xml").toString()}));

        String printed = stdout();
        assertTrue(printed.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>"));
        assertTrue(printed.contains("com.faithcomesbyhearing.android.pt.bibleis"));
        assertEquals("", stderr());
    }

    @Test
    public void writesRebuiltAxmlWhenOutputGiven() throws Exception {
        Path output = tempDir.resolve("out.axml");

        assertEquals(0, AXMLPrinter.run(new String[]{fixture("test.xml").toString(), output.toString()}));

        assertTrue(Files.size(output) > 0);
        // The rebuilt file must itself be printable
        out.reset();
        assertEquals(0, AXMLPrinter.run(new String[]{output.toString()}));
        assertTrue(stdout().contains("LinearLayout"));
    }

    @Test
    public void printsXmlForProtobufAndWarnsOnOutputFile() throws Exception {
        com.android.aapt.Resources.XmlNode node = com.android.aapt.Resources.XmlNode.newBuilder()
                .setElement(com.android.aapt.Resources.XmlElement.newBuilder().setName("manifest"))
                .build();
        Path proto = tempDir.resolve("proto.xml");
        Files.write(proto, node.toByteArray());

        assertEquals(0, AXMLPrinter.run(new String[]{proto.toString(), tempDir.resolve("unused.axml").toString()}));

        assertTrue(stdout().contains("<manifest"));
        assertTrue(stderr().contains("not supported"));
        assertFalse(Files.exists(tempDir.resolve("unused.axml")));
    }

    @Test
    public void truncatedAxmlFailsCleanlyWithoutPrintingPartialXml() throws Exception {
        byte[] full = Files.readAllBytes(fixture("test.xml"));
        Path truncated = tempDir.resolve("truncated.axml");
        Files.write(truncated, java.util.Arrays.copyOf(full, 100));

        assertEquals(1, AXMLPrinter.run(new String[]{truncated.toString()}));

        assertTrue(stderr().startsWith("Error"), stderr());
        assertFalse(stderr().contains("\tat "), "no stack trace expected: " + stderr());
        assertEquals("", stdout());
    }

    @Test
    public void incompleteAxmlIsAnError() throws Exception {
        // Just the AXML header chunk, no string or resource sections
        Path headerOnly = tempDir.resolve("header-only.axml");
        Files.write(headerOnly, TestUtil.ints(0x00080003, 8));

        assertEquals(1, AXMLPrinter.run(new String[]{headerOnly.toString()}));

        assertTrue(stderr().toLowerCase().contains("incomplete"), stderr());
        assertEquals("", stdout());
    }

    @Test
    public void invalidProtobufIsAnError() throws Exception {
        Path invalid = tempDir.resolve("invalid.pb");
        Files.write(invalid, new byte[]{0x0A, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});

        assertEquals(1, AXMLPrinter.run(new String[]{invalid.toString()}));

        assertTrue(stderr().startsWith("Error"), stderr());
    }

    @Test
    public void directoryInsteadOfFileIsAnError() throws Exception {
        assertEquals(1, AXMLPrinter.run(new String[]{tempDir.toString()}));

        assertTrue(stderr().startsWith("Error"), stderr());
    }

    @Test
    public void unwritableOutputFileIsAnError() throws Exception {
        Path missingDirectory = tempDir.resolve("no-such-directory").resolve("out.axml");

        assertEquals(1, AXMLPrinter.run(new String[]{fixture("test.xml").toString(), missingDirectory.toString()}));

        assertTrue(stderr().startsWith("Error"), stderr());
    }
}
