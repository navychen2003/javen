package org.javenstudio.hornet.index;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexWriter;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.index.BufferedDeletes;
import org.javenstudio.common.indexdb.index.CheckAbort;
import org.javenstudio.common.indexdb.index.DeleteQueue;
import org.javenstudio.common.indexdb.index.DeletesStream;
import org.javenstudio.common.indexdb.index.FrozenDeletes;
import org.javenstudio.common.indexdb.index.IndexParams;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.indexdb.index.SegmentMerger;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.index.segment.SegmentReader;

public class AdvancedIndexParams extends IndexParams {

	public AdvancedIndexParams(IAnalyzer analyzer) { 
		super(analyzer);
	}
	
	public AdvancedIndexParams(IAnalyzer analyzer, IIndexContext context) { 
		super(analyzer, context);
	}
	
	@Override
	protected void initParams() {
		super.initParams();
		
		if (mContext == null)
			mContext = AdvancedIndexContext.getOrCreate();
		
		if (mMergePolicy == null)
			mMergePolicy = new TieredMergePolicy();
		
		if (mOpenMode == null)
			mOpenMode = IIndexWriter.OpenMode.CREATE_OR_APPEND;
		
		mReaderPooling = DEFAULT_READER_POOLING;
	}
	
	@Override
	public final DeletesStream newDeletesStream(IndexWriter writer) { 
		return new BufferedDeletesStream();
	}
	
	@Override
	public final DeleteQueue newDeleteQueue(final IndexWriter writer, long generation) { 
		return new DeleteQueue(generation) { 
				@Override
				protected FrozenDeletes newFrozenDeletes(BufferedDeletes deletes, boolean isSegmentPrivate) { 
					return new FrozenBufferedDeletes(writer.getContext(), deletes, isSegmentPrivate);
				}
			};
	}
	
	@Override
	public SegmentMerger newSegmentMerger(IndexWriter writer, 
			ISegmentInfo segmentInfo, IDirectory dir, CheckAbort checkAbort, 
			FieldNumbers globalFieldNumbers) { 
		return new AdvancedMerger(writer, segmentInfo, dir, checkAbort, 
				globalFieldNumbers);
	}
	
	@Override
	public ISegmentReader newSegmentReader(ISegmentReader reader, 
			ISegmentCommitInfo info, Bits liveDocs, int numDocs) throws IOException { 
		return new SegmentReader((SegmentCommitInfo)info, 
				((SegmentReader)reader).getReaders(), liveDocs, numDocs);
	}
	
}
