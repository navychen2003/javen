package org.javenstudio.raptor.dfs.server.common;

import java.io.IOException;

import org.javenstudio.raptor.dfs.protocol.FSConstants;

/**
 * The exception is thrown when external version does not match 
 * current version of the appication.
 * 
 */
public class IncorrectVersionException extends IOException {
  private static final long serialVersionUID = 1L;

  public IncorrectVersionException(int versionReported, String ofWhat) {
    this(versionReported, ofWhat, FSConstants.LAYOUT_VERSION);
  }
  
  public IncorrectVersionException(int versionReported,
                                   String ofWhat,
                                   int versionExpected) {
    super("Unexpected version " 
          + (ofWhat==null ? "" : "of " + ofWhat) + ". Reported: "
          + versionReported + ". Expecting = " + versionExpected + ".");
  }

}

