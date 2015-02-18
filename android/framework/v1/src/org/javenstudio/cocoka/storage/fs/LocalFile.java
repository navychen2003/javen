package org.javenstudio.cocoka.storage.fs;

import java.io.IOException; 
import java.io.File; 
import java.net.URI;

public class LocalFile implements IFile {
	
	private final File mFile; 

	public LocalFile(Path path) throws IOException {
		if (path == null) throw new IOException("path is null"); 
		mFile = new File(path.toUri().getPath()); 
	}
	
	public LocalFile(File file) throws IOException {
		if (file == null) throw new IOException("file is null"); 
		mFile = new File(file.toURI()); 
	}
	
	File getFileImpl() {
		return mFile; 
	}
	
	@Override 
	public IFileSystem getFileSystem() throws IOException {
		return LocalFileSystem.get(null); 
	}
	
	@Override 
	public boolean canRead() {
		return mFile.canRead(); 
	}
	
	@Override 
	public boolean canWrite() {
		return mFile.canWrite(); 
	}
	
	@Override 
	public int compareTo(IFile another) throws IOException {
		if (another == null) 
			return 1; 
		
		if (!(another instanceof LocalFile)) 
			throw new IOException("can only compare to a local file"); 
		
		return mFile.compareTo(((LocalFile)another).mFile); 
	}
	
	@Override 
	public String getAbsolutePath() {
		return mFile.getAbsolutePath(); 
	}
	
	@Override 
	public IFile getAbsoluteFile() throws IOException {
		return new LocalFile(mFile.getAbsoluteFile()); 
	}
	
	@Override 
	public IFile getParentFile() throws IOException {
		return new LocalFile(mFile.getParentFile()); 
	}
	
	@Override 
	public String getCanonicalPath() throws IOException {
		return mFile.getCanonicalPath(); 
	}
	
	@Override 
	public String getName() {
		return mFile.getName(); 
	}
	
	@Override 
	public String getPath() {
		return mFile.getPath(); 
	}
	
	@Override 
	public String getLocation() { 
		return "file://" + getPath(); 
	}
	
	@Override 
	public boolean isAbsolute() {
		return mFile.isAbsolute(); 
	}
	
	@Override 
	public boolean isDirectory() {
		return mFile.isDirectory(); 
	}
	
	@Override 
	public boolean isFile() {
		return mFile.isFile(); 
	}
	
	@Override 
	public boolean isHidden() {
		return mFile.isHidden(); 
	}
	
	@Override 
	public long lastModified() {
		return mFile.lastModified(); 
	}
	
	@Override
	public boolean exists() { 
		return mFile.exists(); 
	}
	
	@Override 
	public long length() {
		return mFile.length(); 
	}
	
	@Override 
	public Path toPath() {
		return new Path(mFile.toURI()); 
	}
	
	@Override 
	public URI toURI() {
		return mFile.toURI(); 
	}
	
	@Override 
	public String toString() {
		return toPath().toString(); 
	}
}
