package org.javenstudio.falcon.datum;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;

public interface ISection extends IData {

	public static interface Collector { 
		public void addSection(ISection section) throws ErrorException;
	}
	
	public ILibrary getLibrary();
	public ISectionRoot getRoot();
	
	public String getOwner();
	public String getChecksum();
	public String getAccessKey();
	
	public String getContentId();
	public String getContentKey();
	public String getContentType();
	
	public String getParentId();
	public String getParentPath();
	public long getContentLength();
	
	public int getSubCount();
	public long getSubLength();
	
	public ISectionSet getSubSections(ISectionQuery query) 
			throws ErrorException;
	
	public int getWidth() throws ErrorException;
	public int getHeight() throws ErrorException;
	public long getDuration() throws ErrorException;
	
	public int getMetaTag(Map<String,Object> tags) throws ErrorException;
	public int getMetaInfo(Map<String,Object> infos) throws ErrorException;
	//public void saveMetaInfo() throws ErrorException;
	
	public String[] getPosters() throws ErrorException;
	public String[] getBackgrounds() throws ErrorException;
	
	public long getModifiedTime();
	public long getIndexedTime();
	
	public boolean isFolder();
	public void close();
	
}
