package org.javenstudio.provider.app;

import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.media.PhotoProvider;
import org.javenstudio.provider.media.photo.PhotoFactory;

public abstract class BasePhotoProvider extends PhotoProvider {

	public BasePhotoProvider(String name, int iconRes, 
			PhotoSet set, PhotoFactory factory) { 
		super(name, iconRes, set, factory);
	}
	
}
