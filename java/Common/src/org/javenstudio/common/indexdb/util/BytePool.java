package org.javenstudio.common.indexdb.util;

import java.util.Arrays;
import java.util.List;

/** 
 * Class that Posting and PostingVector use to write byte
 * streams into shared fixed-size byte[] arrays.  The idea
 * is to allocate slices of increasing lengths For
 * example, the first slice is 5 bytes, the next slice is
 * 14, etc.  We start by writing our bytes into the first
 * 5 bytes.  When we hit the end of the slice, we allocate
 * the next slice and then write the address of the new
 * slice into the last 4 bytes of the previous slice (the
 * "forwarding address").
 *
 * Each slice is filled with 0's initially, and we mark
 * the end with a non-zero byte.  This way the methods
 * that are writing into the slice don't need to record
 * its length and instead allocate a new slice once they
 * hit a non-zero byte. 
 * 
 */
public class BytePool {
  public final static int BYTE_BLOCK_SHIFT = 15;
  public final static int BYTE_BLOCK_SIZE = 1 << BYTE_BLOCK_SHIFT;
  public final static int BYTE_BLOCK_MASK = BYTE_BLOCK_SIZE - 1;

  /** Abstract class for allocating and freeing byte blocks. */
  public abstract static class Allocator {
    protected final int mBlockSize;

    public Allocator(int blockSize) {
      mBlockSize = blockSize;
    }

    public abstract void recycleByteBlocks(byte[][] blocks, int start, int end);

    public void recycleByteBlocks(List<byte[]> blocks) {
      final byte[][] b = blocks.toArray(new byte[blocks.size()][]);
      recycleByteBlocks(b, 0, b.length);
    }

    public byte[] getByteBlock() {
      return new byte[mBlockSize];
    }
  }
  
  /** A simple {@link Allocator} that never recycles. */
  public static final class DirectAllocator extends Allocator {
    
    public DirectAllocator() {
      this(BYTE_BLOCK_SIZE);
    }

    public DirectAllocator(int blockSize) {
      super(blockSize);
    }

    @Override
    public void recycleByteBlocks(byte[][] blocks, int start, int end) {
    }
  }
  
  /** 
   * A simple {@link Allocator} that never recycles, but
   *  tracks how much total RAM is in use. 
   */
  public static class DirectTrackingAllocator extends Allocator {
    private final Counter mBytesUsed;
    
    public DirectTrackingAllocator(Counter bytesUsed) {
      this(BYTE_BLOCK_SIZE, bytesUsed);
    }

    public DirectTrackingAllocator(int blockSize, Counter bytesUsed) {
      super(blockSize);
      mBytesUsed = bytesUsed;
    }

    public byte[] getByteBlock() {
      mBytesUsed.addAndGet(mBlockSize);
      return new byte[mBlockSize];
    }

    @Override
    public void recycleByteBlocks(byte[][] blocks, int start, int end) {
      mBytesUsed.addAndGet(-((end-start)* mBlockSize));
      for (int i = start; i < end; i++) {
        blocks[i] = null;
      }
    }
  };


  private byte[][] mBuffers = new byte[10][];

  private int mBufferUpto = -1;                        // Which buffer we are upto
  private int mByteUpto = BYTE_BLOCK_SIZE;             // Where we are in head buffer

  private byte[] mBuffer;                              // Current head buffer
  private int mByteOffset = -BYTE_BLOCK_SIZE;          // Current head offset

  private final Allocator mAllocator;

  public BytePool(Allocator allocator) {
    mAllocator = allocator;
  }
  
  public final int getByteUpto() { return mByteUpto; }
  public final int getByteOffset() { return mByteOffset; }
  public final byte[] getBuffer() { return mBuffer; }
  public final byte[] getBufferAt(int pos) { return mBuffers[pos]; }
  
  public final byte[] getBufferAtStart(int start) { 
	return mBuffers[start >> BYTE_BLOCK_SHIFT]; 
  }
  
  public final void increaseByteUpto(int count) { mByteUpto += count; }
  
  public void dropBuffersAndReset() {
    if (mBufferUpto != -1) {
      // Recycle all but the first buffer
      mAllocator.recycleByteBlocks(mBuffers, 0, 1+mBufferUpto);

      // Re-use the first buffer
      mBufferUpto = -1;
      mByteUpto = BYTE_BLOCK_SIZE;
      mByteOffset = -BYTE_BLOCK_SIZE;
      mBuffers = new byte[10][];
      mBuffer = null;
    }
  }

  public void reset() {
    if (mBufferUpto != -1) {
      // We allocated at least one buffer

      for (int i=0; i < mBufferUpto; i++) {
        // Fully zero fill buffers that we fully used
        Arrays.fill(mBuffers[i], (byte) 0);
      }

      // Partial zero fill the final buffer
      Arrays.fill(mBuffers[mBufferUpto], 0, mByteUpto, (byte) 0);
          
      if (mBufferUpto > 0) {
        // Recycle all but the first buffer
        mAllocator.recycleByteBlocks(mBuffers, 1, 1+mBufferUpto);
      }

      // Re-use the first buffer
      mBufferUpto = 0;
      mByteUpto = 0;
      mByteOffset = 0;
      mBuffer = mBuffers[0];
    }
  }
  
