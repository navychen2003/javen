package org.javenstudio.falcon.user.global;

import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingAttr;
import org.javenstudio.falcon.util.NamedList;

public class Announcement extends SettingAttr {
	//private static final Logger LOG = Logger.getLogger(Announcement.class);

	public Announcement(String key) {
		super(key);
	}
	
	public void setTitle(String value, boolean ignoreIfExists) { addDescriptionIfNotNull(TITLE_DESC, value, ignoreIfExists); }
	public String getTitle() { return getItemString(TITLE_DESC); }
	
	public void setLink(String value, boolean ignoreIfExists) { addUrlIfNotNull(LINK_URL, value, ignoreIfExists); }
	public String getLink() { return getItemString(LINK_URL); }
	
	public void setBody(String value, boolean ignoreIfExists) { addDescriptionIfNotNull(BODY_DESC, value, ignoreIfExists); }
	public String getBody() { return getItemString(BODY_DESC); }
	
	public void setPoster(String value, boolean ignoreIfExists) { addResourceIfNotNull(POSTER_RES, value, ignoreIfExists); }
	public String getPoster() { return getItemString(POSTER_RES); }
	
	public void setLanguage(String value, boolean ignoreIfExists) { addAttributeIfNotNull(LANGUAGE_ATTR, value, ignoreIfExists); }
	public String getLanguage() { return getItemString(LANGUAGE_ATTR); }
	
	static Factory<Announcement> FACTORY = new Factory<Announcement>() { 
			@Override
			public Announcement create(String key) { 
				return new Announcement(key);
			}
		};
	
	public static Announcement[] loadAnnouncements(Object listVal) 
			throws ErrorException { 
		List<Announcement> list = SettingAttr.loadSettingAttres(listVal, FACTORY);
		if (list != null) 
			return list.toArray(new Announcement[list.size()]);
		return null;
	}
	
	public static Announcement loadAnnouncement(NamedList<Object> item) 
			throws ErrorException { 
		return SettingAttr.loadSettingAttr(item, FACTORY);
	}
	
	public static NamedList<Object>[] toNamedLists(Announcement[] items) 
			throws ErrorException { 
		return SettingAttr.toNamedLists((SettingAttr[])items);
	}
	
	public static NamedList<Object> toNamedList(Announcement item) 
			throws ErrorException { 
		return SettingAttr.toNamedList((SettingAttr)item);
	}
	
}
