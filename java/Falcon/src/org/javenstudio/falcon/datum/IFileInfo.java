package org.javenstudio.falcon.datum;

import org.javenstudio.falcon.ErrorException;

public interface IFileInfo {

	public String getName();
	public String getContentId();
	public String getContentPath();
	public String getContentType();
	
	public IFolderInfo getRootInfo() throws ErrorException;
	public IFolderInfo getParentInfo() throws ErrorException;
	
	public boolean canMove();
	public boolean canDelete();
	public boolean canWrite();
	
}
