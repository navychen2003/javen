package org.javenstudio.falcon.setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public abstract class SettingAttr {
	private static final Logger LOG = Logger.getLogger(SettingAttr.class);

	public static final String TYPE_KEY = "key";
	public static final String TYPE_NAME = "name";
	public static final String TYPE_ADDR = "address";
	public static final String TYPE_NUMBER = "number";
	public static final String TYPE_INTERNETADDR = "internet";
	public static final String TYPE_URL = "url";
	public static final String TYPE_DESC = "desc";
	public static final String TYPE_ATTR = "attr";
	public static final String TYPE_RES = "resource";
	
	public static final String USERID_KEY = "UserID";
	public static final String USERNAME_KEY = "UserName";
	
	public static final String ATTACH_HOSTKEY_KEY = "AttachHostKey";
	public static final String ATTACH_HOSTNAME_KEY = "AttachHostName";
	public static final String ATTACH_USERKEY_KEY = "AttachUserKey";
	public static final String ATTACH_USERNAME_KEY = "AttachUserName";
	public static final String ATTACH_MAILADDR_KEY = "AttachMailAddr";
	
	public static final String FIRST_NAME = "FirstName";
	public static final String LAST_NAME = "LastName";
	public static final String NICK_NAME = "NickName";
	public static final String TITLE_NAME = "TitleName";
	
	public static final String HOME_ADDR = "HomeAddr";
	public static final String COMPANY_ADDR = "CompanyAddr";
	public static final String REGION_ADDR = "RegionAddr";
	
	public static final String PHONE_NUMBER = "PhoneNum";
	public static final String CARD_NUMBER = "CardNum";
	public static final String ID_NUMBER = "IDNum";
	
	public static final String EMAIL_ADDR = "EmailAddr";
	public static final String LINK_URL = "LinkUrl";
	
	public static final String TITLE_DESC = "TitleDesc";
	public static final String ABOUT_DESC = "AboutDesc";
	public static final String DETAILS_DESC = "DetailsDesc";
	public static final String BRIEF_DESC = "BriefDesc";
	public static final String INTRO_DESC = "IntroductionDesc";
	public static final String BODY_DESC = "BodyDesc";
	
	public static final String LANGUAGE_ATTR = "LanguageAttr";
	public static final String SEX_ATTR = "SexAttr";
	public static final String BIRTHDAY_ATTR = "BirthdayAttr";
	public static final String TIMEZONE_ATTR = "TomezoneAttr";
	public static final String TAGS_ATTR = "TagsAttr";
	
	public static final String AVATAR_RES = "AvatarRes";
	public static final String POSTER_RES = "PosterRes";
	public static final String BACKGROUND_RES = "BackgroundRes";
	
	public Item addKey(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_KEY, value, ignoreIfExists);
	}
	
	public Item addKeyIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_KEY, value, ignoreIfExists);
	}
	
	public Item addName(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_NAME, value, ignoreIfExists);
	}
	
	public Item addNameIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_NAME, value, ignoreIfExists);
	}
	
	public Item addAddress(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_ADDR, value, ignoreIfExists);
	}
	
	public Item addAddressIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_ADDR, value, ignoreIfExists);
	}
	
	public Item addNumber(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_NUMBER, value, ignoreIfExists);
	}
	
	public Item addNumberIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_NUMBER, value, ignoreIfExists);
	}
	
	public Item addInternetAddress(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_INTERNETADDR, value, ignoreIfExists);
	}
	
	public Item addInternetAddressIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_INTERNETADDR, value, ignoreIfExists);
	}
	
	public Item addUrl(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_URL, value, ignoreIfExists);
	}
	
	public Item addUrlIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_URL, value, ignoreIfExists);
	}
	
	public Item addDescription(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_DESC, value, ignoreIfExists);
	}
	
	public Item addDescriptionIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_DESC, value, ignoreIfExists);
	}
	
	public Item addAttribute(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_ATTR, value, ignoreIfExists);
	}
	
	public Item addAttributeIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_ATTR, value, ignoreIfExists);
	}
	
	public Item addResource(String name, String value, boolean ignoreIfExists) { 
		return addItem(name, TYPE_RES, value, ignoreIfExists);
	}
	
	public Item addResourceIfNotNull(String name, String value, boolean ignoreIfExists) { 
		return addItemIfNotNull(name, TYPE_RES, value, ignoreIfExists);
	}
	
	public static class Value { 
		private final String mValue;
		private final long mCreatedTime;
		private long mModifiedTime;
		
		private Value(String value, long ctime, long mtime) { 
			if (value == null) throw new NullPointerException();
			mValue = value;
			mCreatedTime = ctime;
			mModifiedTime = mtime;
		}
		
		public String getValue() { return mValue; }
		public long getCreatedTime() { return mCreatedTime; }
		public long getModifiedTime() { return mModifiedTime; }
		public void setModifiedTime(long time) { mModifiedTime = time; }
		
		@Override
		public boolean equals(Object obj) { 
			if (obj == this) return true;
			if (obj == null || !(obj instanceof Value))
				return false;
			
			Value other = (Value)obj;
			return this.getValue().equals(other.getValue());
		}
		
		@Override
		public String toString() { 
			return getClass().getSimpleName() + "{value=" + mValue 
					+ ",ctime=" + mCreatedTime + ",mtime=" + mModifiedTime + "}";
		}
	}
	
	public static class Item { 
		private final String mType;
		private final String mName;
		private final List<Value> mValues;
		
		private Item(String name, String type, Value[] values) { 
			this(name, type);
			if (values != null && values.length > 0) {
				for (Value val : values) { 
					if (val != null) mValues.add(val);
				}
			}
		}
		
		private Item(String name, String type) { 
			if (name == null || type == null) throw new NullPointerException();
			mName = name;
			mType = type;
			mValues = new ArrayList<Value>();
		}
		
		public String getName() { return mName; }
		public String getType() { return mType; }
		
		@Override
		public boolean equals(Object obj) { 
			if (obj == this) return true;
			if (obj == null || !(obj instanceof Item))
				return false;
			
			Item other = (Item)obj;
			return this.getName().equals(other.getName());
		}
		
		public int getValueSize() { 
			synchronized (mValues) {
				return mValues.size(); 
			}
		}
		
		public Value getValueAt(int index) { 
			synchronized (mValues) {
				return mValues.get(index); 
			}
		}
		
		public Value[] getValues() { 
			synchronized (mValues) {
				return mValues.toArray(new Value[mValues.size()]);
			}
		}
		
		//private Value addValue(String value) { 
		//	return addValue(value, 0);
		//}
		
		private Value addValue(String value, long ctime, long mtime) { 
			if (value == null) return null;
			
			if (ctime <= 0) 
				ctime = System.currentTimeMillis();
			
			if (mtime <= 0)
				mtime = ctime;
			
			Value val = new Value(value, ctime, mtime);
			synchronized (mValues) {
				mValues.add(val);
			}
			
			return val;
		}
		
		private Value removeValue(String value) { 
			if (value == null) return null;
			
			synchronized (mValues) { 
				for (int i=0; i < mValues.size(); i++) { 
					Value val = mValues.get(i);
					if (val != null && value.equals(val.getValue())) 
						return mValues.remove(i);
				}
			}
			
			return null;
		}
		
		public Value getValue() { 
			synchronized (mValues) { 
				Value result = null;
				
				for (int i=0; i < mValues.size(); i++) { 
					Value val = mValues.get(i);
					if (val == null) continue;
					if (result == null || result.getModifiedTime() < val.getModifiedTime()) 
						result = val;
				}
				
				return result;
			}
		}
		
		@Override
		public String toString() { 
			StringBuilder sbuf = new StringBuilder();
			synchronized (mValues) { 
				for (int i=0; i < mValues.size(); i++) { 
					Value val = mValues.get(i);
					if (val == null) continue;
					if (sbuf.length() > 0) sbuf.append(',');
					sbuf.append(val.toString());
				}
			}
			return getClass().getSimpleName() + "{name=" + mName 
					+ ",type=" + mType + ",values=[" + sbuf.toString() + "]}";
		}
	}
	
	private final String mKey;
	private long mModifiedTime = 0;
	
	private final Map<String, Item> mItems = 
			new HashMap<String, Item>();
	
	public SettingAttr(String key) {
		if (key == null) throw new NullPointerException();
		mKey = key;
		mModifiedTime = System.currentTimeMillis();
	}
	
	public String getKey() { return mKey; }
	
	public long getModifiedTime() { return mModifiedTime; }
	public void setModifiedTime(long val) { mModifiedTime = val; }
	
	public final void addAll(SettingAttr addr, boolean ignoreIfExists) { 
		if (addr == null) return;
		
		synchronized (mItems) { 
			synchronized (addr.mItems) { 
				for (Item item : addr.mItems.values()) { 
					if (item == null) continue;
					
					for (int i=0; i < item.getValueSize(); i++) { 
						Value val = item.getValueAt(i);
						if (val != null) {
							addItem(item.getName(), item.getType(), 
									val.getValue(), val.getCreatedTime(), val.getModifiedTime(), 
									ignoreIfExists);
						}
					}
				}
			}
		}
	}
	
	public Item addItemIfNotNull(String name, String type, String value, 
			boolean ignoreIfExists) { 
		if (value != null)
			return addItem(name, type, value, ignoreIfExists);
		else
			return null;
	}
	
	public Item addItem(String name, String type, String value, 
			boolean ignoreIfExists) { 
		return addItem(name, type, value, 0, 0, ignoreIfExists);
	}
	
	public Item addItemIfNotNull(String name, String type, String value, 
			long time, boolean ignoreIfExists) { 
		if (value != null)
			return addItem(name, type, value, time, time, ignoreIfExists);
		else
			return null;
	}
	
	public final Item addItem(String name, String type, String value, 
			long ctime, long mtime, boolean ignoreIfExists) { 
		if (name == null || type == null || value == null)
			throw new IllegalArgumentException();
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("addItem: name=" + name + " type=" + type 
					+ " value=" + value + " ctime=" + ctime + " mtime=" + mtime 
					+ " ignoreIfExists=" + ignoreIfExists);
		}
		
		synchronized (mItems) { 
			Item item = mItems.get(name);
			if (item == null) { 
				item = new Item(name, type);
				mItems.put(name, item);
				
			} else { 
				if (!type.equals(item.getType())) { 
					throw new IllegalArgumentException("SettingAttr type: " + type 
							+ " not equals to existed: " + item.getType());
				}
			}
			
			for (int i=0; i < item.getValueSize(); i++) { 
				Value val = item.getValueAt(i);
				if (val != null && value.equals(val.getValue())) {
					if (ignoreIfExists == false) {
						val.setModifiedTime(System.currentTimeMillis());
						if (val.getModifiedTime() > mModifiedTime)
							mModifiedTime = val.getModifiedTime();
					}
					return item;
				}
			}
			
			item.addValue(value, ctime, mtime);
			if (mtime > mModifiedTime)
				mModifiedTime = mtime;
			
			return item;
		}
	}
	
	public final Item addItem(Item item, boolean ignoreIfExists) { 
		if (item == null) return null;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("addItem: item=" + item + " ignoreIfExists=" + ignoreIfExists);
		
		synchronized (mItems) { 
			String name = item.getName();
			Item existed = mItems.get(name);
			
			if (existed == item) return existed;
			if (existed == null) { 
				mItems.put(name, item);
				return item;
			}
			
			if (!existed.getType().equals(item.getType())) { 
				throw new IllegalArgumentException("Input item has wrong type: " 
						+ item.getType() + ", existed is " + existed.getType());
			}
			
			for (int i=0; i < item.getValueSize(); i++) { 
				Value val = item.getValueAt(i);
				if (val != null) {
					addItem(name, item.getType(), 
							val.getValue(), val.getCreatedTime(), val.getModifiedTime(), 
							ignoreIfExists);
				}
			}
			
			return existed;
		}
	}
	
	public Item removeItem(String name) { 
		return removeItem(name, null);
	}
	
	public Item removeItem(String name, String value) { 
		if (name == null) return null;
		
		synchronized (mItems) { 
			Item item = mItems.get(name);
			if (item == null) return null;
			
			if (value == null) 
				item = mItems.remove(name);
			else
				item.removeValue(value);
			
			return item;
		}
	}
	
	public String[] getNames() { 
		synchronized (mItems) { 
			return mItems.keySet().toArray(new String[mItems.size()]);
		}
	}
	
	public String getItemString(String name) { 
		Value val = getItemValue(name);
		if (val != null)
			return val.getValue();
		return null;
	}
	
	public Value getItemValue(String name) { 
		Item item = getItem(name);
		if (item != null) 
			return item.getValue();
		return null;
	}
	
	public Item getItem(String name) { 
		if (name == null) return null;
		
		synchronized (mItems) { 
			return mItems.get(name);
		}
	}
	
	public Item[] getItems() { 
		synchronized (mItems) { 
			return mItems.values().toArray(new Item[mItems.size()]);
		}
	}
	
	public void clear() { 
		synchronized (mItems) { 
			mItems.clear();
		}
	}
	
	public int size() { 
		synchronized (mItems) { 
			return mItems.size();
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + mKey + "}";
	}
	
	public static interface Factory<T extends SettingAttr> { 
		public T create(String key);
	}
	
	public static <T extends SettingAttr> List<T> 
			loadSettingAttres(Object listVal, Factory<T> factory) 
			throws ErrorException { 
		ArrayList<T> list = new ArrayList<T>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadSettingAttres: class=" + (listVal!=null?listVal.getClass():null) 
					+ " list=" + listVal);
		}
		
		if (listVal != null && listVal instanceof List) { 
			List<?> listItem = (List<?>)listVal;
			
			for (int j=0; j < listItem.size(); j++) { 
				Object val = listItem.get(j);
				
				if (LOG.isDebugEnabled())
					LOG.debug("loadSettingAttres: listItem=" + val);
				
				if (val != null && val instanceof NamedList) { 
					@SuppressWarnings("unchecked")
					NamedList<Object> item = (NamedList<Object>)val;
					
					T address = loadSettingAttr(item, factory);
					if (address != null)
						list.add(address);
				}
			}
		}
		
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public static NamedList<Object>[] toNamedLists(SettingAttr[] addrs) 
			throws ErrorException { 
		ArrayList<NamedList<?>> items = new ArrayList<NamedList<?>>();
		
		for (int i=0; addrs != null && i < addrs.length; i++) { 
			SettingAttr addr = addrs[i];
			NamedList<Object> addrInfo = toNamedList(addr);
			if (addr != null && addrInfo != null) 
				items.add(addrInfo);
		}
		
		return items.toArray(new NamedList[items.size()]);
	}
	
	public static <T extends SettingAttr> T 
			loadSettingAttr(NamedList<Object> item, Factory<T> factory) 
			throws ErrorException { 
		if (item == null || factory == null) return null;
		
		//String className = SettingConf.getString(item, "class");
		String key = SettingConf.getString(item, "key");
		long mtime = SettingConf.getLong(item, "mtime");
		SettingAttr.Item[] items = loadSettingAttrItems(item.get("items"));
		
		//if (className == null || !className.equals(SettingAttr.class.getName())) {
		//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
		//			"SettingAttr class name: " + className + " is wrong");
		//}
		
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"SettingAttr key: " + key + " is wrong");
		}
		
		T addr = factory.create(key);
		addr.setModifiedTime(mtime);
		
		if (items != null && items.length > 0) { 
			for (SettingAttr.Item val : items) { 
				addr.addItem(val, true);
			}
		}
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadSettingAttr: data=" + addr);
		
		return addr;
	}
	
	public static <T extends SettingAttr> void
			loadSettingAttr(NamedList<Object> item, T addr) 
			throws ErrorException { 
		if (item == null || addr == null) return;
		
		//String className = SettingConf.getString(item, "class");
		//String key = SettingConf.getString(item, "key");
		long mtime = SettingConf.getLong(item, "mtime");
		SettingAttr.Item[] items = loadSettingAttrItems(item.get("items"));
		
		//if (className == null || !className.equals(SettingAttr.class.getName())) {
		//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
		//			"SettingAttr class name: " + className + " is wrong");
		//}
		
		//if (key == null || key.length() == 0) { 
		//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
		//			"SettingAttr key: " + key + " is wrong");
		//}
		
		//T addr = factory.create(key);
		addr.setModifiedTime(mtime);
		
		if (items != null && items.length > 0) { 
			for (SettingAttr.Item val : items) { 
				addr.addItem(val, true);
			}
		}
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadSettingAttr: data=" + addr);
		
		//return addr;
	}
	
	public static NamedList<Object> toNamedList(SettingAttr item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		//info.add("class", item.getClass().getName());
		info.add("key", item.getKey());
		info.add("mtime", item.getModifiedTime());
		info.add("items", toNamedLists(item.getItems()));
		
		return info;
	}
	
	public static SettingAttr.Item[] loadSettingAttrItems(Object listVal) 
			throws ErrorException { 
		ArrayList<SettingAttr.Item> list = new ArrayList<SettingAttr.Item>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadSettingAttrItems: class=" + (listVal!=null?listVal.getClass():null) 
					+ " list=" + listVal);
		}
		
		if (listVal != null && listVal instanceof List) { 
			List<?> listItem = (List<?>)listVal;
			
			for (int j=0; j < listItem.size(); j++) { 
				Object val = listItem.get(j);
				
				if (LOG.isDebugEnabled())
					LOG.debug("loadSettingAttres: listItem=" + val);
				
				if (val != null && val instanceof NamedList) { 
					@SuppressWarnings("unchecked")
					NamedList<Object> item = (NamedList<Object>)val;
					
					SettingAttr.Item addr = loadSettingAttrItem(item);
					if (addr != null)
						list.add(addr);
				}
			}
		}
		
		return list.toArray(new SettingAttr.Item[list.size()]);
	}
	
	@SuppressWarnings("unchecked")
	public static NamedList<Object>[] toNamedLists(SettingAttr.Item[] addrs) 
			throws ErrorException { 
		ArrayList<NamedList<?>> items = new ArrayList<NamedList<?>>();
		
		for (int i=0; addrs != null && i < addrs.length; i++) { 
			SettingAttr.Item addr = addrs[i];
			NamedList<Object> addrInfo = toNamedList(addr);
			if (addr != null && addrInfo != null) 
				items.add(addrInfo);
		}
		
		return items.toArray(new NamedList[items.size()]);
	}
	
	public static SettingAttr.Item loadSettingAttrItem(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		//String className = SettingConf.getString(item, "class");
		String name = SettingConf.getString(item, "name");
		String type = SettingConf.getString(item, "type");
		SettingAttr.Value[] values = loadSettingAttrValues(item.get("values"));
		
		//if (className == null || !className.equals(SettingAttr.Item.class.getName())) {
		//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
		//			"SettingAttrItem class name: " + className + " is wrong");
		//}
		
		if (name == null || name.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"SettingAttr name: " + name + " is wrong");
		}
		
		if (type == null || type.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"SettingAttr type: " + type + " is wrong");
		}
		
		SettingAttr.Item addr = new SettingAttr.Item(name, type, values);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadSettingAttrItem: item=" + addr);
		
		return addr;
	}
	
	public static NamedList<Object> toNamedList(SettingAttr.Item item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		//info.add("class", item.getClass().getName());
		info.add("name", item.getName());
		info.add("type", item.getType());
		info.add("values", toNamedLists(item.getValues()));
		
		return info;
	}
	
	public static SettingAttr.Value[] loadSettingAttrValues(Object listVal) 
			throws ErrorException { 
		ArrayList<SettingAttr.Value> list = new ArrayList<SettingAttr.Value>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadSettingAttrValues: class=" + (listVal!=null?listVal.getClass():null) 
					+ " list=" + listVal);
		}
		
		if (listVal != null && listVal instanceof List) { 
			List<?> listItem = (List<?>)listVal;
			
			for (int j=0; j < listItem.size(); j++) { 
				Object val = listItem.get(j);
				
				if (LOG.isDebugEnabled())
					LOG.debug("loadSettingAttrValues: listItem=" + val);
				
				if (val != null && val instanceof NamedList) { 
					@SuppressWarnings("unchecked")
					NamedList<Object> item = (NamedList<Object>)val;
					
					SettingAttr.Value value = loadSettingAttrValue(item);
					if (value != null)
						list.add(value);
				}
			}
		}
		
		return list.toArray(new SettingAttr.Value[list.size()]);
	}
	
	@SuppressWarnings("unchecked")
	public static NamedList<Object>[] toNamedLists(SettingAttr.Value[] addrs) 
			throws ErrorException { 
		ArrayList<NamedList<?>> items = new ArrayList<NamedList<?>>();
		
		for (int i=0; addrs != null && i < addrs.length; i++) { 
			SettingAttr.Value addr = addrs[i];
			NamedList<Object> addrInfo = toNamedList(addr);
			if (addr != null && addrInfo != null) 
				items.add(addrInfo);
		}
		
		return items.toArray(new NamedList[items.size()]);
	}
	
	public static SettingAttr.Value loadSettingAttrValue(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		//String className = SettingConf.getString(item, "class");
		String value = SettingConf.getString(item, "value");
		long ctime = SettingConf.getLong(item, "ctime");
		long mtime = SettingConf.getLong(item, "mtime");
		
		//if (className == null || !className.equals(SettingAttr.Value.class.getName())) {
		//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
		//			"SettingAttrValue class name: " + className + " is wrong");
		//}
		
		//if (value == null) value = "";
		if (value == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"SettingAttr value: " + value + " is wrong");
		}
		
		SettingAttr.Value val = new SettingAttr.Value(value, ctime, mtime);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadSettingAttrValue: value=" + val);
		
		return val;
	}
	
	public static NamedList<Object> toNamedList(SettingAttr.Value item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		//info.add("class", item.getClass().getName());
		info.add("value", item.getValue());
		info.add("ctime", item.getCreatedTime());
		info.add("mtime", item.getModifiedTime());
		
		return info;
	}
	
}
