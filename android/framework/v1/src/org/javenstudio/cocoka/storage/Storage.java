package org.javenstudio.cocoka.storage;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException; 
import java.text.SimpleDateFormat;
import java.util.Date;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.IFileSystem;
import org.javenstudio.cocoka.storage.fs.Path;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.common.util.Logger;

public abstract class Storage {
	private static Logger LOG = Logger.getLogger(Storage.class);

	public static final int CACHE_CONTENT_SIZE = 10240; 
	public static final int BUFFER_SIZE = 8192; 
	
	private static SimpleDateFormat sFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	
	private final StorageManager mManager; 
	private final IFileSystem mFileSystem; 
	private final FileLoader mLoader; 
	
	public Storage(StorageManager manager, IFileSystem fs) {
		mManager = manager; 
		mFileSystem = fs; 
		mLoader = new FileLoader(this); 
	}
	
	public final StorageManager getManager() {
		return mManager; 
	}
	
	public final IFileSystem getFileSystem() {
		return mFileSystem; 
	}
	
	public final FileLoader getLoader() {
		return mLoader; 
	}
	
	public abstract IFile getDirectory(); 
	
	public String getDirectoryLocation() { 
		return getDirectory().getLocation(); 
	}
	
	public void clearDirectory() {
		clearFiles(getDirectory()); 
	}
	
	public boolean existsFile(String filepath) {
		if (filepath == null || filepath.length() == 0) 
			return false; 
		
		try {
			final IFileSystem fs = getFileSystem(); 
			
			IFile file = null; 
			if (filepath.startsWith("/")) 
				file = fs.getFile(new Path(filepath)); 
			else
				file = fs.getFile(new Path(getDirectory().toPath(), filepath)); 
			
			return existsFile(file); 
			
		} catch (Exception e) {
			return false; 
		}
	}
	
	public boolean existsFile(IFile file) {
		if (file == null) return false; 
		
		try {
			final IFileSystem fs = getFileSystem(); 
			return fs.exists(file); 
			
		} catch (Exception e) {
			return false; 
		}
	}
	
	public void clearFiles(IFile dir) {
		if (dir == null) return; 
		
		IFile[] files = listFiles(dir); 
		for (int i=0; files != null && i < files.length; i++) {
			IFile file = files[i]; 
			if (file == null) continue; 
			
			if (file.isDirectory()) 
				removeDir(file); 
			else 
				removeFile(file); 
		}
	}
	
	public IFile[] listFiles(String dir) {
		try {
			final IFileSystem fs = getFileSystem(); 
			
			return listFiles(fs.getFile(new Path(dir))); 
			
		} catch (Exception e) {
			LOG.error("list files error at "+dir, e); 
		}
		
		return null; 
	}
	
	public IFile[] listFiles(IFile dir) {
		if (dir == null) return null; 
		
		try {
			final IFileSystem fs = getFileSystem(); 
			
			return fs.listFiles(dir); 
			
		} catch (Exception e) {
			LOG.error("list files error at "+dir.getPath(), e); 
		}
		
		return null; 
	}
	
	public void removeFile(IFile file) {
		if (file == null) return; 
		
		try {
			final IFileSystem fs = getFileSystem(); 
			
			if (file.isDirectory()) {
				removeDir(file); 
				
			} else {
				fs.delete(file); 
				LOG.info("deleted file: "+file.getPath()); 
			}
			
		} catch (Exception e) {
			LOG.error("remove file error at "+file.getPath(), e); 
		}
	}
	
	public void removeDir(IFile dir) {
		if (dir == null) return; 
		
		try {
			final IFileSystem fs = getFileSystem(); 
			
			if (dir.isDirectory()) {
				IFile[] files = listFiles(dir); 
				for (int i=0; files != null && i < files.length; i++) {
					IFile file = files[i]; 
					if (file == null) continue; 
					
					if (file.isDirectory()) 
						removeDir(file); 
					else
						removeFile(file); 
				}
				
				fs.delete(dir); 
				LOG.info("deleted directory: "+dir.getPath()); 
				
			} else
				removeFile(dir); 
			
		} catch (Exception e) {
			LOG.error("remove directory error at "+dir.getPath(), e); 
		}
	}
	
	public int getWriteBufferSize() {
		return BUFFER_SIZE; 
	}
	
	public int getReadBufferSize() {
		return BUFFER_SIZE; 
	}
	
	public IFile newFsFileByName(String filename) throws IOException {
		if (filename == null || filename.length() == 0) { 
			//throw new IOException("filename input null"); 
			return getFileSystem().getFile(getDirectory().toPath()); 
		}
		
		return getFileSystem().getFile(new Path(getDirectory().toPath(), filename)); 
	}
	
	public IFile newFsFileByExtension(String extensionName) throws IOException {
		String name = sFormat.format(new Date(System.currentTimeMillis())).toString(); 
		if (extensionName != null && extensionName.length() > 0) 
			name += "." + extensionName; 
		
		return getFileSystem().getFile(new Path(getDirectory().toPath(), name)); 
	}
	
	public TempFile createTempFile(MimeType type, String fileName, String extensionName) throws IOException {
		StorageFile file = createFile(type, fileName, extensionName); 
		return new TempFile(file.getStorage(), file); 
	}
	
	public StorageFile createFile(MimeType type, String fileName, String extensionName) throws IOException {
		IFile file = fileName == null ? newFsFileByExtension(extensionName) : 
			getFileSystem().getFile(new Path(getDirectory().toPath(), fileName + "." + extensionName)); 
		
		return getFile(type, file); 
	}
	
	public IFile getFsFile(String filename) throws IOException { 
		return getFileSystem().getFile(new Path(getDirectory().toPath(), filename)); 
	}
	
	public StorageFile getFile(MimeType type, String filename) throws IOException {
		return getFile(type, getFsFile(filename)); 
	}
	
	public StorageFile getFileByPath(MimeType type, String filepath) throws IOException {
		IFile file = getFileSystem().getFile(new Path(filepath)); 
		
		return getFile(type, file); 
	}
	
	public final StorageFile getFile(MimeType type, IFile file) throws IOException {
		if (type == null || file == null) 
			return null; 
		
		Class<?> clazz = FileRegistry.lookup(type); 
		
		if (clazz == null) 
			throw new IOException("no StorageFile class defined for "+type); 
		
		if (!StorageFile.class.isAssignableFrom(clazz)) 
			throw new IOException(clazz+" is not a StorageFile class for "+type); 

		try {
			Constructor<?> ctor = clazz.getConstructor(Storage.class, IFile.class); 
			
			StorageFile instance = (StorageFile) ctor.newInstance(this, file); 
			
			if (instance == null) 
				throw new IOException("construct null "+clazz+" object for "+type); 
			
			return instance; 
			
		} catch (NoSuchMethodException e) {
			throw new IOException("construct a "+clazz+" object error for "+type+": "+e); 
		} catch (IllegalAccessException e) {
			throw new IOException("construct a "+clazz+" object error for "+type+": "+e); 
		} catch (InvocationTargetException e) {
			throw new IOException("construct a "+clazz+" object error for "+type+": "+e); 
		} catch (InstantiationException e) {
			throw new IOException("construct a "+clazz+" object error for "+type+": "+e); 
		}
	}
	
}
