package org.javenstudio.common.indexdb.index;

import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.util.MutableBits;

public class FlushedSegment {
	
	private final ISegmentCommitInfo mSegmentInfo;
	private final IFieldInfos mFieldInfos;
	private final BufferedDeletes mSegmentDeletes;
	private final MutableBits mLiveDocs;
	private final int mDelCount;

    public FlushedSegment(ISegmentCommitInfo segmentInfo, IFieldInfos fieldInfos,
    		BufferedDeletes segmentDeletes, MutableBits liveDocs, int delCount) {
    	mSegmentInfo = segmentInfo;
    	mFieldInfos = fieldInfos;
    	mSegmentDeletes = segmentDeletes;
    	mLiveDocs = liveDocs;
    	mDelCount = delCount;
    }
    
    public final ISegmentCommitInfo getCommitInfo() { return mSegmentInfo; }
    public final IFieldInfos getFieldInfos() { return mFieldInfos; }
    public final BufferedDeletes getSegmentDeletes() { return mSegmentDeletes; }
    public final MutableBits getLiveDocs() { return mLiveDocs; }
    public final int getDelCount() { return mDelCount; }
    
}
