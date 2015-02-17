package org.javenstudio.cocoka.storage;

import java.io.IOException; 

import org.javenstudio.cocoka.storage.fs.FileSystems;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.LocalFileSystem;
import org.javenstudio.cocoka.storage.fs.Path;

public class LocalStorage extends Storage {

	private final IFile mDirectory; 
	
	LocalStorage(StorageManager manager, String name, String directory) throws IOException {
		super(manager, FileSystems.get(LocalFileSystem.LOCAL_SCHEME, null), name); 
		
		mDirectory = getFileSystem().getFile(new Path(directory)); 
		getFileSystem().mkdirs(mDirectory); 
	}
	
	@Override 
	public final IFile getDirectory() {
		return mDirectory; 
	}
	
}
