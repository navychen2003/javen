package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Collection;

public interface ISegmentCommitInfo extends Cloneable {

	public ISegmentInfo getSegmentInfo();
	
	public void advanceDelGen();
	public void setDelGen(long delGen);
	public void clearDelGen();
	
	public long getSizeInBytes() throws IOException;
	public long getBufferedDeletesGen();
	public void setBufferedDeletesGen(long v);
	
	public boolean hasDeletions();
	public long getNextDelGen();
	public long getDelGen();
	
	public void setDelCount(int delCount);
	public int getDelCount();
	
	public Collection<String> getFileNames() throws IOException;
	
	public ISegmentCommitInfo clone();
	public String toString(IDirectory dir, int pendingDelCount);
	
}
