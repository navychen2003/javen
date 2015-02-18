package org.javenstudio.falcon.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ILockable;

public interface ISectionRoot extends ISection {
	
	public ILibrary getLibrary();
	public ILockable.Lock getLock();
	
	public ISection getSection(String key) throws ErrorException;
	public void listSection(Collector collector) throws ErrorException;
	
	public int getTotalFolderCount();
	public int getTotalFileCount();
	public long getTotalFileLength();
	
	public long getCreatedTime();
	public long getOptimizedTime();
	public long getIndexedTime();
	public void setIndexedTime(long time);
	
}
