package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TimeZone;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.AccountInfo;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.notify.IAnnouncementNotifyData;
import org.javenstudio.provider.account.notify.IInviteNotifyData;
import org.javenstudio.provider.account.notify.IMessageNotifyData;
import org.javenstudio.provider.account.notify.INotifySource;
import org.javenstudio.provider.account.notify.ISystemNotifyData;
import org.javenstudio.util.StringUtils;

public class AnyboxAccountInfo implements INotifySource {
	private static final Logger LOG = Logger.getLogger(AnyboxAccountInfo.class);
	
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
	
	public class MessageData extends AnyboxImage
			implements IMessageNotifyData.IMessageItem {
		
		private final String mId;
		private final String mType;
		private String mFolder;
		private String mFrom;
		private String mTo;
		private String mCc;
		private String mSubject;
		private String mContentType;
		private String mStatus;
		private String mFlag;
		private long mCreatedTime;
		private long mUpdateTime;
		private long mMessageTime;
		private int mAttachmentCount;
		private String mFromTitle;
		private String mFromAvatar;
		
		private MessageData(String id, String type) {
			mId = id;
			mType = type;
		}
		
		public String getId() { return mId; }
		public String getType() { return mType; }
		public String getFolder() { return mFolder; }
		public String getFrom() { return mFrom; }
		public String getTo() { return mTo; }
		public String getCc() { return mCc; }
		public String getSubject() { return mSubject; }
		public String getContentType() { return mContentType; }
		public String getStatus() { return mStatus; }
		public String getFlag() { return mFlag; }
		public long getCreatedTime() { return mCreatedTime; }
		public long getUpdateTime() { return mUpdateTime; }
		public long getMessageTime() { return mMessageTime; }
		public int getAttachmentCount() { return mAttachmentCount; }
		public String getFromTitle() { return mFromTitle; }
		public String getFromAvatar() { return mFromAvatar; }
		
		@Override
		public String getMessageTitle() {
			String name = getFromTitle();
			if (name == null || name.length() == 0)
				name = getFrom();
			return name;
		}
		
		@Override
		public String getMessageBody() {
			return getSubject();
		}

		private HttpImage mImage = null;
		private String mImageURL = null;
		
		@Override
		public Drawable getAvatarDrawable(int size, int padding) {
			HttpImage image = getImage();
			if (image != null) return image.getThumbnailDrawable(size, size);
			return null;
		}
		
		private synchronized HttpImage getImage() {
			if (mImage == null) {
				String avatarId = getFromAvatar();
				if (avatarId != null && avatarId.length() > 0) {
					String imageURL = AnyboxHelper.getImageURL(getUser(), 
							avatarId, "192t", null);
					
					if (imageURL != null && imageURL.length() > 0) { 
						mImageURL = imageURL;
						mImage = HttpResource.getInstance().getImage(imageURL);
						mImage.addListener(this);
						
						HttpImageItem.requestDownload(mImage, false);
					}
				}
			}
			return mImage;
		}
		
		@Override
		protected boolean isImageLocation(String location) {
			if (location != null && location.length() > 0) {
				String imageURL = mImageURL;
				if (imageURL != null && imageURL.equals(location))
					return true;
			}
			return false;
		}
	}
	
	public class MessagesData implements IMessageNotifyData {
		private final int mCount;
		private final long mUpdateTime;
		
		private MessagesData(int count, long updateTime) {
			mCount = count;
			mUpdateTime = updateTime;
		}
		
		public int getTotalCount() { return mCount; }
		public long getUpdateTime() { return mUpdateTime; }

		@Override
		public IMessageItem[] getItems() {
			return AnyboxAccountInfo.this.getMessages();
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{count=" + mCount 
					+ ",updateTime=" + mUpdateTime + "}";
		}
	}
	
