package org.javenstudio.raptor.dfs.server.protocol;

import java.io.*;

import org.javenstudio.raptor.dfs.protocol.Block;
import org.javenstudio.raptor.io.*;

/**
 * Meta data information for a block
 */
public class BlockMetaDataInfo extends Block {
  static final WritableFactory FACTORY = new WritableFactory() {
    public Writable newInstance() { return new BlockMetaDataInfo(); }
  };
  static {                                      // register a ctor
    WritableFactories.setFactory(BlockMetaDataInfo.class, FACTORY);
  }

  private long lastScanTime;

  public BlockMetaDataInfo() {}

  public BlockMetaDataInfo(Block b, long lastScanTime) {
    super(b);
    this.lastScanTime = lastScanTime;
  }

  public long getLastScanTime() {return lastScanTime;}

  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    super.write(out);
    out.writeLong(lastScanTime);
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    lastScanTime = in.readLong();
  }
}

