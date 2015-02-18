package org.javenstudio.lightning.response;

import java.io.IOException;
import java.io.OutputStream;

public interface ResponseOutput {

	public void setStatus(int status);
	public void setContentType(String type);
	public void setHeader(String name, String value);
	
	public void sendError(int status, String message) throws IOException;
	public OutputStream getOutputStream() throws IOException;
	
}
