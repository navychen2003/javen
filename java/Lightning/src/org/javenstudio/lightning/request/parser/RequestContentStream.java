package org.javenstudio.lightning.request.parser;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.lightning.request.RequestInput;

/**
 * Wrap an HttpServletRequest as a ContentStream
 */
public class RequestContentStream extends ContentStreamBase {
	
	private final RequestInput mInput;
  
	public RequestContentStream(RequestInput input) {
		mInput = input;
    
		contentType = input.getContentType();
		// name = ???
		// sourceInfo = ???
    
		String val = input.getHeader("Content-Length");
		if (val != null) 
			size = Long.valueOf(val);
		
	}

	@Override
	public InputStream getStream() throws IOException {
		return mInput.getInputStream();
	}
	
}
