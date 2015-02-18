package org.javenstudio.falcon.message;

import java.util.ArrayList;
import java.util.List;

public class MessageQuery implements IMessageQuery {

	private final List<String> mFolderNames = new ArrayList<String>();
	
	private final long mResultStart;
	private final int mResultCount;
	
	private String mStreamId = null;
	private String mChatUser = null;
	private String mStatus = null;
	private String mFlag = null;
	private String mAccount = null;
	
	private long mTimeStart = -1;
	private long mTimeEnd = -1;
	private int mRowSize = 1000;
	private int mGroupRowSize = 10;
	private GroupBy mGroupBy = null;
	
	public MessageQuery(long start, int count) { 
		mResultStart = start;
		mResultCount = count;
	}
	
	@Override
	public long getResultStart() {
		return mResultStart;
	}

	@Override
	public int getResultCount() {
		return mResultCount;
	}
	
	@Override
	public String[] getFolderNames() {
		synchronized (mFolderNames) {
			return mFolderNames.toArray(
					new String[mFolderNames.size()]);
		}
	}
	
	public void addFolderName(String folder) {
		if (folder == null || folder.length() == 0)
			return;
		
		synchronized (mFolderNames) {
			for (String name : mFolderNames) {
				if (folder.equals(name)) return;
			}
			
			mFolderNames.add(folder);
		}
	}
	
	@Override
	public String getAccount() {
		return mAccount;
	}
	
	public void setAccount(String account) {
		mAccount = account;
	}
	
	@Override
	public String getChatUser() {
		return mChatUser;
	}
	
	public void setChatUser(String val) {
		mChatUser = val;
	}
	
	@Override
	public String getStreamId() {
		return mStreamId;
	}
	
	public void setStreamId(String val) {
		mStreamId = val;
	}
	
	@Override
	public String getStatus() { 
		return mStatus;
	}
	
	public void setStatus(String val) {
		mStatus = val;
	}
	
	@Override
	public String getFlag() {
		return mFlag;
	}
	
	public void setFlag(String val) {
		mFlag = val;
	}
	
	@Override
	public GroupBy getGroupBy() {
		return mGroupBy;
	}
	
	public void setGroupBy(GroupBy groupby, int rowSize) {
		mGroupBy = groupby;
		mGroupRowSize = rowSize;
	}
	
	@Override
	public int getGroupRowSize() {
		return mGroupRowSize;
	}
	
	@Override
	public int getRowSize() {
		return mRowSize;
	}
	
	public void setRowSize(int size) {
		if (size > 0) mRowSize = size;
	}
	
	@Override
	public long getTimeStart() {
		return mTimeStart;
	}
	
	@Override
	public long getTimeEnd() {
		return mTimeEnd;
	}
	
	public void setTimeRange(long start, long end) {
		mTimeStart = start;
		mTimeEnd = end;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{folders=" + mFolderNames 
				+ ",chatuser=" + mChatUser + ",rowsize=" + mRowSize 
				+ ",status=" + mStatus + ",flag=" + mFlag + ",account=" + mAccount
				+ ",start=" + mResultStart + ",count=" + mResultCount 
				+ ",groupby=" + mGroupBy + ",growsize=" + mGroupRowSize 
				+ ",timestart=" + mTimeStart + ",timeend=" + mTimeEnd 
				+ "}";
	}
	
}
