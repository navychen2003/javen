package org.javenstudio.lightning.core.datum;

import java.util.Date;
import java.util.HashMap;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabaseStore;
import org.javenstudio.falcon.datum.IDatumCore;
import org.javenstudio.falcon.datum.ILibraryStore;
import org.javenstudio.falcon.datum.MetadataLoader;
import org.javenstudio.falcon.datum.StoreInfo;
import org.javenstudio.falcon.datum.cache.MemCache;
import org.javenstudio.falcon.datum.util.BytesBufferPool;
import org.javenstudio.falcon.search.dataimport.ImportContext;
import org.javenstudio.falcon.search.dataimport.ImportProcessor;
import org.javenstudio.falcon.util.AdminParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.NumberUtils;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreFactory;
import org.javenstudio.lightning.core.CoreStore;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;

public class DatumCore extends Core implements IDatumCore, 
		ImportProcessor.Factory {

    private static final int BYTESBUFFE_POOL_SIZE = 100;
    private static final int BYTESBUFFER_SIZE = 100 * 1024;
	
    private final BytesBufferPool mBufferPool =
            new BytesBufferPool(BYTESBUFFE_POOL_SIZE, BYTESBUFFER_SIZE);
	
	private final DatumStore mStore;
	private final MetadataLoader mMetadataLoader;
	private final MemCache mCache;
	
	public DatumCore(CoreFactory factory, String dataDir, 
			DatumConfig config, DatumDescriptor cd, DatumCore prev) 
			throws ErrorException {
		super(factory, dataDir, config, cd, prev);
		
		mMetadataLoader = new DatumMetadataLoader();
		mStore = new DatumStore(this);
		
		mCache = config.getCacheConfig().newInstance();
		registerInfoMBean(mCache);
		
		onInited();
	}

	@Override
	public BytesBufferPool getBufferPool() { 
		return mBufferPool; 
	}
	
	@Override
	public ILibraryStore getLibraryStore() { 
		return mStore;
	}
	
	@Override
	public IDatabaseStore getDatabaseStore() { 
		return mStore;
	}
	
	@Override
	public String getCacheDir() { 
		return getDataDir();
	}
	
	@Override
	public String getStoreDir() { 
		return getLocalStoreDir();
	}
	
	public CoreStore getUserStore() {
		return getDescriptor().getContainer()
				.getContainers().getUserStore();
	}
	
	@Override
	public long getTotalUsableSpace() {
		return getUserStore().getTotalUsableSpace();
	}
	
	@Override
	public StoreInfo[] getStoreInfos() throws ErrorException { 
		return getUserStore().getStoreInfos();
	}
	
	//@Override
	//public String getStoreUri(String uri) throws ErrorException { 
	//	return getUserStore().getStoreUri(uri);
	//}
	
	@Override
	public FileSystem getStoreFs(String uri) throws ErrorException { 
		return getUserStore().getStoreFs(uri);
	}
	
	@Override
	public MetadataLoader getMetadataLoader() { 
		return mMetadataLoader;
	}
	
	@Override
	public MemCache getCache() { 
		return mCache;
	}
	
	@Override
	public Configuration getConfiguration() { 
		return getDescriptor().getContainer().getContainers().getConfiguration();
	}
	
	@Override
	public NamedList<Object> getParsedResponse(Request req, Response rsp)
			throws ErrorException {
		return null;
	}
	
	@Override
	protected synchronized void onClose() { 
		MemCache cache = mCache;
		if (cache != null) 
			cache.close();
		
		super.onClose();
	}
	
	@Override
	public ImportProcessor[] createProcessors(ImportContext context) 
			throws ErrorException { 
		return new ImportProcessor[] { 
				//new DatumImporter(this, context, DatumImporter.DATUM_NAME)
			};
	}
	
	@Override
	public void getCoreStatus(NamedList<Object> info, Params params) 
			throws ErrorException { 
		super.getCoreStatus(info, params);
		
		String idxInfo = params.get(AdminParams.INDEX_INFO);
	    boolean isIndexInfoNeeded = Boolean.parseBoolean(idxInfo == null ? "true" : idxInfo);
		
		info.add("schema", "null");
		
		if (isIndexInfoNeeded) {
			NamedMap<Object> indexInfo = getIndexInfo();
			long size = 0;
			
			indexInfo.add("sizeInBytes", size);
			indexInfo.add("size", NumberUtils.readableSize(size));
			info.add("index", indexInfo);
		}
	}
	
	@Override
	public void getCoreInfo(NamedList<Object> info) throws ErrorException { 
		super.getCoreInfo(info);
		
		info.add("schema", "null");
	}
	
	@Override
	public void getDirectoryInfo(NamedList<Object> info) throws ErrorException { 
		super.getDirectoryInfo(info);
		
		info.add("dirimpl", "null");
		info.add("index", "N/A");
	}
	
	private NamedMap<Object> getIndexInfo() { 
		NamedMap<Object> indexInfo = new NamedMap<Object>();
		
	    indexInfo.add("numDocs", 0);
	    indexInfo.add("maxDoc", 0);
	    indexInfo.add("deletedDocs", 0);

	    indexInfo.add("version", 0); 
	    indexInfo.add("segmentCount", 0);
	    indexInfo.add("current", false);
	    indexInfo.add("hasDeletions", false);
	    indexInfo.add("directory", "null");
	    indexInfo.add("userData", new HashMap<String,String>());
	    
	    indexInfo.add("lastModified", new Date(getStartTime()));
		
		return indexInfo;
	}
	
}
