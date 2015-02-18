package org.javenstudio.falcon.setting.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.UserManager;

public class StorageNode extends HostNode {
	private static final Logger LOG = Logger.getLogger(StorageNode.class);

	private final IHostCluster mCluster;
	private final IAttachUser[] mAttachUsers;
	
	public StorageNode(HostMode mode, String clusterId, String clusterDomain, String clusterSecret,
			String mailDomain, String hostDomain, String hostAddress, String hostName, 
			int httpPort, int httpsPort, String adminUser, String hostKey, int hostHash, String lanAddr, 
			IAttachUser[] attachUsers, IHostCluster cluster) throws ErrorException {
		super(mode, clusterId, clusterDomain, clusterSecret, mailDomain, hostDomain, hostAddress, 
			hostName, httpPort, httpsPort, adminUser, hostKey, hostHash, lanAddr);
		if (cluster == null) throw new NullPointerException();
		mAttachUsers = attachUsers;
		mCluster = cluster;
	}
	
	@Override
	public IHostInfo[] getAttachHosts() {
		Map<String,IHostInfo> map = new HashMap<String,IHostInfo>();
		if (mAttachUsers != null) {
			for (IAttachUser user : mAttachUsers) {
				if (user == null) continue;
				
				if (user instanceof AttachHostUser) {
					AttachHostUser hostUser = (AttachHostUser)user;
					IHostInfo host = hostUser.getHostNode();
					
					if (host != null && !map.containsKey((host.getHostKey())))
						map.put(host.getHostKey(), host);
				}
			}
		}
		return map.values().toArray(new IHostInfo[map.size()]);
	}
	
	@Override
	public final IAttachUser[] getAttachUsers(String hostkey) { 
		if (hostkey != null && hostkey.length() > 0) {
			ArrayList<IAttachUser> list = new ArrayList<IAttachUser>();
			if (mAttachUsers != null) {
				for (IAttachUser user : mAttachUsers) {
					if (user == null) continue;
					
					if (user instanceof AttachHostUser) {
						AttachHostUser hostUser = (AttachHostUser)user;
						IHostInfo host = hostUser.getHostNode();
						
						if (host != null && hostkey.equals(host.getHostKey()))
							list.add(user);
					}
				}
			}
			return list.toArray(new IAttachUser[list.size()]);
		}
		return mAttachUsers; 
	}
	
	@Override
	public String getAttachUserNames(String hostkey, int count) {
		IAttachUser[] attachUsers = getAttachUsers(hostkey);
		StringBuilder sbuf = new StringBuilder();
		if (attachUsers != null) {
			int num = 0;
			for (IAttachUser user : attachUsers) {
				if (user == null) continue;
				if (sbuf.length() > 0) sbuf.append(',');
				sbuf.append(user.getUserName());
				num ++;
				if (count > 0 && num >= count) {
					if (attachUsers.length > count)
						sbuf.append("...");
					break;
				}
			}
		}
		return sbuf.toString();
	}
	
	@Override
	public IHostCluster createCluster(HostManager manager, String clusterId) {
		if (manager == null || clusterId == null) throw new NullPointerException();
		if (!clusterId.equals(mCluster.getClusterId()))
			throw new IllegalArgumentException("Cluster id is wrong");
		return mCluster;
	}
	
	@Override
	public void setHeartbeatData(IHeartbeatData data) {
		super.setHeartbeatData(data);
		
		if (data != null && data instanceof AnyboxHost.HostGetData) {
			AnyboxHost.HostGetData hosts = (AnyboxHost.HostGetData)data;
			AnyboxUser.UserData[] userDatas = hosts.getUsers();
			IAttachUser[] attachUsers = getAttachUsers(null);
			
			if (userDatas != null && attachUsers != null) {
				for (AnyboxUser.UserData userData : userDatas) {
					if (userData == null) continue;
					String useremail = userData.getUserName(); // see HostHelper.processHeartbeat()
					if (useremail == null) continue;
					
					for (IAttachUser attachUser : attachUsers) {
						if (attachUser == null) continue;
						if (useremail.equals(attachUser.getUserEmail())) {
							addStorage(attachUser, userData);
							break;
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onRemoved() {
		super.onRemoved();
		
		IAttachUser[] attachUsers = getAttachUsers(null);
		if (attachUsers != null) {
			for (IAttachUser attachUser : attachUsers) {
				if (attachUser == null) continue;
				removeStorage(attachUser);
			}
		}
	}
	
	private void addStorage(IAttachUser user, AnyboxUser.UserData data) {
		if (user == null || data == null) return;
		
		StorageManager manager = UserManager.getInstance().getStorageManager(user.getUserKey());
		if (manager == null) {
			manager = new StorageManager(user);
			UserManager.getInstance().putStorageManager(user.getUserKey(), manager);
			
			if (LOG.isDebugEnabled())
				LOG.debug("addStorage: create StorageManager, user=" + user);
		}
		
		manager.addStorageNode(this, data);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("addStorage: user=" + user + " node=" + this 
					+ " data=" + data);
		}
	}
	
	private void removeStorage(IAttachUser user) {
		if (user == null) return;
		
		StorageManager manager = UserManager.getInstance().getStorageManager(user.getUserKey());
		if (manager == null) return;
		
		manager.removeStorageNode(this);
		if (LOG.isDebugEnabled()) 
			LOG.debug("removeStorage: user=" + user + " node=" + this);
		
		if (manager.getStorageCount() == 0) {
			UserManager.getInstance().removeStorageManager(user.getUserKey());
			if (LOG.isDebugEnabled()) 
				LOG.debug("removeStorage: remove StorageManager, user=" + user);
		}
	}
	
}
