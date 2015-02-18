package org.javenstudio.provider.media;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.media.album.AlbumBinderLarge;
import org.javenstudio.provider.media.album.AlbumDataSets;
import org.javenstudio.provider.media.album.AlbumItem;
import org.javenstudio.provider.media.album.OnAlbumClickListener;

public class MediaAlbumBinderLarge extends AlbumBinderLarge {

	private final MediaAlbumSource mSource;
	
	public MediaAlbumBinderLarge(MediaAlbumSource source) { 
		mSource = source;
	}
	
	@Override
	protected AlbumDataSets getAlbumDataSets() { 
		return mSource.getAlbumDataSets();
	}
	
	@Override
	protected OnAlbumClickListener getOnAlbumClickListener() { 
		return mSource.getOnAlbumItemClickListener();
	}
	
	@Override
	protected OnAlbumClickListener getOnAlbumViewClickListener() { 
		return mSource.getOnAlbumViewClickListener();
	}

	@Override
	public Provider getProvider() {
		return mSource.getProvider();
	}
	
	@Override
	protected void bindAlbumText(final IActivity activity, final AlbumItem item, View view) { 
		MediaInfo info = item.getAlbum().getAlbumInfo();
		String text = String.format(activity.getResources().getString(R.string.label_album_info), 
				item.getAlbum().getPhotoCount());
		
		TextView titleView = (TextView)view.findViewById(R.id.album_item_title);
		if (titleView != null) { 
			titleView.setText(info.getTitle());
			titleView.setVisibility(View.VISIBLE);
		}
		
		TextView textView = (TextView)view.findViewById(R.id.album_item_text);
		if (textView != null) {
			textView.setText(text);
			textView.setVisibility(View.VISIBLE);
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.album_item_logo);
		Drawable logo = info.getProviderIcon();
		if (logo != null && logoView != null) {
			logoView.setImageDrawable(logo);
			logoView.setVisibility(View.VISIBLE);
		}
	}
	
}
