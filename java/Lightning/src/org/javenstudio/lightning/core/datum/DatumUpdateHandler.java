package org.javenstudio.lightning.core.datum;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.data.SqLibrary;
import org.javenstudio.falcon.datum.data.SqRoot;
import org.javenstudio.falcon.datum.data.SqRootDir;
import org.javenstudio.falcon.datum.data.SqRootFile;
import org.javenstudio.falcon.datum.data.SqRootPoster;
import org.javenstudio.falcon.datum.data.SqRootUpdate;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class DatumUpdateHandler extends DatumHandlerBase {
	private static final Logger LOG = Logger.getLogger(DatumUpdateHandler.class);

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumUpdateHandler(core);
	}
	
	public DatumUpdateHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		
		String action = trim(req.getParam("action"));
		String key = trim(req.getParam("id"));
		
		if (action == null || action.length() == 0) 
			action = "update";
		
		rsp.add("action", toString(action));
		rsp.add("id", toString(key));
		
		boolean result = false;
		
		if (action.equalsIgnoreCase("updateposter")) { 
			result = handleUpdatePoster(req, rsp, user, key);
			
		} else if (action.equalsIgnoreCase("update")) { 
			result = handleUpdate(req, rsp, user, key);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
		
		rsp.add("result", result);
	}
	
	private Map<String,String> getInputMetadatas(Request req) 
			throws ErrorException { 
		Params params = req != null ? req.getParams() : null;
		if (params == null) return null;
		
		Map<String,String> metas = new HashMap<String,String>();
		Iterator<String> it = params.getParameterNamesIterator();
		if (it != null) { 
			while (it.hasNext()) { 
				String paramName = it.next();
				String paramValue = params.get(paramName);
				if (paramName == null) continue;
				
				//if (LOG.isDebugEnabled())
				//	LOG.debug("handleUpdate: input: " + paramName + "=" + paramValue);
				
				if (paramName.startsWith("meta_") || paramName.startsWith("meta.") || 
					paramName.startsWith("meta-")) { 
					String metaName = paramName.substring(5);
					if (metaName != null && metaName.length() > 0) { 
						if (paramValue == null) paramValue = "";
						metaName = metaName.toLowerCase();
						metas.put(metaName, paramValue);
						
						if (LOG.isDebugEnabled())
							LOG.debug("handleUpdate: input metadata: " + metaName + "=" + paramValue);
					}
				}
			}
		}
		return metas;
	}
	
	private boolean handleUpdate(Request req, Response rsp, 
			IMember user, String key) throws ErrorException {
		String accesskey = trim(req.getParam("accesskey"));
		IData data = getData(user, key, IData.Access.UPDATE, accesskey);
		if (data != null) {
			String name = trim(req.getParam("name"));
			String extname = trim(req.getParam("extension"));
			
			if (data instanceof SqLibrary) { 
				return false;
				
			} else if (data instanceof SqRoot) { 
				return SqRootUpdate.setName(user, (SqRoot)data, name);
				
			} else if (data instanceof SqRootDir) {
				return SqRootUpdate.saveMetadata(user, (SqRootDir)data, name, extname);
				
			} else if (data instanceof SqRootFile) { 
				return SqRootUpdate.saveMetadata(user, (SqRootFile)data, name, extname, 
						getInputMetadatas(req));
			}
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Update target is wrong");
	}
	
	private boolean handleUpdatePoster(Request req, Response rsp, 
			IMember user, String key) throws ErrorException {
		String accesskey = trim(req.getParam("accesskey"));
		IData data = getData(user, key, IData.Access.UPDATE, accesskey);
		if (data != null) {
			String[] posters = null;
			String[] backgrounds = null;
			
			if (true) {
				String poster = trim(req.getParam("poster"));
				String background = trim(req.getParam("background"));
				
				if (poster != null && poster.length() > 0) {
					if (poster.equalsIgnoreCase("null"))
						posters = new String[0];
					else
						posters = new String[]{ poster };
				}
				
				if (background != null && background.length() > 0) {
					if (background.equalsIgnoreCase("null"))
						backgrounds = new String[0];
					else
						backgrounds = new String[]{ background };
				}
			}
			
			if (data instanceof SqLibrary) { 
				return false;
				
			} else if (data instanceof SqRoot) { 
				if (posters != null && backgrounds != null)
					return SqRootPoster.setPosterBackground((SqRoot)data, posters, backgrounds);
				else if (posters != null)
					return SqRootPoster.setPoster((SqRoot)data, posters);
				else if (backgrounds != null)
					return SqRootPoster.setBackground((SqRoot)data, backgrounds);
				
			} else if (data instanceof SqRootDir) {
				if (posters != null && backgrounds != null)
					return SqRootPoster.setPosterBackground((SqRootDir)data, posters, backgrounds);
				else if (posters != null)
					return SqRootPoster.setPoster((SqRootDir)data, posters);
				else if (backgrounds != null)
					return SqRootPoster.setBackground((SqRootDir)data, backgrounds);
				
			} else if (data instanceof SqRootFile) { 
				if (posters != null && backgrounds != null)
					return SqRootPoster.setPosterBackground((SqRootFile)data, posters, backgrounds);
				else if (posters != null)
					return SqRootPoster.setPoster((SqRootFile)data, posters);
				else if (backgrounds != null)
					return SqRootPoster.setBackground((SqRootFile)data, backgrounds);
			}
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Update poster target is wrong");
	}
	
}
