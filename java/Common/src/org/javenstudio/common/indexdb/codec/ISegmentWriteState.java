package org.javenstudio.common.indexdb.codec;

import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.util.MutableBits;

public interface ISegmentWriteState {

	public String getSegmentSuffix();
	
	public ISegmentInfo getSegmentInfo();
	public IFieldInfos getFieldInfos();
	
	public MutableBits getLiveDocs();
	public void setLiveDocs(MutableBits liveDocs);
	
	public void increaseDelCountOnFlush(int count);
	
}
