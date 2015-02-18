package org.javenstudio.falcon.datum;

public interface IData {
	
	public static enum Operation { 
		MOVE, DELETE, UPLOAD, COPY, NEWFOLDER, EMPTY
	}
	
	public static enum Action { 
		MOVE, DELETE, COPY, MODIFY
	}
	
	public static enum Access {
		INFO, DETAILS, THUMB, STREAM, DOWNLOAD, LIST, UPDATE, INDEX
	}

	public static final class Util {
		public static String stringOfAccess(Access access) {
			if (access == null) return null;
			switch (access) {
			case INFO: return "info";
			case DETAILS: return "details";
			case THUMB: return "thumb";
			case STREAM: return "stream";
			case DOWNLOAD: return "download";
			case LIST: return "list";
			case UPDATE: return "update";
			case INDEX: return "index";
			default: return "unknown";
			}
		}
	}
	
	public boolean supportOperation(Operation op);
	
	public DataManager getManager();
	public String getName();
	public String getExtension();
	public String getOwner();
	
	public String getContentId();
	public String getContentType();

	//public long getCreatedTime();
	public long getModifiedTime();
	
	public boolean canRead();
	public boolean canMove();
	public boolean canDelete();
	public boolean canWrite();
	public boolean canCopy();
	
}
