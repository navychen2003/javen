package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.IDownloadable;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.dashboard.IHistorySectionData;

public class AnyboxHistory {
	private static final Logger LOG = Logger.getLogger(AnyboxHistory.class);
	
	public static class SectionData extends AnyboxImage 
			implements IHistorySectionData, IDownloadable {
		private final AnyboxDashboard mDashboard;
		private final String mId;
		private String mName;
		private boolean mIsFolder;
		private String mType;
		private String mExtName;
		private String mPath;
		private String mPoster;
		private String mBackground;
		private String mOwner;
		private String mChecksum;
		private long mModifiedTime;
		private long mLength;
		private int mWidth;
		private int mHeight;
		private long mTimeLen;
		private int mSubCount;
		private long mSubLen;
		
		private HistoryData mHistory;
		private UserData mUserData;
		
		private SectionData(AnyboxDashboard dashboard, String id) { 
			if (dashboard == null || id == null) throw new NullPointerException();
			mDashboard = dashboard;
			mId = id; 
		}
		
		public AnyboxDashboard getDashboard() { return mDashboard; }
		public AnyboxAccount getUser() { return getDashboard().getUser(); }
		
		public String getId() { return mId; }
		public String getName() { return mName; }
		public boolean isFolder() { return mIsFolder; }
		public String getType() { return mType; }
		public String getExtension() { return mExtName; }
		public String getPath() { return mPath; }
		public String getPoster() { return mPoster; }
		public String getBackground() { return mBackground; }
		public String getOwner() { return mOwner; }
		public String getChecksum() { return mChecksum; }
		public long getModifiedTime() { return mModifiedTime; }
		public long getLength() { return mLength; }
		public int getWidth() { return mWidth; }
		public int getHeight() { return mHeight; }
		public long getTimeLength() { return mTimeLen; }
		public int getSubCount() { return mSubCount; }
		public long getSubLen() { return mSubLen; }
		
		public HistoryData getHistory() { return mHistory; }
		public UserData getUserData() { return mUserData; }
		
		public String getTitle() { return getName(); }
		public String getContentId() { return getId(); }
		public String getContentType() { return getType(); }
		public String getContentName() { return getName(); }
		
		public Uri getContentUri() { 
			String url = AnyboxHelper.getFileURL(getUser(), getId(), false);
			return url != null ? Uri.parse(url) : null; 
		}
		
		public String getBackgroundURL() { return null; }
		public Drawable getTypeIcon() { return AnyboxSection.getSectionIcon(this); }
		
		public long getRefreshTime() {
			return getDashboard().getRequestTime();
		}
		
		public boolean supportOperation(FileOperation.Operation op) {
			if (op == null || isFolder()) return false;
			if (isAccountOwner()) return true;
			switch (op) {
			case SHARE:
			case DOWNLOAD:
			case OPEN:
				return true;
			default:
				return false;
			}
		}
		
		public boolean isAccountOwner() {
			String owner = getOwner();
			if (owner == null || owner.length() == 0 || 
				owner.equals(getUser().getAccountName())) {
				return true;
			}
			return false;
		}
		
		public void getDetails(IMediaDetails details) {
			if (details == null) return;
			
			String owner = getOwner();
			if (owner == null || owner.length() == 0)
				owner = getUser().getAccountName();
			
			details.add(R.string.details_filename, getName());
			details.add(R.string.details_filepath, getPath());
			details.add(R.string.details_contenttype, getType());
			details.add(R.string.details_owner, owner);
			details.add(R.string.details_filesize, getSizeInfo());
			details.add(R.string.details_lastmodified, 
					AppResources.getInstance().formatReadableTime(getModifiedTime()));
			details.add(R.string.details_checksum, getChecksum());
		}
		
		public void getExifs(IMediaDetails details) {
			if (details == null) return;
			
			AnyboxProperty property = getUser().getApp().getSectionProperty(getId());
			if (property != null) property.getExifs(details);
		}
		
		public String getTitleInfo() {
			int width = getWidth();
			int height = getHeight();
			if (width > 0 && height > 0) 
				return "" + width + "x" + height;
			
			long timelen = getTimeLength();
			if (timelen > 0) 
				return AppResources.getInstance().formatDuration(timelen);
			
			long length = getLength();
			if (length > 0)
				return AppResources.getInstance().formatReadableBytes(length);
			
			return null;
		}
		
		public String getSizeInfo() {
			String text = AppResources.getInstance().formatReadableBytes(getLength());
			String moretxt = null;
			
			int width = getWidth();
			int height = getHeight();
			if (width > 0 && height > 0) {
				moretxt = "" + width + "x" + height;
			} else {
				long timelen = getTimeLength();
				if (timelen > 0) 
					moretxt = AppResources.getInstance().formatDuration(timelen);
			}
			
			if (moretxt != null && moretxt.length() > 0)
				text += "(" + moretxt + ")";
			
			return text;
		}
		
		public String getSizeDetails() {
			return AppResources.getInstance().formatDetailsBytes(getLength());
		}
		
		public long getAccessTime() {
			HistoryData data = getHistory();
			if (data != null) return data.getTime();
			return getModifiedTime();
		}
		
		public String getUserTitle() {
			UserData user = getUserData();
			if (user != null) {
				String name = user.getNick();
				if (name == null || name.length() == 0)
					name = user.getName();
				if (name != null && name.length() > 0)
					return name;
			}
			return getOwner();
		}
		
		private HttpImage mAvatarImage = null;
		private String mAvatarImageURL = null;
		
		public Drawable getUserAvatarDrawable(int size, int padding) {
			HttpImage image = getAvatarImage();
			if (image != null) return image.getThumbnailDrawable(size, size);
			return null;
		}
		
		public Drawable getUserAvatarRoundDrawable(int size, int padding) {
			HttpImage image = getAvatarImage();
			if (image != null) { 
				return image.getRoundThumbnailDrawable(size, size, 
						padding, padding, padding, padding);
			}
			return null;
		}
		
		public synchronized HttpImage getAvatarImage() {
			if (mAvatarImage == null) {
				UserData user = getUserData();
				String avatarId = null;
				if (user != null) avatarId = user.getAvatar();
				if (avatarId != null && avatarId.length() > 0) {
					String imageURL = AnyboxHelper.getImageURL(getUser(), 
							avatarId, "192t", null);
					
					if (imageURL != null && imageURL.length() > 0) { 
						mAvatarImageURL = imageURL;
						mAvatarImage = HttpResource.getInstance().getImage(imageURL);
						mAvatarImage.addListener(this);
						
						HttpImageItem.requestDownload(mAvatarImage, false);
					}
				}
			}
			return mAvatarImage;
		}
		
		private String getImageURL(int size) {
			String posterId = getPoster();
			int posterWidth = 0;
			int posterHeight = 0;
			if (posterId == null || posterId.length() == 0) {
				String type = getType();
				if (type != null && type.startsWith("image/")) {
					posterId = getId();
					posterWidth = getWidth();
					posterHeight = getHeight();
				}
			}
			if (posterId != null && posterId.length() > 0) {
				String params = "" + size; //"1024";
				if (posterWidth > 0 && posterHeight > 0) {
					if (posterWidth <= 256 && posterHeight <= 256)
						params = "256";
				}
				String imageURL = AnyboxHelper.getImageURL(getUser(), 
						posterId, params, null);
				
				return imageURL;
			}
			return null;
		}
		
		public String getPhotoURL() {
			return getImageURL(4096);
		}
		
		public String getPosterURL() {
			return getSectionImageURL();
		}
		
		public String getPosterThumbnailURL() {
			return getSectionImageURL();
		}
		
		public String getSectionImageURL() {
			return getImageURL(1024);
		}
		
		public int getSectionImageWidth() { return getWidth(); }
		public int getSectionImageHeight() { return getHeight(); }
		
		@Override
		protected boolean isImageLocation(String location) {
			if (location != null && location.length() > 0) {
				String avatarImageURL = mAvatarImageURL;
				if (avatarImageURL != null && avatarImageURL.equals(location))
					return true;
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "AnyboxDashboard.SectionData{id=" + getId() 
					+ ",name=" + getName() + ",type=" + getType() + "}";
		}
	}
	
	static class HistoryData {
		private final String mId;
		private String mType;
		private String mLink;
		private String mTitle;
		private String mOwner;
		private String mOp;
		private long mTime;
		
		private HistoryData(String id) { mId = id; }
		
		public String getId() { return mId; }
		public String getType() { return mType; }
		public String getLink() { return mLink; }
		public String getTitle() { return mTitle; }
		public String getOwner() { return mOwner; }
		public String getOp() { return mOp; }
		public long getTime() { return mTime; }
	}
	
	static class UserData {
		private final String mKey;
		private String mName;
		private String mMailAddr;
		private String mNick;
		private String mCategory;
		private String mType;
		private String mFlag;
		private String mAvatar;
		private String mBackground;
		private String mIdle;
		
		private UserData(String key) { mKey = key; }
		
		public String getKey() { return mKey; }
		public String getName() { return mName; }
		public String getMailAddr() { return mMailAddr; }
		public String getNick() { return mNick; }
		public String getCategory() { return mCategory; }
		public String getType() { return mType; }
		public String getFlag() { return mFlag; }
		public String getAvatar() { return mAvatar; }
		public String getBackground() { return mBackground; }
		public String getIdle() { return mIdle; }
	}
	
	static SectionData[] loadSections(AnyboxDashboard dashboard, 
			AnyboxData data) throws IOException {
		if (dashboard == null || data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadSections: data=" + data);
		
		String[] names = data.getNames();
		if (names != null) {
			ArrayList<SectionData> list = new ArrayList<SectionData>();
			for (String name : names) {
				SectionData ad = loadSection(dashboard, data.get(name));
				if (ad != null) list.add(ad);
			}
			
			SectionData[] arr = list.toArray(new SectionData[list.size()]);
			if (arr != null) {
				Arrays.sort(arr, new Comparator<SectionData>() {
						@Override
						public int compare(SectionData lhs,
								SectionData rhs) {
							long ltm = lhs.getAccessTime();
							long rtm = rhs.getAccessTime();
							if (ltm < rtm) return 1;
							else if (ltm > rtm) return -1;
							
							long lm = lhs.getModifiedTime();
							long rm = rhs.getModifiedTime();
							if (lm < rm) return 1;
							else if (lm > rm) return -1;
							
							String lkey = lhs.getId();
							String rkey = rhs.getId();
							if (lkey != null && rkey != null)
								return lkey.compareTo(rkey);
							else if (lkey == null)
								return 1;
							else if (rkey == null)
								return -1;
							
							return 0;
						}
					});
			}
			
			//mSections = arr;
			return arr;
		}
		
		return null;
	}
	
	static SectionData loadSection(AnyboxDashboard dashboard, 
			AnyboxData data) throws IOException {
		if (data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadSection: data=" + data);
		
		String id = data.getString("id");
		SectionData sd = new SectionData(dashboard, id);
		sd.mName = data.getString("name");
		sd.mIsFolder = data.getBool("isfolder", false);
		sd.mType = data.getString("type");
		sd.mExtName = data.getString("extname");
		sd.mPath = data.getString("path");
		sd.mPoster = data.getString("poster");
		sd.mBackground = data.getString("background");
		sd.mOwner = data.getString("owner");
		sd.mChecksum = data.getString("checksum");
		sd.mModifiedTime = data.getLong("mtime", 0);
		sd.mLength = data.getLong("length", 0);
		sd.mWidth = data.getInt("width", 0);
		sd.mHeight = data.getInt("height", 0);
		sd.mTimeLen = data.getLong("timelen", 0);
		sd.mSubCount = data.getInt("subcount", 0);
		sd.mSubLen = data.getLong("sublen", 0);
		
		sd.mHistory = loadHistory(data.get("history"));
		sd.mUserData = loadUser(data.get("user"));
		
		return sd;
	}
	
	static HistoryData loadHistory(AnyboxData data) throws IOException {
		if (data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadHistory: data=" + data);
		
		String id = data.getString("id");
		HistoryData sd = new HistoryData(id);
		sd.mTitle = data.getString("title");
		sd.mType = data.getString("type");
		sd.mOwner = data.getString("owner");
		sd.mLink = data.getString("link");
		sd.mOp = data.getString("op");
		sd.mTime = data.getLong("time", 0);
		
		return sd;
	}
	
	static UserData loadUser(AnyboxData data) throws IOException {
		if (data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadUser: data=" + data);
		
		String id = data.getString("key");
		UserData sd = new UserData(id);
		sd.mName = data.getString("name");
		sd.mType = data.getString("type");
		sd.mMailAddr = data.getString("mailaddr");
		sd.mNick = data.getString("nick");
		sd.mCategory = data.getString("category");
		sd.mFlag = data.getString("flag");
		sd.mAvatar = data.getString("avatar");
		sd.mBackground = data.getString("background");
		sd.mIdle = data.getString("idle");
		
		return sd;
	}
	
}
