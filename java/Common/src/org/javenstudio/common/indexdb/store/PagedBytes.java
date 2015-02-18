package org.javenstudio.common.indexdb.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IBytesReader;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * Represents a logical byte[] as a series of pages.  You
 *  can write-once into the logical byte[] (append only),
 *  using copy, and then retrieve slices (BytesRef) into it
 *  using fill.
 *
 */
public final class PagedBytes {
	
	private static final byte[] EMPTY_BYTES = new byte[0];
	
	private final List<byte[]> mBlocks = new ArrayList<byte[]>();
	private final List<Integer> mBlockEnd = new ArrayList<Integer>();
	private final int mBlockSize;
	private final int mBlockBits;
	private final int mBlockMask;
	private boolean mDidSkipBytes;
	private boolean mFrozen;
	private int mUpto;
	private byte[] mCurrentBlock;

	/** 
	 * Provides methods to read BytesRefs from a frozen
	 *  PagedBytes.
	 *
	 * @see #freeze 
	 */
	public final static class Reader implements IBytesReader {
		private final byte[][] mBlocks;
		private final int[] mBlockEnds;
		private final int mBlockBits;
		private final int mBlockMask;
		private final int mBlockSize;

		public Reader(PagedBytes pagedBytes) {
			mBlocks = new byte[pagedBytes.mBlocks.size()][];
			for (int i=0; i < mBlocks.length; i++) {
				mBlocks[i] = pagedBytes.mBlocks.get(i);
			}
			mBlockEnds = new int[mBlocks.length];
			for (int i=0; i < mBlockEnds.length; i++) {
				mBlockEnds[i] = pagedBytes.mBlockEnd.get(i);
			}
			mBlockBits = pagedBytes.mBlockBits;
			mBlockMask = pagedBytes.mBlockMask;
			mBlockSize = pagedBytes.mBlockSize;
		}

		/**
		 * Gets a slice out of {@link PagedBytes} starting at <i>start</i> with a
		 * given length. If the slice spans across a block border this method will
		 * allocate sufficient resources and copy the paged data.
		 * <p>
		 * Slices spanning more than one block are not supported.
		 * </p>
		 */
		@Override
		public BytesRef fillSlice(BytesRef b, long start, int length) {
			assert length >= 0: "length=" + length;
			assert length <= mBlockSize+1;
			
			final int index = (int) (start >> mBlockBits);
			final int offset = (int) (start & mBlockMask);
			
			b.mLength = length;
			if (mBlockSize - offset >= length) {
				// Within block
				b.mBytes = mBlocks[index];
				b.mOffset = offset;
				
			} else {
				// Split
				b.mBytes = new byte[length];
				b.mOffset = 0;
				
				System.arraycopy(mBlocks[index], offset, b.mBytes, 0, mBlockSize-offset);
				System.arraycopy(mBlocks[1+index], 0, 
						b.mBytes, mBlockSize-offset, length-(mBlockSize-offset));
			}
			
			return b;
		}
    
		/**
		 * Reads length as 1 or 2 byte vInt prefix, starting at <i>start</i>.
		 * <p>
		 * <b>Note:</b> this method does not support slices spanning across block
		 * borders.
		 * </p>
		 * 
		 * @return the given {@link BytesRef}
		 */
		@Override
		public BytesRef fill(BytesRef b, long start) {
			final int index = (int) (start >> mBlockBits);
			final int offset = (int) (start & mBlockMask);
			final byte[] block = b.mBytes = mBlocks[index];

			if ((block[offset] & 128) == 0) {
				b.mLength = block[offset];
				b.mOffset = offset+1;
				
			} else {
				b.mLength = ((block[offset] & 0x7f) << 8) | (block[1+offset] & 0xff);
				b.mOffset = offset+2;
				
				assert b.mLength > 0;
			}
			
			return b;
		}

		/**
		 * Reads length as 1 or 2 byte vInt prefix, starting at <i>start</i>. *
		 * <p>
		 * <b>Note:</b> this method does not support slices spanning across block
		 * borders.
		 * </p>
		 * 
		 * @return the internal block number of the slice.
		 */
		@Override
		public int fillAndGetIndex(BytesRef b, long start) {
			final int index = (int) (start >> mBlockBits);
			final int offset = (int) (start & mBlockMask);
			final byte[] block = b.mBytes = mBlocks[index];

			if ((block[offset] & 128) == 0) {
				b.mLength = block[offset];
				b.mOffset = offset+1;
				
			} else {
				b.mLength = ((block[offset] & 0x7f) << 8) | (block[1+offset] & 0xff);
				b.mOffset = offset+2;
				
				assert b.mLength > 0;
			}
			
			return index;
		}

