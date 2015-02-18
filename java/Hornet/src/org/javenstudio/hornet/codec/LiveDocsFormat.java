package org.javenstudio.hornet.codec;

import java.io.IOException;
import java.util.Collection;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ILiveDocsFormat;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.MutableBits;

/** Format for live/deleted documents */
public abstract class LiveDocsFormat implements ILiveDocsFormat {

	/** Extension of deletes */
	public static final String DELETES_EXTENSION = "del";
	
	private final IIndexFormat mFormat;
	
	protected LiveDocsFormat(IIndexFormat format) { 
		mFormat = format;
	}
	
	@Override
	public final IIndexContext getContext() { 
		return mFormat.getContext(); 
	}
	
	@Override
	public final IIndexFormat getIndexFormat() { 
		return mFormat; 
	}
	
	//@Override
	//public String getDeletesFileName(String segmentName) { 
	//	return getContext().getSegmentFileName(segmentName, DELETES_EXTENSION);
	//}
	
	@Override
	public String getDeletesFileName(String segmentName, long delGen) { 
		return getContext().getFileNameFromGeneration(segmentName, DELETES_EXTENSION, delGen);
	}
	
	/** Creates a new MutableBits, with all bits set, for the specified size. */
	public abstract MutableBits newLiveDocs(int size) throws IOException;

	/** Creates a new mutablebits of the same bits set and size of existing. */
	public abstract MutableBits newLiveDocs(Bits existing) throws IOException;

	/** Read live docs bits. */
	public abstract Bits readLiveDocs(IDirectory dir, ISegmentCommitInfo info) 
			throws IOException;

	/** 
	 * Persist live docs bits.  Use {@link
	 *  SegmentCommitInfo#getNextDelGen} to determine the
	 *  generation of the deletes file you should write to. 
	 */
	public abstract void writeLiveDocs(IDirectory dir, MutableBits bits, ISegmentCommitInfo info, 
			int newDelCount) throws IOException;

	/** Records all files in use by this {@link SegmentCommitInfo} into the files argument. */
	public abstract void recordFiles(ISegmentCommitInfo info, Collection<String> files) 
			throws IOException;
	
}
