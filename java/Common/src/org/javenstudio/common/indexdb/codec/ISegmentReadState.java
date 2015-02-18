package org.javenstudio.common.indexdb.codec;

import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;

public interface ISegmentReadState {

	public String getSegmentSuffix();
	
	public ISegmentInfo getSegmentInfo();
	public IFieldInfos getFieldInfos();
	
}
