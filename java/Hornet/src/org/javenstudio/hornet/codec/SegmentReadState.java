package org.javenstudio.hornet.codec;

import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.ISegmentReadState;

/**
 * Holder class for common parameters used during read.
 */
public class SegmentReadState implements ISegmentReadState {
	
	private final ISegmentInfo mSegmentInfo;
	private final IFieldInfos mFieldInfos;
	private final String mSegmentSuffix;

	public SegmentReadState(ISegmentInfo info, IFieldInfos fieldInfos) {
		this(info, fieldInfos, "");
	}
  
	private SegmentReadState(ISegmentInfo info, IFieldInfos fieldInfos, String segmentSuffix) {
		mSegmentInfo = info;
		mFieldInfos = fieldInfos;
		mSegmentSuffix = segmentSuffix;
	}

	public SegmentReadState(SegmentReadState other, String newSegmentSuffix) {
		mSegmentInfo = other.mSegmentInfo;
		mFieldInfos = other.mFieldInfos;
		mSegmentSuffix = newSegmentSuffix;
	}
	
	@Override
	public final String getSegmentSuffix() { 
		return mSegmentSuffix; 
	}
	
	@Override
	public final ISegmentInfo getSegmentInfo() { 
		return mSegmentInfo; 
	}
	
	@Override
	public final IFieldInfos getFieldInfos() { 
		return mFieldInfos; 
	}
	
}
