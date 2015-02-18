package org.javenstudio.falcon.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatumCore;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.user.device.IDeviceStore;
import org.javenstudio.falcon.user.global.IAnnouncementStore;
import org.javenstudio.falcon.user.profile.IContactStore;
import org.javenstudio.falcon.user.profile.IFriendStore;
import org.javenstudio.falcon.user.profile.IGroupStore;
import org.javenstudio.falcon.user.profile.IHistoryStore;
import org.javenstudio.falcon.user.profile.IMemberStore;
import org.javenstudio.falcon.user.profile.IPreferenceStore;
import org.javenstudio.falcon.user.profile.IProfileStore;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

public interface IUserStore extends IDeviceStore, IContactStore, 
		IProfileStore, IFriendStore, IPreferenceStore, IHistoryStore, 
		IMemberStore, IGroupStore, IAnnouncementStore {

	public Configuration getConfiguration();
	public IAuthService getService() throws ErrorException;
	public IDatumCore getDatumCore() throws ErrorException;
	
	public IHostNode getHostNode();
	public String[] loadManagers() throws ErrorException;
	
	public Path getAuthStorePath(String dirname, String name, 
			boolean forWrite) throws ErrorException;
	public FileSystem getAuthStoreFs() throws ErrorException;
	
}
