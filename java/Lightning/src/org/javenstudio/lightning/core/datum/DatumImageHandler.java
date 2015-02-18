package org.javenstudio.lightning.core.datum;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.IImageSource;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.datum.util.ImageStream;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;
import org.javenstudio.lightning.response.writer.RawResponseWriter;

public class DatumImageHandler extends DatumHandlerBase {
	private static final Logger LOG = Logger.getLogger(DatumImageHandler.class);

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumImageHandler(core);
	}
	
	public static final String PATH_PREFIX = "/image";
	public static final String PATH_REWRITE = PATH_PREFIX + "/";
	
	public DatumImageHandler(DatumCore core) { 
		super(core);
	}
	
	static void rewriteRequest(RequestInput input, ModifiableParams params) { 
		if (input == null || params == null)
			return;
		
		String queryPath = input.getQueryPath();
		if (queryPath == null || queryPath.indexOf('?') >= 0)
			return;
		
		int pos = queryPath.indexOf(PATH_REWRITE);
		if (pos != 0) return;
		
		String newPath = PATH_PREFIX;
		String srcValue = queryPath.substring(pos + PATH_REWRITE.length());
		String tokenValue = null;
		
		int pos2 = srcValue.indexOf('/');
		if (pos2 > 0) { 
			tokenValue = srcValue.substring(0, pos2);
			srcValue = srcValue.substring(pos2+1);
		}
		
		input.setQueryPath(newPath);
		params.set("src", srcValue);
		
		if (tokenValue != null && tokenValue.length() > 0)
			params.set("token", tokenValue);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("rewriteRequest: queryPath=" + queryPath 
					+ " newPath=" + newPath + " params=" + params);
		}
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = null; //UserHelper.checkUserKey(req);
		String accesskey = trim(req.getParam("accesskey"));
		String key = trim(req.getParam("src"));
		
		String type = null;
		String param = null;
		
		if (key != null) { 
			int pos1 = key.lastIndexOf('.');
			if (pos1 >= 0) { 
				type = key.substring(pos1+1);
				key = key.substring(0, pos1);
				
				if (type != null) 
					type = type.toLowerCase();
			}
			
			int pos2 = key.lastIndexOf('_');
			if (pos2 >= 0) { 
				param = key.substring(pos2+1);
				key = key.substring(0, pos2);
				
			} else { 
				int pos3 = key.lastIndexOf('-');
				if (pos3 >= 0) { 
					param = key.substring(pos3+1);
					key = key.substring(0, pos3);
				}
			}
		}
		
		boolean share = SectionHelper.isShareFile(key);
		if (share == false)
			user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		
		IData data = getCacheData(user, key, IData.Access.THUMB, accesskey);
		if (data == null) {
			throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
					key + " not found");
		}
		
		IImageSource imageData = null;
		if (data != null && data instanceof IImageSource)
			imageData = (IImageSource)data;
		
		if (imageData == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					key + " is not a image");
		}
		
		ImageStream stream = new ImageStream(imageData, type, param, share);
		
		req.setResponseWriterType(RawResponseWriter.TYPE);
		rsp.add(RawResponseWriter.CONTENT, stream);
	}
	
	@Override
	public boolean handleResponse(Request req, Response rsp, 
			ResponseOutput output) throws ErrorException { 
		Throwable ex = rsp.getException();
		if (ex != null) { 
			int statusCode = ErrorException.ErrorCode.SERVER_ERROR.getCode();
			if (ex instanceof ErrorException) 
				statusCode = ((ErrorException)ex).getCode();
			
			try {
				output.sendError(statusCode, ex.getMessage());
			} catch (Throwable e) { 
				if (LOG.isWarnEnabled())
					LOG.warn("handlerResponse: sendError failed: " + e, e);
			}
			
			return true;
		}
		
		return false;
	}
	
}
