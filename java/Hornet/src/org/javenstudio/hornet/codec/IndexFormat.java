package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IFieldInfosFormat;
import org.javenstudio.common.indexdb.codec.IFieldsFormat;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ILiveDocsFormat;
import org.javenstudio.common.indexdb.codec.IPostingsFormat;
import org.javenstudio.common.indexdb.codec.ISegmentInfoFormat;
import org.javenstudio.common.indexdb.codec.ISegmentInfosFormat;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;
import org.javenstudio.hornet.index.segment.DirectoryReader;

public abstract class IndexFormat implements IIndexFormat {
	//private static final Logger LOG = Logger.getLogger(IndexFormat.class);

	private final IIndexContext mContext;
	
	private ISegmentInfosFormat mSegmentInfosFormat = null;
	private ISegmentInfoFormat mSegmentInfoFormat = null;
	private ILiveDocsFormat mLiveDocsFormat = null;
	private IFieldInfosFormat mFieldInfosFormat = null;
	private IFieldsFormat mFieldsFormat = null;
	private IPostingsFormat mPostingsFormat = null;
	private ITermVectorsFormat mTermVectorsFormat = null;
	
	protected IndexFormat(IIndexContext context) { 
		mContext = context;
	}
	
	@Override
	public final IIndexContext getContext() { 
		return mContext; 
	}
	
	@Override
	public String getCompoundFileName(String segmentName) { 
		return mContext.getCompoundFileName(segmentName);
	}
	
	protected abstract ISegmentInfosFormat createSegmentInfosFormat();
	protected abstract ISegmentInfoFormat createSegmentInfoFormat();
	protected abstract ILiveDocsFormat createLiveDocsFormat();
	protected abstract IFieldInfosFormat createFieldInfosFormat();
	protected abstract IFieldsFormat createFieldsFormat();
	protected abstract IPostingsFormat createPostingsFormat();
	protected abstract ITermVectorsFormat createTermVectorsFormat();
	
	@Override
	public final ISegmentInfosFormat getSegmentInfosFormat() { 
		synchronized (this) { 
			if (mSegmentInfosFormat == null)
				mSegmentInfosFormat = createSegmentInfosFormat();
			return mSegmentInfosFormat;
		}
	}
	
	@Override
	public final ISegmentInfoFormat getSegmentInfoFormat() { 
		synchronized (this) { 
			if (mSegmentInfoFormat == null)
				mSegmentInfoFormat = createSegmentInfoFormat();
			return mSegmentInfoFormat;
		}
	}
	
	@Override
	public final ILiveDocsFormat getLiveDocsFormat() { 
		synchronized (this) { 
			if (mLiveDocsFormat == null)
				mLiveDocsFormat = createLiveDocsFormat();
			return mLiveDocsFormat;
		}
	}
	
	@Override
	public final IFieldInfosFormat getFieldInfosFormat() { 
		synchronized (this) { 
			if (mFieldInfosFormat == null)
				mFieldInfosFormat = createFieldInfosFormat();
			return mFieldInfosFormat;
		}
	}
	
	@Override
	public final IFieldsFormat getFieldsFormat() { 
		synchronized (this) { 
			if (mFieldsFormat == null)
				mFieldsFormat = createFieldsFormat();
			return mFieldsFormat;
		}
	}
	
	@Override
	public final IPostingsFormat getPostingsFormat() { 
		synchronized (this) { 
			if (mPostingsFormat == null)
				mPostingsFormat = createPostingsFormat();
			return mPostingsFormat;
		}
	}
	
	@Override
	public final ITermVectorsFormat getTermVectorsFormat() { 
		synchronized (this) { 
			if (mTermVectorsFormat == null)
				mTermVectorsFormat = createTermVectorsFormat();
			return mTermVectorsFormat;
		}
	}
	
	@Override
	public boolean existsIndex(IDirectory dir) throws IOException { 
		return DirectoryReader.indexExists(this, dir);
	}
	
}
