package org.javenstudio.lightning.core.datum;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.IFileData;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;
import org.javenstudio.lightning.response.writer.RawResponseWriter;

public class DatumDownloadHandler extends DatumHandlerBase {
	private static final Logger LOG = Logger.getLogger(DatumDownloadHandler.class);

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumDownloadHandler(core);
	}
	
	public static final String PATH_PREFIX = "/download";
	public static final String PATH_REWRITE = PATH_PREFIX + "/";
	
	public DatumDownloadHandler(DatumCore core) { 
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
		String tokenValue = "";
		
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
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String accesskey = trim(req.getParam("accesskey"));
		String name = trim(req.getParam("src"));
		String type = null;
		
		if (name != null) { 
			int pos1 = name.lastIndexOf('.');
			if (pos1 >= 0) { 
				type = name.substring(pos1+1);
				name = name.substring(0, pos1);
				
				if (type != null) 
					type = type.toLowerCase();
			}
		}
		
		IData data = getCacheData(user, name, IData.Access.DOWNLOAD, accesskey);
		if (data == null) {
			throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
					name + " not found");
		}
		
		IFileData fileData = null;
		if (data != null && data instanceof IFileData)
			fileData = (IFileData)data;
		
		if (fileData == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					name + " not file");
		}
		
		rsp.getResponseOutput().setHeader("Content-Disposition", 
				"attachment; filename=\"" + fileData.getName() + "\"");
		
		req.setResponseWriterType(RawResponseWriter.TYPE);
		rsp.add(RawResponseWriter.CONTENT, new FileStream(fileData));
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
	
	static class FileStream extends ContentStreamBase { 
		private final IFileData mData;
		
		public FileStream(IFileData data) { 
			if (data == null) throw new NullPointerException();
			mData = data;
		}
		
		@Override
	    public String getContentType() {
			return mData.getContentType();
		}
		
		@Override
	    public InputStream getStream() throws IOException {
			return mData.open();
		}
	}
	
}
