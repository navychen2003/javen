package org.javenstudio.falcon.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 *
 * @since 1.2
 */
public interface ContentStream {
  String getName();
  String getSourceInfo();
  String getContentType();
  
  /**
   * @return the stream size or <code>null</code> if not known
   */
  Long getSize(); // size if we know it, otherwise null
  
  File getFile(); // local file if has, otherwise null
  
  /**
   * Get an open stream.  You are responsible for closing it.  Consider using 
   * something like:
   * <pre>
   *   InputStream stream = stream.getStream();
   *   try {
   *     // use the stream...
   *   }
   *   finally {
   *     IOUtils.closeQuietly(stream);
   *   }
   *  </pre>
   *  
   * Only the first call to <code>getStream()</code> or <code>getReader()</code>
   * is guaranteed to work.  The runtime behavior for additional calls is undefined.
   *
   * Note: you must call <code>getStream()</code> or <code>getReader()</code> before
   * the attributes (name, contentType, etc) are guaranteed to be set.  Streams may be
   * lazy loaded only when this method is called.
   */
  InputStream getStream() throws IOException;

  /**
   * Get an open stream.  You are responsible for closing it.  Consider using 
   * something like:
   * <pre>
   *   Reader reader = stream.getReader();
   *   try {
   *     // use the reader...
   *   }
   *   finally {
   *     IOUtils.closeQuietly(reader);
   *   }
   *  </pre>
   *  
   * Only the first call to <code>getStream()</code> or <code>getReader()</code>
   * is guaranteed to work.  The runtime behavior for additional calls is undefined.
   *
   * Note: you must call <code>getStream()</code> or <code>getReader()</code> before
   * the attributes (name, contentType, etc) are guaranteed to be set.  Streams may be
   * lazy loaded only when this method is called.
   */
  Reader getReader() throws IOException;
  
  void close();
}
