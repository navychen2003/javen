package org.javenstudio.raptor.io.compress;

import java.io.IOException;

/**
 * Specification of a stream-based 'de-compressor' which can be  
 * plugged into a {@link CompressionInputStream} to compress data.
 * This is modelled after {@link java.util.zip.Inflater}
 * 
 */
public interface Decompressor {
  /**
   * Sets input data for decompression. 
   * This should be called whenever #needsInput() returns 
   * <code>true</code> indicating that more input data is required.
   * 
   * @param b Input data
   * @param off Start offset
   * @param len Length
   */
  public void setInput(byte[] b, int off, int len);
  
  /**
   * Returns true if the input data buffer is empty and 
   * #setInput() should be called to provide more input. 
   * 
   * @return <code>true</code> if the input data buffer is empty and 
   * #setInput() should be called in order to provide more input.
   */
  public boolean needsInput();
  
  /**
   * Sets preset dictionary for compression. A preset dictionary
   * is used when the history buffer can be predetermined. 
   *
   * @param b Dictionary data bytes
   * @param off Start offset
   * @param len Length
   */
  public void setDictionary(byte[] b, int off, int len);
  
  /**
   * Returns <code>true</code> if a preset dictionary is needed for decompression.
   * @return <code>true</code> if a preset dictionary is needed for decompression
   */
  public boolean needsDictionary();

  /**
   * Returns true if the end of the compressed 
   * data output stream has been reached.
   * @return <code>true</code> if the end of the compressed
   * data output stream has been reached.
   */
  public boolean finished();
  
  /**
   * Fills specified buffer with uncompressed data. Returns actual number
   * of bytes of uncompressed data. A return value of 0 indicates that
   * #needsInput() should be called in order to determine if more input
   * data is required.
   * 
   * @param b Buffer for the compressed data
   * @param off Start offset of the data
   * @param len Size of the buffer
   * @return The actual number of bytes of compressed data.
   * @throws IOException
   */
  public int decompress(byte[] b, int off, int len) throws IOException;
  
  /**
   * Resets decompressor so that a new set of input data can be processed.
   */
  public void reset();
  
  /**
   * Closes the decompressor and discards any unprocessed input.
   */
  public void end(); 
}

