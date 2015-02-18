package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.data.DataPath;

final class FlickrPhoto extends FlickrPhotoBase {

	private final FlickrPhotoSet mPhotoSet;
	private final YPhotoEntry mEntry;
	
	public FlickrPhoto(FlickrPhotoSet source, DataPath path, 
			YPhotoEntry entry) { 
		super(source, path, FlickrHelper.getPhotoURL(entry));
		mPhotoSet = source;
		mEntry = entry;
	}

	public FlickrPhotoSet getPhotoSet() { return mPhotoSet; }
	public YPhotoEntry getPhotoEntry() { return mEntry; }
	
	public String getUserId() { return mEntry.ownerId; }
	public String getPhotoId() { return mEntry.photoId; }
	
	@Override
	public String getName() {
		return mEntry.title;
	}

}
