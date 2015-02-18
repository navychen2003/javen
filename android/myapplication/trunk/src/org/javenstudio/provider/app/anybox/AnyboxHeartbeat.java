package org.javenstudio.provider.app.anybox;

import java.io.IOException;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.AccountWork;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.notify.ISystemNotifyData;
import org.javenstudio.util.StringUtils;

public class AnyboxHeartbeat {
	private static final Logger LOG = Logger.getLogger(AnyboxHeartbeat.class);
	
	public class SystemData implements ISystemNotifyData {
		
		private final long mNow;
		private final int mTZRawOffset;
		private final String mNotice;
		
		private SystemData(long now, int tz, String notice) {
			mNow = now;
			mTZRawOffset = tz;
			mNotice = notice; 
		}
		
		public long getNow() { return mNow; }
		public int getTZRawOffset() { return mTZRawOffset; }
		public String getNotice() { return mNotice; }
		
		public boolean hasNotice() { 
			return mNotice != null && mNotice.length() > 0;
		}
	}
	
	public class MessagesData {
		private final int mCount;
		private final long mUpdateTime;
		
		private MessagesData(int count, long updateTime) {
			mCount = count;
			mUpdateTime = updateTime;
		}
		
		public int getTotalCount() { return mCount; }
		public long getUpdateTime() { return mUpdateTime; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{count=" + mCount 
					+ ",updateTime=" + mUpdateTime + "}";
		}
	}
	
	public class InvitesData {
		private final int mCount;
		private final long mUpdateTime;
		
		private InvitesData(int count, long updateTime) {
			mCount = count;
			mUpdateTime = updateTime;
		}
		
		public int getTotalCount() { return mCount; }
		public long getUpdateTime() { return mUpdateTime; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{count=" + mCount 
					+ ",updateTime=" + mUpdateTime + "}";
		}
	}
	
	public class UserData {
		private final String mKey;
		private String mFlag;
		private String mIdle;
		private long mUsableSpace;
		private long mUpdateTime;
		
		private UserData(String key) {
			mKey = key;
		}
		
		public String getKey() { return mKey; }
		public String getFlag() { return mFlag; }
		public String getIdle() { return mIdle; }
		
		public long getUsableSpace() { return mUsableSpace; }
		public long getUpdateTime() { return mUpdateTime; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{key=" + mKey 
					+ ",utime=" + mUpdateTime + "}";
		}
	}
	
	private final AnyboxAccount mUser;
	//private final AnyboxData mData;
	//private final ActionError mError;
	private final long mRequestTime;
	
	private SystemData mSystemData = null;
	private UserData mUserData = null;
	private InvitesData mInvitesData = null;
	private MessagesData mMessagesData = null;
	
	private AnyboxHeartbeat(AnyboxAccount user, 
			AnyboxData data, ActionError error, long requestTime) {
		if (user == null) throw new NullPointerException();
		mRequestTime = requestTime;
		mUser = user;
		//mData = data;
		//mError = error;
	}
	
	public AnyboxAccount getUser() { return mUser; }
	//public AnyboxData getData() { return mData; }
	//public ActionError getError() { return mError; }
	public long getRequestTime() { return mRequestTime; }
	
	public SystemData getSystemData() { return mSystemData; }
	public UserData getUserData() { return mUserData; }
	public InvitesData getInvitesData() { return mInvitesData; }
	public MessagesData getMessagesData() { return mMessagesData; }
	
	private void loadData(AnyboxData data) throws IOException {
		if (data == null) return;
		
		AnyboxData system = data.get("system");
		if (system != null) {
			if (LOG.isDebugEnabled())
				LOG.debug("loadData: system=" + system);
			
			long now = system.getLong("now", 0);
			int tz = system.getInt("tz", 0);
			String notice = system.getString("notice");
			
			mSystemData = new SystemData(now, tz, notice);
		}
		
		AnyboxData user = data.get("user");
		if (user != null) {
			if (LOG.isDebugEnabled())
				LOG.debug("loadData: user=" + user);
			
			String key = user.getString("key");
			String flag = user.getString("flag");
			String idle = user.getString("idle");
			long usable = user.getLong("usable", 0);
			long utime = user.getLong("utime", 0);
			
			UserData userData = new UserData(key);
			userData.mFlag = flag;
			userData.mIdle = idle;
			userData.mUsableSpace = usable;
			userData.mUpdateTime = utime;
			
			mUserData = userData;
			
			loadInvites(user.get("invites"));
			loadMessages(user.get("messages"));
		}
	}
	