		/**
		 * Reads length as 1 or 2 byte vInt prefix, starting at <i>start</i> and
		 * returns the start offset of the next part, suitable as start parameter on
		 * next call to sequentially read all {@link BytesRef}.
		 * 
		 * <p>
		 * <b>Note:</b> this method does not support slices spanning across block
		 * borders.
		 * </p>
		 * 
		 * @return the start offset of the next part, suitable as start parameter on
		 *         next call to sequentially read all {@link BytesRef}.
		 */
		@Override
		public long fillAndGetStart(BytesRef b, long start) {
			final int index = (int) (start >> mBlockBits);
			final int offset = (int) (start & mBlockMask);
			final byte[] block = b.mBytes = mBlocks[index];

			if ((block[offset] & 128) == 0) {
				b.mLength = block[offset];
				b.mOffset = offset+1;
				
				start += 1L + b.mLength;
				
			} else {
				b.mLength = ((block[offset] & 0x7f) << 8) | (block[1+offset] & 0xff);
				b.mOffset = offset+2;
				
				start += 2L + b.mLength;
				
				assert b.mLength > 0;
			}
			
			return start;
		}
  
		/**
		 * Gets a slice out of {@link PagedBytes} starting at <i>start</i>, the
		 * length is read as 1 or 2 byte vInt prefix. If the slice spans across a
		 * block border this method will allocate sufficient resources and copy the
		 * paged data.
		 * <p>
		 * Slices spanning more than one block are not supported.
		 * </p>
		 */
		@Override
		public BytesRef fillSliceWithPrefix(BytesRef b, long start) {
			int index = (int) (start >> mBlockBits);
			int offset = (int) (start & mBlockMask);
			
			byte[] block = mBlocks[index];
			final int length;
			
			assert offset <= block.length-1;
			
			if ((block[offset] & 128) == 0) {
				length = block[offset];
				offset = offset + 1;
				
			} else {
				if (offset == block.length-1) {
					final byte[] nextBlock = mBlocks[++index];
					length = ((block[offset] & 0x7f) << 8) | (nextBlock[0] & 0xff);
					offset = 1;
					block = nextBlock;
					
					assert length > 0; 
					
				} else {
					assert offset < block.length-1;
					
					length = ((block[offset] & 0x7f) << 8) | (block[1+offset] & 0xff);
					offset = offset+2;
					
					assert length > 0;
				}
			}
			
			assert length >= 0: "length=" + length;
			b.mLength = length;

			// NOTE: even though copyUsingLengthPrefix always
			// allocs a new block if the byte[] to be added won't
			// fit in current block,
			// VarDerefBytesImpl.finishInternal does its own
			// prefix + byte[] writing which can span two mBlocks,
			// so we support that here on decode:
			if (mBlockSize - offset >= length) {
				// Within block
				b.mOffset = offset;
				b.mBytes = mBlocks[index];
				
			} else {
				// Split
				b.mBytes = new byte[length];
				b.mOffset = 0;
				
				System.arraycopy(mBlocks[index], offset, b.mBytes, 0, mBlockSize-offset);
				System.arraycopy(mBlocks[1+index], 0, 
						b.mBytes, mBlockSize-offset, length-(mBlockSize-offset));
			}
			
			return b;
		}

		public byte[][] getBlocks() {
			return mBlocks;
		}

		public int[] getBlockEnds() {
			return mBlockEnds;
		}
	}

	/** 
	 * 1&lt;&lt;blockBits must be bigger than biggest single
	 *  BytesRef slice that will be pulled 
	 */
	public PagedBytes(int blockBits) {
		mBlockSize = 1 << blockBits;
		mBlockBits = blockBits;
		mBlockMask = mBlockSize-1;
		mUpto = mBlockSize;
	}

	/** Read this many bytes from in */
	public void copy(IIndexInput in, long byteCount) throws IOException {
		while (byteCount > 0) {
			int left = mBlockSize - mUpto;
			if (left == 0) {
				if (mCurrentBlock != null) {
					mBlocks.add(mCurrentBlock);
					mBlockEnd.add(mUpto);
				}
				mCurrentBlock = new byte[mBlockSize];
				mUpto = 0;
				left = mBlockSize;
			}
			
			if (left < byteCount) {
				in.readBytes(mCurrentBlock, mUpto, left, false);
				mUpto = mBlockSize;
				byteCount -= left;
				
			} else {
				in.readBytes(mCurrentBlock, mUpto, (int) byteCount, false);
				mUpto += byteCount;
				break;
			}
		}
	}

