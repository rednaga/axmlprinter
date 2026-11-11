package android.content.res;

import android.content.res.chunk.ChunkType;
import android.content.res.chunk.ChunkUtil;
import android.content.res.chunk.sections.StringSection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Helpers for building real (non-mocked) binary AXML fragments in tests.
 */
public final class TestUtil {

    private TestUtil() {
    }

    /** Little endian bytes for the given ints. */
    public static byte[] ints(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int value : values) {
            buffer.putInt(value);
        }
        return buffer.array();
    }

    /** A real IntReader over little endian ints. */
    public static IntReader reader(int... values) {
        return reader(ints(values));
    }

    public static IntReader reader(byte[] bytes) {
        return new IntReader(new ByteArrayInputStream(bytes), false);
    }

    /**
     * Bytes of a UTF-16 string pool chunk (including the chunk type), with the strings laid out in the given
     * order and offsets assigned to the strings in {@code offsetOrder} (a permutation of the string indices).
     */
    public static byte[] utf16StringPool(String[] strings, int[] offsetOrder) {
        int count = strings.length;
        // Data for each string: u16 length, chars, u16 terminator
        int[] dataSize = new int[count];
        int dataTotal = 0;
        for (int i = 0; i < count; i++) {
            dataSize[i] = 2 + strings[i].length() * 2 + 2;
            dataTotal += dataSize[i];
        }
        int padding = (4 - (dataTotal % 4)) % 4;

        int headerSize = 7 * 4;
        int stringsStart = headerSize + count * 4;
        int size = stringsStart + dataTotal + padding;

        // Position of each string's data within the data area, in layout order
        int[] position = new int[count];
        int running = 0;
        for (int index : offsetOrder) {
            position[index] = running;
            running += dataSize[index];
        }

        ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(ChunkType.STRING_SECTION.getIntType());
        buffer.putInt(size);
        buffer.putInt(count);
        buffer.putInt(0); // style count
        buffer.putInt(0); // flags (UTF-16)
        buffer.putInt(stringsStart);
        buffer.putInt(0); // styles start
        for (int i = 0; i < count; i++) {
            buffer.putInt(position[i]);
        }
        for (int index : offsetOrder) {
            buffer.putShort((short) strings[index].length());
            for (char c : strings[index].toCharArray()) {
                buffer.putShort((short) c);
            }
            buffer.putShort((short) 0);
        }
        return buffer.array();
    }

    public static byte[] utf16StringPool(String... strings) {
        int[] order = new int[strings.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        return utf16StringPool(strings, order);
    }

    /** Parses a UTF-16 pool built by {@link #utf16StringPool(String...)} with the real parser. */
    public static StringSection stringSection(String... strings) throws IOException {
        return (StringSection) ChunkUtil.createChunk(reader(utf16StringPool(strings)));
    }

    public static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
