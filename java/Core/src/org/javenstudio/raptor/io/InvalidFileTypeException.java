package org.javenstudio.raptor.io;

import java.io.IOException;


/**
 * Used when file type differs from the desired file type. like 
 * getting a file when a directory is expected. Or a wrong file type. 
 * @author sanjaydahiya
 *
 */
public class InvalidFileTypeException extends IOException {
  private static final long serialVersionUID = 1L;

  public InvalidFileTypeException() {
    super();
  }

  public InvalidFileTypeException(String msg) {
    super(msg);
  }

}
