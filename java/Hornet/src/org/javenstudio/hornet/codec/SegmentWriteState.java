package org.javenstudio.hornet.codec;

import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.BufferedDeletes;
import org.javenstudio.common.indexdb.util.MutableBits;

public class SegmentWriteState implements ISegmentWriteState {

	private final ISegmentInfo mSegmentInfo;
	private final IFieldInfos mFieldInfos;
	private final String mSegmentSuffix;
	
	private int mDelCountOnFlush = 0;
	
	// Deletes to apply while we are flushing the segment.  A
	// Term is enrolled in here if it was deleted at one
	// point, and it's mapped to the docIDUpto, meaning any
	// docID < docIDUpto containing this term should be
	// deleted.
	private final BufferedDeletes mSegmentDeletes;

	// Lazily created:
	private MutableBits mLiveDocs;
	
	public SegmentWriteState(ISegmentInfo segmentInfo, IFieldInfos fieldInfos, 
			BufferedDeletes segDeletes) { 
		mSegmentInfo = segmentInfo;
		mFieldInfos = fieldInfos;
		mSegmentDeletes = segDeletes;
		mSegmentSuffix = "";
	}
	
	public SegmentWriteState(SegmentWriteState state, String segmentSuffix) { 
		mSegmentInfo = state.mSegmentInfo;
		mFieldInfos = state.mFieldInfos;
		mSegmentDeletes = state.mSegmentDeletes;
		mSegmentSuffix = segmentSuffix;
	}
	
	@Override
	public final ISegmentInfo getSegmentInfo() { 
		return mSegmentInfo; 
	}
	
	@Override
	public final String getSegmentSuffix() { 
		return mSegmentSuffix; 
	}
	
	@Override
	public final IFieldInfos getFieldInfos() { 
		return mFieldInfos; 
	}
	
	public final int getDelCountOnFlush() { 
		return mDelCountOnFlush; 
	}
	
	public void setDelCountOnFlush(int count) { 
		mDelCountOnFlush = count; 
	}
	
	@Override
	public void increaseDelCountOnFlush(int count) { 
		mDelCountOnFlush += count;
	}
	
	@Override
	public final MutableBits getLiveDocs() { 
		return mLiveDocs; 
	}
	
	@Override
	public void setLiveDocs(MutableBits liveDocs) { 
		mLiveDocs = liveDocs; 
	}
	
}
