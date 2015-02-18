package org.javenstudio.provider.app.flickr;

import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.comment.IMediaComment;
import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.android.data.comment.MediaComments;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.worker.job.Job;

public abstract class FlickrPhotoBase extends FlickrMediaItem {

	private final FlickrSource mSource;
	private final HttpImage mImage;
	private final IMediaComments mComments;
	
	private YPhotoInfoEntry mInfoEntry = null;
	private YPhotoExifEntry mExifEntry = null;
	private YPhotoCommentEntry mCommentEntry = null;
	private HttpImage mAvatar = null;
	
	public FlickrPhotoBase(FlickrSource source, DataPath path, String photoURL) { 
		super(path, nextVersionNumber());
		mImage = HttpResource.getInstance().getImage(photoURL);
		mSource = source;
		
		mComments = new MediaComments() { 
				@Override
				public int getCommentCount() {
					YPhotoCommentEntry entry = mCommentEntry;
					return entry != null && entry.comments != null 
							? entry.comments.length : 0;
				}
	
				@Override
				public IMediaComment getCommentAt(int index) {
					YPhotoCommentEntry entry = mCommentEntry;
					if (entry != null && entry.comments != null && 
						index >= 0 && index < entry.comments.length) {
						return entry.comments[index];
					}
					return null;
				}
				@Override
				public void onViewBinded(LoadCallback callback, boolean reclick) { 
					loadPhotoCommentEntry(callback, true, reclick);
				}
			};
	}

	public final FlickrSource getSource() { return mSource; }
	public final HttpImage getPhotoImage() { return mImage; }
	
	public abstract FlickrMediaSet getPhotoSet();
	
    @Override
    public Uri getContentUri() { 
    	return getPhotoImage().getContentUri();
    }
	
	@Override
	public Drawable getProviderIcon() { 
		int iconRes = mSource.getSourceIconRes(); 
		if (iconRes != 0) 
			return ResourceHelper.getResources().getDrawable(iconRes);
		return null;
	}
	
	public synchronized YPhotoInfoEntry getPhotoInfoEntry() { 
		if (mInfoEntry == null) { 
			FlickrHelper.fetchPhotoInfo(getPhotoId(), 
				new YPhotoInfoEntry.FetchListener() {
					public void onPhotoInfoFetching(String source) {}
					@Override
					public void onPhotoInfoFetched(YPhotoInfoEntry entry) {
						if (entry != null && entry.photoId != null && 
							entry.photoId.length() > 0) {
							setPhotoInfoEntry(entry);
						}
					}
				}, false);
		}
		return mInfoEntry; 
	}
	
	synchronized void setPhotoInfoEntry(YPhotoInfoEntry entry) { 
		mInfoEntry = entry;
		
		if (entry != null && mAvatar == null) { 
			String iconURL = FlickrHelper.getIconURL(
					entry.ownerId, entry.iconfarm, entry.iconserver);
			
			if (iconURL != null)
				mAvatar = HttpResource.getInstance().getImage(iconURL);
		}
	}
	
	public synchronized YPhotoExifEntry getPhotoExifEntry() { 
		if (mExifEntry == null) { 
			FlickrHelper.fetchPhotoExif(getPhotoId(), 
				new YPhotoExifEntry.FetchListener() {
					public void onPhotoExifFetching(String source) {}
					@Override
					public void onPhotoExifFetched(YPhotoExifEntry entry) {
						if (entry != null && entry.photoId != null && 
							entry.photoId.length() > 0) {
							setPhotoExifEntry(entry);
						}
					}
				}, false);
		}
		return mExifEntry; 
	}
	
	synchronized void setPhotoExifEntry(YPhotoExifEntry entry) { 
		mExifEntry = entry;
	}
	
	synchronized void loadPhotoCommentEntry(final LoadCallback callback, 
			boolean schedule, boolean refetch) { 
		if (mCommentEntry == null || refetch) 
			fetchPhotoCommentEntry(callback, schedule, refetch);
	}
	
	synchronized void fetchPhotoCommentEntry(final LoadCallback callback, 
			boolean schedule, boolean refetch) { 
		FlickrHelper.fetchPhotoComment(getPhotoId(), 
			new YPhotoCommentEntry.FetchListener() {
				public void onPhotoCommentFetching(String source) { 
					if (callback != null) callback.onLoading();
				}
				@Override
				public void onPhotoCommentFetched(YPhotoCommentEntry entry) {
					if (entry != null && entry.photoId != null && 
						entry.photoId.length() > 0) {
						setPhotoCommentEntry(entry);
					}
					if (callback != null) callback.onLoaded();
				}
				@Override
				public YCommentItem createCommentItem() { 
					return new YCommentItem(getPhotoSet().getUserClickListener(), 
							mSource.getSourceIconRes());
				}
			}, schedule, refetch);
	}
	
	synchronized void setPhotoCommentEntry(YPhotoCommentEntry entry) { 
		mCommentEntry = entry;
	}
	
