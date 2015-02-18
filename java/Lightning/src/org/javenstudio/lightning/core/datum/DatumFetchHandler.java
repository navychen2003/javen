package org.javenstudio.lightning.core.datum;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.http.HttpHelper;
import org.javenstudio.lightning.http.IHttpResult;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.writer.RawResponseWriter;

public class DatumFetchHandler extends DatumHandlerBase {
	private static final Logger LOG = Logger.getLogger(DatumFetchHandler.class);

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumFetchHandler(core);
	}
	
	public DatumFetchHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		handleFetch(req, rsp);
	}
	
	private void handleFetch(Request req, Response rsp) throws ErrorException {
		String url = trim(req.getParam("url"));
		
		if (url != null && url.length() > 0) {
			if (LOG.isDebugEnabled())
				LOG.debug("handleFetch: fetch url: " + url);
			
			URI fetchUri = URI.create(url);
			IHttpResult result = HttpHelper.fetchURL(fetchUri);
			
			if (result != null) {
				String disposition = getContentDisposition(result);
				if (disposition != null && disposition.length() > 0)
					rsp.getResponseOutput().setHeader("Content-Disposition", disposition);
				
				req.setResponseWriterType(RawResponseWriter.TYPE);
				rsp.add(RawResponseWriter.CONTENT, new EntityStream(result));
				
				return;
			}
		}
		
		rsp.add("url", url);
	}
	
	static String getContentDisposition(IHttpResult result) {
		if (result == null) return null;
		
		URI fetchUri = result.getURL();
		String contentType = result.getContentType();
		long contentLength = result.getContentLength();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("handleFetch: contentType=" + contentType 
					+ " contentLength=" + contentLength);
			
			IHttpResult.IHeader[] headers = result.getAllHeaders();
			if (headers != null) {
				for (IHttpResult.IHeader header : headers) {
					if (header != null) {
						LOG.debug("handleFetch: header: " + header.getName() 
								+ ": " + header.getValue());
					}
				}
			}
		}
		
		if (contentType != null) contentType = contentType.toLowerCase();
		if (contentType != null && contentType.startsWith("text/")) {
			// do nothing
		} else {
			IHttpResult.IHeader[] headers = result.getHeaders("Content-Disposition");
			String disposition = null;
			
			if (headers != null && headers.length > 0) {
				for (IHttpResult.IHeader header : headers) {
					if (header != null) {
						String value = header.getValue();
						if (value != null && value.length() > 0) {
							disposition = value;
							break;
						}
					}
				}
			}
			
			if (disposition == null || disposition.length() == 0) {
				String filename = fetchUri != null ? fetchUri.getPath() : null;
				if (filename != null && filename.length() > 0) {
					int pos1 = filename.lastIndexOf('/');
					int pos2 = filename.lastIndexOf('\\');
					
					if (pos1 >= 0 || pos2 >= 0) {
						int pos = pos1;
						if (pos2 > pos1) pos = pos2;
						if (pos >= 0) {
							filename = filename.substring(pos+1);
						}
					}
					
					if (filename != null && filename.length() > 0) {
						if (filename.lastIndexOf('.') < 0)
							filename += ".dat";
					}
				}
				
				if (filename == null || filename.length() == 0)
					filename = "fetch-" + System.currentTimeMillis() + ".dat";
				
				disposition = "attachment; filename=\"" + filename + "\"";
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("handleFetch: Content-Disposition: " + disposition);
			
			return disposition;
		}
		
		return null;
	}
	
	static class EntityStream extends ContentStreamBase { 
		private final IHttpResult mData;
		
		public EntityStream(IHttpResult data) { 
			if (data == null) throw new NullPointerException();
			mData = data;
		}
		
		@Override
	    public String getContentType() {
			String contentType = mData.getContentType();
			if (contentType == null || contentType.length() == 0) {
				contentType = "application/binary";
			}
			return contentType;
		}
		
		@Override
	    public InputStream getStream() throws IOException {
			try {
				return mData.getContentAsStream();
			} catch (ErrorException ee) {
				Throwable e = ee.getCause();
				if (e == null) e = ee;
				if (e instanceof IOException) throw (IOException)e;
				throw new IOException(e);
			}
		}
	}
	
}
