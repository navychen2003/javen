package org.javenstudio.cocoka.util;

import java.io.InputStream;

public interface ContentFile {

	public String getContentType(); 
	public long getContentLength(); 
	
	public String getLocation(); 
	public String getFilePath(); 
	public String getFileName(); 
	
	public InputStream openFile(); 
	
}
