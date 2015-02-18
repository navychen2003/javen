package org.javenstudio.raptor.dfs.server.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.dfs.protocol.Block;
import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableUtils;

/** A class to implement an array of BlockLocations
 *  It provide efficient customized serialization/deserialization methods
 *  in stead of using the default array (de)serialization provided by RPC
 */
public class BlocksWithLocations implements Writable {

  /**
   * A class to keep track of a block and its locations
   */
  public static class BlockWithLocations  implements Writable {
    Block block;
    String datanodeIDs[];
    
    /** default constructor */
    public BlockWithLocations() {
      block = new Block();
      datanodeIDs = null;
    }
    
    /** constructor */
    public BlockWithLocations(Block b, String[] datanodes) {
      block = b;
      datanodeIDs = datanodes;
    }
    
    /** get the block */
    public Block getBlock() {
      return block;
    }
    
    /** get the block's locations */
    public String[] getDatanodes() {
      return datanodeIDs;
    }
    
    /** deserialization method */
    public void readFields(DataInput in) throws IOException {
      block.readFields(in);
      int len = WritableUtils.readVInt(in); // variable length integer
      datanodeIDs = new String[len];
      for(int i=0; i<len; i++) {
        datanodeIDs[i] = Text.readString(in);
      }
    }
    
    /** serialization method */
    public void write(DataOutput out) throws IOException {
      block.write(out);
      WritableUtils.writeVInt(out, datanodeIDs.length); // variable length int
      for(String id:datanodeIDs) {
        Text.writeString(out, id);
      }
    }
  }

  private BlockWithLocations[] blocks;

  /** default constructor */
  BlocksWithLocations() {
  }

  /** Constructor with one parameter */
  public BlocksWithLocations( BlockWithLocations[] blocks ) {
    this.blocks = blocks;
  }

  /** getter */
  public BlockWithLocations[] getBlocks() {
    return blocks;
  }

  /** serialization method */
  public void write( DataOutput out ) throws IOException {
    WritableUtils.writeVInt(out, blocks.length);
    for(int i=0; i<blocks.length; i++) {
      blocks[i].write(out);
    }
  }

  /** deserialization method */
  public void readFields(DataInput in) throws IOException {
    int len = WritableUtils.readVInt(in);
    blocks = new BlockWithLocations[len];
    for(int i=0; i<len; i++) {
      blocks[i] = new BlockWithLocations();
      blocks[i].readFields(in);
    }
  }
}

