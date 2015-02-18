package org.javenstudio.raptor.fs;

import java.io.File;

public abstract class LocalFileStatus extends FileStatus {

	private final File mFile;
	
	public LocalFileStatus(File file, long length, boolean isdir, boolean ishidden, 
			int block_replication, long blocksize, long modification_time, Path path) {
		super(length, isdir, ishidden, block_replication, blocksize, 
				modification_time, path);
		mFile = file;
	}
	
	public File getFile() { return mFile; }
	
}
