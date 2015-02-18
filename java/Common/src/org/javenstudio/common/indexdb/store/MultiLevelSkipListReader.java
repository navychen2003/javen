package org.javenstudio.common.indexdb.store;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.util.MathUtil;

/**
 * This abstract class reads skip lists with multiple levels.
 * 
 * See {@link MultiLevelSkipListWriter} for the information about the encoding 
 * of the multi level skip lists. 
 * 
 * Subclasses must implement the abstract method {@link #readSkipData(int, IndexInput)}
 * which defines the actual format of the skip data.
 * 
 */
public abstract class MultiLevelSkipListReader {
	private final IIndexContext mContext;
	
	// the maximum number of skip levels possible for this index
	private int mMaxNumberOfSkipLevels; 
  
	// number of levels in this skip list
	private int mNumberOfSkipLevels;
  
	// Expert: defines the number of top skip levels to buffer in memory.
	// Reducing this number results in less memory usage, but possibly
	// slower performance due to more random I/Os.
	// Please notice that the space each level occupies is limited by
	// the skipInterval. The top level can not contain more than
	// skipLevel entries, the second top level can not contain more
	// than skipLevel^2 entries and so forth.
	private int mNumberOfLevelsToBuffer = 1;
  
	private int mDocCount;
	private boolean mHaveSkipped;
  
	private IndexInput[] mSkipStream;    // skipStream for each level
	private long mSkipPointer[];         // the start pointer of each skip level
	private int mSkipInterval[];         // skipInterval of each level
	private int[] mNumSkipped;           // number of docs skipped per level
    
	private int[] mSkipDoc;              // doc id of current skip entry per level 
	private int mLastDoc;                // doc id of last read skip entry with docId <= target
	private long[] mChildPointer;        // child pointer of current skip entry per level
	private long mLastChildPointer;      // childPointer of last read skip entry with docId <= target
  
	private boolean mInputIsBuffered;
  
	public MultiLevelSkipListReader(IndexInput skipStream, int maxSkipLevels, int skipInterval) {
		mContext = skipStream.getContext();
		mSkipStream = new IndexInput[maxSkipLevels];
		mSkipPointer = new long[maxSkipLevels];
		mChildPointer = new long[maxSkipLevels];
		mNumSkipped = new int[maxSkipLevels];
		mMaxNumberOfSkipLevels = maxSkipLevels;
		mSkipInterval = new int[maxSkipLevels];
		mSkipStream [0]= skipStream;
		mInputIsBuffered = (skipStream instanceof BufferedIndexInput);
		mSkipDoc = new int[maxSkipLevels];
		mSkipInterval[0] = skipInterval;
		
		for (int i = 1; i < maxSkipLevels; i++) {
			// cache skip intervals
			mSkipInterval[i] = mSkipInterval[i - 1] * skipInterval;
		}
	}

	protected IIndexContext getContext() { 
		return mContext;
	}
  
	/** 
	 * Returns the id of the doc to which the last call of {@link #skipTo(int)}
	 *  has skipped. 
	 */
	public int getDoc() {
		return mLastDoc;
	}
  
	/** 
	 * Skips entries to the first beyond the current whose document number is
	 *  greater than or equal to <i>target</i>. Returns the current doc count. 
	 */
	public int skipTo(int target) throws IOException {
		if (!mHaveSkipped) {
			// first time, load skip levels
			loadSkipLevels();
			mHaveSkipped = true;
		}
  
		// walk up the levels until highest level is found that has a skip
		// for this target
		int level = 0;
		while (level < mNumberOfSkipLevels - 1 && target > mSkipDoc[level + 1]) {
			level ++;
		}    

		while (level >= 0) {
			if (target > mSkipDoc[level]) {
				if (!loadNextSkip(level)) 
					continue;
			} else {
				// no more skips on this level, go down one level
				if (level > 0 && mLastChildPointer > mSkipStream[level - 1].getFilePointer()) 
					seekChild(level - 1);
				
				level--;
			}
		}
    
		return mNumSkipped[0] - mSkipInterval[0] - 1;
	}
  
	private boolean loadNextSkip(int level) throws IOException {
		// we have to skip, the target document is greater than the current
		// skip list entry        
		setLastSkipData(level);
      
		mNumSkipped[level] += mSkipInterval[level];
      
		if (mNumSkipped[level] > mDocCount) {
			// this skip list is exhausted
			mSkipDoc[level] = Integer.MAX_VALUE;
			if (mNumberOfSkipLevels > level) 
				mNumberOfSkipLevels = level; 
			
			return false;
		}

		// read next skip entry
		mSkipDoc[level] += readSkipData(level, mSkipStream[level]);
    
		if (level != 0) {
			// read the child pointer if we are not on the leaf level
			mChildPointer[level] = mSkipStream[level].readVLong() + mSkipPointer[level - 1];
		}
    
		return true;
	}
  
