package org.javenstudio.provider.app.flickr;

import org.javenstudio.provider.media.MediaAlbumBinderList;
import org.javenstudio.provider.media.MediaAlbumSource;

final class FlickrAlbumBinderList extends MediaAlbumBinderList {

	public FlickrAlbumBinderList(MediaAlbumSource source) { 
		super(source);
	}
	
	//@Override
	//protected void bindAlbumText(final IActivity activity, final AlbumItem item, View view) { 
	//	MediaInfo info = item.getAlbum().getAlbumInfo();
	//	String text = String.format(activity.getResources().getString(R.string.label_album_info), 
	//			item.getAlbum().getPhotoCount());
	//	
	//	TextView titleView = (TextView)view.findViewById(R.id.media_album_item_title);
	//	titleView.setText(info.getTitle());
	//	titleView.setLines(2);
	//	
	//	TextView textView = (TextView)view.findViewById(R.id.media_album_item_text);
	//	textView.setVisibility(View.GONE);
	//	
	//	TextView dateView = (TextView)view.findViewById(R.id.media_album_item_date);
	//	dateView.setText(text);
	//	
	//	ImageView logoView = (ImageView)view.findViewById(R.id.media_album_item_logo);
	//	Drawable logo = info.getProviderIcon();
	//	if (logo != null) {
	//		logoView.setImageDrawable(logo);
	//		logoView.setVisibility(View.VISIBLE);
	//	}
	//}
	
}
