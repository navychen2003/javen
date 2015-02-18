package org.javenstudio.raptor.fs;


/** Thrown for unexpected filesystem errors, presumed to reflect disk errors
 * in the native filesystem. */
public class FSError extends Error {
  private static final long serialVersionUID = 1L;

  FSError(Throwable cause) {
    super(cause);
  }
}

