package org.javenstudio.falcon.user.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class FriendGroup {
	private static final Logger LOG = Logger.getLogger(FriendGroup.class);

	private final Map<String, Friend> mFriends = new HashMap<String, Friend>();
	private final FriendManager mManager;
	private final String mType;
	
	public FriendGroup(FriendManager manager, String type) { 
		if (manager == null || type == null) throw new NullPointerException();
		mManager = manager;
		mType = type;
	}

	public FriendManager getManager() { return mManager; }
	public String getFriendType() { return mType; }
	
	public Friend addFriend(Friend friend) { 
		if (friend == null) return null;
		
		synchronized (mFriends) { 
			String key = friend.getKey();
			
			Friend item = mFriends.get(key);
			if (item != null) { 
				//item.addAll(friend);
				return item;
			}
			
			mFriends.put(key, friend);
			return friend;
		}
	}
	
	public boolean existFriend(String key) { 
		if (key == null) return false;
		
		synchronized (mFriends) { 
			return mFriends.containsKey(key);
		}
	}
	
	public Friend removeFriend(String key) { 
		if (key == null) return null;
		
		synchronized (mFriends) { 
			return mFriends.remove(key);
		}
	}
	
	public Friend getFriend(String key) { 
		if (key == null) return null;
		
		synchronized (mFriends) { 
			return mFriends.get(key);
		}
	}
	
	public String[] getFriendKeys() { 
		synchronized (mFriends) { 
			return mFriends.keySet().toArray(new String[mFriends.size()]);
		}
	}
	
	public Friend[] getFriends() { 
		synchronized (mFriends) { 
			return mFriends.values().toArray(new Friend[mFriends.size()]);
		}
	}
	
	public void clearFriends() { 
		synchronized (mFriends) { 
			mFriends.clear();
		}
	}
	
	public int getFriendSize() { 
		synchronized (mFriends) { 
			return mFriends.size();
		}
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("close: type=" + getFriendType());
		
		clearFriends();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{type=" + mType + "}";
	}
	
	static void loadFriendGroup(FriendManager manager, 
			NamedList<Object> item) throws ErrorException { 
		if (manager == null || item == null) return;
		
		String type = SettingConf.getString(item, "type");
		Friend[] friends = loadFriends(item.get("friends"));
		
		if (type == null || type.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"FriendGroup type: " + type + " is wrong");
		}
		
		if (friends != null && friends.length > 0) { 
			for (Friend friend : friends) { 
				manager.addFriend(friend, type);
			}
		}
	}
	
	static NamedList<Object> toNamedList(FriendGroup item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("type", item.getFriendType());
		info.add("friends", toNamedLists(item.getFriends()));
		
		return info;
	}
	
	private static Friend[] loadFriends(Object listVal) 
			throws ErrorException { 
		ArrayList<Friend> list = new ArrayList<Friend>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadFriends: class=" + (listVal!=null?listVal.getClass():null) 
					+ " list=" + listVal);
		}
		
		if (listVal != null && listVal instanceof List) { 
			List<?> listItem = (List<?>)listVal;
			
			for (int j=0; j < listItem.size(); j++) { 
				Object val = listItem.get(j);
				
				if (LOG.isDebugEnabled())
					LOG.debug("loadFriends: listItem=" + val);
				
				if (val != null && val instanceof NamedList) { 
					@SuppressWarnings("unchecked")
					NamedList<Object> item = (NamedList<Object>)val;
					
					Friend friend = Friend.loadFriend(item);
					if (friend != null)
						list.add(friend);
				}
			}
		}
		
		return list.toArray(new Friend[list.size()]);
	}
	
	@SuppressWarnings("unchecked")
	private static NamedList<Object>[] toNamedLists(Friend[] friends) 
			throws ErrorException { 
		ArrayList<NamedList<?>> items = new ArrayList<NamedList<?>>();
		
		for (int i=0; friends != null && i < friends.length; i++) { 
			Friend friend = friends[i];
			NamedList<Object> friendInfo = Friend.toNamedList(friend);
			if (friend != null && friendInfo != null) 
				items.add(friendInfo);
		}
		
		return items.toArray(new NamedList[items.size()]);
	}
	
}
