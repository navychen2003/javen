package org.javenstudio.common.indexdb.util;

import java.util.Arrays;

public class IntPool {
	
	/** 
	 * Initial chunks size of the shared int[] blocks used to
     * store postings data 
     */
	public final static int INT_BLOCK_SHIFT = 13;
	public final static int INT_BLOCK_SIZE = 1 << INT_BLOCK_SHIFT;
	public final static int INT_BLOCK_MASK = INT_BLOCK_SIZE - 1;
	
	public static class Allocator { 
		private final Counter mBytesUsed;
		
		public Allocator(Counter bytesUsed) { 
			mBytesUsed = bytesUsed;
		}
		
		/** Allocate another int[] from the shared pool */
		public int[] getIntBlock() {
			int[] b = new int[INT_BLOCK_SIZE];
			mBytesUsed.addAndGet(INT_BLOCK_SIZE*JvmUtil.NUM_BYTES_INT);
			return b;
		}
	  
		public void recycleIntBlocks(int[][] blocks, int offset, int length) {
			mBytesUsed.addAndGet(-(length *(INT_BLOCK_SIZE*JvmUtil.NUM_BYTES_INT)));
		}
	}
	
	
	private int[][] mBuffers = new int[10][];

	private int mBufferUpto = -1;            	// Which buffer we are upto
	private int mIntUpto = INT_BLOCK_SIZE; 		// Where we are in head buffer

	private int[] mBuffer;                     	// Current head buffer
	private int mIntOffset = -INT_BLOCK_SIZE;	// Current head offset

	private final Allocator mAllocator;
	
	public IntPool(Allocator allocator) {
		mAllocator = allocator;
	}

	public int getIntUpto() { return mIntUpto; }
	public void increaseIntUpto(int count) { mIntUpto += count; }
	
	public int getIntOffset() { return mIntOffset; }
	public int[] getBuffer() { return mBuffer; }
	public int[] getBufferAt(int pos) { return mBuffers[pos]; }
	
	public void nextBuffer(int numPostingInt) { 
		// Init stream slices
		if (numPostingInt + mIntUpto > INT_BLOCK_SIZE) 
			nextBuffer();
	}
	
	public int[] getBufferAtStart(int intStart) { 
		return mBuffers[intStart >> INT_BLOCK_SHIFT];
	}
	
	public void reset() {
		if (mBufferUpto != -1) {
			// Reuse first buffer
			if (mBufferUpto > 0) {
				mAllocator.recycleIntBlocks(mBuffers, 1, mBufferUpto-1);
				Arrays.fill(mBuffers, 1, mBufferUpto, null);
			}
			mBufferUpto = 0;
			mIntUpto = 0;
			mIntOffset = 0;
			mBuffer = mBuffers[0];
		}
	}

	public void nextBuffer() {
		if (1+mBufferUpto == mBuffers.length) {
			int[][] newBuffers = new int[(int) (mBuffers.length*1.5)][];
			System.arraycopy(mBuffers, 0, newBuffers, 0, mBuffers.length);
			mBuffers = newBuffers;
		}
		mBuffer = mBuffers[1+mBufferUpto] = mAllocator.getIntBlock();
		mBufferUpto ++;

		mIntUpto = 0;
		mIntOffset += INT_BLOCK_SIZE;
	}
	
}
