package org.javenstudio.falcon.publication.table;

import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.raptor.bigdb.util.Bytes;

class TNameType {
	
	public static final TNameType ATTR_PUBLISHID = 
			new TNameTypeCannotChangeAndRemove(IPublication.ATTR_PUBLISHID, String.class);

	public static final TNameType ATTR_SERVICETYPE = 
			new TNameTypeCannotChangeAndRemove(IPublication.ATTR_SERVICETYPE, String.class);
	
	public static final TNameType ATTR_CHANNEL = 
			new TNameTypeCannotRemove(IPublication.ATTR_CHANNEL, String.class) {
				@Override
				public void checkChange(TPublicationService service, Object val, Object oldVal) {
					super.checkChange(service, val, oldVal);
					if (val != null && val instanceof String) {
						if (!service.hasChannelName((String)val)) {
							throw new IllegalArgumentException("Channel: " + val + " not found");
						}
					} else {
						throw new IllegalArgumentException("Wrong channel value: " + val);
					}
				}
			};
	
	public static final TNameType ATTR_CHANNELFROM = 
			new TNameType(IPublication.ATTR_CHANNELFROM, String.class) {
				@Override
				public void checkChange(TPublicationService service, Object val, Object oldVal) {
					super.checkChange(service, val, oldVal);
					if (val != null && val instanceof String) {
						if (!service.hasChannelName((String)val)) {
							throw new IllegalArgumentException("Channel: " + val + " not found");
						}
					} else {
						throw new IllegalArgumentException("Wrong channel value: " + val);
					}
				}
			};
	
	public static final TNameType ATTR_PUBLISHTYPE = 
			new TNameType(IPublication.ATTR_PUBLISHTYPE, String.class);
	
	public static final TNameType ATTR_STREAMID = 
			new TNameTypeCannotRemove(IPublication.ATTR_STREAMID, String.class);
	
	public static final TNameType ATTR_REPLYID = 
			new TNameType(IPublication.ATTR_REPLYID, String.class);
	
	public static final TNameType ATTR_CATEGORY = 
			new TNameType(IPublication.ATTR_CATEGORY, String.class);
	
	public static final TNameType ATTR_LANGUAGE = 
			new TNameType(IPublication.ATTR_LANGUAGE, String.class);
	
	public static final TNameType ATTR_OWNER = 
			new TNameTypeCannotChangeAndRemove(IPublication.ATTR_OWNER, String.class);
	
	public static final TNameType ATTR_FLAG = 
			new TNameType(IPublication.ATTR_FLAG, String.class);
	
	public static final TNameType ATTR_STATUS = 
			new TNameType(IPublication.ATTR_STATUS, String.class);
	
	public static final TNameType ATTR_RATING = 
			new TNameType(IPublication.ATTR_RATING, Integer.class);
	
	public static final TNameType ATTR_CREATEDTIME = 
			new TNameType(IPublication.ATTR_CREATEDTIME, Long.class);
	
	public static final TNameType ATTR_PUBLISHTIME = 
			new TNameType(IPublication.ATTR_PUBLISHTIME, Long.class);
	
	public static final TNameType ATTR_UPDATETIME = 
			new TNameType(IPublication.ATTR_UPDATETIME, Long.class);
	
	
	public static final TNameType HEADER_TITLE = 
			new TNameType(IPublication.HEADER_TITLE, String.class);
	
	public static final TNameType HEADER_SUBTITLE = 
			new TNameType(IPublication.HEADER_SUBTITLE, String.class);
	
	public static final TNameType HEADER_SUBJECT = 
			new TNameType(IPublication.HEADER_SUBJECT, String.class);
	
	public static final TNameType HEADER_LINK = 
			new TNameType(IPublication.HEADER_LINK, String.class);
	
	public static final TNameType HEADER_TAGS = 
			new TNameType(IPublication.HEADER_TAGS, String.class);
	
	public static final TNameType HEADER_HEADERLINES = 
			new TNameType(IPublication.HEADER_HEADERLINES, String.class);
	
