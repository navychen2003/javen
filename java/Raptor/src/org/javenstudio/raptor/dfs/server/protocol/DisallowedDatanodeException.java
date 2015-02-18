package org.javenstudio.raptor.dfs.server.protocol;

import java.io.IOException;

import org.javenstudio.raptor.dfs.protocol.DatanodeID;


/**
 * This exception is thrown when a datanode tries to register or communicate
 * with the namenode when it does not appear on the list of included nodes, 
 * or has been specifically excluded.
 * 
 */
public class DisallowedDatanodeException extends IOException {
  private static final long serialVersionUID = 1L;

  public DisallowedDatanodeException(DatanodeID nodeID) {
    super("Datanode denied communication with namenode: " + nodeID.getName());
  }
}

