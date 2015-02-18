package org.javenstudio.common.indexdb.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;

public interface IIndexFormat {

	public IIndexContext getContext();
	public String getCompoundFileName(String segmentName);
	
	public ISegmentInfosFormat getSegmentInfosFormat();
	public ISegmentInfoFormat getSegmentInfoFormat();
	public ILiveDocsFormat getLiveDocsFormat();
	
	public IFieldInfosFormat getFieldInfosFormat();
	public IFieldsFormat getFieldsFormat();
	
	public IPostingsFormat getPostingsFormat();
	public ITermVectorsFormat getTermVectorsFormat();
	
	public boolean existsIndex(IDirectory dir) throws IOException;
	
}
