package org.javenstudio.cocoka.storage.fs;

import java.io.IOException; 
import java.io.InputStream; 
import java.io.OutputStream; 

public interface IFileSystem {
	
	public IFile getFile(Path path) throws IOException; 

	public boolean delete(IFile file) throws IOException; 
	public boolean exists(IFile file) throws IOException; 
	
	public IFile[] listFiles(IFile dir) throws IOException; 
	public IFile[] listFiles(IFile dir, IFileFilter filter) throws IOException; 
	
	public boolean mkdir(IFile dir)  throws IOException; 
	public boolean mkdirs(IFile dir)  throws IOException; 
	
	public boolean createNewFile(IFile file) throws IOException; 
	
	public boolean renameTo(IFile src, IFile dest) throws IOException; 
	
	public InputStream open(IFile file) throws IOException; 
	public OutputStream create(IFile file) throws IOException; 
	public OutputStream append(IFile file) throws IOException; 
	
}
