package org.javenstudio.common.indexdb.index.segment;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.IIndexFormat;

/** 
 * Embeds a [read-only] SegmentInfo and adds per-commit
 *  fields.
 */
public class SegmentCommitInfo implements ISegmentCommitInfo {

	private final IIndexFormat mFormat;
	private final ISegmentInfo mInfo;

	// How many deleted docs in the segment:
	private int mDelCount;

	// Generation number of the live docs file (-1 if there
	// are no deletes yet):
	private long mDelGen;

	// NOTE: only used in-RAM by IW to track buffered deletes;
	// this is never written to/read from the Directory
	private long mBufferedDeletesGen;
	
	private volatile long mSizeInBytes = -1;

	public SegmentCommitInfo(IIndexFormat format, 
			ISegmentInfo info, int delCount, long delGen) {
		mFormat = format;
		mInfo = info;
		mDelCount = delCount;
		mDelGen = delGen;
	}

	@Override
	public final ISegmentInfo getSegmentInfo() { 
		return mInfo;
	}
	
	@Override
	public void advanceDelGen() {
		if (mDelGen == -1) 
			mDelGen = 1;
		else 
			mDelGen++;
		
		mSizeInBytes = -1;
	}

	@Override
	public long getSizeInBytes() throws IOException {
		if (mSizeInBytes == -1) {
			final Collection<String> files = new HashSet<String>();
			mFormat.getLiveDocsFormat().recordFiles(this, files);
			
			long sum = mInfo.getSizeInBytes();
			for (final String fileName : getFileNames()) {
				sum += mInfo.getDirectory().getFileLength(fileName);
			}
			mSizeInBytes = sum;
		}

		return mSizeInBytes;
	}

	@Override
	public Collection<String> getFileNames() throws IOException {
		Collection<String> files = new HashSet<String>(mInfo.getFileNames());

		// Must separately add any live docs files:
		mFormat.getLiveDocsFormat().recordFiles(this, files);

		return files;
	}

	@Override
	public long getBufferedDeletesGen() {
		return mBufferedDeletesGen;
	}

	@Override
	public void setBufferedDeletesGen(long v) {
		mBufferedDeletesGen = v;
		mSizeInBytes =  -1;
	}
  
	@Override
	public void clearDelGen() {
		mDelGen = -1;
		mSizeInBytes =  -1;
	}

	@Override
	public void setDelGen(long delGen) {
		mDelGen = delGen;
		mSizeInBytes =  -1;
	}

	@Override
	public boolean hasDeletions() {
		return mDelGen != -1;
	}

	@Override
	public long getNextDelGen() {
		if (mDelGen == -1) 
			return 1;
		else 
			return mDelGen + 1;
	}

	@Override
	public long getDelGen() {
		return mDelGen;
	}
  
	@Override
	public int getDelCount() {
		return mDelCount;
	}

	@Override
	public void setDelCount(int delCount) {
		mDelCount = delCount;
		assert delCount <= mInfo.getDocCount();
	}

	@Override
	public String toString(IDirectory dir, int pendingDelCount) {
		return mInfo.toString(dir, mDelCount + pendingDelCount);
	}

	@Override
	public String toString() {
		String s = mInfo.toString(mInfo.getDirectory(), mDelCount);
		if (mDelGen != -1) 
			s += ":delGen=" + mDelGen;
		
		return s;
	}

	@Override
	public SegmentCommitInfo clone() {
		return new SegmentCommitInfo(mFormat, mInfo, mDelCount, mDelGen);
	}
  
}
