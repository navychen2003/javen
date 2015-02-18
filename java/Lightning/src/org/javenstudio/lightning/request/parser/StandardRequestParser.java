package org.javenstudio.lightning.request.parser;

import java.util.List;
import java.util.Locale;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.SimpleParams;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.request.RequestParser;

/**
 * The default Logic
 */
public class StandardRequestParser implements RequestParser {
	private static final Logger LOG = Logger.getLogger(StandardRequestParser.class);

	private final MultipartRequestParser mMultipartParser;
	private final FormDataRequestParser mFormdataParser;
	private final RawRequestParser mRawParser;
	
	public StandardRequestParser(MultipartRequestParser multi, 
			FormDataRequestParser form, RawRequestParser raw) { 
		mMultipartParser = multi;
		mFormdataParser = form;
		mRawParser = raw;
	}
	
	@Override
	public Params parseParamsAndFillStreams(RequestInput input,
			List<ContentStream> streams) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("parseParamsAndFillStreams: input=" + input);
		
	    final String method = input.getMethod().toUpperCase(Locale.ROOT);
	    if ("GET".equals(method) || "HEAD".equals(method)) 
	    	return new SimpleParams(input.getParameterMap());
	    
	    if ("POST".equals(method)) {
	    	String contentType = input.getContentType();
	    	if (contentType != null) {
		        //int idx = contentType.indexOf(';');
		        //if (idx > 0) // remove the charset definition "; charset=utf-8"
		        //	contentType = contentType.substring(0, idx);
		        
		        //if ("application/x-www-form-urlencoded".equals(contentType.toLowerCase(Locale.ROOT))) {
		        //	// just get the params from parameterMap
		        //	return new SimpleParams(input.getParameterMap()); 
		        //}
		        
		        //if (input.isMultipartContent()) 
		        //	return mMultipartParser.parseParamsAndFillStreams(input, streams);
	    		
	    		if (FormDataRequestParser.isFormData(input))
	    			return mFormdataParser.parseParamsAndFillStreams(input, streams);
	    		
	    		if (MultipartRequestParser.isMultipartContent(input))
	    			return mMultipartParser.parseParamsAndFillStreams(input, streams);
	    	}
	    	
	    	return mRawParser.parseParamsAndFillStreams(input, streams);
	    }
	    
	    throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    		"Unsupported method: " + method);
	}

}
