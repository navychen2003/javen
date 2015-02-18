package org.javenstudio.lightning.response.writer;

import java.io.Writer;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public abstract class BaseTextWriter extends TextWriter {

	private final Request mRequest;
	private final Response mResponse;
	
	private boolean mDoIndent = false;
	
	public BaseTextWriter(Writer writer, Request request, Response response) 
			throws ErrorException { 
		super(writer); 
		mRequest = request;
		mResponse = response;
		
		String indent = request.getParam("indent");
		if (indent != null && ("true".equals(indent) || "on".equals(indent)))
			mDoIndent = true;
	}
	
	public final Request getRequest() { return mRequest; }
	public final Response getResponse() { return mResponse; }
	
	@Override
	public boolean isIndent() { 
		return mDoIndent;
	}
	
	public void setIndent(boolean doIndent) {
		mDoIndent = doIndent;
	}
	
}
