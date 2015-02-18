package org.javenstudio.raptor.dfs.protocol;

/**
 * This class provides an interface for accessing list of blocks that
 * has been implemented as long[].
 * This class is usefull for block report. Rather than send block reports
 * as a Block[] we can send it as a long[].
 *
 */
public class BlockListAsLongs {
  /**
   * A block as 3 longs
   *   block-id and block length and generation stamp
   */
  private static final int LONGS_PER_BLOCK = 3;
  
  private static int index2BlockId(int index) {
    return index*LONGS_PER_BLOCK;
  }
  private static int index2BlockLen(int index) {
    return (index*LONGS_PER_BLOCK) + 1;
  }
  private static int index2BlockGenStamp(int index) {
    return (index*LONGS_PER_BLOCK) + 2;
  }
  
  private long[] blockList;
  
  /**
   * Converting a block[] to a long[]
   * @param blockArray - the input array block[]
   * @return the output array of long[]
   */
  
  public static long[] convertToArrayLongs(final Block[] blockArray) {
    long[] blocksAsLongs = new long[blockArray.length * LONGS_PER_BLOCK];

    BlockListAsLongs bl = new BlockListAsLongs(blocksAsLongs);
    assert bl.getNumberOfBlocks() == blockArray.length;

    for (int i = 0; i < blockArray.length; i++) {
      bl.setBlock(i, blockArray[i]);
    }
    return blocksAsLongs;
  }

  /**
   * Constructor
   * @param iBlockList - BlockListALongs create from this long[] parameter
   */
  public BlockListAsLongs(final long[] iBlockList) {
    if (iBlockList == null) {
      blockList = new long[0];
    } else {
      if (iBlockList.length%LONGS_PER_BLOCK != 0) {
        // must be multiple of LONGS_PER_BLOCK
        throw new IllegalArgumentException();
      }
      blockList = iBlockList;
    }
  }

  
  /**
   * The number of blocks
   * @return - the number of blocks
   */
  public int getNumberOfBlocks() {
    return blockList.length/LONGS_PER_BLOCK;
  }
  
  
  /**
   * The block-id of the indexTh block
   * @param index - the block whose block-id is desired
   * @return the block-id
   */
  public long getBlockId(final int index)  {
    return blockList[index2BlockId(index)];
  }
  
  /**
   * The block-len of the indexTh block
   * @param index - the block whose block-len is desired
   * @return - the block-len
   */
  public long getBlockLen(final int index)  {
    return blockList[index2BlockLen(index)];
  }

  /**
   * The generation stamp of the indexTh block
   * @param index - the block whose block-len is desired
   * @return - the generation stamp
   */
  public long getBlockGenStamp(final int index)  {
    return blockList[index2BlockGenStamp(index)];
  }
  
  /**
   * Set the indexTh block
   * @param index - the index of the block to set
   * @param b - the block is set to the value of the this block
   */
  void setBlock(final int index, final Block b) {
    blockList[index2BlockId(index)] = b.getBlockId();
    blockList[index2BlockLen(index)] = b.getNumBytes();
    blockList[index2BlockGenStamp(index)] = b.getGenerationStamp();
  }
}

