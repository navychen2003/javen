package org.javenstudio.raptor.io;

import java.io.IOException;

/**
 * Used when target file already exists for any operation and 
 * is not configured to be overwritten.  
 *
 */
public class FileAlreadyExistsException extends IOException {
  private static final long serialVersionUID = 1L;

  public FileAlreadyExistsException() {
    super();
  }

  public FileAlreadyExistsException(String msg) {
    super(msg);
  }
}
