package org.javenstudio.lightning.response.writer;

import java.io.IOException;
import java.io.Writer;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseWriter;

public class XMLResponseWriter implements ResponseWriter {

	public static final String TYPE = "xml";
	
	protected XMLWriter newXMLWriter(Writer writer, 
			Request req, Response rsp) throws ErrorException { 
		return new XMLWriter(writer, req, rsp);
	}
	
	@Override
	public void write(Writer writer, Request request, Response response)
			throws ErrorException {
		try {
		    XMLWriter w = newXMLWriter(writer, request, response);
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
		return CONTENT_TYPE_XML_UTF8;
	}

	@Override
	public void init(NamedList<?> args) throws ErrorException {
		// do nothing
	}

}
