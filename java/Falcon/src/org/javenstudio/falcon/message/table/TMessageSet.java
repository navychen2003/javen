package org.javenstudio.falcon.message.table;

import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.IMessageSet;

final class TMessageSet implements IMessageSet {

	private final TMessage[] mMessages;
	private final int mTotalCount;
	private final int mStart;
	private final long mUpdateTime;
	private int mIndex = 0;
	
	public TMessageSet(TMessage[] messages, 
			int totalCount, int start, long updateTime) {
		mMessages = messages;
		mTotalCount = totalCount;
		mStart = start;
		mUpdateTime = updateTime;
	}
	
	@Override
	public long getUpdateTime() {
		return mUpdateTime;
	}
	
	@Override
	public int getTotalCount() {
		return mTotalCount;
	}
	
	@Override
	public int getStart() {
		return mStart;
	}
	
	@Override
	public IMessage[] getMessages() {
		return mMessages;
	}
	
	@Override
	public synchronized void first() {
		mIndex = 0;
	}
	
	@Override
	public synchronized IMessage next() {
		if (mMessages != null && mMessages.length > 0) {
			if (mIndex >= 0 && mIndex < mMessages.length) 
				return mMessages[mIndex++];
		}
		return null;
	}
	
}
