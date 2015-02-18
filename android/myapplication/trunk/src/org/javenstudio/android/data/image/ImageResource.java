package org.javenstudio.android.data.image;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.cocoka.storage.MediaStorageProvider;

public abstract class ImageResource {

	public abstract DataApp getApplication();
	public abstract MediaStorageProvider getStorageProvider();
	public abstract Image getImage(String source);
	
}
