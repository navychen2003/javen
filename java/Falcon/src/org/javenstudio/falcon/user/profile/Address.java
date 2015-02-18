package org.javenstudio.falcon.user.profile;

import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingAttr;
import org.javenstudio.falcon.util.NamedList;

public class Address extends SettingAttr {
	//private static final Logger LOG = Logger.getLogger(Address.class);

	public Address(String key) {
		super(key);
	}
	
	public void setNickName(String value, boolean ignoreIfExists) { addNameIfNotNull(NICK_NAME, value, ignoreIfExists); }
	public String getNickName() { return getItemString(NICK_NAME); }
	
	public void setFirstName(String value, boolean ignoreIfExists) { addNameIfNotNull(FIRST_NAME, value, ignoreIfExists); }
	public String getFirstName() { return getItemString(FIRST_NAME); }
	
	public void setLastName(String value, boolean ignoreIfExists) { addNameIfNotNull(LAST_NAME, value, ignoreIfExists); }
	public String getLastName() { return getItemString(LAST_NAME); }
	
	public void setSex(String value, boolean ignoreIfExists) { addAttributeIfNotNull(SEX_ATTR, value, ignoreIfExists); }
	public String getSex() { return getItemString(SEX_ATTR); }
	
	public void setBirthday(String value, boolean ignoreIfExists) { addAttributeIfNotNull(BIRTHDAY_ATTR, value, ignoreIfExists); }
	public String getBirthday() { return getItemString(BIRTHDAY_ATTR); }
	
	public void setTags(String value, boolean ignoreIfExists) { addAttributeIfNotNull(TAGS_ATTR, value, ignoreIfExists); }
	public String getTags() { return getItemString(TAGS_ATTR); }
	
	public void setRegion(String value, boolean ignoreIfExists) { addAddressIfNotNull(REGION_ADDR, value, ignoreIfExists); }
	public String getRegion() { return getItemString(REGION_ADDR); }
	
	public void setTitle(String value, boolean ignoreIfExists) { addDescriptionIfNotNull(TITLE_DESC, value, ignoreIfExists); }
	public String getTitle() { return getItemString(TITLE_DESC); }
	
	public void setBrief(String value, boolean ignoreIfExists) { addDescriptionIfNotNull(BRIEF_DESC, value, ignoreIfExists); }
	public String getBrief() { return getItemString(BRIEF_DESC); }
	
	public void setIntroduction(String value, boolean ignoreIfExists) { addDescriptionIfNotNull(INTRO_DESC, value, ignoreIfExists); }
	public String getIntroduction() { return getItemString(INTRO_DESC); }
	
	public void setAvatar(String value, boolean ignoreIfExists) { addResourceIfNotNull(AVATAR_RES, value, ignoreIfExists); }
	public String getAvatar() { return getItemString(AVATAR_RES); }
	
	public void setBackground(String value, boolean ignoreIfExists) { addResourceIfNotNull(BACKGROUND_RES, value, ignoreIfExists); }
	public String getBackground() { return getItemString(BACKGROUND_RES); }
	
	static Factory<Address> FACTORY = new Factory<Address>() { 
			@Override
			public Address create(String key) { 
				return new Address(key);
			}
		};
	
	public static Address[] loadAddresses(Object listVal) 
			throws ErrorException { 
		List<Address> list = SettingAttr.loadSettingAttres(listVal, FACTORY);
		if (list != null) 
			return list.toArray(new Address[list.size()]);
		return null;
	}
	
	public static Address loadAddress(NamedList<Object> item) 
			throws ErrorException { 
		return SettingAttr.loadSettingAttr(item, FACTORY);
	}
	
	public static NamedList<Object>[] toNamedLists(Address[] addrs) 
			throws ErrorException { 
		return SettingAttr.toNamedLists((SettingAttr[])addrs);
	}
	
	public static NamedList<Object> toNamedList(Address item) 
			throws ErrorException { 
		return SettingAttr.toNamedList((SettingAttr)item);
	}
	
}
