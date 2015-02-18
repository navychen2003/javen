package org.javenstudio.falcon.publication;

import org.javenstudio.falcon.ErrorException;

public interface IPublication {

	public static final String APPLICATION = "Application";
	public static final String WINAPP = "WinApp";
	public static final String IOSAPP = "IOSApp";
	public static final String ANDROIDAPP = "AndroidApp";
	public static final String WEBAPP = "WebApp";
	
	public static final String EDITORCHOICE = "EditorChoice";
	public static final String POPULAR = "Popular";
	public static final String POSTER = "Poster";
	
	public static final String DEFAULT = "Default";
	public static final String DRAFT = "Draft";
	public static final String TRASH = "Trash";
	
	public static final String STATUS_DRAFT = "draft";
	public static final String STATUS_READ = "read";
	public static final String STATUS_NEW = "new";
	
	public static final String FLAG_FAVORITE = "favorite";
	public static final String FLAG_STARRED = "starred";
	
	public static final String ATTR_PUBLISHID = "publishid";
	public static final String ATTR_SERVICETYPE = "stype";
	public static final String ATTR_CHANNEL = "channel";
	public static final String ATTR_CHANNELFROM = "channelfrom";
	public static final String ATTR_PUBLISHTYPE = "publishtype";
	public static final String ATTR_STREAMID = "streamid";
	public static final String ATTR_REPLYID = "replyid";
	public static final String ATTR_CATEGORY = "category";
	public static final String ATTR_LANGUAGE = "lang";
	public static final String ATTR_OWNER = "owner";
	public static final String ATTR_FLAG = "flag";
	public static final String ATTR_STATUS = "status";
	public static final String ATTR_RATING = "rating";
	public static final String ATTR_CREATEDTIME = "ctime";
	public static final String ATTR_PUBLISHTIME = "ptime";
	public static final String ATTR_UPDATETIME = "utime";
	
	public static final String HEADER_TITLE = "title";
	public static final String HEADER_SUBTITLE = "subtitle";
	public static final String HEADER_SUBJECT = "subject";
	public static final String HEADER_LINK = "link";
	public static final String HEADER_TAGS = "tags";
	public static final String HEADER_HEADERLINES = "headers";
	public static final String HEADER_CONTENTTYPE = "contenttype";
	
	public static final String HEADER_AUTHOR = "author";
	public static final String HEADER_APPNAME = "appname";
	public static final String HEADER_PACKAGENAME = "packagename";
	public static final String HEADER_PLATFORM = "platform";
	public static final String HEADER_VERSIONCODE = "vcode";
	public static final String HEADER_VERSION = "version";
	public static final String HEADER_COMPANYNAME = "companyname";
	public static final String HEADER_COMPANYSITE = "companysite";
	public static final String HEADER_POSTER = "poster";
	
	public static final String CONTENT_BODY = "body";
	public static final String CONTENT_SOURCE = "source";
	public static final String CONTENT_ATTACHMENTS = "attachments";
	public static final String CONTENT_SCREENSHOTS = "screenshots";
	
	public static final class Util {
		public static boolean hasStatus(String val) {
			if (val != null) {
				if (val.equals(STATUS_DRAFT) || val.equals(STATUS_READ) || 
					val.equals(STATUS_NEW))
					return true;
			}
			return false;
		}
		public static boolean hasFlag(String val) {
			if (val != null) {
				if (val.equals(FLAG_FAVORITE) || val.equals(FLAG_STARRED))
					return true;
			}
			return false;
		}
	}
	
	public IPublicationService getService();
	public String getId();
	
	public INameValue<?> getAttr(String name);
	public INameValue<?>[] getAttrs();
	public int getAttrInt(String name);
	public long getAttrLong(String name);
	public float getAttrFloat(String name);
	public boolean getAttrBool(String name);
	public byte[] getAttrBytes(String name);
	public String getAttrString(String name);
	
	public INameValue<?> getHeader(String name);
	public INameValue<?>[] getHeaders();
	public int getHeaderInt(String name);
	public long getHeaderLong(String name);
	public float getHeaderFloat(String name);
	public boolean getHeaderBool(String name);
	public byte[] getHeaderBytes(String name);
	public String getHeaderString(String name);
	
	public INameValue<?> getContent(String name);
	public INameValue<?>[] getContents();
	public int getContentInt(String name);
	public long getContentLong(String name);
	public float getContentFloat(String name);
	public boolean getContentBool(String name);
	public byte[] getContentBytes(String name);
	public String getContentString(String name);
	
	public static interface Builder {
		public Builder setAttr(String name, int val);
		public Builder setAttr(String name, long val);
		public Builder setAttr(String name, float val);
		public Builder setAttr(String name, boolean val);
		public Builder setAttr(String name, byte[] val);
		public Builder setAttr(String name, String val);
		
		public Builder setHeader(String name, int val);
		public Builder setHeader(String name, long val);
		public Builder setHeader(String name, float val);
		public Builder setHeader(String name, boolean val);
		public Builder setHeader(String name, byte[] val);
		public Builder setHeader(String name, String val);
		
		public Builder setContent(String name, int val);
		public Builder setContent(String name, long val);
		public Builder setContent(String name, float val);
		public Builder setContent(String name, boolean val);
		public Builder setContent(String name, byte[] val);
		public Builder setContent(String name, String val);
		
		public IPublication save() throws ErrorException;
	}
	
}
