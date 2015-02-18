package org.javenstudio.falcon.user.profile;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.user.Group;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class MemberManager {
	private static final Logger LOG = Logger.getLogger(MemberManager.class);

	public static final String ROLE_OWNER = "owner";
	public static final String ROLE_MANAGER = "manager";
	public static final String ROLE_MEMBER = "member";
	
	public static class GroupMember { 
		private final String mKey;
		private final String mName;
		private final String mRole;
		
		GroupMember(String key, String name, String role) { 
			if (key == null || name == null) throw new NullPointerException();
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
	
	private final Group mUser;
	private final IMemberStore mStore;
	
	private final Map<String, GroupMember> mMembers = 
			new HashMap<String, GroupMember>();
	
	private final Map<String, Invite> mInvites = 
			new HashMap<String, Invite>();
	
	private volatile boolean mLoaded = false;
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return MemberManager.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "MemberManager(" + MemberManager.this.getUser().getUserName() + ")";
			}
		};
	
	public MemberManager(Group user, IMemberStore store) throws ErrorException { 
		if (user == null || store == null) throw new NullPointerException();
		mUser = user;
		mStore = store;
		loadMembers(false);
	}
	
	public Group getUser() { return mUser; }
	public IMemberStore getStore() { return mStore; }
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
	
	public synchronized int getInviteCount(String type) { 
		if (type == null) return 0;
		
		synchronized (mUser) { 
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
	
	public synchronized GroupMember getMember(String key) { 
		if (key == null) return null;
		
		synchronized (mUser) { 
			synchronized (mMembers) { 
				for (GroupMember member : mMembers.values()) { 
					if (member == null) continue;
					if (key.equals(member.getKey())) return member;
				}
				
				return null;
			}
		}
	}
	
	public synchronized GroupMember removeMember(String key) { 
		if (key == null) return null;
		
		synchronized (mUser) { 
			synchronized (mMembers) { 
				GroupMember removed = mMembers.remove(key);
				
				return removed;
			}
		}
	}
	
	public synchronized GroupMember addMember(String key, 
			String name, String role) { 
		if (key == null || name == null) return null;
		return addMember(new GroupMember(key, name, role));
	}
	
	public synchronized GroupMember addMember(GroupMember gm) { 
		if (gm == null) return null;
		
		synchronized (mUser) { 
			synchronized (mMembers) { 
				GroupMember member = mMembers.get(gm.getKey());
				if (member == null) { 
					member = gm;
					mMembers.put(member.getKey(), member);
				}
				
				return member;
			}
		}
	}
	
	public synchronized GroupMember[] getMembers() { 
		synchronized (mUser) { 
			synchronized (mMembers) { 
				return mMembers.values().toArray(new GroupMember[mMembers.size()]);
			}
		}
	}
	
	public synchronized int getMemberCount() { 
		synchronized (mUser) { 
			synchronized (mMembers) { 
				return mMembers.size();
			}
		}
	}
	
	public synchronized void saveMembers() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("saveMembers: user=" + mUser);
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (mUser) {
				synchronized (mMembers) { 
					getStore().saveMemberList(this, toNamedList(getMembers()));
					getStore().saveInviteList(this, toNamedList(getInvites()));
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadMembers(boolean force) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadMembers: user=" + mUser);
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (mUser) {
				synchronized (mMembers) { 
					if (mLoaded && force == false)
						return;
					
					mMembers.clear();
					mInvites.clear();
					
					loadMemberList(this, getStore().loadMemberList(this));
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
				synchronized (mUser) {
					synchronized (mMembers) { 
						getUser().removeMemberManager();
						
						mMembers.clear();
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
	
	static void loadMemberList(MemberManager manager, 
			NamedList<Object> item) throws ErrorException { 
		if (manager == null || item == null) return;
		
		for (int i=0; i < item.size(); i++) { 
			@SuppressWarnings("unused")
			String key = item.getName(i);
			Object val = item.getVal(i);
			
			if (LOG.isDebugEnabled())
				LOG.debug("loadMembers: listItem=" + val);
			
			if (val != null && val instanceof NamedList) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> listItem = (NamedList<Object>)val;
				
				GroupMember member = loadMember(listItem);
				if (member != null)
					manager.addMember(member);
			}
		}
	}
	
	static NamedList<Object> toNamedList(GroupMember[] list) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; list != null && i < list.length; i++) { 
			GroupMember member = list[i];
			NamedList<Object> item = toNamedList(member);
			if (member != null && item != null) 
				items.add(member.getKey(), item);
		}
		
		return items;
	}
	
	static void loadInviteList(MemberManager manager, 
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
	
	static GroupMember loadMember(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		String key = SettingConf.getString(item, "key");
		String name = SettingConf.getString(item, "name");
		String role = SettingConf.getString(item, "role");
		
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Member key: " + key + " is wrong");
		}
		
		if (name == null || name.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Member name: " + name + " is wrong");
		}
		
		GroupMember member = new GroupMember(key, name, role);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadMember: member=" + member);
		
		return member;
	}
	
	static NamedList<Object> toNamedList(GroupMember item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("key", toString(item.getKey()));
		info.add("name", toString(item.getName()));
		info.add("role", toString(item.getRole()));
		
		return info;
	}
	
	public static String toString(Object o) { 
		if (o == null) return "";
		if (o instanceof String) return (String)o;
		if (o instanceof CharSequence) return ((CharSequence)o).toString();
		return o.toString();
	}
	
}
