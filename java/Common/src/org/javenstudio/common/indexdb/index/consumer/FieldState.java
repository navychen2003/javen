package org.javenstudio.common.indexdb.index.consumer;

import org.javenstudio.common.indexdb.IFieldState;

/**
 * This class tracks the number and position / offset parameters of terms
 * being added to the index. The information collected in this class is
 * also used to calculate the normalization factor for a field.
 */
public class FieldState implements IFieldState {
	private String mName;
	private int mPosition;
	private int mLength;
	private int mNumOverlap;
	private int mOffset;
	private int mMaxTermFrequency;
	private int mUniqueTermCount;
	private float mBoost;

  	public FieldState(String name) {
  		mName = name;
  	}
  
  	public FieldState(String name, int position, int length, 
  			int numOverlap, int offset, float boost) {
  		mName = name;
  		mPosition = position;
  		mLength = length;
  		mNumOverlap = numOverlap;
  		mOffset = offset;
  		mBoost = boost;
  	}

  	@Override
  	public String toString() { 
  		StringBuilder sbuf = new StringBuilder();
  		sbuf.append(getClass().getSimpleName());
  		sbuf.append('{');
  		sbuf.append("name=").append(mName);
  		sbuf.append(",position=").append(mPosition);
  		sbuf.append(",length=").append(mLength);
  		sbuf.append(",numOverlap=").append(mNumOverlap);
  		sbuf.append(",offset=").append(mOffset);
  		sbuf.append(",maxTermFrequency=").append(mMaxTermFrequency);
  		sbuf.append(",uniqueTermCount=").append(mUniqueTermCount);
  		sbuf.append(",boost=").append(mBoost);
  		sbuf.append('}');
  		return sbuf.toString();
  	}
  	
  	/**
  	 * Re-initialize the state
  	 */
  	final void reset() {
  		mPosition = 0;
  		mLength = 0;
  		mNumOverlap = 0;
  		mOffset = 0;
  		mMaxTermFrequency = 0;
  		mUniqueTermCount = 0;
  		mBoost = 1.0f;
  		//mToken = null;
  	}

  	/**
  	 * Get the last processed term position.
  	 * @return the position
  	 */
  	public int getPosition() {
  		return mPosition;
  	}

  	final void setPosition(int position) { 
  		mPosition = position;
  	}
  	
  	final void increasePosition(int count) { 
  		mPosition += count;
  	}
  	
  	/**
  	 * Get total number of terms in this field.
  	 * @return the length
  	 */
  	public int getLength() {
  		return mLength;
  	}

  	final void setLength(int length) {
  		mLength = length;
  	}
  
  	final void increaseLength(int count) { 
  		mLength += count;
  	}
  	
  	/**
  	 * Get the number of terms with <code>positionIncrement == 0</code>.
  	 * @return the numOverlap
  	 */
  	public int getNumOverlap() {
  		return mNumOverlap;
  	}

  	final void setNumOverlap(int numOverlap) {
  		mNumOverlap = numOverlap;
  	}
  
  	final void increaseNumOverlap(int count) { 
  		mNumOverlap += count;
  	}
  	
  	/**
  	 * Get end offset of the last processed term.
  	 * @return the offset
  	 */
  	public int getOffset() {
  		return mOffset;
  	}

  	final void increaseOffset(int count) { 
  		mOffset += count;
  	}
  	
  	/**
  	 * Get boost value. This is the cumulative product of
  	 * document boost and field boost for all field instances
  	 * sharing the same field name.
  	 * @return the boost
  	 */
  	public float getBoost() {
  		return mBoost;
  	}
  
  	final void setBoost(float boost) {
  		mBoost = boost;
  	}

  	/**
  	 * Get the maximum term-frequency encountered for any term in the field.  A
  	 * field containing "the quick brown fox jumps over the lazy dog" would have
  	 * a value of 2, because "the" appears twice.
  	 */
  	public int getMaxTermFrequency() {
  		return mMaxTermFrequency;
  	}
  
  	final void setMaxTermFrequency(int val) { 
  		mMaxTermFrequency = val;
  	}
  	
  	/**
  	 * Return the number of unique terms encountered in this field.
  	 */
  	public int getUniqueTermCount() {
  		return mUniqueTermCount;
  	}
  
  	final void increaseUniqueTermCount(int count) { 
  		mUniqueTermCount += count;
  	}
  	
  	/**
  	 * Return the field's name
  	 */
  	public String getName() {
  		return mName;
  	}
  	
}
