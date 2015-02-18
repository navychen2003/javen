package org.javenstudio.falcon.datum;

import org.javenstudio.falcon.ErrorException;

public interface IFolderInfo extends IFileInfo {
	
	public interface Filter { 
		public boolean accept(IFileInfo file);
	}
	
	public IFolderInfo[] listFolderInfos(boolean refresh, Filter filter) 
			throws ErrorException;
	
	public IFileInfo[] listFileInfos(boolean refresh, Filter filter) 
			throws ErrorException;
	
	public boolean isHomeFolder();
	public boolean isRootFolder();
	
}
