package org.javenstudio.falcon.message;

import org.javenstudio.falcon.ErrorException;

public interface IMessage {

	//public static final String ALL = "All";
	public static final String DEFAULT = "Default";
	public static final String SYSTEM = "System";
	public static final String LOGON = "Logon";
	
	public static final String INBOX = "Inbox";
	public static final String OUTBOX = "Outbox";
	public static final String DRAFT = "Draft";
	public static final String TRASH = "Trash";
	
	public static final String STATUS_DRAFT = "draft";
	public static final String STATUS_QUEUED = "queued";
	public static final String STATUS_SENDING = "sending";
	public static final String STATUS_SENT = "sent";
	public static final String STATUS_FAILED = "failed";
	public static final String STATUS_DELETED = "deleted";
	public static final String STATUS_REPLIED = "replied";
	public static final String STATUS_STARRED = "starred";
	public static final String STATUS_READ = "read";
	public static final String STATUS_NEW = "new";
	
	public static final String FLAG_FAVORITE = "favorite";
	public static final String FLAG_STARRED = "starred";
	
	public static final class Util {
		public static boolean hasStatus(String val) {
			if (val != null) {
				if (val.equals(STATUS_DRAFT) || val.equals(STATUS_QUEUED) || 
					val.equals(STATUS_SENDING) || val.equals(STATUS_SENT) ||
					val.equals(STATUS_FAILED) || val.equals(STATUS_DELETED) ||
					val.equals(STATUS_REPLIED) || val.equals(STATUS_STARRED) ||
					val.equals(STATUS_READ) || val.equals(STATUS_NEW))
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
	
	public IMessageService getService();
	
	public String getMessageId();
	public String getMessageType();
	public String getAccount();
	public String getFlag();
	public String getStatus();
	public String getFolder();
	public String getFolderFrom();
	public String getStreamId();
	public String getReplyId();
	
	public String getFrom();
	public String getTo();
	public String getCc();
	public String getBcc();
	public String getReplyTo();
	public String getHeaderLines();
	
	public String getSubject();
	public String getContentType();
	public String getBody();
	
	public String getSourceFile();
	public String[] getAttachmentFiles();
	
	public long getCreatedTime();
	public long getUpdateTime();
	public long getMessageTime();
	
	public static interface Builder {
		public Builder setMessageType(String val);
		public Builder setMessageTime(long val);
		public Builder setUpdateTime(long val);
		
		public Builder setAccount(String account);
		public Builder setFlag(String val);
		public Builder setStatus(String val);
		public Builder setFolder(String val);
		public Builder setFolderFrom(String val);
		public Builder setStreamId(String val);
		public Builder setReplyId(String val);
		
		public Builder setFrom(String val);
		public Builder setTo(String val);
		public Builder setCc(String val);
		public Builder setBcc(String val);
		public Builder setReplyTo(String val);
		public Builder setHeaderLines(String val);
		
		public Builder setSubject(String val);
		public Builder setContentType(String val);
		public Builder setBody(String val);
		
		public Builder setSourceFile(String val);
		public Builder setAttachmentFiles(String[] vals);
		
		public IMessage save() throws ErrorException;
	}
	
}
