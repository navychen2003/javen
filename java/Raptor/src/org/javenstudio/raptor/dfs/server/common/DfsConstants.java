package org.javenstudio.raptor.dfs.server.common;


/************************************
 * Some handy internal DFS constants
 *
 ************************************/

public interface DfsConstants {
  /**
   * Type of the node
   */
  static public enum NodeType {
    NAME_NODE,
    DATA_NODE;
  }

  // Startup options
  static public enum StartupOption{
    FORMAT          ("-format"),
    REGULAR         ("-regular"),
    UPGRADE         ("-upgrade"),
    ROLLBACK        ("-rollback"),
    FINALIZE        ("-finalize"),
    IMPORT          ("-importCheckpoint"); 
    
    private String name = null;
    private StartupOption(String arg) {this.name = arg;}
    public String getName() {return name;}
  }

  // Timeouts for communicating with DataNode for streaming writes/reads
  public static int READ_TIMEOUT = 60 * 1000;
  public static int WRITE_TIMEOUT = 8 * 60 * 1000;
  public static int WRITE_TIMEOUT_EXTENSION = 5 * 1000; //for write pipeline

}
