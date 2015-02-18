package org.javenstudio.lightning.servlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.javenstudio.lightning.response.ResponseOutput;

public class ServletResponseOutput implements ResponseOutput {

	private final HttpServletResponse mResponse;
	
	public ServletResponseOutput(HttpServletResponse response) { 
		mResponse = response;
	}

	@Override
	public void setStatus(int status) {
		mResponse.setStatus(status);
	}

	@Override
	public void setHeader(String name, String value) { 
		mResponse.setHeader(name, value);
	}
	
	public String getHeader(String name) { 
		return mResponse.getHeader(name);
	}
	
	@Override
	public void setContentType(String type) {
		mResponse.setContentType(type);
	}
	
	public String getContentType() { 
		return mResponse.getContentType();
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException { 
		return mResponse.getOutputStream();
	}
	
	@Override
	public void sendError(int status, String message) throws IOException { 
		mResponse.sendError(status, message);
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{" 
				+ "contentType=" + getContentType() + "}";
	}
	
}