	private void loadInvites(AnyboxData data) throws IOException {
		if (data == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadInvites: invites=" + data);
		
		int count = data.getInt("count", 0);
		long updateTime = data.getLong("utime", 0);
		
		mInvitesData =  new InvitesData(count, updateTime);
	}
	
	private void loadMessages(AnyboxData data) throws IOException {
		if (data == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadMessages: messages=" + data);
		
		int count = data.getInt("count", 0);
		long updateTime = data.getLong("utime", 0);
		
		mMessagesData = new MessagesData(count, updateTime);
	}
	
	public static ActionError getHeartbeat(AnyboxAccount user, 
			ProviderCallback callback) {
		if (user == null) return null;
		
		AnyboxAccountInfo accountInfo = user.getAnyboxAccountInfo();
		
		String url = user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/user/heartbeat?wt=secretjson&action=access" 
				+ "&token=" + StringUtils.URLEncode(user.getAuthToken());
		
		HeartbeatListener listener = new HeartbeatListener(user);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			AnyboxHeartbeat heartbeat = listener.mHeartbeat;
			if (heartbeat != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					error = null;
					heartbeat.loadData(listener.mData);
					user.setHeartbeat(heartbeat);
					
					checkFetchAccountInfo(user, heartbeat, accountInfo);
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isErrorEnabled())
				LOG.error("getHeartbeat: error: " + e, e);
		}
		
		if (callback != null && error != null) 
			callback.onActionError(error);
		
		return error;
	}
	
	static void checkFetchAccountInfo(AnyboxAccount user, 
			AnyboxHeartbeat heartbeat, AnyboxAccountInfo accountInfo) {
		if (user == null || heartbeat == null)
			return;
		
		boolean fetchAccountInfo = false;
		if (accountInfo != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("checkFetchAccountInfo: accountInfo.requestTime=" + accountInfo.getRequestTime() 
						+ " heartbeatTime=" + heartbeat.getRequestTime());
			}
			if (accountInfo.getRequestTime() > heartbeat.getRequestTime())
				return;
			
			if (fetchAccountInfo == false) {
				AnyboxAccountInfo.UserData userData1 = accountInfo.getUserData();
				AnyboxHeartbeat.UserData userData2 = heartbeat.getUserData();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("checkFetchAccountInfo: accountInfo.userData=" + userData1 
							+ " heartbeat.userData=" + userData2);
				}
				
				if (userData1 != null && userData2 != null) {
					if (userData1.getUpdateTime() < userData2.getUpdateTime())
						fetchAccountInfo = true;
				}
			}
			
			if (fetchAccountInfo == false) {
				AnyboxAccountInfo.InvitesData invitesData1 = accountInfo.getInvitesData();
				AnyboxHeartbeat.InvitesData invitesData2 = heartbeat.getInvitesData();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("checkFetchAccountInfo: accountInfo.invitesData=" + invitesData1 
							+ " heartbeat.invitesData=" + invitesData2);
				}
				
				if (invitesData1 != null && invitesData2 != null) {
					if (invitesData1.getUpdateTime() < invitesData2.getUpdateTime() ||
						invitesData1.getTotalCount() != invitesData2.getTotalCount()) {
						fetchAccountInfo = true;
					}
				}
			}
			
			if (fetchAccountInfo == false) {
				AnyboxAccountInfo.MessagesData messagesData1 = accountInfo.getMessagesData();
				AnyboxHeartbeat.MessagesData messagesData2 = heartbeat.getMessagesData();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("checkFetchAccountInfo: accountInfo.messagesData=" + messagesData1 
							+ " heartbeat.messagesData=" + messagesData2);
				}
				
				if (messagesData1 != null && messagesData2 != null) {
					if (messagesData1.getUpdateTime() < messagesData2.getUpdateTime() ||
						messagesData1.getTotalCount() != messagesData2.getTotalCount()) {
						fetchAccountInfo = true;
					}
				}
			}
		} else
			fetchAccountInfo = true;
		
		if (fetchAccountInfo)
			user.getApp().getWork().scheduleWork(user, AccountWork.WorkType.ACCOUNTINFO, 0);
	}
	
	static class HeartbeatListener extends AnyboxApi.SecretJSONListener {
		private final AnyboxAccount mUser;
		private AnyboxHeartbeat mHeartbeat = null;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public HeartbeatListener(AnyboxAccount user) {
			if (user == null) throw new NullPointerException();
			mUser = user;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			mHeartbeat = new AnyboxHeartbeat(mUser, data, error, 
					System.currentTimeMillis());
			if (LOG.isDebugEnabled())
				LOG.debug("handleData: data=" + data);
			
			if (error != null) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("handleData: response error: " + error, 
							error.getException());
				}
			}
		}

		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.ACCOUNT_HEARTBEAT;
		}
	}

}
