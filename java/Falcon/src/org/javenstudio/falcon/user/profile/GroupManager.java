package org.javenstudio.falcon.user.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.Member;
import org.javenstudio.falcon.user.User;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class GroupManager {
	private static final Logger LOG = Logger.getLogger(GroupManager.class);

	public static class GroupUser { 
		private final String mKey;
		private final String mName;
		private final String mRole;
		
		public GroupUser(String key, String name, String role) { 
			if (key == null || name == null || role == null) 
				throw new NullPointerException();
			mKey = key;
			mName = name;
			mRole = role;
		}
		
		public String getKey() { return mKey; }
		public String getName() { return mName; }
		public String getRole() { return mRole; }
		
		@Override
		public String toString() { 
			return getClass().getSimpleName() + "{key=" + mKey 
					+ ",name=" + mName + ",role=" + mRole + "}";
		}
	}
	
	private final Member mUser;
	private final IGroupStore mStore;
	
	private final Map<String, GroupUser> mGroups = 
			new HashMap<String, GroupUser>();
	
	private final Map<String, Invite> mInvites = 
			new HashMap<String, Invite>();
	
	private volatile boolean mLoaded = false;
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return GroupManager.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "GroupManager(" + GroupManager.this.getUser().getUserName() + ")";
			}
		};
	
	public GroupManager(Member user, IGroupStore store) throws ErrorException { 
		if (user == null || store == null) throw new NullPointerException();
		mUser = user;
		mStore = store;
		loadGroups(false);
	}
	
	public Member getUser() { return mUser; }
	public IGroupStore getStore() { return mStore; }
	public ILockable.Lock getLock() { return mLock; }
	
	public synchronized Invite removeInvite(String key) { 
		if (key == null) return null;
		
		synchronized (mStore) { 
			synchronized (mInvites) { 
				return mInvites.remove(key);
			}
		}
	}
	
	public synchronized Invite addInvite(Invite invite) { 
		if (invite == null) return null;
		
		synchronized (mStore) { 
			synchronized (mInvites) { 
				mInvites.put(invite.getKey(), invite);
				return invite;
			}
		}
	}
	
	public synchronized Invite getInvite(String key) { 
		if (key == null) return null;
		
		synchronized (mStore) { 
			synchronized (mInvites) { 
				return mInvites.get(key);
			}
		}
	}
	
	public synchronized Invite[] getInvites() { 
		synchronized (mStore) { 
			synchronized (mInvites) { 
				return mInvites.values().toArray(new Invite[mInvites.size()]);
			}
		}
	}
	
	public synchronized int getInviteCount(String type) { 
		if (type == null) return 0;
		
		synchronized (mStore) { 
			synchronized (mInvites) { 
				int count = 0;
				
				for (Invite invite : mInvites.values()) { 
					if (invite == null) continue;
					if (type.equals(invite.getType()))
						count ++;
				}
				
				return count;
			}
		}
	}
	
	public synchronized boolean existGroup(String key) { 
		if (key == null) return false;
		
		synchronized (mStore) { 
			synchronized (mGroups) { 
				for (GroupUser group : mGroups.values()) { 
					if (group == null) continue;
					if (key.equals(group.getKey())) return true;
				}
				
				return false;
			}
		}
	}
	
	public synchronized GroupUser removeGroup(String key) { 
		if (key == null) return null;
		
		synchronized (mStore) { 
			synchronized (mGroups) { 
				GroupUser removed = mGroups.remove(key);
				return removed;
			}
		}
	}
	
	public GroupUser addGroup(String key, String name, String role) { 
		if (key == null || name == null || role == null) return null;
		return addGroup(new GroupUser(key, name, role));
	}
	
	public synchronized GroupUser addGroup(GroupUser gm) { 
		if (gm == null) return null;
		
		synchronized (mStore) { 
			synchronized (mGroups) { 
				GroupUser group = mGroups.get(gm.getKey());
				if (group == null) { 
					group = gm;
					mGroups.put(group.getKey(), group);
				}
				
				return group;
			}
		}
	}
	
	public synchronized GroupUser[] getGroups() { 
		synchronized (mStore) { 
			synchronized (mGroups) { 
				return mGroups.values().toArray(new GroupUser[mGroups.size()]);
			}
		}
	}
	
	public synchronized void saveGroups() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("saveGroups");
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (mStore) {
				synchronized (mGroups) { 
					getStore().saveGroupList(this, toNamedList(getGroups()));
					getStore().saveInviteList(this, toNamedList(getInvites()));
					
					((User)getUser()).setModifiedTime(System.currentTimeMillis());
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadGroups(boolean force) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadGroups: force=" + force);
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (mStore) {
				synchronized (mGroups) { 
					if (mLoaded && force == false)
						return;
					
					mGroups.clear();
					mInvites.clear();
					
					loadGroupList(this, getStore().loadGroupList(this));
					loadInviteList(this, getStore().loadInviteList(this));
					
					mLoaded = true;
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.READ);
		}
	}
	
	public boolean isClosed() { return mClosed; }
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
		
		try {
			getLock().lock(ILockable.Type.READ, null);
			try {
				synchronized (mStore) {
					synchronized (mGroups) { 
						getUser().removeGroupManager();
						
						mGroups.clear();
						mInvites.clear();
						mLoaded = false;
					}
				}
			} finally { 
				getLock().unlock(ILockable.Type.READ);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
	public synchronized IGroup[] getGroups(String role) {
		ArrayList<IGroup> groups = new ArrayList<IGroup>();
		try {
			GroupManager gm = this;
			if (gm != null) {
				GroupManager.GroupUser[] gus = gm.getGroups();
				if (gus != null) {
					for (GroupManager.GroupUser gu : gus) {
						if (gu != null && (role == null || role.length() == 0 || gu.getRole().equals(role))) {
							IUser usr = UserHelper.getLocalUserByName(gu.getName());
							
							if (usr != null && usr instanceof IGroup) {
								IGroup group = (IGroup)usr;
								groups.add(group);
							}
						}
					}
				}
			}
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("getGroups: error: " + e, e);
		}
		return groups.toArray(new IGroup[groups.size()]);
	}
	
	static void loadGroupList(GroupManager manager, 
			NamedList<Object> item) throws ErrorException { 
		if (manager == null || item == null) return;
		
		for (int i=0; i < item.size(); i++) { 
			@SuppressWarnings("unused")
			String key = item.getName(i);
			Object val = item.getVal(i);
			
			if (LOG.isDebugEnabled())
				LOG.debug("loadGroups: listItem=" + val);
			
			if (val != null && val instanceof NamedList) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> listItem = (NamedList<Object>)val;
				
				GroupUser group = loadGroup(listItem);
				if (group != null)
					manager.addGroup(group);
			}
		}
	}
	
	static NamedList<Object> toNamedList(GroupUser[] list) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; list != null && i < list.length; i++) { 
			GroupUser group = list[i];
			NamedList<Object> item = toNamedList(group);
			if (group != null && item != null) 
				items.add(group.getKey(), item);
		}
		
		return items;
	}
	
	static void loadInviteList(GroupManager manager, 
			NamedList<Object> items) throws ErrorException { 
		if (manager == null || items == null) return;
		
		for (int i=0; i < items.size(); i++) { 
			String name = items.getName(i);
			Object value = items.getVal(i);
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("loadInviteList: name=" + name + " value=" + value);
			
			if (value != null && value instanceof NamedList) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> item = (NamedList<Object>)value;
				
				Invite invite = Invite.loadInvite(item);
				manager.addInvite(invite);
			}
		}
	}
	
	static NamedList<Object> toNamedList(Invite[] list) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; list != null && i < list.length; i++) { 
			Invite invite = list[i];
			NamedList<Object> item = Invite.toNamedList(invite);
			if (invite != null && item != null) 
				items.add(invite.getKey(), item);
		}
		
		return items;
	}
	
	static GroupUser loadGroup(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		String key = SettingConf.getString(item, "key");
		String name = SettingConf.getString(item, "name");
		String role = SettingConf.getString(item, "role");
		
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Group key: " + key + " is wrong");
		}
		
		if (name == null || name.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Group name: " + name + " is wrong");
		}
		
		if (role == null || role.length() == 0) { 
			//throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
			//		"Group role: " + role + " is wrong");
			
			role = MemberManager.ROLE_MEMBER;
		}
		
		GroupUser group = new GroupUser(key, name, role);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadGroup: group=" + group);
		
		return group;
	}
	
	static NamedList<Object> toNamedList(GroupUser item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("key", SettingConf.toString(item.getKey()));
		info.add("name", SettingConf.toString(item.getName()));
		info.add("role", SettingConf.toString(item.getRole()));
		
		return info;
	}
	
}
