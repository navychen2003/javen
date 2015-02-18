package org.javenstudio.provider.app.picasa;

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
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.worker.job.Job;

public abstract class PicasaPhotoBase extends PicasaMediaItem {

	private final PicasaSource mSource;
	private final HttpImage mImage;
	private final HttpImage mAvatar;
	
	private GPhotoInfoEntry mInfoEntry = null;
	private GPhotoCommentEntry mCommentEntry = null;
	
	private final IMediaComments mComments;
	private final String mPhotoLocation;
	
	public PicasaPhotoBase(PicasaSource source, DataPath path, 
			String photoURL, String imageURL, String avatarURL) { 
		super(path, nextVersionNumber());
		mImage = HttpResource.getInstance().getImage(GPhotoHelper.normalizePhotoLocation(imageURL));
		mAvatar = HttpResource.getInstance().getImage(avatarURL);
		mPhotoLocation = photoURL;
		mSource = source;
		
		mComments = new MediaComments() { 
				@Override
				public int getCommentCount() {
					GPhotoCommentEntry entry = mCommentEntry;
					return entry != null && entry.comments != null 
							? entry.comments.length : 0;
				}
	
				@Override
				public IMediaComment getCommentAt(int index) {
					GPhotoCommentEntry entry = mCommentEntry;
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

	public final PicasaSource getSource() { return mSource; }
	public final HttpImage getPhotoImage() { return mImage; }
	
	public abstract PicasaMediaSet getPhotoSet();
	
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
	
	public synchronized GPhotoInfoEntry getPhotoInfoEntry() { 
		if (mInfoEntry == null) { 
			GPhotoHelper.fetchPhotoInfo(mPhotoLocation, 
				new GPhotoInfoEntry.FetchListener() {
					public void onPhotoInfoFetching(String source) {}
					@Override
					public void onPhotoInfoFetched(GPhotoInfoEntry entry) {
						if (entry != null && entry.photoId != null && 
							entry.photoId.length() > 0) {
							setPhotoInfoEntry(entry);
						}
					}
				}, false);
		}
		return mInfoEntry; 
	}
	
	synchronized void setPhotoInfoEntry(GPhotoInfoEntry entry) { 
		mInfoEntry = entry;
	}
	
	synchronized void loadPhotoCommentEntry(LoadCallback callback, 
			boolean schedule, boolean refetch) { 
		if (mCommentEntry == null || refetch) 
			fetchPhotoCommentEntry(callback, schedule, refetch);
	}
	
	synchronized void fetchPhotoCommentEntry(final LoadCallback callback, 
			boolean schedule, boolean refetch) { 
		GPhotoHelper.fetchPhotoComment(GPhotoHelper.getPhotoCommentLocation(mPhotoLocation), 
			new GPhotoCommentEntry.FetchListener() {
				public void onPhotoCommentFetching(String source) { 
					if (callback != null) callback.onLoading();
				}
				@Override
				public void onPhotoCommentFetched(GPhotoCommentEntry entry) {
					if (entry != null && entry.photoId != null && 
						entry.photoId.length() > 0) {
						setPhotoCommentEntry(entry);
					}
					if (callback != null) callback.onLoaded();
				}
				@Override
				public GCommentItem createCommentItem() { 
					return new GCommentItem(getPhotoSet().getUserClickListener(), 
							mSource.getSourceIconRes());
				}
			}, schedule, refetch);
	}
	
	synchronized void setPhotoCommentEntry(GPhotoCommentEntry entry) { 
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
    public String getAvatarLocation() { 
    	return mAvatar != null ? mAvatar.getLocation() : null; 
    }
    
    @Override
	public Image getBitmapImage() { return mImage; }
	public Image getAvatarImage() { return mAvatar; }
	
	@Override
	public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
		return mImage != null ? mImage.requestImage(holder, type) : null;
	}

	@Override
	public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
		return mImage != null ? mImage.requestLargeImage(holder) : null;
	}
	
	@Override
	public void getDetails(IMediaDetails details) { 
		super.getDetails(details);
		
	}
	
	@Override
	public void getExifs(final IMediaDetails details) { 
		super.getExifs(details);
		
		GPhotoInfoEntry info = getPhotoInfoEntry();
		if (info == null) { 
			GPhotoHelper.fetchPhotoInfo(mPhotoLocation, 
				new GPhotoInfoEntry.FetchListener() {
					@Override
					public void onPhotoInfoFetching(String source) { 
						details.onLoading();
					}
					@Override
					public void onPhotoInfoFetched(GPhotoInfoEntry entry) {
						if (entry != null && entry.photoId != null && 
							entry.photoId.length() > 0) {
							setPhotoInfoEntry(entry);
							getExifs(details, entry);
						}
						details.onLoaded();
					}
				}, true);
		} else {
			getExifs(details, info);
		}
	}
	
	private void getExifs(IMediaDetails details, GPhotoInfoEntry info) { 
		if (details == null || info == null) return;
		
		details.clear();
		details.add(R.string.exifs_datetime, Utilities.formatDate(info.exifTime));
		details.add(R.string.exifs_imagewidth, info.widthStr);
		details.add(R.string.exifs_imagelength, info.heightStr);
		details.add(R.string.exifs_make, info.exifMake);
		details.add(R.string.exifs_model, info.exifModel);
		details.add(R.string.exifs_aperture, info.exifFstop);
		details.add(R.string.exifs_iso, info.exifIso);
		details.add(R.string.exifs_exposuretime, info.exifExposure);
		details.add(R.string.exifs_focallength, info.exifFocallength);
		details.add(R.string.exifs_flash, info.exifFlash);
	}
	
	@Override
	public IMediaComments getComments(LoadCallback callback) { 
		loadPhotoCommentEntry(null, false, false);
		return mComments; 
	}
	
}
