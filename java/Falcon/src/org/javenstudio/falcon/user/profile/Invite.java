package org.javenstudio.falcon.user.profile;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.user.IInvite;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public final class Invite implements IInvite {
	private static final Logger LOG = Logger.getLogger(Invite.class);

	public static final String TYPE_IN = "in";
	public static final String TYPE_OUT = "out";
	
	private final String mKey;
	private final String mName;
	private final String mType;
	
	private String mMessage = null;
	private long mTime = System.currentTimeMillis();
	
	public Invite(String key, String name, String type) { 
		if (key == null || name == null || type == null) 
			throw new NullPointerException();
		mKey = key;
		mName = name;
		mType = type;
	}
	
	public String getKey() { return mKey; }
	public String getName() { return mName; }
	public String getType() { return mType; }
	
	public long getTime() { return mTime; }
	public void setTime(long time) { mTime = time; }
	
	public String getMessage() { return mMessage; }
	public void setMessage(String text) { mMessage = text; }
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + mKey + ",name=" + mName + "}";
	}

	public static Invite loadInvite(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		String key = SettingConf.getString(item, "key");
		String name = SettingConf.getString(item, "name");
		String type = SettingConf.getString(item, "type");
		String message = SettingConf.getString(item, "message");
		long time = SettingConf.getLong(item, "time");
		
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Invite key: " + key + " is wrong");
		}
		
		if (name == null || name.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Invite name: " + name + " is wrong");
		}
		
		if (type == null || type.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Invite type: " + type + " is wrong");
		}
		
		Invite invite = new Invite(key, name, type);
		
		invite.setMessage(message);
		invite.setTime(time);
				
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadInvite: invite=" + invite);
		
		return invite;
	}
	
	public static NamedList<Object> toNamedList(Invite item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("key", item.getKey());
		info.add("name", item.getName());
		info.add("type", item.getType());
		info.add("message", item.getMessage());
		info.add("time", item.getTime());
		
		return info;
	}
	
}
