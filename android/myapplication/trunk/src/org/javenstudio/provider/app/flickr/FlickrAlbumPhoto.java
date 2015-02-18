package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.data.DataPath;

final class FlickrAlbumPhoto extends FlickrPhotoBase {

	private final FlickrAlbumPhotoSet mPhotoSet;
	private final YPhotoItemEntry mEntry;
	
	public FlickrAlbumPhoto(FlickrAlbumPhotoSet source, DataPath path, 
			YPhotoItemEntry entry) { 
		super(source, path, FlickrHelper.getPhotoURL(entry));
		mPhotoSet = source;
		mEntry = entry;
	}

	public FlickrAlbumPhotoSet getPhotoSet() { return mPhotoSet; }
	public YPhotoItemEntry getPhotoEntry() { return mEntry; }
	
	public String getPhotoId() { return mEntry.photoId; }
	
	@Override
	public String getUserId() { 
		YPhotoItemEntry.PhotoSetInfo info = mPhotoSet.getPhotoSetInfo();
		if (info != null) 
			return info.ownerId;
		
		return null; 
	}
	
	@Override
	public String getName() {
		return mEntry.title;
	}

}
