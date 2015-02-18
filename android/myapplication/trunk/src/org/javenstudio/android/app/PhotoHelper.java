package org.javenstudio.android.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.data.AbstractMediaItem;
import org.javenstudio.cocoka.data.AbstractMediaSet;
import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.data.IMediaSet;
import org.javenstudio.cocoka.graphics.DelegatedDrawable;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.view.PhotoActionDetails;
import org.javenstudio.cocoka.view.PhotoActionExifs;
import org.javenstudio.cocoka.view.PhotoPage;
import org.javenstudio.cocoka.view.SlideshowPage;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.provider.OnClickListener;
import org.javenstudio.provider.media.IMediaPhoto;
import org.javenstudio.provider.publish.comment.PhotoActionComments;

public class PhotoHelper {
	//private static final Logger LOG = Logger.getLogger(PhotoHelper.class);

	// must public
	public static class PhotoPageImpl extends PhotoPage { 
		
		@Override
	    protected IMediaObject getMediaObject() { 
			Object photoObj = ((PhotoActivity)mActivity).getPhotoObject();
			if (photoObj == null) return null;
			
			if (photoObj instanceof IMediaSet) {
				IMediaSet mediaSet = (IMediaSet)photoObj;
				return mediaSet.getItemCount() == 1 ? mediaSet.findItem(0) : mediaSet;
			}
			
			if (photoObj instanceof IMediaItem) { 
				IMediaItem mediaItem = (IMediaItem)photoObj;
				return mediaItem;
			}
			
			return null;
	    }
		
		@Override
		protected void launchSlideshow() { 
			Bundle data = new Bundle();
			mActivity.getGLStateManager().startState(SlideshowPageImpl.class, data);
		}
	}
	
    // must public
    public static class SlideshowPageImpl extends SlideshowPage { 
    	
		@Override
	    protected IMediaSet getMediaObject() { 
			Object photoObj = ((PhotoActivity)mActivity).getPhotoObject();
			if (photoObj == null) return null;
			
			if (photoObj instanceof IMediaSet) {
				IMediaSet mediaSet = (IMediaSet)photoObj;
				return mediaSet;
			}
			
			if (photoObj instanceof IMediaItem) { 
				IMediaItem mediaItem = (IMediaItem)photoObj;
				return newPhotoList(new IMediaItem[]{ mediaItem }, 0);
			}
			
			return null;
	    }
	}
	
    public static IMediaSet newPhotoList(final IMediaItem[] items, final int indexHint) {
		if (items == null || items.length == 0) 
			return null;
		
		return new AbstractMediaSet() {
				private final long mVersion = nextVersionNumber();
				private final int mIndexHint = (indexHint >= 0 && indexHint < items.length) 
						? indexHint : 0;
				
				@Override
				public List<IMediaItem> getItemList(int start, int count) { 
					List<IMediaItem> list = new ArrayList<IMediaItem>();
					for (int i=start; i < start+count && items != null && i < items.length; i++) { 
						IMediaItem item = items[i];
						if (item != null) 
							list.add(item);
					}
					return list;
				}
	
				@Override
				public int getItemCount() {
					return items != null ? items.length : 0;
				}
				
				@Override
				public int getIndexHint() { 
					return mIndexHint;
				}
				
				@Override
				public long reloadData() { 
					return mVersion; 
				}
			};
	}
    
    public static IMediaSet newPhotoList(final Image[] images) { 
    	return newPhotoList(images, null);
    }
    