	/** Copy BytesRef in */
	public void copy(BytesRef bytes) throws IOException {
		int byteCount = bytes.mLength;
		int bytesUpto = bytes.mOffset;
		
		while (byteCount > 0) {
			int left = mBlockSize - mUpto;
			if (left == 0) {
				if (mCurrentBlock != null) {
					mBlocks.add(mCurrentBlock);
					mBlockEnd.add(mUpto);          
				}
				mCurrentBlock = new byte[mBlockSize];
				mUpto = 0;
				left = mBlockSize;
			}
			
			if (left < byteCount) {
				System.arraycopy(bytes.mBytes, bytesUpto, mCurrentBlock, mUpto, left);
				mUpto = mBlockSize;
				byteCount -= left;
				bytesUpto += left;
				
			} else {
				System.arraycopy(bytes.mBytes, bytesUpto, mCurrentBlock, mUpto, byteCount);
				mUpto += byteCount;
				break;
			}
		}
	}

	/** 
	 * Copy BytesRef in, setting BytesRef out to the result.
	 * Do not use this if you will use freeze(true).
	 * This only supports bytes.length <= blockSize 
	 */
	public void copy(BytesRef bytes, BytesRef out) throws IOException {
		int left = mBlockSize - mUpto;
		
		if (bytes.mLength > left || mCurrentBlock==null) {
			if (mCurrentBlock != null) {
				mBlocks.add(mCurrentBlock);
				mBlockEnd.add(mUpto);
				mDidSkipBytes = true;
			}
			
			mCurrentBlock = new byte[mBlockSize];
			mUpto = 0;
			left = mBlockSize;
			
			assert bytes.mLength <= mBlockSize;
			// TODO: we could also support variable block sizes
		}

		out.mBytes = mCurrentBlock;
		out.mOffset = mUpto;
		out.mLength = bytes.mLength;

		System.arraycopy(bytes.mBytes, bytes.mOffset, mCurrentBlock, mUpto, bytes.mLength);
		mUpto += bytes.mLength;
	}

	/** Commits final byte[], trimming it if necessary and if trim=true */
	public Reader freeze(boolean trim) {
		if (mFrozen) 
			throw new IllegalStateException("already frozen");
		
		if (mDidSkipBytes) 
			throw new IllegalStateException("cannot freeze when copy(BytesRef, BytesRef) was used");
		
		if (trim && mUpto < mBlockSize) {
			final byte[] newBlock = new byte[mUpto];
			System.arraycopy(mCurrentBlock, 0, newBlock, 0, mUpto);
			mCurrentBlock = newBlock;
		}
		
		if (mCurrentBlock == null) 
			mCurrentBlock = EMPTY_BYTES;
		
		mBlocks.add(mCurrentBlock);
		mBlockEnd.add(mUpto); 
		mFrozen = true;
		mCurrentBlock = null;
		
		return new Reader(this);
	}

	public long getPointer() {
		if (mCurrentBlock == null) 
			return 0;
		else 
			return (mBlocks.size() * ((long) mBlockSize)) + mUpto;
	}

	/** 
	 * Copy bytes in, writing the length as a 1 or 2 byte
	 *  vInt prefix. 
	 */
	public long copyUsingLengthPrefix(BytesRef bytes) throws IOException {
		if (bytes.mLength >= 32768) 
			throw new IllegalArgumentException("max length is 32767 (got " + bytes.mLength + ")");

		if (mUpto + bytes.mLength + 2 > mBlockSize) {
			if (bytes.mLength + 2 > mBlockSize) {
				throw new IllegalArgumentException("block size " + mBlockSize + 
						" is too small to store length " + bytes.mLength + " bytes");
			}
			
			if (mCurrentBlock != null) {
				mBlocks.add(mCurrentBlock);
				mBlockEnd.add(mUpto);        
			}
			
			mCurrentBlock = new byte[mBlockSize];
			mUpto = 0;
		}

		final long pointer = getPointer();

		if (bytes.mLength < 128) {
			mCurrentBlock[mUpto++] = (byte) bytes.mLength;
		} else {
			mCurrentBlock[mUpto++] = (byte) (0x80 | (bytes.mLength >> 8));
			mCurrentBlock[mUpto++] = (byte) (bytes.mLength & 0xff);
		}
		
		System.arraycopy(bytes.mBytes, bytes.mOffset, mCurrentBlock, mUpto, bytes.mLength);
		mUpto += bytes.mLength;

		return pointer;
	}

