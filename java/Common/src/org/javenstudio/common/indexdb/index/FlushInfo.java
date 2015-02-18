package org.javenstudio.common.indexdb.index;

/**
 * <p>A FlushInfo provides information required for a FLUSH context.
 *  It is used as part of an {@link IOContext} in case of FLUSH context.</p>
 */
public class FlushInfo {

	private final int mNumDocs;
	private final long mEstimatedSegmentSize;
  
	/**
	 * <p>Creates a new {@link FlushInfo} instance from
	 * the values required for a FLUSH {@link IOContext} context.
	 * 
	 * These values are only estimates and are not the actual values.
	 * 
	 */
	public FlushInfo(int numDocs, long estimatedSegmentSize) {
		mNumDocs = numDocs;
		mEstimatedSegmentSize = estimatedSegmentSize;
	}

	public final int getNumDocs() { return mNumDocs; }
	public final long getEstimatedSegmentSize() { return mEstimatedSegmentSize; }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mEstimatedSegmentSize ^ (mEstimatedSegmentSize >>> 32));
		result = prime * result + mNumDocs;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass())
			return false;
		
		FlushInfo other = (FlushInfo) obj;
		if (mEstimatedSegmentSize != other.mEstimatedSegmentSize)
			return false;
		if (mNumDocs != other.mNumDocs)
			return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "FlushInfo{numDocs=" + mNumDocs + ", estimatedSegmentSize="
				+ mEstimatedSegmentSize + "}";
	}

}
