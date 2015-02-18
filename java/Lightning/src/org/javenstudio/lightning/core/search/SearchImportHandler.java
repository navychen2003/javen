package org.javenstudio.lightning.core.search;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.dataimport.ImportAttributes;
import org.javenstudio.falcon.search.dataimport.ImportContext;
import org.javenstudio.falcon.search.dataimport.ImportProcessor;
import org.javenstudio.falcon.search.handler.ImportHandler;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.handler.DataImportHandler;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class SearchImportHandler extends SearchHandlerBase {
	static final Logger LOG = Logger.getLogger(SearchImportHandler.class);

	public static RequestHandler createHandler(ISearchCore core) { 
		return new DataImportHandler(new SearchImportHandler(core));
	}
	
	private final ISearchCore mCore;
	private ImportHandler mHandler = null;
	
	private SearchImportHandler(ISearchCore core) { 
		if (core == null) throw new NullPointerException();
		mCore = core;
	}
	
	private ImportHandler getHandler() throws ErrorException { 
		if (mHandler == null) 
			mHandler = new ImportHandler(new ImportContextImpl(mCore));
		
		return mHandler;
	}
	
	@Override
	public void init(NamedList<?> args) throws ErrorException {
		super.init(args);
		getHandler().init(args);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		getHandler().handleRequestBody((ISearchRequest)req, (ISearchResponse)rsp);
	}
	
	@Override
	public String getMBeanDescription() {
		return getClass().getName();
	}
	
	static class ImportContextImpl extends ImportContext {
		
		private final Map<String, ImportProcessor> mProcessors = 
				new HashMap<String, ImportProcessor>();
		
		private final ISearchCore mCore;
		private final ImportAttributes mAttributes;
		
		public ImportContextImpl(ISearchCore core) throws ErrorException { 
			mCore = core;
			mAttributes = new ImportAttributes();
		}
		
		public Map<String, ImportProcessor> getProcessors() { 
			synchronized (mProcessors) {
				if (mProcessors.size() <= 0) { 
					try { 
						if (LOG.isDebugEnabled())
							LOG.debug("initProcessors: init");
						
						initProcessors();
					} catch (Throwable e) { 
						if (LOG.isWarnEnabled())
							LOG.warn("initProcessors: error: " + e, e);
					}
				}
				return mProcessors;
			}
		}
		
		private void initProcessors() throws ErrorException { 
			ISearchCore core = mCore;
			if (core != null && core instanceof Core) { 
				CoreContainers containers = ((Core)core).getDescriptor().getContainer().getContainers();
				for (Core c : containers.getCores()) { 
	    			if (c != null && c instanceof ImportProcessor.Factory) { 
	    				ImportProcessor.Factory factory = (ImportProcessor.Factory)c;
	    				ImportProcessor[] processors = factory.createProcessors(this);
	    				addProcessors(processors);
	    			}
				}
			}
		}
		
		private void addProcessors(ImportProcessor[] processors) throws ErrorException { 
			if (processors == null || processors.length == 0)
				return;
			
			for (ImportProcessor p : processors) { 
				if (p == null) continue;
				String entityName = p.getEntityName();
				
				if (entityName == null || entityName.length() == 0) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"ImportProcessor has empty entityName");
				}
				
				if (mProcessors.containsKey(entityName)) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"ImportProcessor with entityName: " + entityName + " already exists");
				}
				
				mProcessors.put(entityName, p);
			}
		}
		
		@Override
		public String[] getEntityNames() {
			synchronized (mProcessors) {
				return getProcessors().keySet().toArray(new String[getProcessors().size()]);
			}
		}

		@Override
		public ImportAttributes getAttributes() {
			return mAttributes;
		}

		@Override
		public ImportProcessor getProcessor(String entityName) 
				throws ErrorException {
			synchronized (mProcessors) {
				ImportProcessor p = getProcessors().get(entityName);
				if (p != null) return p;
			}
			
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Entity: " + entityName + " not found");
		}

		@Override
		public ISearchCore getSearchCore() {
			return mCore;
		}
	}
	
}
