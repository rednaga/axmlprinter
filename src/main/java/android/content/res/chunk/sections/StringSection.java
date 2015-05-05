package android.content.res.chunk.sections;

import java.io.IOException;
import java.util.ArrayList;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import android.content.res.chunk.PoolItem;
import android.content.res.chunk.types.Chunk;

public class StringSection extends GenericChunkSection implements Chunk, ChunkSection {

    // This specific tag appears unused but might need to be implemented? or used as an unknown?
    @SuppressWarnings("unused")
    private final int SORTED_FLAG = 1 << 0;
    private final int UTF8_FLAG = 1 << 8;

    private int stringChunkCount;
    private int styleChunkCount;
    private int stringChunkFlags;
    private int stringChunkPoolOffset;
    private int styleChunkPoolOffset;

    // FIXME:
    // This likely could just be an ordered array of Strings if the Integer is just ordered and the key..
    private ArrayList<PoolItem> stringChunkPool;
    private ArrayList<PoolItem> styleChunkPool;

    public StringSection(ChunkType chunkType, IntReader inputReader) {
        super(chunkType, inputReader);
    }

    @Override
    public void readHeader(IntReader inputReader) throws IOException {
        stringChunkCount = inputReader.readInt();
        styleChunkCount = inputReader.readInt();
        stringChunkFlags = inputReader.readInt();

        stringChunkPoolOffset = inputReader.readInt();
        // TODO : Check that these loose conditions are ok to abide by
        if ((stringChunkCount != 0) || (stringChunkPoolOffset != 0)) {
            //
        }
        stringChunkPool = new ArrayList<PoolItem>();

        styleChunkPoolOffset = inputReader.readInt();
        // TODO : Check that these loose conditions are ok to abide by
        if ((styleChunkCount != 0) || (styleChunkPoolOffset != 0)) {
            //
        }
        styleChunkPool = new ArrayList<PoolItem>();
    }

    @Override
    public void readSection(IntReader inputReader) throws IOException {
        for (int i = 0; i < stringChunkCount; i++) {
            stringChunkPool.add(new PoolItem(inputReader.readInt(), null));
        }
        if (!stringChunkPool.isEmpty()) {
            readPool(stringChunkPool, stringChunkCount, stringChunkFlags, inputReader);
        }
        // TODO : Does this need the flags?
        // FIXME: This is potentially wrong
        for (int i = 0; i < styleChunkCount; i++) {
            styleChunkPool.add(new PoolItem(inputReader.readInt(), null));
        }
        if (!styleChunkPool.isEmpty()) {
            readPool(styleChunkPool, styleChunkCount, stringChunkFlags, inputReader);
        }
    }

    // TODO : Ensure we goto the proper offset in the case it isn't in proper order
    private void readPool(ArrayList<PoolItem> pool, int poolSize, int flags, IntReader inputReader) throws IOException {
        int offset = 0;
        for (PoolItem item : pool) {
            // XXX: This assumes that the pool is ordered...
            inputReader.skip(item.getOffset() - offset);
            offset = item.getOffset();

            int length = 0;
            if ((flags & UTF8_FLAG) != 0) {
                length = inputReader.readByte();
                offset += 1;
            } else {
                length = inputReader.readShort();
                offset += 2;
            }
            StringBuilder result = new StringBuilder(length);
            for (; length != 0; length -= 1) {
                if ((flags & UTF8_FLAG) != 0) {
                    result.append((char) inputReader.readByte());
                    offset += 1;
                } else {
                    result.append((char) inputReader.readShort());
                    offset += 2;
                }
            }
            item.setString(result.toString());
        }
    }

    public String getString(int index) {
        // index--;
        if ((index > -1) && (index < stringChunkCount)) {
            return stringChunkPool.get(index).getString();
        }

        return "";
    }

    // FIXME: This is potentially wrong
    public String getStyle(int index) {
        return styleChunkPool.get(index).getString();
    }

    @Override
    public String toXML(StringSection stringSection, ResourceSection resourceSection, int indent) {
        return null;
    }
}
