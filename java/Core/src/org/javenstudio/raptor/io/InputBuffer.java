package org.javenstudio.raptor.io;

import java.io.*;


/** A reusable {@link InputStream} implementation that reads from an in-memory
 * buffer.
 *
 * <p>This saves memory over creating a new InputStream and
 * ByteArrayInputStream each time data is read.
 *
 * <p>Typical usage is something like the following:<pre>
 *
 * InputBuffer buffer = new InputBuffer();
 * while (... loop condition ...) {
 *   byte[] data = ... get data ...;
 *   int dataLength = ... get data length ...;
 *   buffer.reset(data, dataLength);
 *   ... read buffer using InputStream methods ...
 * }
 * </pre>
 * @see DataInputBuffer
 * @see DataOutput
 */
public class InputBuffer extends FilterInputStream {

  private static class Buffer extends ByteArrayInputStream {
    public Buffer() {
      super(new byte[] {});
    }

    public void reset(byte[] input, int start, int length) {
      this.buf = input;
      this.count = start+length;
      this.mark = start;
      this.pos = start;
    }

    public int getPosition() { return pos; }
    public int getLength() { return count; }
  }

  private Buffer buffer;
  
  /** Constructs a new empty buffer. */
  public InputBuffer() {
    this(new Buffer());
  }

  private InputBuffer(Buffer buffer) {
    super(buffer);
    this.buffer = buffer;
  }

  /** Resets the data that the buffer reads. */
  public void reset(byte[] input, int length) {
    buffer.reset(input, 0, length);
  }

  /** Resets the data that the buffer reads. */
  public void reset(byte[] input, int start, int length) {
    buffer.reset(input, start, length);
  }

  /** Returns the current position in the input. */
  public int getPosition() { return buffer.getPosition(); }

  /** Returns the length of the input. */
  public int getLength() { return buffer.getLength(); }

}
