package org.javenstudio.raptor.dfs.server.protocol;

import java.io.*;
import java.util.List;

import org.javenstudio.raptor.dfs.protocol.Block;
import org.javenstudio.raptor.dfs.protocol.DatanodeInfo;
import org.javenstudio.raptor.dfs.server.namenode.DatanodeDescriptor.BlockTargetPair;
import org.javenstudio.raptor.io.*;


/****************************************************
 * A BlockCommand is an instruction to a datanode 
 * regarding some blocks under its control.  It tells
 * the DataNode to either invalidate a set of indicated
 * blocks, or to copy a set of indicated blocks to 
 * another DataNode.
 * 
 ****************************************************/
public class BlockCommand extends DatanodeCommand {
  Block blocks[];
  DatanodeInfo targets[][];

  public BlockCommand() {}

  /**
   * Create BlockCommand for transferring blocks to another datanode
   * @param blocktargetlist    blocks to be transferred 
   */
  public BlockCommand(int action, List<BlockTargetPair> blocktargetlist) {
    super(action);

    blocks = new Block[blocktargetlist.size()]; 
    targets = new DatanodeInfo[blocks.length][];
    for(int i = 0; i < blocks.length; i++) {
      BlockTargetPair p = blocktargetlist.get(i);
      blocks[i] = p.block;
      targets[i] = p.targets;
    }
  }

  private static final DatanodeInfo[][] EMPTY_TARGET = {};

  /**
   * Create BlockCommand for the given action
   * @param blocks blocks related to the action
   */
  public BlockCommand(int action, Block blocks[]) {
    super(action);
    this.blocks = blocks;
    this.targets = EMPTY_TARGET;
  }

  public Block[] getBlocks() {
    return blocks;
  }

  public DatanodeInfo[][] getTargets() {
    return targets;
  }

  ///////////////////////////////////////////
  // Writable
  ///////////////////////////////////////////
  static {                                      // register a ctor
    WritableFactories.setFactory
      (BlockCommand.class,
       new WritableFactory() {
         public Writable newInstance() { return new BlockCommand(); }
       });
  }

  public void write(DataOutput out) throws IOException {
    super.write(out);
    out.writeInt(blocks.length);
    for (int i = 0; i < blocks.length; i++) {
      blocks[i].write(out);
    }
    out.writeInt(targets.length);
    for (int i = 0; i < targets.length; i++) {
      out.writeInt(targets[i].length);
      for (int j = 0; j < targets[i].length; j++) {
        targets[i][j].write(out);
      }
    }
  }

  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    this.blocks = new Block[in.readInt()];
    for (int i = 0; i < blocks.length; i++) {
      blocks[i] = new Block();
      blocks[i].readFields(in);
    }

    this.targets = new DatanodeInfo[in.readInt()][];
    for (int i = 0; i < targets.length; i++) {
      this.targets[i] = new DatanodeInfo[in.readInt()];
      for (int j = 0; j < targets[i].length; j++) {
        targets[i][j] = new DatanodeInfo();
        targets[i][j].readFields(in);
      }
    }
  }
}

