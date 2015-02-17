package org.javenstudio.cocoka.storage;

public class FileCache {

	private final StorageFile mFile; 
	private String mKey = null; 
	private long mLoadTime = 0; 
	private byte[] mContent = null; 
	
	public FileCache(StorageFile file) {
		mFile = file; 
	}
	
	public String getKey() { return mKey; } 
	protected void setKey(String s) { mKey = s; } 
	
	public long getLoadTime() { return mLoadTime; } 
	protected void setLoadTime(long time) { mLoadTime = time; } 
	
	public StorageFile getCacheFile() { return mFile; } 
	
	public byte[] getContent() { return mContent; } 
	protected void setContent(byte[] data) { mContent = data; } 
	
	public int getCachedCount() { return 0; }
	public long getCachedSize() { return 0; }
	
	//public void recycle() { }
	
}
