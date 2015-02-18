package org.javenstudio.raptor.io;

import java.io.IOException;
import java.util.List;

/** Encapsulate a list of {@link IOException} into an {@link IOException} */
public class MultipleIOException extends IOException {
  /** Require by {@link java.io.Serializable} */
  private static final long serialVersionUID = 1L;
  
  private final List<IOException> exceptions;
  
  /** Constructor is private, use {@link #createIOException(List)}. */
  private MultipleIOException(List<IOException> exceptions) {
    super(exceptions.size() + " exceptions " + exceptions);
    this.exceptions = exceptions;
  }

  /** @return the underlying exceptions */
  public List<IOException> getExceptions() {return exceptions;}

  /** A convenient method to create an {@link IOException}. */
  public static IOException createIOException(List<IOException> exceptions) {
    if (exceptions == null || exceptions.isEmpty()) {
      return null;
    }
    if (exceptions.size() == 1) {
      return exceptions.get(0);
    }
    return new MultipleIOException(exceptions);
  }
}

