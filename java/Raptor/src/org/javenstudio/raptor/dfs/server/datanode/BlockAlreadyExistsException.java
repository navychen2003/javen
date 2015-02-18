package org.javenstudio.raptor.dfs.server.datanode;

import java.io.IOException;

/**
 * Exception indicating that the target block already exists 
 * and is not set to be recovered/overwritten.  
 */
class BlockAlreadyExistsException extends IOException {
  private static final long serialVersionUID = 1L;

  public BlockAlreadyExistsException() {
    super();
  }

  public BlockAlreadyExistsException(String msg) {
    super(msg);
  }
}

