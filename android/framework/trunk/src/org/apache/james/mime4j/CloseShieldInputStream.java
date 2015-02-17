package org.apache.james.mime4j;

import java.io.InputStream;
import java.io.IOException;

/**
 * InputStream that shields its underlying input stream from
 * being closed.
 * 
 */
public class CloseShieldInputStream extends InputStream {

    /**
     * Underlying InputStream
     */
    private InputStream is;

    public CloseShieldInputStream(InputStream is) {
        this.is = is;
    }

    public InputStream getUnderlyingStream() {
        return is;
    }

    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        checkIfClosed();
        return is.read();
    }

    /**
     * @see java.io.InputStream#available()
     */
    public int available() throws IOException {
        checkIfClosed();
        return is.available();
    }

    
    /**
     * Set the underlying InputStream to null
     */
    public void close() throws IOException {
        is = null;
    }

    /**
     * @see java.io.FilterInputStream#reset()
     */
    public synchronized void reset() throws IOException {
        checkIfClosed();
        is.reset();
    }

    /**
     * @see java.io.FilterInputStream#markSupported()
     */
    public boolean markSupported() {
        if (is == null)
            return false;
        return is.markSupported();
    }

    /**
     * @see java.io.FilterInputStream#mark(int)
     */
    public synchronized void mark(int readlimit) {
        if (is != null)
            is.mark(readlimit);
    }

    /**
     * @see java.io.FilterInputStream#skip(long)
     */
    public long skip(long n) throws IOException {
        checkIfClosed();
        return is.skip(n);
    }

    /**
     * @see java.io.FilterInputStream#read(byte[])
     */
    public int read(byte b[]) throws IOException {
        checkIfClosed();
        return is.read(b);
    }

    /**
     * @see java.io.FilterInputStream#read(byte[], int, int)
     */
    public int read(byte b[], int off, int len) throws IOException {
        checkIfClosed();
        return is.read(b, off, len);
    }

    /**
     * Check if the underlying InputStream is null. If so throw an Exception
     * 
     * @throws IOException if the underlying InputStream is null
     */
    private void checkIfClosed() throws IOException {
        if (is == null)
            throw new IOException("Stream is closed");
    }
}