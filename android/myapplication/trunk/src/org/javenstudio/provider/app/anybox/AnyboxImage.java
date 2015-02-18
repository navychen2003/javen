package org.javenstudio.provider.app.anybox;

import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.common.util.Logger;

public class AnyboxImage implements ImageListener {
	private static final Logger LOG = Logger.getLogger(AnyboxImage.class);

	private int mFetchRequest = 0;
	
	@Override
	public void onImageEvent(Image image, ImageEvent event) {
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (!isImageLocation(location)) return;
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
			
			onHttpImageEvent(image, e);
		}
	}
	
	protected boolean isImageLocation(String location) {
		return false;
	}
	
	protected void onHttpImageEvent(final Image image, HttpEvent event) { 
		//postUpdateViews();
	}
	
}
