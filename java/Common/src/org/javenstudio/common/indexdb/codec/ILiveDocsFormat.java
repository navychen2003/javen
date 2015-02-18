package org.javenstudio.common.indexdb.codec;

import java.io.IOException;
import java.util.Collection;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.MutableBits;

/** Format for live/deleted documents */
public interface ILiveDocsFormat {

	public IIndexContext getContext();
	public IIndexFormat getIndexFormat();
	
	//public String getDeletesFileName(String segmentName);
	public String getDeletesFileName(String segmentName, long delGen);
	
	/** Creates a new MutableBits, with all bits set, for the specified size. */
	public MutableBits newLiveDocs(int size) throws IOException;

	/** Creates a new mutablebits of the same bits set and size of existing. */
	public MutableBits newLiveDocs(Bits existing) throws IOException;

	/** Read live docs bits. */
	public Bits readLiveDocs(IDirectory dir, ISegmentCommitInfo info) 
			throws IOException;
	
	/** 
	 * Persist live docs bits.  Use {@link
	 *  SegmentCommitInfo#getNextDelGen} to determine the
	 *  generation of the deletes file you should write to. 
	 */
	public void writeLiveDocs(IDirectory dir, MutableBits bits, ISegmentCommitInfo info, 
			int newDelCount) throws IOException;

	/** Records all files in use by this {@link SegmentCommitInfo} into the files argument. */
	public void recordFiles(ISegmentCommitInfo info, Collection<String> files) 
			throws IOException;
	
}