	public static IMediaSet newPhotoList(final Image[] images, final Image imageHint) { 
		final List<IMediaItem> items = new ArrayList<IMediaItem>();
		int indexHint = 0;
		
		if (imageHint != null && !imageHint.existBitmap()) 
			return null;
		
		for (int i=0; images != null && i < images.length; i++) { 
			final Image image = images[i];
			if (image == null || !image.existBitmap()) 
				continue;
			
			if (image == imageHint) indexHint = items.size();
			
			items.add(new AbstractMediaItem() {
					private final long mVersion = nextVersionNumber();
					private PhotoActionDetails mMediaDetails = null;
					private PhotoActionExifs mMediaExifs = null;
				
					@Override
					public Job<BitmapRef> requestThumbnail(BitmapHolder holder, 
							IMediaItem.RequestListener listener) {
						return image.requestThumbnail(holder);
					}
					@Override
					public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder, 
							IMediaItem.RequestListener listener) {
						return image.requestLargeImage(holder);
					}
					@Override
					public Drawable getThumbnailDrawable(int width, int height) { 
						return image.getThumbnailDrawable(width, height);
					}
					@Override
					public int getSupportedOperations() {
						final String mimeType = image.getContentType();
						int operation = SUPPORT_SHARE;
								//SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_CROP | 
								//SUPPORT_SETAS | SUPPORT_EDIT | SUPPORT_INFO;
				        
				        if (MediaUtils.isSupportedByRegionDecoder(mimeType)) 
				            operation |= SUPPORT_FULL_IMAGE;

				        if (MediaUtils.isRotationSupported(mimeType)) 
				            operation |= SUPPORT_ROTATE;

				        return operation;
					}
					@Override
					public long getSize() { 
						return image.getContentLength();
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
					public Uri getContentUri(int op) { 
						return image.getContentUri();
					}
					//@Override
					//public String getShareText() { 
					//	return image.getShareText();
					//}
					@Override
					public int getMediaType() {
						return MEDIA_TYPE_IMAGE;
					}
					@Override
					public String getName() {
						return image.getFileName();
					}
					@Override
					public String getTitle() { 
						return getName();
					}
					@Override
					public String getSubTitle() { 
						long size = image.getContentLength();
						if (size > 0) 
							return Utilities.formatSize(size);
						return null;
					}
					@Override
					public synchronized ActionItem[] getActionItems(Activity activity) { 
						if (mMediaDetails == null) {
							mMediaDetails = new PhotoActionDetails(activity, 
									activity.getString(R.string.label_photo_details));
							mMediaDetails.setDefault(true);
							image.getDetails(mMediaDetails);
						}
						if (mMediaExifs == null) {
							mMediaExifs = new PhotoActionExifs(activity, 
									activity.getString(R.string.label_photo_exifs));
							image.getExifs(mMediaExifs);
						}
						return new ActionItem[] { mMediaDetails, mMediaExifs };
					}
					@Override
					public Drawable getProviderIcon() { 
						return image.getProviderIcon();
					}
				});
		}
		
		return newPhotoList(items.toArray(new IMediaItem[items.size()]), indexHint);
	}
	
	public static IMediaSet newPhotoList(final Photo[] photos) { 
    	return newPhotoList(photos, null);
    }
    
