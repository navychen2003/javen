package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.notify.IAnnouncementNotifyData;

final class AnyboxAnnouncement {
	private static final Logger LOG = Logger.getLogger(AnyboxAnnouncement.class);

	static class AnnouncementData extends AnyboxImage 
			implements IAnnouncementNotifyData.IAnnouncementItem {
		private final AnyboxAccount mUser;
		private final String mKey;
		private String mLang;
		private String mTitle;
		private String mLink;
		private String mBody;
		private String mBodyText;
		private String mPoster;
		private long mTime;
		
		AnnouncementData(AnyboxAccount user, String key) { 
			if (user == null || key == null) throw new NullPointerException();
			mUser = user;
			mKey = key; 
		}
		
		public AnyboxAccount getUser() { return mUser; }
		
		public String getKey() { return mKey; }
		public String getLang() { return mLang; }
		public String getTitle() { return mTitle; }
		public String getLink() { return mLink; }
		public String getBody() { return mBody; }
		public String getPoster() { return mPoster; }
		public long getTime() { return mTime; }
		
		@Override
		public String getAnnouncementTitle() {
			return getTitle();
		}
		
		@Override
		public String getAnnouncementBody() {
			return mBodyText;
		}
		
		@Override
		public long getPublishTime() {
			return getTime();
		}
		
		private HttpImage mImage = null;
		private String mImageURL = null;
		
		public Drawable getPosterDrawable(int size, int padding) {
			HttpImage image = getImage();
			if (image != null) return image.getThumbnailDrawable(size, size);
			return null;
		}
		
		public Drawable getPosterRoundDrawable(int size, int padding) {
			HttpImage image = getImage();
			if (image != null) { 
				return image.getRoundThumbnailDrawable(size, size, 
						padding, padding, padding, padding);
			}
			return null;
		}
		
		private synchronized HttpImage getImage() {
			if (mImage == null) {
				String posterId = getPoster();
				if (posterId != null && posterId.length() > 0) {
					String imageURL = AnyboxHelper.getImageURL(getUser(), 
							posterId, "192t", null);
					
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
	
	static AnnouncementData[] loadAnnouncements(AnyboxAccount user, 
			AnyboxData data) throws IOException {
		if (user == null || data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadAnnouncements: data=" + data);
		
		String[] names = data.getNames();
		if (names != null) {
			ArrayList<AnnouncementData> list = new ArrayList<AnnouncementData>();
			for (String name : names) {
				AnnouncementData ad = loadAnnouncement(user, data.get(name));
				if (ad != null) list.add(ad);
			}
			
			AnnouncementData[] arr = list.toArray(new AnnouncementData[list.size()]);
			if (arr != null) {
				Arrays.sort(arr, new Comparator<AnnouncementData>() {
						@Override
						public int compare(AnnouncementData lhs,
								AnnouncementData rhs) {
							long ltm = lhs.getTime();
							long rtm = rhs.getTime();
							if (ltm < rtm) return 1;
							else if (ltm > rtm) return -1;
							
							String lkey = lhs.getKey();
							String rkey = rhs.getKey();
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
			
			//mAnnouncementData = new AnnouncementNotifyData(arr);
			//mAnnouncements = arr;
			
			return arr;
		}
		
		return null;
	}
	
	static AnnouncementData loadAnnouncement(AnyboxAccount user, 
			AnyboxData data) throws IOException {
		if (user == null || data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadAnnouncement: data=" + data);
		
		String key = data.getString("key");
		AnnouncementData a = new AnnouncementData(user, key);
		a.mLang = data.getString("lang");
		a.mTitle = data.getString("title");
		a.mLink = data.getString("link");
		a.mBody = data.getString("body");
		a.mPoster = data.getString("poster");
		a.mTime = data.getLong("mtime", 0);
		
		String text = ParseUtils.extractContentFromHtml(a.mBody);
		a.mBodyText = text;
		
		return a;
	}
	
}
