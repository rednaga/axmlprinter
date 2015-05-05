package android.content.res.chunk;

public class PoolItem {
    private int itemOffset;
    private String itemData;

    public PoolItem(int offset, String data) {
        itemOffset = offset;
        itemData = data;
    }

    public int getOffset() {
        return itemOffset;
    }

    public void setString(String data) {
        itemData = data;
    }

    public String getString() {
        return itemData;
    }
}
