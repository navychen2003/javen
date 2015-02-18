package org.javenstudio.raptor.dfs.protocol;


/**
 * 
 * The Client transfers data to/from datanode using a streaming protocol.
 *
 */
public interface DataTransferProtocol {
  
  /** Version for data transfers between clients and datanodes
   * This should change when serialization of DatanodeInfo, not just
   * when protocol changes. It is not very obvious. 
   */
  /*
   * Version 14:
   *    OP_REPLACE_BLOCK is sent from the Balancer server to the destination,
   *    including the block id, source, and proxy.
   *    OP_COPY_BLOCK is sent from the destination to the proxy, which contains
   *    only the block id.
   *    A reply to OP_COPY_BLOCK sends the block content.
   *    A reply to OP_REPLACE_BLOCK includes an operation status.
   */
  public static final int DATA_TRANSFER_VERSION = 14;

  // Processed at datanode stream-handler
  public static final byte OP_WRITE_BLOCK = (byte) 80;
  public static final byte OP_READ_BLOCK = (byte) 81;
  public static final byte OP_READ_METADATA = (byte) 82;
  public static final byte OP_REPLACE_BLOCK = (byte) 83;
  public static final byte OP_COPY_BLOCK = (byte) 84;
  public static final byte OP_BLOCK_CHECKSUM = (byte) 85;
  
  public static final int OP_STATUS_SUCCESS = 0;  
  public static final int OP_STATUS_ERROR = 1;  
  public static final int OP_STATUS_ERROR_CHECKSUM = 2;  
  public static final int OP_STATUS_ERROR_INVALID = 3;  
  public static final int OP_STATUS_ERROR_EXISTS = 4;  
  public static final int OP_STATUS_CHECKSUM_OK = 5;  


}
