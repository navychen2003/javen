package org.javenstudio.cocoka.data;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.opengl.ScreenNail;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;

public abstract class AbstractMediaItem extends MediaHelper 
		implements IMediaItem, IMediaControls {

	private final long mIdentity = ResourceHelper.getIdentity();
	
	public final long getIdentity() {
		return mIdentity;
	}
	
	@Override
	public int getWidth() {
		return 0;
	}

	@Override
	public int getHeight() {
		return 0;
	}

	@Override
	public long getSize() { 
		return 0;
	}
	
	@Override
	public Job<BitmapRef> requestThumbnail(BitmapHolder holder, 
			RequestListener listener) {
		return null;
	}

	@Override
	public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder, 
			RequestListener listener) {
		return null;
	}

	@Override
	public Drawable getThumbnailDrawable(int width, int height) { 
		return null;
	}
	
	@Override
	public int getRotation() {
		return 0;
	}

	@Override
	public int getSupportedOperations() {
		return 0; //SUPPORT_ALL;
	}

	@Override
	public boolean handleOperation(Activity activity, int op) {
		return false;
	}
	
	@Override
	public boolean onOperationError(Activity activity, int op, 
			int error, String action) {
		return false;
	}
	
	@Override
	public int getMediaType() {
		return MEDIA_TYPE_IMAGE;
	}

	@Override
	public long getVersion() { 
		return 0;
	}
	
	@Override
	public long getDateInMs() { 
		return 0;
	}
	
	@Override
	public boolean isLocalItem() { 
		return false;
	}
	
	@Override
	public ScreenNail getScreenNail() { 
		return null;
	}
	
	@Override
	public IMediaControls getControls() { 
		return this;
	}
	
	@Override
	public String getTitle() { 
		return null;
	}
	
	@Override
	public String getSubTitle() { 
		return null;
	}
	
	@Override
	public String getActionTitle() { 
		return null;
	}
	
	@Override
	public String getActionSubTitle() { 
		return null;
	}
	
	@Override
	public View getActionCustomView(Activity activity) { 
		return null;
	}
	
	@Override
	public boolean showControls() { 
		return false;
	}
	
	@Override
	public int getStatisticCount(int type) { 
		return -1;
	}
	
	@Override
	public ActionItem[] getActionItems(Activity activity) { 
		return null;
	}
	
	@Override
	public Drawable getProviderIcon() { 
		return null;
	}
	
	@Override
	public boolean delete() throws IOException { 
		return false;
	}
	
	@Override
	public Intent getShareIntent() {
		return null;
	}
	
	//@Override
	//public Uri getShareStreamUri() { 
	//	return getContentUri(SUPPORT_SHARE);
	//}
	
	//@Override
	//public String getShareText() { 
	//	return null;
	//}
	
	//@Override
	//public String getShareHtml() { 
	//	return null;
	//}
	
	//@Override
	//public String getShareType() { 
	//	return MediaHelper.getMimeType(getMediaType());
	//}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "-" + getIdentity() + "{}";
	}
	
}
