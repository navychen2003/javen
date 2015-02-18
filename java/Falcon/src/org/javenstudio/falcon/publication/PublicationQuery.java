package org.javenstudio.falcon.publication;

import java.util.ArrayList;
import java.util.List;

public class PublicationQuery implements IPublicationQuery {

	private final List<String> mChannelNames = new ArrayList<String>();
	
	private final long mResultStart;
	private final int mResultCount;
	
	private String mStreamId = null;
	private String mStatus = null;
	private String mFlag = null;
	private String mOwner = null;
	
	private long mTimeStart = -1;
	private long mTimeEnd = -1;
	private int mRowSize = 1000;
	private int mGroupRowSize = 10;
	private GroupBy mGroupBy = null;
	
	public PublicationQuery(long start, int count) { 
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
	public String[] getChannelNames() {
		synchronized (mChannelNames) {
			return mChannelNames.toArray(
					new String[mChannelNames.size()]);
		}
	}
	
	public void addChannelName(String channel) {
		if (channel == null || channel.length() == 0)
			return;
		
		synchronized (mChannelNames) {
			for (String name : mChannelNames) {
				if (channel.equals(name)) return;
			}
			
			mChannelNames.add(channel);
		}
	}
	
	@Override
	public String getOwner() {
		return mOwner;
	}
	
	public void setOwner(String owner) {
		mOwner = owner;
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
		return getClass().getSimpleName() + "{channels=" + mChannelNames 
				+ ",rowsize=" + mRowSize + ",status=" + mStatus + ",flag=" + mFlag 
				+ ",owner=" + mOwner + ",start=" + mResultStart + ",count=" + mResultCount 
				+ ",groupby=" + mGroupBy + ",growsize=" + mGroupRowSize 
				+ ",timestart=" + mTimeStart + ",timeend=" + mTimeEnd 
				+ "}";
	}
	
}
