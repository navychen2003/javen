package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.util.BytePool;

final class ByteBlockPool extends BytePool {

	public ByteBlockPool(Allocator allocator) {
		super(allocator);
	}
	
	final void nextBuffer(int numPostingInt) { 
		if (ByteBlockPool.BYTE_BLOCK_SIZE - getByteUpto() < numPostingInt*ByteBlockPool.FIRST_LEVEL_SIZE) 
			nextBuffer();
	}
	
	final int newSlice() { 
		return newSlice(ByteBlockPool.FIRST_LEVEL_SIZE);
	}
	
	/**
	 * Writes the pools content to the given {@link DataOutput}
	 */
	final void writePool(final IIndexOutput out) throws IOException {
		int bytesOffset = getByteOffset();
		int block = 0;
		while (bytesOffset > 0) {
			out.writeBytes(getBufferAt(block++), BYTE_BLOCK_SIZE);
			bytesOffset -= BYTE_BLOCK_SIZE;
		}
		out.writeBytes(getBufferAt(block), getByteUpto());
	}
	
}
