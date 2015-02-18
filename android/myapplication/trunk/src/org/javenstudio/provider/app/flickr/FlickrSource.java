package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.data.DataApp;

interface FlickrSource {

	public DataApp getDataApp();
	public String getTopSetLocation();
	public int getSourceIconRes();
	
}
