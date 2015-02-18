package org.javenstudio.common.indexdb.store;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.store.ram.RAMOutputStream;
import org.javenstudio.common.indexdb.util.MathUtil;

/**
 * This abstract class writes skip lists with multiple levels.
 * 
 * Example for skipInterval = 3:
 *                                                     c            (skip level 2)
 *                 c                 c                 c            (skip level 1) 
 *     x     x     x     x     x     x     x     x     x     x      (skip level 0)
 * d d d d d d d d d d d d d d d d d d d d d d d d d d d d d d d d  (posting list)
 *     3     6     9     12    15    18    21    24    27    30     (df)
 * 
 * d - document
 * x - skip data
 * c - skip data with child pointer
 * 
 * Skip level i contains every skipInterval-th entry from skip level i-1.
 * Therefore the number of entries on level i is: floor(df / ((skipInterval ^ (i + 1))).
 * 
 * Each skip entry on a level i>0 contains a pointer to the corresponding skip entry in list i-1.
 * This guarantees a logarithmic amount of skips to find the target document.
 * 
 * While this class takes care of writing the different skip levels,
 * subclasses must define the actual format of the skip data.
 * 
 */
public abstract class MultiLevelSkipListWriter {
	private final IIndexContext mContext; 
  
	// number of levels in this skip list
	private int mNumberOfSkipLevels;
  
	// the skip interval in the list with level = 0
	private int mSkipInterval;
  
	// for every skip level a different buffer is used 
	private RAMOutputStream[] mSkipBuffer;

	protected MultiLevelSkipListWriter(IIndexContext context, int skipInterval, int maxSkipLevels, int df) {
		mContext = context;
		mSkipInterval = skipInterval;
    
		// calculate the maximum number of skip levels for this document frequency
		mNumberOfSkipLevels = MathUtil.log(df, skipInterval);
    
		// make sure it does not exceed maxSkipLevels
		if (mNumberOfSkipLevels > maxSkipLevels) 
			mNumberOfSkipLevels = maxSkipLevels;
	}
  
	protected void init() {
		mSkipBuffer = new RAMOutputStream[mNumberOfSkipLevels];
		for (int i = 0; i < mNumberOfSkipLevels; i++) {
			mSkipBuffer[i] = new RAMOutputStream(mContext);
		}
	}

	protected void resetSkip() {
		// creates new buffers or empties the existing ones
		if (mSkipBuffer == null) {
			init();
		} else {
			for (int i = 0; i < mSkipBuffer.length; i++) {
				mSkipBuffer[i].reset();
			}
		}      
	}

	/**
	 * Subclasses must implement the actual skip data encoding in this method.
	 *  
	 * @param level the level skip data shall be writing for
	 * @param skipBuffer the skip buffer to write to
	 */
	protected abstract void writeSkipData(int level, IIndexOutput skipBuffer) throws IOException;
  
	/**
	 * Writes the current skip data to the buffers. The current document frequency determines
	 * the max level is skip data is to be written to. 
	 * 
	 * @param df the current document frequency 
	 * @throws IOException
	 */
	public void bufferSkip(int df) throws IOException {
		long childPointer = 0;
		int numLevels;
   
		// determine max level
		for (numLevels = 0; (df % mSkipInterval) == 0 && numLevels < mNumberOfSkipLevels; 
			df /= mSkipInterval) {
			numLevels++;
		}
    
		for (int level = 0; level < numLevels; level++) {
			writeSkipData(level, mSkipBuffer[level]);
      
			long newChildPointer = mSkipBuffer[level].getFilePointer();
      
			if (level != 0) {
				// store child pointers for all levels except the lowest
				mSkipBuffer[level].writeVLong(childPointer);
			}
      
			//remember the childPointer for the next level
			childPointer = newChildPointer;
		}
	}

	/**
	 * Writes the buffered skip lists to the given output.
	 * 
	 * @param output the IndexOutput the skip lists shall be written to 
	 * @return the pointer the skip list starts
	 */
	public long writeSkip(IIndexOutput output) throws IOException {
		long skipPointer = output.getFilePointer();
		
		if (mSkipBuffer == null || mSkipBuffer.length == 0) 
			return skipPointer;
    
		for (int level = mNumberOfSkipLevels - 1; level > 0; level--) {
			long length = mSkipBuffer[level].getFilePointer();
			if (length > 0) {
				output.writeVLong(length);
				mSkipBuffer[level].writeTo(output);
			}
		}
		mSkipBuffer[0].writeTo(output);
    
		return skipPointer;
	}

}
