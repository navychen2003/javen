package org.javenstudio.falcon.setting.cluster;

import java.util.HashMap;
import java.util.Map;

public class StorageManager {

	static class StorageNodeItem implements IStorageInfo {
		private final StorageNode mHostNode;
		private final AnyboxUser.UserData mUserData;
		private final long mRequestTime = System.currentTimeMillis();
		
		public StorageNodeItem(StorageNode node, AnyboxUser.UserData data) {
			if (node == null || data == null) throw new NullPointerException();
			mHostNode = node;
			mUserData = data;
		}
		
		public StorageNode getHostNode() { return mHostNode; }
		public AnyboxUser.UserData getStorageUser() { return mUserData; }
		public long getRequestTime() { return mRequestTime; }
		
		public AnyboxLibrary.LibraryData[] getStorageLibraries() {
			return getStorageUser().getLibraries();
		}
	}
	
	private final IAttachUser mUser;
	
	private final Map<String,StorageNodeItem> mNodes = 
			new HashMap<String,StorageNodeItem>();
	
	public StorageManager(IAttachUser user) {
		if (user == null) throw new NullPointerException();
		mUser = user;
	}
	
	public IAttachUser getAttachUser() { return mUser; }
	
	void addStorageNode(StorageNode node, AnyboxUser.UserData data) {
		if (node == null || data == null) return;
		synchronized (mNodes) {
			mNodes.put(node.getHostKey(), new StorageNodeItem(node, data));
		}
	}
	
	void removeStorageNode(StorageNode node) {
		if (node == null) return;
		synchronized (mNodes) {
			mNodes.remove(node.getHostKey());
		}
	}
	
	public int getStorageCount() {
		synchronized (mNodes) {
			return mNodes.size();
		}
	}
	
	public IStorageInfo[] getStorages() {
		synchronized (mNodes) {
			return mNodes.values().toArray(new IStorageInfo[mNodes.size()]);
		}
	}
	
}
