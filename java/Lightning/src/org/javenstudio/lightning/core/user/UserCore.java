package org.javenstudio.lightning.core.user;

import java.util.Date;
import java.util.HashMap;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.UserManager;
import org.javenstudio.falcon.util.AdminParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.NumberUtils;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreFactory;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserCore extends Core {

	public UserCore(CoreFactory factory, String dataDir, 
			UserConfig config, UserDescriptor cd, UserCore prev) 
			throws ErrorException {
		super(factory, dataDir, config, cd, prev);
		onInited();
	}

	public UserManager getUserManager() { 
		return getDescriptor().getContainer()
				.getContainers().getUserStore().getUserManager();
	}
	
	@Override
	public NamedList<Object> getParsedResponse(Request req, Response rsp)
			throws ErrorException {
		return null;
	}
	
	@Override
	protected synchronized void onClose() { 
		super.onClose();
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
