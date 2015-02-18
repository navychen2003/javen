package org.javenstudio.provider.app.local;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.provider.media.MediaPhotoBinderLarge;
import org.javenstudio.provider.media.MediaPhotoSource;
import org.javenstudio.provider.media.photo.PhotoItem;

final class LocalPhotoBinderLarge extends MediaPhotoBinderLarge {

	public LocalPhotoBinderLarge(MediaPhotoSource source) { 
		super(source);
	}
	
	@Override
	protected void bindPhotoText(final IActivity activity, final PhotoItem item, View view) { 
		if (item == null || view == null) return;
		
		TextView titleView = (TextView)view.findViewById(R.id.photo_item_title);
		if (titleView != null) {
			titleView.setText(item.getPhoto().getPhotoInfo().getTitle());
			titleView.setLines(1);
			titleView.setVisibility(View.VISIBLE);
		}
		
		TextView textView = (TextView)view.findViewById(R.id.photo_item_text);
		if (textView != null) { 
			textView.setText(item.getPhoto().getPhotoInfo().getSubTitle());
			textView.setLines(1);
			textView.setVisibility(View.VISIBLE);
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.photo_item_logo);
		if (logoView != null) { 
			logoView.setImageDrawable(item.getPhoto().getPhotoInfo().getProviderIcon());
			logoView.setVisibility(View.VISIBLE);
		}
	}
	
}
