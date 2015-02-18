package org.javenstudio.falcon.datum.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.falcon.ErrorException;

public interface FileStream {

	public String getName();
	public String getContentType();
	
	public long getSize();
	public InputStream getStream() throws IOException;
	public void close();
	
	public boolean loadMetadata(FileMetaCollector collector)
			throws IOException, ErrorException;
	
	public File getFile();
	
}
