package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.data.DataPath;
import org.javenstudio.cocoka.util.Utilities;

final class FlickrFavorite extends FlickrPhotoBase {

	private final FlickrFavoriteSet mPhotoSet;
	private final YFavoriteEntry mEntry;
	
	public FlickrFavorite(FlickrFavoriteSet source, DataPath path, 
			YFavoriteEntry entry) { 
		super(source, path, FlickrHelper.getPhotoURL(entry));
		mPhotoSet = source;
		mEntry = entry;
	}

	public FlickrFavoriteSet getPhotoSet() { return mPhotoSet; }
	public YFavoriteEntry getPhotoEntry() { return mEntry; }
	
	public String getUserId() { return mEntry.ownerId; }
	public String getPhotoId() { return mEntry.photoId; }
	
	@Override
	public String getName() {
		return mEntry.title;
	}

	@Override
	public String getPhotoDate() { 
		return Utilities.formatDate(mEntry.datefaved);
	}
	
}
