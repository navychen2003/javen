package org.javenstudio.falcon.search.update;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.params.UpdateParams;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.MapParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;

public class ContentLoaders extends ContentLoader {

	private final ISearchCore mCore;
	private Map<String,ContentLoader> mLoaders = null;
	
	public ContentLoaders(ISearchCore core) { 
		mCore = core;
	}
	
	public void initLoaders(NamedList<?> args) throws ErrorException { 
		if (mLoaders != null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"content loaders already inited");
		}
		
		// Since backed by a non-thread safe Map, it should not be modifiable
		mLoaders = Collections.unmodifiableMap(createDefaultLoaders(args));
	}
	
	@Override
	public void load(ISearchRequest req, ISearchResponse rsp, ContentStream stream, 
			UpdateProcessor processor) throws ErrorException {
		if (mLoaders == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"ContentLoaders not initialized"); 
		}
		
		String type = req.getParams().get(UpdateParams.ASSUME_CONTENT_TYPE);
		if (type == null) 
			type = stream.getContentType();
		
		if (type == null) { 
			// Normal requests will not get here.
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Missing ContentType");
		}
  
		int idx = type.indexOf(';');
		if (idx > 0) 
			type = type.substring(0, idx);
  
		ContentLoader loader = mLoaders.get(type);
		if (loader == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported ContentType: " + type + " Not in: " + mLoaders.keySet());
		}
		
		if (loader.getDefaultWT() != null) 
			setDefaultWT(req, loader);
		
		loader.load(req, rsp, stream, processor);
	}

	protected void setDefaultWT(ISearchRequest req, ContentLoader loader) 
			throws ErrorException {
		Params params = req.getParams();
		if (params.get(CommonParams.WT) == null) {
			String wt = loader.getDefaultWT();
			
			// Make sure it is a valid writer
			if (mCore.hasResponseWriter(req)) {
				Map<String,String> map = new HashMap<String,String>(1);
				map.put(CommonParams.WT, wt);
				
				req.setParams(Params.wrapDefaults(params, new MapParams(map)));
			}
		}
	}
	
	protected Map<String,ContentLoader> createDefaultLoaders(NamedList<?> args) 
			throws ErrorException {
		Params p = Params.toParams(args);
		
		Map<String,ContentLoader> registry = new HashMap<String,ContentLoader>();
		registry.put("application/xml", new XMLContentLoader(mCore).init(p));
		//registry.put("application/json", new JsonLoader().init(p) );
		//registry.put("application/csv", new CSVLoader().init(p) );
		//registry.put("application/javabin", new JavabinLoader().init(p) );
		//registry.put("text/csv", registry.get("application/csv") );
		//registry.put("text/xml", registry.get("application/xml") );
		//registry.put("text/json", registry.get("application/json") );
		
		return registry;
	}
	
}
