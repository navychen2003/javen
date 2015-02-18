package org.javenstudio.falcon.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.fs.Path;

public interface ILibraryStore {

	public void loadLibraryList(DataManager manager) 
			throws ErrorException;
	
	public void saveLibraryList(DataManager manager, 
			ILibrary[] libraries) throws ErrorException;
	
	public Path getLibraryPath(DataManager manager, 
			ILibrary library) throws ErrorException;
	
}
