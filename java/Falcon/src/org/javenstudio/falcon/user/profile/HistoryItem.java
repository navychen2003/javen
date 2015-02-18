package org.javenstudio.falcon.user.profile;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class HistoryItem {
	private static final Logger LOG = Logger.getLogger(HistoryItem.class);
	
	private final String mId;
	private final String mType;
	private String mLink = null;
	private String mTitle = null;
	private String mOwner = null;
	private String mOperation = null;
	private long mTime = 0;
	
	public HistoryItem(String id, String type) { 
		if (id == null || type == null) throw new NullPointerException();
		mId = id;
		mType = type;
		mTime = System.currentTimeMillis();
	}
	
	public String getContentId() { return mId; }
	public String getContentType() { return mType; }
	
	public String getTitle() { return mTitle; }
	public void setTitle(String title) { mTitle = title; }
	
	public String getLink() { return mLink; }
	public void setLink(String link) { mLink = link; }
	
	public String getOwner() { return mOwner; }
	public void setOwner(String owner) { mOwner = owner; }
	
	public String getOperation() { return mOperation; }
	public void setOperation(String op) { mOperation = op; }
	
	public long getTime() { return mTime; }
	public void setTime(long time) { mTime = time; }
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || !(obj instanceof HistoryItem)) return false;
		
		HistoryItem other = (HistoryItem)obj;
		return this.getContentId().equals(other.getContentId());
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + mId 
				+ ",type=" + mType + ",link=" + mLink + ",title=" + mTitle 
				+ ",owner=" + mOwner + ",op=" + mOperation + ",time=" + mTime + "}";
	}
	
	static HistoryItem loadHistoryItem(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		String id = SettingConf.getString(item, "id");
		String type = SettingConf.getString(item, "type");
		String link = SettingConf.getString(item, "link");
		String title = SettingConf.getString(item, "title");
		String owner = SettingConf.getString(item, "owner");
		String operation = SettingConf.getString(item, "op");
		long time = SettingConf.getLong(item, "time");
		
		if (id == null || id.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"History id: " + id + " is wrong");
		}
		
		if (type == null || type.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"History type: " + type + " is wrong");
		}
		
		HistoryItem history = new HistoryItem(id, type);
		history.setLink(link);
		history.setOperation(operation);
		history.setTitle(title);
		history.setOwner(owner);
		history.setTime(time);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadHistoryItem: item=" + history);
		
		return history;
	}
	
	static NamedList<Object> toNamedList(HistoryItem item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("id", item.getContentId());
		info.add("type", item.getContentType());
		info.add("title", item.getTitle());
		info.add("owner", item.getOwner());
		info.add("link", item.getLink());
		info.add("op", item.getOperation());
		info.add("time", item.getTime());
		
		return info;
	}
	
}
