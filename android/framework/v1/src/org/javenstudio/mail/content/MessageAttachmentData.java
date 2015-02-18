package org.javenstudio.mail.content;

import java.io.IOException;
import java.io.InputStream;

public interface MessageAttachmentData {

	public boolean isAlternativePart();
	
	public String getMimeType(); 
	public String getFileName(); 
	public long getSize(); 
	
	public String getContentId(); 
	public String getContentType(); 
	public String getContentTransferEncoding(); 
	
	public byte[] getContentBytes(); 
	public InputStream openInputStream() throws IOException; 
	
}