	public final class PagedBytesDataInput extends DataInput {
		private int mCurrentBlockIndex;
		private int mCurrentBlockUpto;
		private byte[] mCurrentBlock;

		PagedBytesDataInput() {
			mCurrentBlock = mBlocks.get(0);
		}

		@Override
		public PagedBytesDataInput clone() {
			PagedBytesDataInput clone = getDataInput();
			clone.setPosition(getPosition());
			return clone;
		}

		/** Returns the current byte position. */
		public long getPosition() {
			return mCurrentBlockIndex * mBlockSize + mCurrentBlockUpto;
		}
  
		/** 
		 * Seek to a position previously obtained from
		 *  {@link #getPosition}. 
		 */
		public void setPosition(long pos) {
			mCurrentBlockIndex = (int) (pos >> mBlockBits);
			mCurrentBlock = mBlocks.get(mCurrentBlockIndex);
			mCurrentBlockUpto = (int) (pos & mBlockMask);
		}

		@Override
		public byte readByte() {
			if (mCurrentBlockUpto == mBlockSize) 
				nextBlock();
			
			return mCurrentBlock[mCurrentBlockUpto++];
		}

		@Override
		public void readBytes(byte[] b, int offset, int len) {
			assert b.length >= offset + len;
			final int offsetEnd = offset + len;
			
			while (true) {
				final int blockLeft = mBlockSize - mCurrentBlockUpto;
				final int left = offsetEnd - offset;
				
				if (blockLeft < left) {
					System.arraycopy(mCurrentBlock, mCurrentBlockUpto,
							b, offset, blockLeft);
					nextBlock();
					offset += blockLeft;
					
				} else {
					// Last block
					System.arraycopy(mCurrentBlock, mCurrentBlockUpto,
							b, offset, left);
					mCurrentBlockUpto += left;
					break;
				}
			}
		}

		private void nextBlock() {
			mCurrentBlockIndex++;
			mCurrentBlockUpto = 0;
			mCurrentBlock = mBlocks.get(mCurrentBlockIndex);
		}
	}

	public final class PagedBytesDataOutput extends DataOutput {
		@Override
		public void writeByte(byte b) {
			if (mUpto == mBlockSize) {
				if (mCurrentBlock != null) {
					mBlocks.add(mCurrentBlock);
					mBlockEnd.add(mUpto);
				}
				mCurrentBlock = new byte[mBlockSize];
				mUpto = 0;
			}
			mCurrentBlock[mUpto++] = b;
		}

		@Override
		public void writeBytes(byte[] b, int offset, int length) throws IOException {
			assert b.length >= offset + length;
			if (length == 0) 
				return;

			if (mUpto == mBlockSize) {
				if (mCurrentBlock != null) {
					mBlocks.add(mCurrentBlock);
					mBlockEnd.add(mUpto);
				}
				mCurrentBlock = new byte[mBlockSize];
				mUpto = 0;
			}
          
			final int offsetEnd = offset + length;
			while (true) {
				final int left = offsetEnd - offset;
				final int blockLeft = mBlockSize - mUpto;
				
				if (blockLeft < left) {
					System.arraycopy(b, offset, mCurrentBlock, mUpto, blockLeft);
					mBlocks.add(mCurrentBlock);
					mBlockEnd.add(mBlockSize);
					mCurrentBlock = new byte[mBlockSize];
					mUpto = 0;
					offset += blockLeft;
					
				} else {
					// Last block
					System.arraycopy(b, offset, mCurrentBlock, mUpto, left);
					mUpto += left;
					break;
				}
			}
		}

		/** Return the current byte position. */
		public long getPosition() {
			if (mCurrentBlock == null) 
				return 0;
			else 
				return mBlocks.size() * mBlockSize + mUpto;
		}
	}

	/** 
	 * Returns a DataInput to read values from this
	 *  PagedBytes instance. 
	 */
	public PagedBytesDataInput getDataInput() {
		if (!mFrozen) 
			throw new IllegalStateException("must call freeze() before getDataInput");
		
		return new PagedBytesDataInput();
	}

	/** 
	 * Returns a DataOutput that you may use to write into
	 *  this PagedBytes instance.  If you do this, you should
	 *  not call the other writing methods (eg, copy);
	 *  results are undefined. 
	 */
	public PagedBytesDataOutput getDataOutput() {
		if (mFrozen) 
			throw new IllegalStateException("cannot get DataOutput after freeze()");
		
		return new PagedBytesDataOutput();
	}
	
}
