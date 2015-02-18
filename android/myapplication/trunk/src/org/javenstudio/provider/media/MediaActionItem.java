package org.javenstudio.provider.media;

import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.app.ActionItem;

public class MediaActionItem extends ActionItem {

	private final IMediaSource mItem;
	
	public MediaActionItem(String tag, int iconRes, Drawable icon, 
			OnClickListener l, IMediaSource item) {
		super(tag, iconRes, icon, l);
		mItem = item;
	}
	
	public IMediaSource getSourceItem() { return mItem; }
	
}
