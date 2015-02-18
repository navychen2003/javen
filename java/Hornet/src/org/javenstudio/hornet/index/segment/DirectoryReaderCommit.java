package org.javenstudio.hornet.index.segment;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.index.IndexCommit;

final class DirectoryReaderCommit extends IndexCommit {
	
	private final IDirectory mDirectory;
	private final String mSegmentsFileName;
	private final Collection<String> mFiles;
	private final Map<String,String> mUserData;
    private final int mSegmentCount;
    private final long mGeneration;

	DirectoryReaderCommit(ISegmentInfos infos) throws IOException {
		mDirectory = infos.getDirectory();
		mSegmentsFileName = infos.getSegmentsFileName();
		mUserData = infos.getUserData();
		mFiles = Collections.unmodifiableCollection(infos.getFileNames(true));
		mGeneration = infos.getGeneration();
		mSegmentCount = infos.size();
    }

    @Override
    public int getSegmentCount() {
    	return mSegmentCount;
    }

    @Override
    public String getSegmentsFileName() {
    	return mSegmentsFileName;
    }

    @Override
    public Collection<String> getFileNames() {
    	return mFiles;
    }

    @Override
    public IDirectory getDirectory() {
    	return mDirectory;
    }

    @Override
    public long getGeneration() {
    	return mGeneration;
    }

    @Override
    public boolean isDeleted() {
      return false;
    }

    @Override
    public Map<String,String> getUserData() {
      return mUserData;
    }

    @Override
    public void delete() {
    	throw new UnsupportedOperationException("This IndexCommit does not support deletions");
    }
    
    @Override
    public String toString() {
    	return "DirectoryReaderCommit(" + mSegmentsFileName + ")";
    }
    
}
