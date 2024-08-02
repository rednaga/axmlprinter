/*
 * Copyright 2008 Android4ME
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.content.res;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple helper class that allows reading of integers.
 * <p>
 * TODO: implement buffering
 *
 * @author Dmitry Skiba
 */
public class IntReader {

    private static final int BYTE_LENGTH = 1;
    private static final int SHORT_LENGTH = 2;
    private static final int INT_LENGTH = 4;

    private InputStream stream;
    private boolean bigEndian;
    private int bytesRead;

	private static final Logger LOGGER = Logger.getLogger(IntReader.class.getName());

    public IntReader(InputStream stream, boolean bigEndian) {
        reset(stream, bigEndian);
    }

    /**
     * Reset the POJO to use a new stream.
     *
     * @param newStream   the {@code InputStream} to use
     * @param isBigEndian a boolean for whether or not the stream is in Big Endian format
     */
    public void reset(InputStream newStream, boolean isBigEndian) {
        stream = newStream;
        bigEndian = isBigEndian;
        bytesRead = 0;
    }

    /**
     * Close the current stream being used by the POJO.
     */
    public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error closing stream", e);
            }
            reset(null, false);
        }
    }

    public int readByte() throws IOException {
        return readInt(BYTE_LENGTH);
    }
 
    public int readShort() throws IOException {
        return readInt(SHORT_LENGTH);
    }
 
    public int readInt() throws IOException {
        return readInt(INT_LENGTH);
    }

    /**
     * Read an integer of a certain length from the current stream.
     *
     * @param length to read
     * @return
     * @throws IOException
     */
    public int readInt(int length) throws IOException {
        if ((length < 0) || (length > INT_LENGTH)) {
            throw new IllegalArgumentException();
        }
        int result = 0;
        int byteRead = 0;
        if (bigEndian) {
            for (int i = (length - 1) * 8; i >= 0; i -= 8) {
                byteRead = stream.read();
                bytesRead++;
                if (byteRead == -1) {
                    throw new EOFException();
                }
                result |= (byteRead << i);
            }
        } else {
            length *= 8;
            for (int i = 0; i != length; i += 8) {
                byteRead = stream.read();
                bytesRead++;
                if (byteRead == -1) {
                    throw new EOFException();
                }
                result |= (byteRead << i);
            }
        }

        return result;
    }

    /**
     * Skip a specific number of bytes in the stream.
     *
     * @param bytes
     * @throws IOException
     */
    public void skip(int bytes) throws IOException {
        if (bytes > 0) {
            if (stream.skip(bytes) != bytes) {
                throw new EOFException();
            }
            bytesRead += bytes;
        }
    }

    public void skipInt() throws IOException {
        skip(INT_LENGTH);
    }

    public int getBytesRead() {
        return bytesRead;
    }
}
