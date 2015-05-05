package android.content.res.chunk;

public enum ChunkType {
    BUFFER {
        // This is a faked type
        @Override
        public int getIntType() {
            return 0;
        }
    },
    ATTRIBUTE {
        // This is a faked type
        // XXX : Unneeded?
        @Override
        public int getIntType() {
            return 0;
        }
    },
    AXML_HEADER {
        @Override
        public int getIntType() {
            return 0x00080003;
        }
    },
    STRING_SECTION {
        @Override
        public int getIntType() {
            return 0x001C0001;
        }
    },
    RESOURCE_SECTION {
        @Override
        public int getIntType() {
            return 0x00080180;
        }
    },
    START_NAMESPACE {
        @Override
        public int getIntType() {
            return 0x00100100;
        }
    },
    END_NAMESPACE {
        @Override
        public int getIntType() {
            return 0x00100101;
        }
    },
    START_TAG {
        @Override
        public int getIntType() {
            return 0x00100102;
        }
    },
    END_TAG {
        @Override
        public int getIntType() {
            return 0x00100103;
        }
    },
    TEXT_TAG {
        @Override
        public int getIntType() {
            return 0x00100104;
        }
    };

    public abstract int getIntType();
}
