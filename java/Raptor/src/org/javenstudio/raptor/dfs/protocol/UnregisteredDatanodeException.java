package org.javenstudio.raptor.dfs.protocol;

import java.io.IOException;


/**
 * This exception is thrown when a datanode that has not previously 
 * registered is trying to access the name node.
 * 
 */
public class UnregisteredDatanodeException extends IOException {
  private static final long serialVersionUID = 1L;

  public UnregisteredDatanodeException(DatanodeID nodeID) {
    super("Unregistered data node: " + nodeID.getName());
  }

  public UnregisteredDatanodeException(DatanodeID nodeID, 
                                       DatanodeInfo storedNode) {
    super("Data node " + nodeID.getName() 
          + " is attempting to report storage ID "
          + nodeID.getStorageID() + ". Node " 
          + storedNode.getName() + " is expected to serve this storage.");
  }
}

