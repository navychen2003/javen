package org.javenstudio.provider.media;

import org.javenstudio.provider.Provider;
import org.javenstudio.provider.media.photo.OnPhotoClickListener;
import org.javenstudio.provider.media.photo.PhotoBinderSmall;
import org.javenstudio.provider.media.photo.PhotoDataSets;

public class MediaPhotoBinderSmall extends PhotoBinderSmall {

	private final MediaPhotoSource mSource;
	
	public MediaPhotoBinderSmall(MediaPhotoSource source) { 
		mSource = source;
	}
	
	@Override
	protected PhotoDataSets getPhotoDataSets() {
		return mSource.getPhotoDataSets();
	}

	@Override
	protected OnPhotoClickListener getOnPhotoClickListener() {
		return mSource.getOnPhotoItemClickListener();
	}

	@Override
	protected OnPhotoClickListener getOnPhotoViewClickListener() {
		return mSource.getOnPhotoViewClickListener();
	}

	@Override
	protected OnPhotoClickListener getOnPhotoUserClickListener() {
		return mSource.getOnPhotoUserClickListener();
	}
	
	@Override
	public Provider getProvider() {
		return mSource.getProvider();
	}

}