  public void nextBuffer() {
    if (1+mBufferUpto == mBuffers.length) {
      byte[][] newBuffers = new byte[ArrayUtil.oversize(mBuffers.length+1,
                                                        JvmUtil.NUM_BYTES_OBJECT_REF)][];
      System.arraycopy(mBuffers, 0, newBuffers, 0, mBuffers.length);
      mBuffers = newBuffers;
    }
    mBuffer = mBuffers[1+mBufferUpto] = mAllocator.getByteBlock();
    mBufferUpto ++;

    mByteUpto = 0;
    mByteOffset += BYTE_BLOCK_SIZE;
  }

  public int newSlice(final int size) {
    if (mByteUpto > BYTE_BLOCK_SIZE-size)
      nextBuffer();
    final int upto = mByteUpto;
    mByteUpto += size;
    mBuffer[mByteUpto-1] = 16;
    return upto;
  }

  // Size of each slice.  These arrays should be at most 16
  // elements (index is encoded with 4 bits).  First array
  // is just a compact way to encode X+1 with a max.  Second
  // array is the length of each slice, ie first slice is 5
  // bytes, next slice is 14 bytes, etc.
  
  public final static int[] nextLevelArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 9};
  public final static int[] levelSizeArray = {5, 14, 20, 30, 40, 40, 80, 80, 120, 200};
  public final static int FIRST_LEVEL_SIZE = levelSizeArray[0];

  public int allocSlice(final byte[] slice, final int upto) {
    final int level = slice[upto] & 15;
    final int newLevel = nextLevelArray[level];
    final int newSize = levelSizeArray[newLevel];

    // Maybe allocate another block
    if (mByteUpto > BYTE_BLOCK_SIZE-newSize)
      nextBuffer();

    final int newUpto = mByteUpto;
    final int offset = newUpto + mByteOffset;
    mByteUpto += newSize;

    // Copy forward the past 3 bytes (which we are about
    // to overwrite with the forwarding address):
    mBuffer[newUpto] = slice[upto-3];
    mBuffer[newUpto+1] = slice[upto-2];
    mBuffer[newUpto+2] = slice[upto-1];

    // Write forwarding address at end of last slice:
    slice[upto-3] = (byte) (offset >>> 24);
    slice[upto-2] = (byte) (offset >>> 16);
    slice[upto-1] = (byte) (offset >>> 8);
    slice[upto] = (byte) offset;
        
    // Write new level:
    mBuffer[mByteUpto-1] = (byte) (16|newLevel);

    return newUpto+3;
  }

  // Fill in a BytesRef from term's length & bytes encoded in
  // byte block
  public final BytesRef setBytesRef(BytesRef term, int textStart) {
    final byte[] bytes = term.mBytes = mBuffers[textStart >> BYTE_BLOCK_SHIFT];
    int pos = textStart & BYTE_BLOCK_MASK;
    if ((bytes[pos] & 0x80) == 0) {
      // length is 1 byte
      term.mLength = bytes[pos];
      term.mOffset = pos+1;
    } else {
      // length is 2 bytes
      term.mLength = (bytes[pos]&0x7f) + ((bytes[pos+1]&0xff)<<7);
      term.mOffset = pos+2;
    }
    assert term.mLength >= 0;
    return term;
  }
  
  /**
   * Dereferences the byte block according to {@link BytesRef} offset. The offset 
   * is interpreted as the absolute offset into the {@link BytePool}.
   */
  public final BytesRef deref(BytesRef bytes) {
    final int offset = bytes.mOffset;
    byte[] buffer = mBuffers[offset >> BYTE_BLOCK_SHIFT];
    int pos = offset & BYTE_BLOCK_MASK;
    bytes.mBytes = buffer;
    bytes.mOffset = pos;
    return bytes;
  }
  
  /**
   * Copies the given {@link BytesRef} at the current positions (
   * {@link #byteUpto} across buffer boundaries
   */
  public final void copy(final BytesRef bytes) {
    int length = bytes.mLength;
    int offset = bytes.mOffset;
    int overflow = (length + mByteUpto) - BYTE_BLOCK_SIZE;
    do {
      if (overflow <= 0) { 
        System.arraycopy(bytes.mBytes, offset, mBuffer, mByteUpto, length);
        mByteUpto += length;
        break;
      } else {
        final int bytesToCopy = length-overflow;
        System.arraycopy(bytes.mBytes, offset, mBuffer, mByteUpto, bytesToCopy);
        offset += bytesToCopy;
        length -= bytesToCopy;
        nextBuffer();
        overflow = overflow - BYTE_BLOCK_SIZE;
      }
    }  while(true);
  }
  
  public final BytesRef copyFrom(final BytesRef bytes) {
    final int length = bytes.mLength;
    final int offset = bytes.mOffset;
    bytes.mOffset = 0;
    bytes.grow(length);
    int bufferIndex = offset >> BYTE_BLOCK_SHIFT;
    byte[] buffer = mBuffers[bufferIndex];
    int pos = offset & BYTE_BLOCK_MASK;
    int overflow = (pos + length) - BYTE_BLOCK_SIZE;
    do {
      if (overflow <= 0) {
        System.arraycopy(buffer, pos, bytes.mBytes, bytes.mOffset, bytes.mLength);
        bytes.mLength = length;
        bytes.mOffset = 0;
        break;
      } else {
        final int bytesToCopy = length - overflow;
        System.arraycopy(buffer, pos, bytes.mBytes, bytes.mOffset, bytesToCopy);
        pos = 0;
        bytes.mLength -= bytesToCopy;
        bytes.mOffset += bytesToCopy;
        buffer = mBuffers[++bufferIndex];
        overflow = overflow - BYTE_BLOCK_SIZE;
      }
    } while (true);
    return bytes;
  }

}

