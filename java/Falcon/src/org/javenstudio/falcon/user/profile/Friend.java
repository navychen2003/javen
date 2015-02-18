package org.javenstudio.falcon.user.profile;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class Friend {
	private static final Logger LOG = Logger.getLogger(Friend.class);
	
	public static final String TYPE_FAMILY = "family";
	public static final String TYPE_FRIEND = "friend";

	private final String mKey;
	private final String mName;
	private String mTitle = null;
	
	public Friend(String key, String name) { 
		if (key == null || name == null) throw new NullPointerException();
		mKey = key;
		mName = name;
	}
	
	public String getKey() { return mKey; }
	public String getName() { return mName; }
	
	public String getTitle() { return mTitle; }
	public void setTitle(String title) { mTitle = title; }
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + mKey + ",name=" + mName + "}";
	}
	
	static Friend loadFriend(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		String key = SettingConf.getString(item, "key");
		String name = SettingConf.getString(item, "name");
		
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Friend key: " + key + " is wrong");
		}
		
		if (name == null || name.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Friend name: " + name + " is wrong");
		}
		
		Friend friend = new Friend(key, name);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadFriend: friend=" + friend);
		
		return friend;
	}
	
	static NamedList<Object> toNamedList(Friend item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("key", item.getKey());
		info.add("name", item.getName());
		
		return info;
	}
	
}
