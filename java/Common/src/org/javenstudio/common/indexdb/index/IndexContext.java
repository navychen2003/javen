package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IFixedBitSet;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.util.Logger;

public abstract class IndexContext implements IIndexContext {
	static final Logger LOG = Logger.getLogger(IndexContext.class);

	protected static IndexContext sInstance = null;
	protected static Object sLock = new Object();
	
	public static IndexContext getInstance() { 
		synchronized (sLock) { 
			if (sInstance == null) 
				throw new RuntimeException("IndexContext not created");
			
			return sInstance;
		}
	}
	
	protected IIndexFormat mIndexFormat;
	protected MergeInfo mMergeInfo = null;
	protected FlushInfo mFlushInfo = null;
	
	protected IndexContext() {
		synchronized (sLock) { 
			if (sInstance != null) {
				throw new RuntimeException("IndexContext already created: " 
						+ sInstance.getClass().getName());
			}
			sInstance = this;
		}
	}
	
	@Override
	public final IIndexFormat getIndexFormat() { 
		if (mIndexFormat == null) throw new NullPointerException();
		return mIndexFormat;
	}
	
	public MergeInfo getMergeInfo() { return mMergeInfo; }
	public FlushInfo getFlushInfo() { return mFlushInfo; }
	
	public void setMergeInfo(MergeInfo info) { 
		mMergeInfo = info; 
		
		if (info != null && LOG.isDebugEnabled())
			LOG.debug("setMergeInfo: " + info);
	}
	
	public void setFlushInfo(FlushInfo info) { 
		mFlushInfo = info; 
		
		if (info != null && LOG.isDebugEnabled())
			LOG.debug("setFlushInfo: " + info);
	}
	
	public abstract IFixedBitSet newFixedBitSet(int numBits);
	
	public abstract ISegmentReader newSegmentReader(ISegmentCommitInfo si) 
			throws IOException;
	
	public abstract ISegmentReader newSegmentReader(ISegmentReader reader, 
			Bits liveDocs, int numDocs) throws IOException;
	
	@Override
	public int findReaderIndex(int n, List<IAtomicReaderRef> leaves) { 
		return ReaderUtil.subIndex(n, leaves);
	}
	
	@Override
	public String getFileNameFromGeneration(String base, String ext, long gen) { 
		return IndexFileNames.getFileNameFromGeneration(base, ext, gen);
	}
	
	@Override
	public String getSegmentFileName(String segmentName, String ext) { 
		return IndexFileNames.getSegmentFileName(segmentName, ext);
	}
	
	@Override
	public String getSegmentFileName(String segmentName, String segmentSuffix, String ext) { 
		return IndexFileNames.getSegmentFileName(segmentName, segmentSuffix, ext);
	}
	
	@Override
	public String getCompoundFileName(String name) { 
		return IndexFileNames.getCompoundFileName(name);
	}
	
	@Override
	public String getCompoundEntriesFileName(String name) { 
		return IndexFileNames.getCompoundEntriesFileName(name);
	}
	
	@Override
	public String stripSegmentName(String filename) { 
		return IndexFileNames.stripSegmentName(filename);
	}
	
	@Override
	public String parseSegmentName(String filename) { 
		return IndexFileNames.parseSegmentName(filename);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
	
}
