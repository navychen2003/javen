package org.javenstudio.lightning.response;

import java.io.OutputStream;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.request.Request;

/**
 * Implementations of <code>BinaryQueryResponseWriter</code> are used to
 * write response in binary format
 * Functionality is exactly same as its parent class <code>QueryResponseWriter</code>
 * But it may not implement the <code>write(Writer writer, Request request, Response response)</code>
 * method  
 *
 */
public interface BinaryResponseWriter extends ResponseWriter {

    /** Use it to write the reponse in a binary format */
    public void write(OutputStream out, Request request, Response response) 
    		throws ErrorException;
	
}
