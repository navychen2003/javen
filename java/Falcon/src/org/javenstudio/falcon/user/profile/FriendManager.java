package org.javenstudio.falcon.user.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.Member;
import org.javenstudio.falcon.user.User;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class FriendManager {
	private static final Logger LOG = Logger.getLogger(FriendManager.class);

	private final Member mUser;
	private final IFriendStore mStore;
	
	private final Map<String, FriendGroup> mFriends = 
			new HashMap<String, FriendGroup>();
	
	private final Map<String, Invite> mInvites = 
			new HashMap<String, Invite>();
	
	private volatile boolean mLoaded = false;
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return FriendManager.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "FriendManager(" + FriendManager.this.getUser().getUserName() + ")";
			}
		};
	
	public FriendManager(Member user, IFriendStore store) throws ErrorException { 
		if (user == null || store == null) throw new NullPointerException();
		mUser = user;
		mStore = store;
		loadFriends(false);
	}
	
	public Member getUser() { return mUser; }
	public IFriendStore getStore() { return mStore; }
	public ILockable.Lock getLock() { return mLock; }
	
	public synchronized Invite removeInvite(String key) { 
		if (key == null) return null;
		
		synchronized (mUser) { 
			synchronized (mInvites) { 
				return mInvites.remove(key);
			}
		}
	}
	
	public synchronized Invite addInvite(Invite invite) { 
		if (invite == null) return null;
		
		synchronized (mUser) { 
			synchronized (mInvites) { 
				mInvites.put(invite.getKey(), invite);
				return invite;
			}
		}
	}
	
	public synchronized Invite getInvite(String key) { 
		if (key == null) return null;
		
		synchronized (mUser) { 
			synchronized (mInvites) { 
				return mInvites.get(key);
			}
		}
	}
	
	public synchronized Invite[] getInvites() { 
		synchronized (mUser) { 
			synchronized (mInvites) { 
				return mInvites.values().toArray(new Invite[mInvites.size()]);
			}
		}
	}
	
	public synchronized Invite[] getInvites(String type) { 
		if (type == null) return null;
		
		synchronized (mUser) { 
			synchronized (mInvites) { 
				ArrayList<Invite> list = new ArrayList<Invite>();
				
				for (Invite invite : mInvites.values()) { 
					if (invite == null) continue;
					if (type.equals(invite.getType()))
						list.add(invite);
				}
				
				return list.toArray(new Invite[list.size()]);
			}
		}
	}
	
	public InviteSet getInviteSet(String type) {
		Invite[] invites = getInvites(type);
		if (invites == null) invites = new Invite[0];
		long utime = 0;
		for (Invite invite : invites) {
			if (invite != null && invite.getTime() > utime)
				utime = invite.getTime();
		}
		return new InviteSet(invites, utime);
	}
	
	public synchronized Friend getFriend(String key) { 
		if (key == null) return null;
		
		synchronized (mUser) { 
			synchronized (mFriends) { 
				for (FriendGroup group : mFriends.values()) { 
					if (group == null) continue;
					
					Friend friend = group.getFriend(key);
					if (friend != null) return friend;
				}
				
				return null;
			}
		}
	}
	
	public synchronized Friend removeFriend(String key) { 
		if (key == null) return null;
		
		synchronized (mUser) { 
			synchronized (mFriends) { 
				Friend friend = null;
				
				for (FriendGroup group : mFriends.values()) { 
					if (group == null) continue;
					Friend removed = group.removeFriend(key);
					if (removed != null) friend = removed;
				}
				
				return friend;
			}
		}
	}
	
	public synchronized Friend addFriend(Friend contact, String type) { 
		if (contact == null || type == null) return null;
		
		synchronized (mUser) { 
			synchronized (mFriends) { 
				FriendGroup group = mFriends.get(type);
				if (group == null) { 
					group = new FriendGroup(this, type);
					mFriends.put(type, group);
				}
				
				return group.addFriend(contact);
			}
		}
	}
	
	public synchronized FriendGroup[] getFriendGroups() { 
		synchronized (mUser) { 
			synchronized (mFriends) { 
				return mFriends.values().toArray(new FriendGroup[mFriends.size()]);
			}
		}
	}
	
	public synchronized String[] getFriendTypes() { 
		synchronized (mUser) { 
			synchronized (mFriends) { 
				return mFriends.keySet().toArray(new String[mFriends.size()]);
			}
		}
	}
	
	public synchronized FriendGroup getFriendGroup(String type) { 
		synchronized (mUser) { 
			synchronized (mFriends) { 
				return mFriends.get(type);
			}
		}
	}
	
	public synchronized void saveFriends() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("saveFriends: user=" + mUser);
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (mUser) {
				synchronized (mFriends) { 
					getStore().saveFriendList(this, toNamedList(getFriendGroups()));
					getStore().saveInviteList(this, toNamedList(getInvites()));
					
					User user = (User)getUser();
					user.setInvites(getInviteSet(Invite.TYPE_IN));
					user.setModifiedTime(System.currentTimeMillis());
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadFriends(boolean force) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadFriends: user=" + mUser);
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (mUser) {
				synchronized (mFriends) { 
					if (mLoaded && force == false)
						return;
					
					mFriends.clear();
					mInvites.clear();
					
					loadGroupList(this, getStore().loadFriendList(this));
					loadInviteList(this, getStore().loadInviteList(this));
					
					User user = (User)getUser();
					user.setInvites(getInviteSet(Invite.TYPE_IN));
					
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
				synchronized (mUser) {
					synchronized (mFriends) { 
						FriendGroup[] groups = mFriends.values().toArray(
								new FriendGroup[mFriends.size()]);
						
						if (groups != null) { 
							for (FriendGroup group : groups) { 
								if (group != null) group.close();
							}
						}
						
						getUser().removeFriendManager();
						
						mFriends.clear();
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
	
	static void loadGroupList(FriendManager manager, 
			NamedList<Object> items) throws ErrorException { 
		if (manager == null || items == null) return;
		
		for (int i=0; i < items.size(); i++) { 
			String name = items.getName(i);
			Object value = items.getVal(i);
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("loadGroupList: name=" + name + " value=" + value);
			
			if (value != null && value instanceof NamedList) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> item = (NamedList<Object>)value;
				
				FriendGroup.loadFriendGroup(manager, item);
			}
		}
	}
	
	static NamedList<Object> toNamedList(FriendGroup[] list) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; list != null && i < list.length; i++) { 
			FriendGroup group = list[i];
			NamedList<Object> item = FriendGroup.toNamedList(group);
			if (group != null && item != null) 
				items.add(group.getFriendType(), item);
		}
		
		return items;
	}
	
	static void loadInviteList(FriendManager manager, 
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
	
}
