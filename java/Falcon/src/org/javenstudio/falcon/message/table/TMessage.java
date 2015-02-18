package org.javenstudio.falcon.message.table;

import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.MessageHelper;

final class TMessage implements IMessage {

	private final TMessageService mService;
	private final String mId;
	
	private String mFrom = null;
	private String mTo = null;
	private String mCc = null;
	private String mBcc = null;
	private String mReplyTo = null;
	private String mHeaderLines = null;
	private String mType = null;
	private String mSubject = null;
	private String mContentType = null;
	private String mBody = null;
	
	private String mAccount = null;
	private String mFlag = null;
	private String mStatus = null;
	private String mStreamId = null;
	private String mFolder = null;
	private String mFolderFrom = null;
	private String mReplyId = null;
	
	private String mSourceFile = null;
	private String[] mAttachmentFiles = null;
	
	private long mCreatedTime = 0;
	private long mUpdateTime = 0;
	private long mMessageTime = 0;
	
	public TMessage(TMessageService service, String id) { 
		if (service == null || id == null) throw new NullPointerException();
		mService = service;
		mId = id;
		//mCreatedTime = System.currentTimeMillis();
	}
	
	public TMessageService getService() { return mService; }
	public String getMessageId() { return mId; }
	
	public String getAccount() { return mAccount; }
	public void setAccount(String val) { mAccount = val; }
	
	public String getFolder() { return mFolder; }
	public void setFolder(String val) { mFolder = val; }
	
	public String getFolderFrom() { return mFolderFrom; }
	public void setFolderFrom(String val) { mFolderFrom = val; }
	
	public String getReplyId() { return mReplyId; }
	public void setReplyId(String id) { mReplyId = id; }
	
	public String getFrom() { return mFrom; }
	public void setFrom(String from) { mFrom = from; }
	
	public String getTo() { return mTo; }
	public void setTo(String to) { mTo = to; }
	
	public String getCc() { return mCc; }
	public void setCc(String cc) { mCc = cc; }
	
	public String getBcc() { return mBcc; }
	public void setBcc(String bcc) { mBcc = bcc; }
	
	public String getReplyTo() { return mReplyTo; }
	public void setReplyTo(String to) { mReplyTo = to; }
	
	public String getFlag() { return mFlag; }
	public void setFlag(String val) { mFlag = val; }
	
	public String getStatus() { return mStatus; }
	public void setStatus(String status) { mStatus = status; }
	
	public long getCreatedTime() { return mCreatedTime; }
	public void setCreatedTime(long time) { mCreatedTime = time; }
	
	public long getMessageTime() { return mMessageTime; }
	public void setMessageTime(long time) { mMessageTime = time; }
	
	public long getUpdateTime() { return mUpdateTime; }
	public void setUpdateTime(long time) { mUpdateTime = time; }
	
	public String getMessageType() { return mType; }
	public void setMessageType(String val) { mType = val; }
	
	public String getSubject() { return mSubject; }
	public void setSubject(String val) { mSubject = val; }
	
	public String getContentType() { return mContentType; }
	public void setContentType(String val) { mContentType = val; }
	
	public String getBody() { return mBody; }
	public void setBody(String val) { mBody = val; }
	
	public String getHeaderLines() { return mHeaderLines; }
	public void setHeaderLines(String val) { mHeaderLines = val; }
	
	public String getSourceFile() { return mSourceFile; }
	public void setSourceFile(String val) { mSourceFile = val; }
	
	public String[] getAttachmentFiles() { return mAttachmentFiles; }
	public void setAttachmentFiles(String[] vals) { mAttachmentFiles = vals; }
	
	@Override
	public synchronized String getStreamId() { 
		if (mStreamId == null || mStreamId.length() == 0)
			mStreamId = MessageHelper.getStreamKey(mId);
		return mStreamId; 
	}
	
	public synchronized void setStreamId(String id) { 
		mStreamId = id; 
	}
	
	@Override
	public boolean equals(Object obj) { 
		if (obj == this) return true;
		if (obj == null || !(obj instanceof TMessage)) return false;
		
		TMessage other = (TMessage)obj;
		return this.getMessageId().equals(other.getMessageId());
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + mId + ",account=" + mAccount
				+ ",folder=" + mFolder + ",folderFrom=" + mFolderFrom
				+ ",replyId=" + mReplyId + ",streamId=" + getStreamId()
				+ ",from=" + mFrom + ",to=" + mTo + ",cc=" + mCc + ",bcc=" + mBcc
				+ ",replyTo=" + mReplyTo + ",subject=" + mSubject 
				+ ",contentType=" + mContentType + ",type=" + mType + ",source=" + mSourceFile 
				+ ",attachments=[" + MessageHelper.combineValues(mAttachmentFiles)
				+ "],mtime=" + mMessageTime + ",ctime=" + mCreatedTime 
				+ ",utime=" + mUpdateTime + ",status=" + mStatus + "}";
	}
	
}
