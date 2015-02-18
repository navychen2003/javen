package org.javenstudio.cocoka.net.http.download;

import org.javenstudio.cocoka.storage.fs.IFile;

public interface SynchronizeCallback {

	public interface Checker { 
		public boolean isExisted(String filename); 
	}
	
	public boolean canRemove(IFile file, Checker checker); 
	
}
