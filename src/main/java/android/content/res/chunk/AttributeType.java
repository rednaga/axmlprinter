package android.content.res.chunk;

public enum AttributeType {

    STRING {
        @Override
        public int getIntType() {
            return 0x03000008;
        }
    },
    INT {
        @Override
        public int getIntType() {
            return 0x10000008;
        }
    },
    RESOURCE {
        @Override
        public int getIntType() {
            return 0x01000008;
        }
    },
    BOOLEAN {
        @Override
        public int getIntType() {
            return 0x12000008;
        }
    };

    public abstract int getIntType();

}
