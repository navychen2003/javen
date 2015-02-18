package org.javenstudio.cocoka.storage.fs;

import java.io.IOException; 
import java.net.URI; 

public interface IFile {
	public IFileSystem getFileSystem() throws IOException; 

	public boolean canRead(); 
	public boolean canWrite(); 
	
	public int compareTo(IFile another) throws IOException;
	
	public String getAbsolutePath(); 
	public IFile getAbsoluteFile() throws IOException; 
	
	public IFile getParentFile() throws IOException; 
	
	public String getCanonicalPath() throws IOException; 
	
	public String getName(); 
	public String getPath(); 
	public String getLocation(); 
	
	public boolean exists();
	public boolean isAbsolute(); 
	
	public boolean isDirectory(); 
	public boolean isFile(); 
	
	public boolean isHidden(); 
	
	public long lastModified(); 
	
	public long length(); 
	
	public Path toPath(); 
	public URI toURI(); 
	
	public String toString(); 
	
}
