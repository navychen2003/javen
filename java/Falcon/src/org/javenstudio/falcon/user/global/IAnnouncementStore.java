package org.javenstudio.falcon.user.global;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IAnnouncementStore {

	public NamedList<Object> loadAnnouncementList(AnnouncementManager manager) 
			throws ErrorException;
	
	public void saveAnnouncementList(AnnouncementManager manager, 
			NamedList<Object> items) throws ErrorException;
	
}