	/** Seeks the skip entry on the given level */
	protected void seekChild(int level) throws IOException {
		mSkipStream[level].seek(mLastChildPointer);
		mNumSkipped[level] = mNumSkipped[level + 1] - mSkipInterval[level + 1];
		mSkipDoc[level] = mLastDoc;
		if (level > 0) {
			mChildPointer[level] = mSkipStream[level].readVLong() + mSkipPointer[level - 1];
		}
	}

	public void close() throws IOException {
		for (int i = 1; i < mSkipStream.length; i++) {
			if (mSkipStream[i] != null) 
				mSkipStream[i].close();
		}
	}

	/** initializes the reader */
	public void init(long skipPointer, int df) {
		mSkipPointer[0] = skipPointer;
		mDocCount = df;
		assert skipPointer >= 0 && skipPointer <= mSkipStream[0].length() 
				: "invalid skip pointer: " + skipPointer + ", length=" + mSkipStream[0].length();
		Arrays.fill(mSkipDoc, 0);
		Arrays.fill(mNumSkipped, 0);
		Arrays.fill(mChildPointer, 0);
    
		mHaveSkipped = false;
		for (int i = 1; i < mNumberOfSkipLevels; i++) {
			mSkipStream[i] = null;
		}
	}
  
	/** Loads the skip levels  */
	private void loadSkipLevels() throws IOException {
		mNumberOfSkipLevels = MathUtil.log(mDocCount, mSkipInterval[0]);
		if (mNumberOfSkipLevels > mMaxNumberOfSkipLevels) 
			mNumberOfSkipLevels = mMaxNumberOfSkipLevels;

		mSkipStream[0].seek(mSkipPointer[0]);
    
		int toBuffer = mNumberOfLevelsToBuffer;
    
		for (int i = mNumberOfSkipLevels - 1; i > 0; i--) {
			// the length of the current level
			long length = mSkipStream[0].readVLong();
      
			// the start pointer of the current level
			mSkipPointer[i] = mSkipStream[0].getFilePointer();
			if (toBuffer > 0) {
				// buffer this level
				mSkipStream[i] = new SkipBuffer(mSkipStream[0], (int) length);
				toBuffer --;
				
			} else {
				// clone this stream, it is already at the start of the current level
				mSkipStream[i] = (IndexInput) mSkipStream[0].clone();
				if (mInputIsBuffered && length < getContext().getInputBufferSize()) 
					((BufferedIndexInput) mSkipStream[i]).setBufferSize((int) length);
        
				// move base stream beyond the current level
				mSkipStream[0].seek(mSkipStream[0].getFilePointer() + length);
			}
		}
   
		// use base stream for the lowest level
		mSkipPointer[0] = mSkipStream[0].getFilePointer();
	}
  
	/**
	 * Subclasses must implement the actual skip data encoding in this method.
	 *  
	 * @param level the level skip data shall be read from
	 * @param skipStream the skip stream to read from
	 */  
	protected abstract int readSkipData(int level, IndexInput skipStream) throws IOException;
  
	/** Copies the values of the last read skip entry on this level */
	protected void setLastSkipData(int level) {
		mLastDoc = mSkipDoc[level];
		mLastChildPointer = mChildPointer[level];
	}
  
	/** used to buffer the top skip levels */
	private final static class SkipBuffer extends IndexInput {
		private byte[] mData;
		private long mPointer;
		private int mPos;
    
		SkipBuffer(IndexInput input, int length) throws IOException {
			super(input.getContext());
			mData = new byte[length];
			mPointer = input.getFilePointer();
			input.readBytes(mData, 0, length);
		}
    
		@Override
		public void close() throws IOException {
			mData = null;
		}

		@Override
		public long getFilePointer() {
			return mPointer + mPos;
		}

		@Override
		public long length() {
			return mData.length;
		}

		@Override
		public byte readByte() throws IOException {
			return mData[mPos++];
		}

		@Override
		public void readBytes(byte[] b, int offset, int len) throws IOException {
			System.arraycopy(mData, mPos, b, offset, len);
			mPos += len;
		}

		@Override
		public void seek(long pos) throws IOException {
			mPos =  (int) (pos - mPointer);
		}
	}
	
}