	public static IMediaSet newPhotoList(final Photo[] photos, final Photo photoHint) { 
		final List<IMediaItem> items = new ArrayList<IMediaItem>();
		int indexHint = 0;
		
		if (photoHint != null && !photoHint.getBitmapImage().existBitmap()) 
			return null;
		
		for (int i=0; photos != null && i < photos.length; i++) { 
			final Photo photo = photos[i];
			if (photo == null || !photo.getBitmapImage().existBitmap()) 
				continue;
			
			if (photo == photoHint) indexHint = items.size();
			
			items.add(new AbstractMediaItem() {
					private final long mVersion = nextVersionNumber();
					private PhotoActionDetails mMediaDetails = null;
					private PhotoActionExifs mMediaExifs = null;
				
					@Override
					public Job<BitmapRef> requestThumbnail(BitmapHolder holder, 
							IMediaItem.RequestListener listener) {
						return photo.getBitmapImage().requestThumbnail(holder);
					}
					@Override
					public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder, 
							IMediaItem.RequestListener listener) {
						return photo.getBitmapImage().requestLargeImage(holder);
					}
					@Override
					public Drawable getThumbnailDrawable(int width, int height) { 
						return photo.getBitmapImage().getThumbnailDrawable(width, height);
					}
					@Override
					public int getSupportedOperations() {
						final String mimeType = photo.getBitmapImage().getContentType();
						int operation = photo.getSupportedOperations();
								//SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_CROP | 
								//SUPPORT_SETAS | SUPPORT_EDIT | SUPPORT_INFO;
				        
				        if (MediaUtils.isSupportedByRegionDecoder(mimeType)) 
				            operation |= SUPPORT_FULL_IMAGE;

				        if (MediaUtils.isRotationSupported(mimeType)) 
				            operation |= SUPPORT_ROTATE;

				        operation |= SUPPORT_SHARE;
				        
				        return operation;
					}
					@Override
					public boolean delete() throws IOException { 
						try { 
							boolean result = photo.delete();
							if (result) photo.notifyDirty();
							return result;
						} catch (Throwable e) { 
							throw new IOException(e);
						}
					}
					@Override
					public long getSize() { 
						return photo.getBitmapImage().getContentLength();
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
					public Uri getContentUri(int op) { 
						return photo.getContentUri();
					}
					//@Override
					//public String getShareText() { 
					//	return photo.getMediaInfo().getShareText();
					//}
					//@Override
					//public String getShareType() { 
					//	return photo.getMediaInfo().getShareType();
					//}
					@Override
					public int getMediaType() {
						return photo.getMediaInfo().getMediaType();
					}
					@Override
					public String getName() {
						return photo.getBitmapImage().getFileName();
					}
					@Override
					public String getTitle() { 
						return getName();
					}
					@Override
					public String getSubTitle() { 
						long size = photo.getBitmapImage().getContentLength();
						if (size > 0) 
							return Utilities.formatSize(size);
						return null;
					}
					@Override
					public synchronized ActionItem[] getActionItems(Activity activity) { 
						if (mMediaDetails == null) {
							mMediaDetails = new PhotoActionDetails(activity, 
									activity.getString(R.string.label_photo_details));
							mMediaDetails.setDefault(true);
							photo.getMediaInfo().getDetails(mMediaDetails);
						}
						if (mMediaExifs == null) {
							mMediaExifs = new PhotoActionExifs(activity, 
									activity.getString(R.string.label_photo_exifs));
							photo.getMediaInfo().getExifs(mMediaExifs);
						}
						return new ActionItem[] { mMediaDetails, mMediaExifs };
					}
					@Override
					public Drawable getProviderIcon() { 
						return photo.getMediaInfo().getProviderIcon();
					}
				});
		}
		
		return newPhotoList(items.toArray(new IMediaItem[items.size()]), indexHint);
	}
	
	public static IMediaSet newPhotoList(final IMediaPhoto photo) { 
		return newPhotoList(new IMediaPhoto[]{ photo }, null);
	}
	
	public static IMediaSet newPhotoList(final IMediaPhoto[] photos) { 
		return newPhotoList(photos, null);
	}
	
