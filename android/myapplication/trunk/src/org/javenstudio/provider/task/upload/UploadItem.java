package org.javenstudio.provider.task.upload;

import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.DataBinderListener;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class UploadItem extends DataBinderItem 
		implements ImageListener {
	private static final Logger LOG = Logger.getLogger(UploadItem.class);
	
	private final UploadProvider mProvider;
	
	private int mFetchRequest = 0;
	
	public UploadItem(UploadProvider p) { 
		if (p == null) throw new NullPointerException();
		mProvider = p;
	}
	
	public UploadProvider getProvider() { return mProvider; }
	
	public abstract int getViewRes();
	public abstract void bindView(IActivity activity, UploadBinder binder, View view);
	
	public void updateView(View view, boolean restartSlide) {}
	public void onViewBinded(IActivity activity) {}
	
	public String getImageLocation() { return null; }
	public boolean isFetching() { return mFetchRequest > 0; }
	
	@Override
	public void onImageEvent(Image image, ImageEvent event) { 
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (!location.equals(getImageLocation())) 
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
		postUpdateView();
	}
	
	public void postUpdateView() {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					setCachedImageDrawable(null);
					onUpdateViewOnVisible(false);
				}
			});
	}
	
	protected void onUpdateViewOnVisible(boolean restartSlide) { 
		updateView(getBindView(), restartSlide);
	}
	
	@Override
	public void bindHeaderView(View view) { 
		if (view == null) return;
		
		DataBinderListener listener = getProvider().getBindListener();
		if (listener != null && listener.onBindHeaderView(this, view))
			return;
	}
	
}