	public static final TNameType HEADER_CONTENTTYPE = 
			new TNameType(IPublication.HEADER_CONTENTTYPE, String.class);
	
	
	public static final TNameType HEADER_AUTHOR = 
			new TNameType(IPublication.HEADER_AUTHOR, String.class);
	
	public static final TNameType HEADER_APPNAME = 
			new TNameType(IPublication.HEADER_APPNAME, String.class);
	
	public static final TNameType HEADER_PACKAGENAME = 
			new TNameType(IPublication.HEADER_PACKAGENAME, String.class);
	
	public static final TNameType HEADER_PLATFORM = 
			new TNameType(IPublication.HEADER_PLATFORM, String.class);
	
	public static final TNameType HEADER_VERSIONCODE = 
			new TNameType(IPublication.HEADER_VERSIONCODE, String.class);
	
	public static final TNameType HEADER_VERSION = 
			new TNameType(IPublication.HEADER_VERSION, String.class);
	
	public static final TNameType HEADER_COMPANYNAME = 
			new TNameType(IPublication.HEADER_COMPANYNAME, String.class);
	
	public static final TNameType HEADER_COMPANYSITE = 
			new TNameType(IPublication.HEADER_COMPANYSITE, String.class);
	
	public static final TNameType HEADER_POSTER = 
			new TNameType(IPublication.HEADER_POSTER, String.class);
	
	
	public static final TNameType CONTENT_BODY = 
			new TNameType(IPublication.CONTENT_BODY, String.class);
	
	public static final TNameType CONTENT_SOURCE = 
			new TNameType(IPublication.CONTENT_SOURCE, String.class);
	
	public static final TNameType CONTENT_ATTACHMENTS = 
			new TNameType(IPublication.CONTENT_ATTACHMENTS, String.class);
	
	public static final TNameType CONTENT_SCREENSHOTS = 
			new TNameType(IPublication.CONTENT_SCREENSHOTS, String.class);
	
	
	private final String mName;
	private final Class<?> mValClass;
	private final byte[] mNameBytes;
	
	public TNameType(String name, Class<?> clazz) {
		if (name == null || clazz == null) throw new NullPointerException();
		mName = name;
		mValClass = clazz;
		mNameBytes = Bytes.toBytes(name);
	}
	
	public String getName() { return mName; }
	public Class<?> getValueClass() { return mValClass; }
	public byte[] getNameBytes() { return mNameBytes; }
	
	public void checkChange(TPublicationService service, 
			Object val, Object oldVal) {
		if (val == null) return;
		if (!getValueClass().isAssignableFrom(val.getClass()))
			throw new IllegalArgumentException("Wrong value class: " + val.getClass());
	}
	
	public void checkRemove(TPublicationService service, Object val) {
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{name=" + mName 
				+ ",valClass=" + mValClass + "}";
	}
	
	static class TNameTypeCannotChangeAndRemove extends TNameType {
		public TNameTypeCannotChangeAndRemove(String name, Class<?> clazz) {
			super(name, clazz);
		}
		
		@Override
		public void checkChange(TPublicationService service, Object val, Object oldVal) {
			super.checkChange(service, val, oldVal);
			if (oldVal != null && !oldVal.equals(val)) {
				throw new IllegalArgumentException(
						"Field: " + getName() + " cannot change");
			}
		}
		
		@Override
		public void checkRemove(TPublicationService service, Object val) {
			super.checkRemove(service, val);
			if (val != null) { 
				throw new IllegalArgumentException(
						"Field: " + getName() + " cannot remove");
			}
		}
	}
	
	static class TNameTypeCannotRemove extends TNameType {
		public TNameTypeCannotRemove(String name, Class<?> clazz) {
			super(name, clazz);
		}
		
		@Override
		public void checkChange(TPublicationService service, Object val, Object oldVal) {
			super.checkChange(service, val, oldVal);
		}
		
		@Override
		public void checkRemove(TPublicationService service, Object val) {
			super.checkRemove(service, val);
			if (val != null) { 
				throw new IllegalArgumentException(
						"Field: " + getName() + " cannot remove");
			}
		}
	}
	
}
