package org.javenstudio.lightning.response.writer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.mime.Base64Util;

public class SecretJSONResponseWriter extends JSONResponseWriter {

	public static final String TYPE = "secretjson";
	
	@Override
	public void write(Writer writer, Request request, Response response)
			throws ErrorException {
		try {
			StringWriter sw = new StringWriter();
		    JSONWriter w = newJSONWriter(sw, request, response);
		    try {
		    	w.writeResponse();
		    } finally {
		    	w.close();
		    }
		    
		    //writer.write("BASE64:");
		    writer.write(Base64Util.encodeSecret(sw.toString(), "UTF-8"));
		    writer.flush();
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
}
