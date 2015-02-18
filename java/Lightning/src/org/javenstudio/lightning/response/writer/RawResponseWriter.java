package org.javenstudio.lightning.response.writer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.BinaryResponseWriter;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseWriter;
import org.javenstudio.lightning.response.ResponseWriters;

/**
 * Writes a ContentStream directly to the output.
 *
 * <p>
 * This writer is a special case that extends and alters the
 * QueryResponseWriter contract.  If Response contains a
 * ContentStream added with the key {@link #CONTENT}
 * then this writer will output that stream exactly as is (with it's
 * Content-Type).  if no such ContentStream has been added, then a
 * "base" QueryResponseWriter will be used to write the response
 * according to the usual contract.  The name of the "base" writer can
 * be specified as an initialization param for this writer, or it
 * defaults to the "standard" writer.
 * </p>
 * 
 */
public class RawResponseWriter implements BinaryResponseWriter {
	private static final Logger LOG = Logger.getLogger(RawResponseWriter.class);

	public static final String TYPE = "raw";
	
	/** 
	 * The key that should be used to add a ContentStream to the 
	 * Response if you intend to use this Writer.
	 */
	public static final String CONTENT = "content";
	
	private String mBaseType = null;
	
	public ResponseWriter getBaseWriter(Request request) { 
		return ResponseWriters.getDefaultWriter(mBaseType);
	}

	@Override
	public String getContentType(Request request, Response response)
			throws ErrorException {
		Object obj = response.getValue(CONTENT);
	    if (obj != null && (obj instanceof ContentStream)) 
	    	return ((ContentStream)obj).getContentType();
	    
	    return null;
	}

	@Override
	public void init(NamedList<?> args) throws ErrorException {
		if (args != null) { 
			Object base = args.get("base");
			if (base != null) 
				mBaseType = base.toString();
		}
	}

	@Override
	public void write(Writer writer, Request request, Response response)
			throws ErrorException {
	    Object obj = response.getValue(CONTENT);
	    if (obj != null && (obj instanceof ContentStream)) {
	    	try {
		    	// copy the contents to the writer...
		    	ContentStream content = (ContentStream)obj;
		    	Reader reader = content.getReader();
		    	if (reader == null) { 
		    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
			    			"CONTENT object return null reader");
		    	}
		    	try {
		    		int count = IOUtils.copy(reader, writer);
		    		
		    		if (LOG.isDebugEnabled()) {
		    			LOG.debug("write: written " + count + " characters from " 
		    					+ reader + " to " + writer);
		    		}
		    	} finally {
		    		reader.close();
		    	}
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
	    	}
	    } else {
	    	getBaseWriter(request).write(writer, request, response);
	    }
	}
	
	@Override
	public void write(OutputStream out, Request request, Response response)
			throws ErrorException {
	    Object obj = response.getValue(CONTENT);
	    if (obj != null && (obj instanceof ContentStream)) {
	    	try {
		    	// copy the contents to the writer...
		    	ContentStream content = (ContentStream)obj;
		    	InputStream in = content.getStream();
		    	if (in == null) { 
		    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
			    			"CONTENT object return null stream");
		    	}
		    	try {
		    		int len = in.available();
		    		
		    		if (LOG.isDebugEnabled()) {
		    			LOG.debug("write: writting " + len + " bytes from " 
		    					+ in + " to " + out);
		    		}
		    		
		    		int count = IOUtils.copy(in, out);
		    		
		    		if (LOG.isDebugEnabled()) {
		    			LOG.debug("write: written " + count + " of " + len 
		    					+ " bytes from " + in + " to " + out);
		    		}
		    	} finally {
		    		in.close();
		    	}
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
	    	}
	    } else {
	    	//getBaseWriter( request ).write( writer, request, response );
	    	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    			"did not find a CONTENT object");
	    }
	}

}
