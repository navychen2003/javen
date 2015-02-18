package org.javenstudio.cocoka.storage;

import java.io.IOException; 

import org.javenstudio.cocoka.storage.fs.FileSystems;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.LocalFileSystem;
import org.javenstudio.cocoka.storage.fs.Path;

public class LocalStorage extends Storage {

	private static StorageListener mListener = null;
	
	public static void setStorageListener(StorageListener listener) { 
		mListener = listener;
	}
	
	private final IFile mDirectory; 
	
	public LocalStorage(StorageManager manager, String directory) throws IOException {
		super(manager, FileSystems.get(LocalFileSystem.LOCAL_SCHEME, null)); 
		
		mDirectory = getFileSystem().getFile(new Path(directory)); 
		
		getFileSystem().mkdirs(mDirectory); 
		
		StorageListener listener = mListener;
		if (listener != null) 
			listener.onStorageOpened(this); 
	}
	
	@Override 
	public final IFile getDirectory() {
		return mDirectory; 
	}
	
}
