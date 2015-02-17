package org.javenstudio.cocoka.storage.fs;

import java.io.IOException; 
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File; 
import java.io.FileFilter;
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.net.URI; 

import org.javenstudio.common.util.Logger;

public class LocalFileSystem implements IFileSystem {
	private static Logger LOG = Logger.getLogger(LocalFileSystem.class);
	
	public static final String LOCAL_SCHEME = "file"; 

	private static LocalFileSystem sInstance = new LocalFileSystem(); 
	
	public static IFileSystem get(URI uri) throws IOException {
		return sInstance; 
	}
	
	private LocalFileSystem() {}
	
	@Override 
	public IFile getFile(Path path) throws IOException {
		return new LocalFile(path); 
	}
	
	private File getFileImpl(IFile file) throws IOException {
		if (file == null) 
			throw new IOException("file is null"); 
		
		if (!(file instanceof LocalFile)) 
			throw new IOException("not a local file"); 
		
		return ((LocalFile)file).getFileImpl(); 
	}
	
	@Override
	public IFile[] listRoots() throws IOException {
		return localToIFile(File.listRoots());
	}
	
	@Override 
	public boolean delete(IFile file) throws IOException {
		if (file == null) return false;
		
		if (LOG.isWarnEnabled())
			LOG.warn("delete: " + file);
		
		return getFileImpl(file).delete(); 
	}
	
	@Override 
	public boolean exists(IFile file) throws IOException {
		return getFileImpl(file).exists(); 
	}
	
	@Override 
	public IFile[] listFiles(IFile dir) throws IOException {
		return localToIFile(getFileImpl(dir).listFiles()); 
	}
	
	@Override 
	public IFile[] listFiles(IFile dir, final IFileFilter filter) throws IOException {
		return localToIFile(getFileImpl(dir).listFiles(new FileFilter() {
			public boolean accept(File file) {
				return filter != null ? filter.accept(file.getPath()) : true; 
			}
		})); 
	}
	
	private static IFile[] localToIFile(File[] files) throws IOException {
		if (files == null) 
			return null; 
		
		IFile[] subfiles = new IFile[files.length]; 
		for (int i=0; i < files.length; i++) {
			subfiles[i] = new LocalFile(files[i]); 
		}
		
		FsHelper.sortFiles(subfiles); 
		
		return subfiles; 
	}
	
	@Override 
	public boolean mkdir(IFile dir) throws IOException {
		//return getFileImpl(dir).mkdir(); 
		return mkdirs(dir); 
	}
	
	@Override 
	public boolean mkdirs(IFile dir) throws IOException {
		return getFileImpl(dir).mkdirs(); 
		//return mkdirFile(getFileImpl(dir)); 
	}
	
	@SuppressWarnings("unused")
	private boolean mkdirFile(File dir) throws IOException { 
		if (dir == null) return false;
		
		if (dir.exists()) 
			return true;
		
		File parent = dir.getParentFile();
		if (parent != null && !mkdirFile(parent)) 
			return false;
		
		boolean result = dir.mkdir();
		
		if (LOG.isDebugEnabled())
			LOG.debug("mkdir: " + dir.getAbsolutePath() + " " + result);
		
		return result;
	}
	
	private void mkdirsIfNotExists(IFile file) throws IOException { 
		IFile parent = file.getParentFile(); 
		if (parent != null && !exists(parent) && !mkdirs(parent)) {
			if (LOG.isDebugEnabled())
				LOG.debug("directory: " + parent.getAbsolutePath() + " cannot created or existed"); 
		}
	}
	
	@Override 
	public boolean createNewFile(IFile file) throws IOException {
		mkdirsIfNotExists(file);
		return getFileImpl(file).createNewFile(); 
	}
	
	@Override 
	public boolean renameTo(IFile src, IFile dest) throws IOException {
		return getFileImpl(src).renameTo(getFileImpl(dest)); 
	}
	
	@Override 
	public InputStream open(IFile file) throws IOException {
		mkdirsIfNotExists(file);
		return new FileInputStream(getFileImpl(file)); 
	}
	
	@Override 
	public OutputStream create(IFile file) throws IOException {
		mkdirsIfNotExists(file);
		return new FileOutputStream(getFileImpl(file)); 
	}
	
	@Override 
	public OutputStream append(IFile file) throws IOException {
		mkdirsIfNotExists(file);
		return new FileOutputStream(getFileImpl(file), true); 
	}
}
