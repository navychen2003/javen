package org.javenstudio.provider.library;

import java.io.IOException;

import android.app.Activity;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.app.BaseResources;
import org.javenstudio.cocoka.data.AbstractMediaItem;
import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.graphics.DelegatedDrawable;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.view.PhotoActionDetails;
import org.javenstudio.cocoka.view.PhotoActionExifs;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.common.util.Logger;

public class SectionPhotoItem extends AbstractMediaItem 
		implements ImageListener {
	private static final Logger LOG = Logger.getLogger(SectionPhotoItem.class);
	
	private long mVersion = nextVersionNumber();
	
	private final SectionPhotoSet mPhotoSet;
	private final IPhotoData mData;
	private final String mImageURL;
	private final String mThumbnailURL;
	private final HttpImage mImage;
	private final HttpImage mThumbnail;
	private int mFetchRequest = 0;
	
	private PhotoActionDetails mMediaDetails = null;
	private PhotoActionExifs mMediaExifs = null;
	
	private IMediaItem.RequestListener mImageListener = null;
	private IMediaItem.RequestListener mThumbnailListener = null;

	public SectionPhotoItem(SectionPhotoSet photoSet, IPhotoData data) {
		if (photoSet == null || data == null) throw new NullPointerException();
		mPhotoSet = photoSet;
		mData = data;
		
		String thumbnailURL = data.getPosterThumbnailURL();
		if (thumbnailURL != null && thumbnailURL.length() > 0) {
			mThumbnailURL = thumbnailURL;
			mThumbnail = HttpResource.getInstance().getImage(thumbnailURL);
			mThumbnail.addListener(this);
		} else {
			mThumbnailURL = null;
			mThumbnail = null;
		}
		
		String imageURL = data.getPhotoURL();
		if (imageURL != null && imageURL.length() > 0) { 
			mImageURL = imageURL;
			mImage = HttpResource.getInstance().getImage(imageURL);
			mImage.addListener(this);
		} else {
			mImageURL = null;
			mImage = null;
		}
	}
	
	public SectionPhotoSet getPhotoSet() { return mPhotoSet; }
	public IPhotoData getPhotoData() { return mData; }
	public HttpImage getImage() { return mImage; }
	public HttpImage getThumbnail() { return mThumbnail; }
	
	public String getOwnerAvatarLocation() { return null; }
	public HttpImage getOwnerAvatarImage() { return null; }
	
	@Override
	public void onImageEvent(Image image, ImageEvent event) {
		if (image == null || event == null || !(image instanceof HttpImage)) 
			return;
		
		final String location = image.getLocation();
		HttpImage httpImage = null;
		
		synchronized (this) { 
			if (location != null && location.length() > 0) {
				String imageURL = mImageURL;
				if (imageURL != null && imageURL.equals(location)) {
					httpImage = mImage;
				}
				String thumbnailURL = mThumbnailURL;
				if (thumbnailURL != null && thumbnailURL.equals(location)) {
					httpImage = mThumbnail;
				}
				String avatarURL = getOwnerAvatarLocation();
				if (avatarURL != null && avatarURL.equals(location)) {
					httpImage = (HttpImage)image;
				}
			}
			
			if (httpImage == null) return;
			if (LOG.isDebugEnabled())
				LOG.debug("onImageEvent: location=" + location + " event=" + event);
		}
		
		if (event instanceof HttpEvent) { 
			HttpEvent e = (HttpEvent)event;
			switch (e.getEventType()) { 
			case FETCH_START: 
				if (mFetchRequest < 0) mFetchRequest = 0;
				mFetchRequest ++;
				break;
			default: 
				mFetchRequest --;
				if (mFetchRequest < 0) mFetchRequest = 0;
				break;
			}
			
			if (httpImage == mImage) {
				mVersion ++;
				IMediaItem.RequestListener listener = mImageListener;
				if (listener != null) { 
					switch (e.getEventType()) { 
					case FETCH_START: 
						listener.onMediaItemPrepare(this);
						break;
					case FETCH_FINISH:
						listener.onMediaItemDone(this, true);
						break;
					default:
						listener.onMediaItemDone(this, false);
						break;
					}
				}
				
			} else if (httpImage == mThumbnail) {
				mVersion ++;
				IMediaItem.RequestListener listener = mThumbnailListener;
				if (listener != null) { 
					switch (e.getEventType()) { 
					case FETCH_START: 
						listener.onMediaItemPrepare(this);
						break;
					case FETCH_FINISH:
						listener.onMediaItemDone(this, true);
						break;
					default:
						listener.onMediaItemDone(this, false);
						break;
					}
				}
			}
			
			onHttpImageEvent(image, e);
		}
	}
	
	protected void onHttpImageEvent(final Image image, HttpEvent event) { 
		if (image == null) return;
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					image.invalidateDrawables();
				}
			});
	}
	
	@Override
	public Job<BitmapRef> requestThumbnail(BitmapHolder holder, 
			IMediaItem.RequestListener listener) {
		HttpImage image = getThumbnail();
		if (image != null) {
			mThumbnailListener = listener;
			image.checkDownload(HttpImage.RequestType.DOWNLOAD, true);
			return image.requestThumbnail(holder);
		}
		return null;
	}
	
	@Override
	public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder, 
			IMediaItem.RequestListener listener) {
		HttpImage image = getImage();
		if (image != null) {
			mImageListener = listener;
			image.checkDownload(HttpImage.RequestType.DOWNLOAD, true);
			return image.requestLargeImage(holder);
		}
		return null;
	}
	
	@Override
	public Drawable getThumbnailDrawable(int width, int height) { 
		HttpImage image = getThumbnail();
		if (image == null || !image.existBitmap()) {
			image = getImage();
			if (image != null) image.existBitmap();
		}
 		if (image != null) return image.getThumbnailDrawable(width, height);
		return null;
	}
	
	@Override
	public int getSupportedOperations() {
		final String mimeType = getPhotoData().getType();
		int operation = SUPPORT_SETAS | SUPPORT_INFO;
        
        if (MediaUtils.isSupportedByRegionDecoder(mimeType)) 
            operation |= SUPPORT_FULL_IMAGE;

        if (MediaUtils.isRotationSupported(mimeType)) 
            operation |= SUPPORT_ROTATE;

        if (getPhotoData().supportOperation(FileOperation.Operation.SHARE))
        	operation |= SUPPORT_SHARE;
        
        if (getPhotoData().supportOperation(FileOperation.Operation.DOWNLOAD))
        	operation |= SUPPORT_DOWNLOAD;
        
        return operation;
	}
	
	@Override
	public boolean handleOperation(Activity activity, int op) {
		if (op == SUPPORT_INFO) 
			return getPhotoSet().onActionDetails(activity, getPhotoData());
		else if (op == SUPPORT_DOWNLOAD)
			return getPhotoSet().onActionDownload(activity, getPhotoData());
		return false;
	}
	
	@Override
	public boolean onOperationError(Activity activity, int op, 
			int error, String action) {
		if (LOG.isDebugEnabled()) { 
			LOG.debug("onOperationError: op=" + op 
					+ " error=" + error + " action=" + action);
		}
		return false;
	}
	
	@Override
	public boolean delete() throws IOException { 
		return false;
	}
	
	@Override
	public long getSize() { 
		return getPhotoData().getLength();
	}
	
	@Override
	public long getVersion() { 
		return mVersion;
	}
	
	@Override
	public boolean showControls() { 
		return true;
	}
	
	@Override
	public int getStatisticCount(int type) { 
		return 0; //photo.getPhotoInfo().getStatisticCount(type);
	}
	
	@Override
	public synchronized ActionItem[] getActionItems(Activity activity) { 
		HttpImage image = getImage();
		if (image == null || !image.existBitmap()) {
			//image = getThumbnail();
			//if (image == null || !image.existBitmap())
				image = null;
		}
		
		PhotoActionDetails details = mMediaDetails;
		if (details == null) {
			details = new PhotoActionDetails(activity, 
					activity.getString(R.string.label_photo_details));
			details.setDefault(true);
			
			getPhotoData().getDetails(details);
			
			if (details.getCount() > 0)
				mMediaDetails = details;
			else if (image != null) 
				image.getDetails(details);
		}
		
		PhotoActionExifs exifs = mMediaExifs;
		if (exifs == null) {
			exifs = new PhotoActionExifs(activity, 
					activity.getString(R.string.label_photo_exifs));
			
			getPhotoData().getExifs(exifs);
			
			if (exifs.getCount() > 0) 
				mMediaExifs = exifs;
			else if (image != null) 
				image.getExifs(exifs);
		}
		
		return new ActionItem[] { details, exifs };
	}
	
	@Override
	public Uri getContentUri(int op) { 
		HttpImage image = getImage();
		if (image != null && image.existBitmap()) 
			return image.getContentUri();
		return getPhotoData().getContentUri();
	}
	
	@Override
	public int getMediaType() {
		String type = getPhotoData().getType();
		if (type != null) {
			if (type.startsWith("image/")) return MEDIA_TYPE_IMAGE;
			else if (type.startsWith("audio/")) return MEDIA_TYPE_AUDIO;
			else if (type.startsWith("video/")) return MEDIA_TYPE_VIDEO;
		}
		return MEDIA_TYPE_UNKNOWN;
	}
	
	@Override
	public String getName() {
		return getPhotoData().getName();
	}
	
	@Override
	public String getTitle() { 
		return getName();
	}
	
	@Override
	public String getSubTitle() { 
		return getPhotoData().getSizeInfo();
	}
	
	@Override
	public String getActionTitle() { 
		return getOwnerTitle();
	}
	
	@Override
	public View getActionCustomView(final Activity activity) { 
		String title = getOwnerTitle();
		if (title == null || title.length() == 0) 
			return null;
		
		LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(org.javenstudio.cocoka.app.R.layout.actionbar_custom_item, null); 
		TextView titleView = (TextView)view.findViewById(org.javenstudio.cocoka.app.R.id.actionbar_custom_item_title);
		
		titleView.setText(title);
		titleView.setCompoundDrawablesWithIntrinsicBounds(
				getOwnerAvatarDrawable(activity), null, null, null);
		titleView.setTextColor(getOwnerTitleColor());
		
		view.setBackgroundResource(getOwnerTitleBackgroundRes());
		view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onOwnerTitleClick(activity);
				}
			});
		
		return view;
	}
	
	protected Drawable getOwnerAvatarDrawable(Activity activity) { 
		Drawable icon = null;
		Image image = getOwnerAvatarImage();
		if (image != null) icon = image.getRoundThumbnailDrawable(48, 48);
		
		DelegatedDrawable d = new DelegatedDrawable(icon, 48, 48, 2);
		d.setBackground(AppResources.getInstance().getResources().getDrawable(
				AppResources.getInstance().getDrawableRes(AppResources.drawable.card_avatar_round_selector)));
		
		return d;
	}
	
	@Override
	public Drawable getProviderIcon() { 
		return getPhotoSet().getProviderIcon();
	}
	
	public String getOwnerTitle() { 
		return getPhotoData().getOwner(); 
	}
	
	protected void onOwnerTitleClick(Activity activity) {}
	
	protected int getOwnerTitleColor() { return Color.WHITE; }
	
	protected int getOwnerTitleBackgroundRes() {
		int backgroundRes = AppResources.getInstance().getDrawableRes(BaseResources.drawable.photo_action_background);
		if (backgroundRes == 0) backgroundRes = org.javenstudio.cocoka.app.R.drawable.photo_action_background;
		return backgroundRes;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "-" + getIdentity() 
				+ "{data=" + mData + ",version=" + mVersion + "}";
	}
	
}
