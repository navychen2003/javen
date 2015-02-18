package org.javenstudio.provider.app.picasa;

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

final class GCommentItem implements IMediaComment, ImageListener {
	private static final Logger LOG = Logger.getLogger(GCommentItem.class);
	
	private final List<ChangeNotifier> mNotifiers = new ArrayList<ChangeNotifier>();
	private final long mIdentity = ResourceHelper.getIdentity();
	
	private final PicasaUserClickListener mUserClickListener;
	private final int mIconRes;
	
	private HttpImage mAvatar = null;
	private int mFetchRequest = 0;
	
	public String id;
	
	public String updatedStr;
	public long updated;
	
	public String title;
	public String content;
	
	public String authorName;
	public String authorNickName;
	public String authorThumbnail;
	public String authorUser;
	
	public String photoId;

	public GCommentItem(PicasaUserClickListener listener, int iconRes) { 
		mUserClickListener = listener;
		mIconRes = iconRes;
	}
	
	@Override
	public CharSequence getTitle() {
		return title; //InformationHelper.formatTitleSpanned(title);
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
		return updated;
	}
	
	@Override
	public Drawable getProviderIcon() { 
		if (mIconRes != 0) 
			return ResourceHelper.getResources().getDrawable(mIconRes);
		return null;
	}
	
	@Override
	public synchronized Drawable getAuthorDrawable(int width, int height) {
		if (mAvatar == null && authorThumbnail != null && authorThumbnail.length() > 0) {
			mAvatar = HttpResource.getInstance().getImage(authorThumbnail);
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
					mUserClickListener.onPicasaUserClick(authorUser, authorName, authorThumbnail);
				}
			};
	}
	
	@Override
	public void onImageEvent(Image image, ImageEvent event) { 
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (!location.equals(authorThumbnail)) 
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
		return "GCommentItem-" + mIdentity;
	}
	
}