	public static IMediaSet newPhotoList(final IMediaPhoto[] photos, final IMediaPhoto photoHint) { 
		final List<IMediaItem> items = new ArrayList<IMediaItem>();
		int indexHint = 0;
		
		if (photoHint != null && !photoHint.getBitmapImage().existBitmap())
			return null;
		
		for (int i=0; photos != null && i < photos.length; i++) { 
			final IMediaPhoto photo = photos[i];
			if (photo == null || !photo.getBitmapImage().existBitmap()) 
				continue;
			
			if (photo == photoHint) indexHint = items.size();
			
			items.add(new AbstractMediaItem() {
					private final long mVersion = nextVersionNumber();
					private PhotoActionDetails mMediaDetails = null;
					private PhotoActionExifs mMediaExifs = null;
					private PhotoActionComments mMediaComments = null;
				
					@Override
					public Job<BitmapRef> requestThumbnail(BitmapHolder holder, 
							IMediaItem.RequestListener listener) {
						return photo.getBitmapImage().requestThumbnail(holder);
					}
					@Override
					public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder, 
							IMediaItem.RequestListener listener) {
						return photo.getBitmapImage().requestLargeImage(holder);
					}
					@Override
					public Drawable getThumbnailDrawable(int width, int height) { 
						return photo.getBitmapImage().getThumbnailDrawable(width, height);
					}
					@Override
					public int getSupportedOperations() {
						final String mimeType = photo.getBitmapImage().getContentType();
						int operation = photo.getPhotoData().getSupportedOperations();
								//SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_CROP | 
								//SUPPORT_SETAS | SUPPORT_EDIT | SUPPORT_INFO;
				        
				        if (MediaUtils.isSupportedByRegionDecoder(mimeType)) 
				            operation |= SUPPORT_FULL_IMAGE;

				        if (MediaUtils.isRotationSupported(mimeType)) 
				            operation |= SUPPORT_ROTATE;

				        operation |= SUPPORT_SHARE;
				        
				        return operation;
					}
					@Override
					public boolean delete() throws IOException { 
						try { 
							boolean result = photo.getPhotoData().delete();
							if (result) photo.getPhotoData().notifyDirty();
							return result;
						} catch (Throwable e) { 
							throw new IOException(e);
						}
					}
					@Override
					public long getSize() { 
						return photo.getBitmapImage().getContentLength();
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
						return photo.getPhotoInfo().getStatisticCount(type);
					}
					@Override
					public synchronized ActionItem[] getActionItems(Activity activity) { 
						if (mMediaDetails == null) {
							mMediaDetails = new PhotoActionDetails(activity, 
									activity.getString(R.string.label_photo_details));
							mMediaDetails.setDefault(true);
							photo.getPhotoInfo().getDetails(mMediaDetails);
						}
						if (mMediaExifs == null) {
							mMediaExifs = new PhotoActionExifs(activity, 
									activity.getString(R.string.label_photo_exifs));
							photo.getPhotoInfo().getExifs(mMediaExifs);
						}
						if (mMediaComments == null) { 
							IMediaComments comments = photo.getPhotoInfo().getComments((PhotoActivity)activity);
							if (comments != null) {
								mMediaComments = new PhotoActionComments(activity, comments, 
										activity.getString(R.string.label_photo_comments));
							}
						}
						return new ActionItem[] { mMediaDetails, mMediaExifs, mMediaComments };
					}
					@Override
					public Uri getContentUri(int op) { 
						return photo.getContentUri();
					}
					//@Override
					//public String getShareText() { 
					//	return photo.getPhotoInfo().getShareText();
					//}
					//@Override
					//public String getShareType() { 
					//	return photo.getPhotoInfo().getShareType();
					//}
					@Override
					public int getMediaType() {
						return photo.getPhotoInfo().getMediaType();
					}
					@Override
					public String getName() {
						return getTitle();
					}
					@Override
					public String getTitle() { 
						return photo.getPhotoInfo().getTitle();
					}
					@Override
					public String getSubTitle() { 
						return photo.getPhotoInfo().getSubTitle();
					}
					@Override
					public String getActionTitle() { 
						return photo.getPhotoInfo().getAuthor();
					}
					@Override
					public View getActionCustomView(final Activity activity) { 
						String title = photo.getPhotoInfo().getAuthor();
						if (title == null || title.length() == 0) 
							return null;
						
						LayoutInflater inflater = LayoutInflater.from(activity);
						View view = inflater.inflate(org.javenstudio.cocoka.app.R.layout.actionbar_custom_item, null); 
						TextView titleView = (TextView)view.findViewById(org.javenstudio.cocoka.app.R.id.actionbar_custom_item_title);
						
						titleView.setText(title);
						titleView.setCompoundDrawablesWithIntrinsicBounds(
								getAvatarDrawable(activity), null, null, null);
						titleView.setTextColor(Color.WHITE);
						
						view.setBackgroundResource(org.javenstudio.cocoka.app.R.drawable.photo_action_background);
						view.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									final OnClickListener listener = photo.getTitleClickListener();
									if (listener != null) 
										listener.onClick(activity);
								}
							});
						
						return view;
					}
					private Drawable getAvatarDrawable(Activity activity) { 
						Drawable icon = null;
						Image image = photo.getAvatarImage();
						if (image != null) icon = image.getThumbnailDrawable(48, 48);
						
						DelegatedDrawable d = new DelegatedDrawable(icon, 48, 48);
						d.setBackground(activity.getResources().getDrawable(
								AppResources.getInstance().getDrawableRes(AppResources.drawable.card_avatar_selector)));
						
						return d;
					}
					@Override
					public Drawable getProviderIcon() { 
						return photo.getPhotoInfo().getProviderIcon();
					}
				});
		}
		
		return newPhotoList(items.toArray(new IMediaItem[items.size()]), indexHint);
	}
	
}
