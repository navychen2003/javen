package org.javenstudio.common.indexdb.index;

/**
 * <p>A MergeInfo provides information required for a MERGE context.
 *  It is used as part of an {@link IOContext} in case of MERGE context.</p>
 */
public class MergeInfo {
  
	private final int mTotalDocCount;
	private final long mEstimatedMergeBytes;
	private final boolean mIsExternal;
	private final int mMergeMaxNumSegments;

	/**
	 * <p>Creates a new {@link MergeInfo} instance from
	 * the values required for a MERGE {@link IOContext} context.
	 * 
	 * These values are only estimates and are not the actual values.
	 * 
	 */
	public MergeInfo(int totalDocCount, long estimatedMergeBytes, 
			boolean isExternal, int mergeMaxNumSegments) {
		mTotalDocCount = totalDocCount;
		mEstimatedMergeBytes = estimatedMergeBytes;
		mIsExternal = isExternal;
		mMergeMaxNumSegments = mergeMaxNumSegments;
	}

	public final int getTotalDocCount() { return mTotalDocCount; }
	public final long getEstimatedMergeBytes() { return mEstimatedMergeBytes; }
	public final boolean isExternal() { return mIsExternal; }
	public final int getMergeMaxNumSegments() { return mMergeMaxNumSegments; }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mEstimatedMergeBytes ^ (mEstimatedMergeBytes >>> 32));
		result = prime * result + (mIsExternal ? 1231 : 1237);
		result = prime * result + mMergeMaxNumSegments;
		result = prime * result + mTotalDocCount;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		MergeInfo other = (MergeInfo) obj;
		if (mEstimatedMergeBytes != other.mEstimatedMergeBytes)
			return false;
		if (mIsExternal != other.mIsExternal)
			return false;
		if (mMergeMaxNumSegments != other.mMergeMaxNumSegments)
			return false;
		if (mTotalDocCount != other.mTotalDocCount)
			return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "MergeInfo{totalDocCount=" + mTotalDocCount
				+ ", estimatedMergeBytes=" + mEstimatedMergeBytes + ", isExternal="
				+ mIsExternal + ", mergeMaxNumSegments=" + mMergeMaxNumSegments + "}";
	}
	
}
