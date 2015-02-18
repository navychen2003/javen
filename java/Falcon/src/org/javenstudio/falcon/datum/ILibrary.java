package org.javenstudio.falcon.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.fs.FileSystem;

public interface ILibrary extends IData {

	public DataManager getManager();
	public FileSystem getStoreFs();
	public DataCache getCache();
	public ILockable.Lock getLock();
	
	public String getName();
	public String getHostName();
	public String getOwner();
	
	public String getContentId();
	public String getContentKey();
	public String getContentType();
	
	public int getTotalFolderCount();
	public int getTotalFileCount();
	public long getTotalFileLength();
	
	public int getSectionCount() throws ErrorException;
	public ISectionRoot getSectionAt(int index) throws ErrorException;
	
	public ISectionSet getSections(ISectionQuery query) throws ErrorException;
	public ISection getSection(String key) throws ErrorException;
	
	public String[] getPosters() throws ErrorException;
	public String[] getBackgrounds() throws ErrorException;
	
	public int getMaxEntries();
	
	public long getCreatedTime();
	public long getModifiedTime();
	public long getOptimizedTime();
	public long getIndexedTime();
	public void onIndexed(long time);
	
	public boolean isChanged();
	public boolean isDefault();
	
	public void close();
	public void removeAndClose() throws ErrorException;
	
	public IFolderInfo getFolderInfo();
	
}
