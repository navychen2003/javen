package org.javenstudio.cocoka.data;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.cocoka.opengl.ScreenNail;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;

public interface IMediaItem extends IMediaObject {

	public static interface RequestListener {
		public void onMediaItemPrepare(IMediaItem item);
		public void onMediaItemDone(IMediaItem item, boolean ready);
	}
	
	public String getName();
	public Uri getContentUri(int op);
	
	public Intent getShareIntent();
	//public Uri getShareStreamUri();
	//public String getShareHtml();
	//public String getShareText();
	//public String getShareType();
	
	public int getWidth();
	public int getHeight();
	public long getSize();
	
	public Job<BitmapRef> requestThumbnail(BitmapHolder holder, 
			RequestListener listener);
	public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder, 
			RequestListener listener);
	
	public Drawable getThumbnailDrawable(int width, int height);
	
	public int getRotation();
	public int getSupportedOperations();
	public int getMediaType();
	
	public long getVersion();
	public long getDateInMs();
	
	public boolean isLocalItem();
	public ScreenNail getScreenNail();
	
	public IMediaControls getControls();
	
	public boolean handleOperation(Activity activity, int op);
	public boolean onOperationError(Activity activity, int op, int error, String action);
	public boolean delete() throws IOException;
	
}
