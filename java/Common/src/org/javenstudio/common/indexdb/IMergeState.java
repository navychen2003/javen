package org.javenstudio.common.indexdb;

public interface IMergeState {

	public ISegmentInfo getSegmentInfo();
	public IFieldInfos getFieldInfos();
	
	public int getReaderCount();
	public IAtomicReader getReaderAt(int index);
	public ISegmentReader getMatchingSegmentReaderAt(int index);
	
	public IFieldInfo getFieldInfo();
	public void setFieldInfo(IFieldInfo info);
	
	public IPayloadProcessor.Provider getPayloadProcessorProvider();
	public IPayloadProcessor.Reader getPayloadProcessorReaderAt(int index);
	
	public IPayloadProcessor getCurrentPayloadProcessorAt(int index);
	public void setCurrentPayloadProcessorAt(int index, IPayloadProcessor processor);
	
	public void checkAbort(double units) throws MergeAbortedException;
	
}
