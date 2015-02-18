package org.javenstudio.raptor.bigdb.util;

import java.io.IOException;

/** Thrown when the file system needs to be upgraded */
public class FileSystemVersionException extends IOException {
  private static final long serialVersionUID = 1004053363L;

  /** default constructor */
  public FileSystemVersionException() {
    super();
  }

  /** @param s message */
  public FileSystemVersionException(String s) {
    super(s);
  }

}
