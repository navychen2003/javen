package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.NodeXml;
import org.javenstudio.common.util.Logger;

public class FlickrFavoriteSet extends FlickrPhotoSet {
	private static final Logger LOG = Logger.getLogger(FlickrFavoriteSet.class);
	
	public FlickrFavoriteSet(DataApp app, String userId, int iconRes) { 
		super(app, FlickrHelper.USER_FAVORITES_URL + userId, "UserFavorites", iconRes, false, false);
	}
	
	synchronized boolean addFavorite(YFavoriteEntry entry) {
		if (entry == null || entry.photoId == null) 
			return false;
		
		DataPath path = DataPath.fromString("/" + PREFIX + "/photos/" 
				+ entry.photoId);
		
		if (containsMediaItem(path.toString()))
			return false;
		
		return addMediaItem(new FlickrFavorite(this, path, entry));
	}
	
	protected synchronized void onFetched(ReloadCallback callback, 
    		String location, String content) { 
    	if (location == null || content == null || content.length() == 0) 
    		return;
    	
    	clearMediaItems();
    	
    	try { 
			NodeXml.Handler handler = new NodeXml.Handler("rsp"); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			NodeXml xml = handler.getEntity(); 
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node rspChild = xml.getChildAt(i);
				if (rspChild != null && "photos".equalsIgnoreCase(rspChild.getName())) {
					YPhotoEntry.ResultInfo resultInfo = YPhotoEntry.parseInfo(rspChild);
					mResultInfo = resultInfo;
					
					for (int j=0; j < rspChild.getChildCount(); j++) {
						Node photosChild = rspChild.getChildAt(j);
						if (photosChild != null && "photo".equalsIgnoreCase(photosChild.getName())) {
							try { 
								YFavoriteEntry entry = YFavoriteEntry.parseEntry(photosChild); 
								if (entry != null)
									addFavorite(entry);
							} catch (Throwable e) { 
								if (LOG.isWarnEnabled())
									LOG.warn("parse entry error: " + e.toString(), e); 
							}
						}
					}
					
					if (getTopSetLocation().equals(location))
						saveContent(location, resultInfo);
				}
			}
    	} catch (Throwable e) { 
			callback.onActionError(new ActionError(ActionError.Action.FAVORITE, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    }
	
}
