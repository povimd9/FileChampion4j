package dev.filechampion.filechampion4j;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * A java.io.Reader extension that securely overwrites the buffer when closed.
 */
public class SecureFileReader extends Reader {
    private final Reader reader;
    private final char[] buffer;

    /**
     * SecureFileReader constructor.
     * @param reader (Reader) the reader to wrap.
     * @param bufferSize (int) the size of the buffer.
     */
    public SecureFileReader(Reader reader, int bufferSize) {
        this.reader = reader;
        this.buffer = new char[bufferSize];
    }

    /**
     * Read buffer of defined size and offset.
     * @param cbuf (char[]) the buffer to read into.
     * @param off (int) the offset to start reading from.
     * @param len (int) the length of the buffer to read.
     * @return (int) the number of characters read.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    /**
     * Overwrite the buffer with null characters and close the reader.
     * @throws IOException if an I/O error occurs while closing the reader.
     */
    @Override
    public void close() throws IOException {
        try {
            Arrays.fill(buffer, '\u0000');
        } finally {
            reader.close();
        }
    }
}
