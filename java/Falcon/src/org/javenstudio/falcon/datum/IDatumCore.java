package org.javenstudio.falcon.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.cache.MemCache;
import org.javenstudio.falcon.datum.util.BytesBufferPool;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;

public interface IDatumCore {

	public ILibraryStore getLibraryStore();
	public IDatabaseStore getDatabaseStore();
	
	public BytesBufferPool getBufferPool();
	public MemCache getCache();
	
	public Configuration getConfiguration();
	public MetadataLoader getMetadataLoader();
	
	//public String getStoreUri(String uri) throws ErrorException;
	public FileSystem getStoreFs(String uri) throws ErrorException;
	public StoreInfo[] getStoreInfos() throws ErrorException;
	public long getTotalUsableSpace();
	
	public String getStoreDir();
	public String getCacheDir();
	
	//public String getHostName();
	public String getFriendlyName();
	
}
