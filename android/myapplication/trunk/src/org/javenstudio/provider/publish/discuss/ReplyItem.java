package org.javenstudio.provider.publish.discuss;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Logger;

public class ReplyItem extends DataBinderItem implements ImageListener {
	private static final Logger LOG = Logger.getLogger(ReplyItem.class);
	
	private final ReplyProvider mProvider;
	private final IReplyData mData;
	
	private HttpImage mAvatar = null;
	private int mFetchRequest = 0;
	
	public ReplyItem(ReplyProvider p, IReplyData data) { 
		mProvider = p;
		mData = data;
		
		if (data == null) throw new NullPointerException();
	}
	
	public ReplyProvider getProvider() { return mProvider; }
	public IReplyData getReplyData() { return mData; }
	
	public CharSequence getSummary() { 
		return InformationHelper.formatContentSpanned(getReplyData().getMessage()); 
	}
	
	public String getCreateDate() { 
		return Utilities.formatDate(getReplyData().getCreateDate()); 
	}
	
	public String getTitle() { 
		return getReplyData().getReplySubject(); 
	}
	
	public String getUserName() { 
		return getReplyData().getUserName(); 
	}
	
	public View.OnClickListener getUserClickListener() { 
		return getReplyData().getUserClickListener(); 
	}
	
	public Drawable getProviderIcon() { 
		return getReplyData().getProviderIcon(); 
	}
	
	public boolean isFetching() { return mFetchRequest > 0; }
	
	public synchronized Drawable getUserIcon(int width, int height) { 
		if (mAvatar == null) {
			mAvatar = HttpResource.getInstance().getImage(
					getReplyData().getAvatarLocation());
			
			if (mAvatar != null)
				mAvatar.addListener(this);
			
			HttpImageItem.requestDownload(mAvatar, false);
		}
		
		if (mAvatar != null) 
			return mAvatar.getThumbnailDrawable(width, height);
		
		return null;
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
				break;
			}
			
			onHttpImageEvent(image, e);
		}
	}
	
	protected void onHttpImageEvent(final Image image, HttpEvent event) { 
		postUpdateViews();
	}
	
	public void postUpdateViews() {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					setCachedImageDrawable(null);
					onUpdateViewsOnVisible(false);
				}
			});
	}
	
	protected void onUpdateViewsOnVisible(boolean restartSlide) {
		ReplyBinder binder = getReplyBinder();
		if (binder != null) binder.onUpdateViews(this);
	}
	
	private ReplyBinder getReplyBinder() { 
		final ReplyBinder binder = (ReplyBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return null;
		
		return binder;
	}
	
}