	public class InviteData extends AnyboxImage
			implements IInviteNotifyData.IInviteItem {
		
		private final String mKey;
		private final String mName;
		private final String mType;
		private String mMessage;
		private long mTime;
		private String mTitle;
		private String mAvatar;
		
		private InviteData(String key, String name, String type) {
			mKey = key;
			mName = name;
			mType = type;
		}
		
		public String getKey() { return mKey; }
		public String getName() { return mName; }
		public String getType() { return mType; }
		public String getMessage() { return mMessage; }
		public String getTitle() { return mTitle; }
		public String getAvatar() { return mAvatar; }
		public long getTime() { return mTime; }
		
		@Override
		public String getInviteType() {
			return getType();
		}
		
		@Override
		public String getInviteTitle() {
			String name = getTitle();
			if (name == null || name.length() == 0) 
				name = getName();
			return name;
		}
		
		@Override
		public String getInviteMessage() {
			return getMessage();
		}
		
		@Override
		public long getInviteTime() {
			return getTime();
		}
		
		private HttpImage mImage = null;
		private String mImageURL = null;
		
		@Override
		public Drawable getAvatarDrawable(int size, int padding) {
			HttpImage image = getImage();
			if (image != null) return image.getThumbnailDrawable(size, size);
			return null;
		}
		
		private synchronized HttpImage getImage() {
			if (mImage == null) {
				String avatarId = getAvatar();
				if (avatarId != null && avatarId.length() > 0) {
					String imageURL = AnyboxHelper.getImageURL(getUser(), 
							avatarId, "192t", null);
					
					if (imageURL != null && imageURL.length() > 0) { 
						mImageURL = imageURL;
						mImage = HttpResource.getInstance().getImage(imageURL);
						mImage.addListener(this);
						
						HttpImageItem.requestDownload(mImage, false);
					}
				}
			}
			return mImage;
		}
		
		@Override
		protected boolean isImageLocation(String location) {
			if (location != null && location.length() > 0) {
				String imageURL = mImageURL;
				if (imageURL != null && imageURL.equals(location))
					return true;
			}
			return false;
		}
	}
	
	public class InvitesData implements IInviteNotifyData {
		private final int mCount;
		private final long mUpdateTime;
		
		private InvitesData(int count, long updateTime) {
			mCount = count;
			mUpdateTime = updateTime;
		}
		
		public int getTotalCount() { return mCount; }
		public long getUpdateTime() { return mUpdateTime; }

		@Override
		public IInviteItem[] getItems() {
			return AnyboxAccountInfo.this.getInvites();
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{count=" + mCount 
					+ ",updateTime=" + mUpdateTime + "}";
		}
	}
	
	public class UserData implements AccountInfo {
		private final String mKey;
		private String mName;
		private String mMailAddr;
		private String mNick;
		private String mCategory;
		private String mEmail;
		private String mAvatar;
		private String mBackground;
		private String mToken;
		private String mClient;
		private String mDeviceKey;
		private String mAuthKey;
		private String mType;
		private String mFlag;
		private String mIdle;
		private long mUsedSpace;
		private long mUsableSpace;
		private long mFreeSpace;
		private long mCapacity;
		private long mPurchased;
		private long mUpdateTime;
		
		private UserData(String key) {
			mKey = key;
		}
		
		public String getUserKey() { return mKey; }
		public String getUserName() { return mName; }
		public String getNickName() { return mNick; }
		public String getMailAddress() { return mMailAddr; }
		public String getType() { return mType; }
		public String getCategory() { return mCategory; }
		public String getAvatar() { return mAvatar; }
		public String getBackground() { return mBackground; }
		public String getEmail() { return mEmail; }
		public String getFlag() { return mFlag; }
		public String getToken() { return mToken; }
		public String getClient() { return mClient; }
		public String getDeviceKey() { return mDeviceKey; }
		public String getAuthKey() { return mAuthKey; }
		public String getIdle() { return mIdle; }
		
		public long getUsedSpace() { return mUsedSpace; }
		public long getUsableSpace() { return mUsableSpace; }
		public long getFreeSpace() { return mFreeSpace; }
		public long getPurchased() { return mPurchased; }
		public long getCapacity() { return mCapacity; }
		public long getUpdateTime() { return mUpdateTime; }
		
		public String getFullName() {
			return getUser().getAccountData().getFullName();
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{key=" + mKey 
					+ ",name=" + mName + ",utime=" + mUpdateTime + "}";
		}
	}
	
	private final AnyboxAccount mUser;
	//private final AnyboxData mData;
	//private final ActionError mError;
	private final long mRequestTime;
	private final int mTZRawOffset;
	
	private SystemData mSystemData = null;
	private UserData mUserData = null;
	private InvitesData mInvitesData = null;
	private InviteData[] mInvites = null;
	private MessagesData mMessagesData = null;
	private MessageData[] mMessages = null;
	
	private long mNewOpenedTime = 0;
	
	private AnyboxAccountInfo(AnyboxAccount user, 
			AnyboxData data, ActionError error, long requestTime) {
		if (user == null) throw new NullPointerException();
		mRequestTime = requestTime;
		mTZRawOffset = TimeZone.getDefault().getRawOffset();
		mUser = user;
		//mData = data;
		//mError = error;
	}
	
	public AnyboxAccount getUser() { return mUser; }
	//public AnyboxData getData() { return mData; }
	//public ActionError getError() { return mError; }
	public long getRequestTime() { return mRequestTime; }
	public int getTZRawOffset() { return mTZRawOffset; }
	
