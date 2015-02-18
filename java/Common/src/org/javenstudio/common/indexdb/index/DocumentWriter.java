package org.javenstudio.common.indexdb.index;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.util.BytePool;
import org.javenstudio.common.indexdb.util.Counter;
import org.javenstudio.common.indexdb.util.IntPool;

public abstract class DocumentWriter {

	public abstract IIndexContext getContext();
	public abstract IDirectory getDirectory();
	public abstract SegmentWriter getSegmentWriter();
	public abstract DocState getDocState();
	
	public abstract Counter getBytesUsedCounter();
	public abstract BytePool.Allocator getByteAllocator();
	public abstract IntPool.Allocator getIntAllocator();
	
	public abstract ISegmentInfo getSegmentInfo();
	public abstract DeleteQueue getDeleteQueue();
	
	public abstract int getNumDocsInRAM();
	public abstract long getBytesUsed();
	
	public abstract boolean checkAndResetHasAborted();
	
	public abstract void initialize();
	public abstract void abort();
	
	public abstract FrozenDeletes prepareFlush();
	public abstract FlushedSegment flush() throws IOException;
	
	public abstract int updateDocuments(Iterable<? extends IDocument> docs, IAnalyzer analyzer, 
			ITerm delTerm) throws IOException;
	
	public abstract void updateDocument(IDocument doc, IAnalyzer analyzer, 
			ITerm delTerm) throws IOException;
	
}
