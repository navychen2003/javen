package org.javenstudio.mail;

import java.io.IOException;
import java.io.OutputStream;

import org.javenstudio.mail.util.IOUtils;

public interface OutputBody extends Body, IOUtils.Stopper {

	public OutputStream getOutputStream() throws IOException; 
	
	public void finishOutput(long count); 
	
}