	public SystemData getSystemData() { return mSystemData; }
	public UserData getUserData() { return mUserData; }
	public InvitesData getInvitesData() { return mInvitesData; }
	public InviteData[] getInvites() { return mInvites; }
	public MessagesData getMessagesData() { return mMessagesData; }
	public MessageData[] getMessages() { return mMessages; }
	
	public long getNewOpenedTime() { return mNewOpenedTime; }
	public void setNewOpenedTime(long time) { mNewOpenedTime = time; }
	
	public boolean hasNew() {
		InvitesData invitesData = getInvitesData();
		MessagesData messagesData = getMessagesData();
		
		if (invitesData != null && invitesData.getTotalCount() > 0) {
			if (invitesData.getUpdateTime() > getNewOpenedTime())
				return true;
		}
		
		if (messagesData != null && messagesData.getTotalCount() > 0) {
			if (messagesData.getUpdateTime() > getNewOpenedTime())
				return true;
		}
		
		return false;
	}
	
	private void loadData(AnyboxData data) throws IOException {
		if (data == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadData: requestTime=" + getRequestTime() 
					+ " tzRawOffset=" + getTZRawOffset());
		}
		
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
			UserData userData = new UserData(key);
			userData.mName = user.getString("name");
			userData.mMailAddr = user.getString("mailaddr");
			userData.mNick = user.getString("nick");
			userData.mCategory = user.getString("category");
			userData.mEmail = user.getString("email");
			userData.mAvatar = user.getString("avatar");
			userData.mBackground = user.getString("background");
			userData.mToken = user.getString("token");
			userData.mClient = user.getString("client");
			userData.mDeviceKey = user.getString("devicekey");
			userData.mAuthKey = user.getString("authkey");
			userData.mType = user.getString("type");
			userData.mFlag = user.getString("flag");
			userData.mIdle = user.getString("idle");
			userData.mUsedSpace = user.getLong("used", 0);
			userData.mUsableSpace = user.getLong("usable", 0);
			userData.mFreeSpace = user.getLong("free", 0);
			userData.mCapacity = user.getLong("capacity", 0);
			userData.mPurchased = user.getLong("purchased", 0);
			userData.mUpdateTime = user.getLong("utime", 0);
			
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
		
		if (count <= 0) mInvites = null;
		loadInviteList(data.get("invites"));
	}
	
