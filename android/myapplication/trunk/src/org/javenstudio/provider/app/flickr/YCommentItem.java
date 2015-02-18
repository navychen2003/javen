package org.javenstudio.provider.app.flickr;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.data.comment.IMediaComment;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.ChangeNotifier;
import org.javenstudio.common.util.Logger;

final class YCommentItem implements IMediaComment, ImageListener {
	private static final Logger LOG = Logger.getLogger(YCommentItem.class);
	
	private final List<ChangeNotifier> mNotifiers = new ArrayList<ChangeNotifier>();
	private final long mIdentity = ResourceHelper.getIdentity();
	
	private final FlickrUserClickListener mUserClickListener;
	private final int mIconRes;
	
	private HttpImage mAvatar = null;
	private int mFetchRequest = 0;
	
	public String commentId;
	public String author;
	public String authorName;
	public String iconserver;
	public String iconfarm;
	public String datecreateStr;
	public long datecreate;
	public String content;
	
	public YCommentItem(FlickrUserClickListener listener, int iconRes) { 
		mUserClickListener = listener;
		mIconRes = iconRes;
	}

	@Override
	public CharSequence getTitle() {
		return authorName; //InformationHelper.formatTitleSpanned(authorName);
	}

	@Override
	public CharSequence getContent() {
		return InformationHelper.formatContentSpanned(content);
	}

	@Override
	public CharSequence getAuthor() {
		return authorName;
	}

	@Override
	public long getPostTime() {
		return datecreate;
	}
	
	@Override
	public Drawable getProviderIcon() { 
		if (mIconRes != 0) 
			return ResourceHelper.getResources().getDrawable(mIconRes);
		return null;
	}
	
	@Override
	public synchronized Drawable getAuthorDrawable(int width, int height) {
		if (mAvatar == null) {
			String iconurl = FlickrHelper.getIconURL(author, iconfarm, iconserver);
			mAvatar = HttpResource.getInstance().getImage(iconurl);
			mAvatar.addListener(this);
			
			HttpImageItem.requestDownload(mAvatar, false);
		}
		
		if (mAvatar != null) 
			return mAvatar.getThumbnailDrawable(width, height);
		
		return null;
	}
	
	@Override
	public View.OnClickListener getAuthorClickListener() { 
		if (mUserClickListener == null)
			return null;
		
		return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mUserClickListener.onFlickrUserClick(author, authorName);
				}
			};
	}
	
	@Override
	public void onImageEvent(Image image, ImageEvent event) { 
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (mAvatar == null || !location.equals(mAvatar.getLocation())) 
				return;
		}
		
		if (event instanceof HttpEvent) { 
			HttpEvent e = (HttpEvent)event;
			
			if (LOG.isDebugEnabled())
				LOG.debug("onImageEvent: entry=" + this + " event=" + e.getEventType());
			
			switch (e.getEventType()) { 
			case FETCH_START: 
				if (mFetchRequest < 0) mFetchRequest = 0;
				mFetchRequest ++;
				break;
				
			default: 
				mFetchRequest --;
				if (mFetchRequest < 0) mFetchRequest = 0;
				
				synchronized (mNotifiers) {
					for (ChangeNotifier notifier : mNotifiers) {
						if (notifier != null) 
							notifier.onChange(false);
					}
				}
				
				break;
			}
		}
	}
	
	public boolean isFetching() { return mFetchRequest > 0; }
	
	@Override
	public void addNotifier(ChangeNotifier notifier) {
		if (notifier == null) return;
		
		synchronized (mNotifiers) { 
			for (ChangeNotifier cn : mNotifiers) { 
				if (cn == notifier) return;
			}
			mNotifiers.add(notifier);
			
			//if (LOG.isDebugEnabled()) 
			//	LOG.debug("addNotifier: " + notifier);
		}
	}
	
	@Override
	public String toString() { 
		return "YCommentItem-" + mIdentity;
	}
	
}
