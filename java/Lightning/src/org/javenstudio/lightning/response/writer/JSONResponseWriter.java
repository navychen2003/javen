package org.javenstudio.lightning.response.writer;

import java.io.IOException;
import java.io.Writer;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseWriter;

public class JSONResponseWriter implements ResponseWriter {
	
	public static final String TYPE = "json";
	public static final String CONTENT_TYPE_JSON_UTF8 = "application/json; charset=UTF-8";
	
	private String mContentType = CONTENT_TYPE_JSON_UTF8;
	
	protected JSONWriter newJSONWriter(Writer writer, 
			Request req, Response rsp) throws ErrorException { 
		return new JSONWriter(writer, req, rsp);
	}
	
	@Override
	public void write(Writer writer, Request request, Response response)
			throws ErrorException {
		try {
		    JSONWriter w = newJSONWriter(writer, request, response);
		    try {
		    	w.writeResponse();
		    } finally {
		    	w.close();
		    }
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	@Override
	public String getContentType(Request request, Response response)
			throws ErrorException {
		return mContentType;
	}

	@Override
	public void init(NamedList<?> args) throws ErrorException {
		String contentType = (String) args.get("content-type");
	    if (contentType != null) 
	    	mContentType = contentType;
	}

}