	private void loadInviteList(AnyboxData data) throws IOException {
		if (data == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadInviteList: invites=" + data);
		
		String[] names = data.getNames();
		ArrayList<InviteData> list = new ArrayList<InviteData>();
		
		if (names != null) {
			for (String name : names) {
				InviteData invite = loadInvite(data.get(name));
				if (invite != null) list.add(invite);
			}
		}
		
		InviteData[] invites = list.toArray(new InviteData[list.size()]);
		if (invites != null) {
			Arrays.sort(invites, new Comparator<InviteData>() {
					@Override
					public int compare(InviteData lhs, InviteData rhs) {
						if (lhs != null || rhs != null) {
							if (lhs == null) return 1;
							if (rhs == null) return -1;
							
							long ltm = lhs.getInviteTime();
							long rtm = rhs.getInviteTime();
							if (ltm > rtm) return -1;
							else if (ltm < rtm) return 1;
							
							String lkey = lhs.getKey();
							String rkey = lhs.getKey();
							if (lkey != null && rkey != null)
								return lkey.compareTo(rkey);
							
							if (lkey == null) return 1;
							if (rkey == null) return -1;
						}
						return 0;
					}
				});
		}
		
		mInvites = invites;
	}
	
	private void loadMessages(AnyboxData data) throws IOException {
		if (data == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadMessages: messages=" + data);
		
		int count = data.getInt("count", 0);
		long updateTime = data.getLong("utime", 0);
		
		mMessagesData = new MessagesData(count, updateTime);
		
		if (count <= 0) mMessages = null;
		loadMessageList(data.get("messages"));
	}
	
	private void loadMessageList(AnyboxData data) throws IOException {
		if (data == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadMessageList: messages=" + data);
		
		String[] names = data.getNames();
		ArrayList<MessageData> list = new ArrayList<MessageData>();
		
		if (names != null) {
			for (String name : names) {
				MessageData message = loadMessage(data.get(name));
				if (message != null) list.add(message);
			}
		}
		
		MessageData[] messages = list.toArray(new MessageData[list.size()]);
		if (messages != null) {
			Arrays.sort(messages, new Comparator<MessageData>() {
					@Override
					public int compare(MessageData lhs, MessageData rhs) {
						if (lhs != null || rhs != null) {
							if (lhs == null) return 1;
							if (rhs == null) return -1;
							
							long ltm = lhs.getMessageTime();
							long rtm = rhs.getMessageTime();
							if (ltm > rtm) return -1;
							else if (ltm < rtm) return 1;
							
							String lkey = lhs.getId();
							String rkey = lhs.getId();
							if (lkey != null && rkey != null)
								return lkey.compareTo(rkey);
							
							if (lkey == null) return 1;
							if (rkey == null) return -1;
						}
						return 0;
					}
				});
		}
		
		mMessages = messages;
	}
	
	private InviteData loadInvite(AnyboxData data) throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadInvite: invite=" + data);
		
		String key = data.getString("key");
		String name = data.getString("name");
		String type = data.getString("type");
		String message = data.getString("message");
		String title = data.getString("title");
		String avatar = data.getString("avatar");
		long time = data.getLong("time", 0);
		
		InviteData invite = new InviteData(key, name, type);
		invite.mMessage = message;
		invite.mTitle = title;
		invite.mAvatar = avatar;
		invite.mTime = time;
		
		return invite;
	}
	
	private MessageData loadMessage(AnyboxData data) throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadMessage: message=" + data);
		
		String id = data.getString("id");
		String type = data.getString("type");
		String folder = data.getString("folder");
		String from = data.getString("from");
		String to = data.getString("to");
		String cc = data.getString("cc");
		String subject = data.getString("subject");
		String ctype = data.getString("ctype");
		String status = data.getString("status");
		String flag = data.getString("flag");
		long ctime = data.getLong("ctime", 0);
		long utime = data.getLong("utime", 0);
		long mtime = data.getLong("mtime", 0);
		int attcount = data.getInt("attcount", 0);
		String fromtitle = data.getString("fromtitle");
		String fromavatar = data.getString("fromavatar");
		
		MessageData message = new MessageData(id, type);
		message.mFolder = folder;
		message.mFrom = from;
		message.mTo = to;
		message.mCc = cc;
		message.mSubject = subject;
		message.mContentType = ctype;
		message.mStatus = status;
		message.mFlag = flag;
		message.mCreatedTime = ctime;
		message.mUpdateTime = utime;
		message.mMessageTime = mtime;
		message.mAttachmentCount = attcount;
		message.mFromTitle = fromtitle;
		message.mFromAvatar = fromavatar;
		
		return message;
	}
	
	public static ActionError getAccountInfo(AnyboxAccount user, 
			ProviderCallback callback) {
		if (user == null) return null;
		
		String url = user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/user/heartbeat?wt=secretjson&action=info" 
				+ "&maxinvites=3&maxmessages=3&maxworks=3"
				+ "&token=" + StringUtils.URLEncode(user.getAuthToken());
		
		AccountInfoListener listener = new AccountInfoListener(user);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			AnyboxAccountInfo accountInfo = listener.mAccountInfo;
			if (accountInfo != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					error = null;
					accountInfo.loadData(listener.mData);
					user.setAnyboxAccountInfo(accountInfo);
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isErrorEnabled())
				LOG.error("accountInfo: error: " + e, e);
		}
		
		if (callback != null && error != null) 
			callback.onActionError(error);
		
		return error;
	}
	
	static class AccountInfoListener extends AnyboxApi.SecretJSONListener {
		private final AnyboxAccount mUser;
		private AnyboxAccountInfo mAccountInfo = null;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public AccountInfoListener(AnyboxAccount user) {
			if (user == null) throw new NullPointerException();
			mUser = user;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			mAccountInfo = new AnyboxAccountInfo(mUser, data, error, 
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
			return ActionError.Action.ACCOUNT_INFO;
		}
	}

	@Override
	public AccountUser getAccountUser() {
		return getUser();
	}
	
	@Override
	public ISystemNotifyData getSystemNotifyData() {
		SystemData data = getSystemData();
		if (data.hasNotice()) return data;
		return null;
	}

	@Override
	public IInviteNotifyData getInviteNotifyData() {
		IInviteNotifyData data = getInvitesData();
		if (data != null && data.getTotalCount() > 0) return data;
		return null;
	}

	@Override
	public IMessageNotifyData getMessageNotifyData() {
		IMessageNotifyData data = getMessagesData();
		if (data != null && data.getTotalCount() > 0) return data;
		return null;
	}

	@Override
	public IAnnouncementNotifyData getAnnouncementNotifyData() {
		AnyboxDashboard dashboard = getUser().getDashboard();
		if (dashboard != null) { 
			IAnnouncementNotifyData data = dashboard.getAnnouncementData();
			if (data != null && data.getTotalCount() > 0)
				return data;
		}
		return null;
	}
	
}