	@Override
	public DataApp getDataApp() { 
		return mSource.getDataApp();
	}
	
    @Override
    public int getMediaType() {
        return MEDIA_TYPE_IMAGE;
    }
	
    @Override
    public String getLocation() { 
    	return mImage != null ? mImage.getLocation() : null; 
    }
    
    @Override
    public Image getBitmapImage() { 
    	return mImage; 
    }
    
	@Override
	public String getAuthor() { 
		YPhotoInfoEntry entry = getPhotoInfoEntry();
		if (entry != null) 
			return entry.username;
		
		return null;
	}
	
	public String getPhotoToken() { 
		YPhotoInfoEntry entry = getPhotoInfoEntry();
		if (entry != null) 
			return entry.taken;
		
		return null;
	}
	
	public String getPhotoDate() { 
		return getPhotoToken();
	}
	
	@Override
	public int getStatisticCount(int type) { 
		if (type == MediaInfo.COUNT_COMMENT) { 
			YPhotoInfoEntry entry = getPhotoInfoEntry();
			if (entry != null && entry.countComment > 0) 
				return entry.countComment;
		}
		return super.getStatisticCount(type); 
	}
	
	@Override
	public String getSubTitle() {
		//YPhotoInfoEntry entry = getPhotoInfoEntry();
		//if (entry != null) 
		//	return entry.taken;
		
		long size = mImage != null ? mImage.getContentLength() : 0;
		if (size > 0) 
			return Utilities.formatSize(size);
		
		return null;
	}
	
    @Override
    public long getDateInMs() { 
    	YPhotoInfoEntry entry = getPhotoInfoEntry();
    	if (entry != null) 
    		return entry.lastupdate;
    	
    	return mImage.getDateInMs(); 
    }
    
    @Override
    public long getSize() { 
    	return mImage.getContentLength(); 
    }
	
	@Override
	public String getAvatarLocation() { 
		getPhotoInfoEntry();
		return mAvatar != null ? mAvatar.getLocation() : null; 
	}
	
	@Override
	public Image getAvatarImage() { 
		getPhotoInfoEntry();
		return mAvatar; 
	}
	
	@Override
	public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
		return mImage != null ? mImage.requestImage(holder, type) : null;
	}

	@Override
	public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
		return mImage != null ? mImage.requestLargeImage(holder) : null;
	}
	
	@Override
	public void getDetails(final IMediaDetails details) { 
		super.getDetails(details);
		details.add(R.string.details_title, InformationHelper.formatTitleSpanned(getTitle()));
		
		YPhotoInfoEntry entry = getPhotoInfoEntry();
		if (entry == null) {
			FlickrHelper.fetchPhotoInfo(getPhotoId(), 
				new YPhotoInfoEntry.FetchListener() {
					@Override
					public void onPhotoInfoFetching(String source) { 
						details.onLoading();
					}
					@Override
					public void onPhotoInfoFetched(YPhotoInfoEntry entry) {
						if (entry != null && entry.photoId != null && 
							entry.photoId.length() > 0) {
							setPhotoInfoEntry(entry);
							getDetails(details, entry);
						}
						details.onLoaded();
					}
				}, true);
		} else { 
			getDetails(details, entry);
		}
	}
	
	private void getDetails(IMediaDetails details, YPhotoInfoEntry entry) { 
		if (details == null || entry == null) 
			return;
		
		details.add(R.string.details_taken, entry.taken);
		details.add(R.string.details_author, entry.username);
		details.add(R.string.details_description, 
				InformationHelper.formatSummarySpanned(entry.description));
	}
	
	@Override
	public void getExifs(final IMediaDetails details) { 
		super.getExifs(details);
		
		YPhotoExifEntry entry = getPhotoExifEntry();
		if (entry == null) { 
			FlickrHelper.fetchPhotoExif(getPhotoId(), 
				new YPhotoExifEntry.FetchListener() {
					@Override
					public void onPhotoExifFetching(String source) { 
						details.onLoading();
					}
					@Override
					public void onPhotoExifFetched(YPhotoExifEntry entry) {
						if (entry != null && entry.photoId != null && 
							entry.photoId.length() > 0) {
							setPhotoExifEntry(entry);
							getExifs(details, entry);
						}
						details.onLoaded();
					}
				}, true);
		} else { 
			getExifs(details, entry);
		}
	}
	
	private void getExifs(IMediaDetails details, YPhotoExifEntry entry) { 
		if (details == null || entry == null) 
			return;
		
		details.clear();
		
		YPhotoExifEntry.ExifItem[] items = entry.exifs;
		for (int i=0; items != null && i < items.length; i++) { 
			YPhotoExifEntry.ExifItem item = items[i];
			if (item == null) continue;
			
			String value = item.clean;
			if (value == null || value.length() == 0) 
				value = item.raw;
			
			details.add(item.label, value);
		}
	}
	
	@Override
	public IMediaComments getComments(LoadCallback callback) { 
		loadPhotoCommentEntry(null, false, false);
		return mComments; 
	}
	
}
