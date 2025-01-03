/*
 * Copyright 2015-2025 Red Naga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.content.res.chunk.types;

import android.content.res.IntReader;
import android.content.res.chunk.ChunkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author tstrazzere
 */
public class StartTagTest {

    private StartTag underTest;

    private IntReader mockReader;
    private ChunkType mockChunkType;

    @BeforeEach
    public void setUp() throws Exception {
        mockReader = mock(IntReader.class);
        // Mock the namespace data
        when(mockReader.readInt()).thenReturn(9 * 4, 0x17, 0xFFFFFFFF, 0xFFFFFFFF, 0x41, 0x140014, 0, 0);

        mockChunkType = ChunkType.START_TAG;

        underTest = new StartTag(mockChunkType, mockReader);
    }

    @Test
    public void testToBytes() throws Exception {
        byte[] expected = {
                // START_TAG
                (byte) 0x02, (byte) 0x01, (byte) 0x10, (byte) 0x00,
                // size
                (byte) (9 * 4), (byte) 0x00, (byte) 0x00, (byte) 0x00,
                // line number
                (byte) 0x17, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                // unknown
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                // namespace uri
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                // name
                (byte) 0x41, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                // flags
                (byte) 0x14, (byte) 0x00, (byte) 0x14, (byte) 0x00,
                // attribute count
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                // attribute class
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        };

        byte[] actual = underTest.toBytes();
        assertArrayEquals(expected, actual);
    }
}