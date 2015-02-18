package org.javenstudio.provider.app.flickr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.FetchData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.NodeXml;
import org.javenstudio.common.util.Logger;

public class FlickrAlbumSet extends FlickrMediaSet 
		implements FlickrSource {
	private static final Logger LOG = Logger.getLogger(FlickrAlbumSet.class);
	
	static final String PREFIX = "flickr";
	
	private final List<MediaSet> mMediaSets = new ArrayList<MediaSet>();
	private final Set<String> mMediaPaths = new HashSet<String>();
	
	private final DataApp mApplication;
	private final String mLocation;
	private final String mName;
	private final int mIconRes;
	
	private long mFetchId = -1;
	private YPhotoSetEntry.ResultInfo mResultInfo = null;
	private boolean mReloaded = false;
	
	private FlickrAlbumSet(DataApp app, String location, String name, int iconRes) { 
		super(toDataPath(location), nextVersionNumber());
		mApplication = app;
		mLocation = location;
		mName = name;
		mIconRes = iconRes;
		
		FetchData data = ContentHelper.getInstance().queryFetch(location);
		if (data != null) 
			mFetchId = data.getId();
	}
	
	public DataApp getDataApp() { return mApplication; }
	public String getTopSetLocation() { return mLocation; }
	
	public YPhotoSetEntry.ResultInfo getResultInfo() { return mResultInfo; }
	
	@Override
	public int getSourceIconRes() { 
		return mIconRes;
	}
	
	@Override
	public Drawable getProviderIcon() { 
		int iconRes = getSourceIconRes(); 
		if (iconRes != 0) 
			return ResourceHelper.getResources().getDrawable(iconRes);
		return null;
	}
	
	@Override
	public String getName() {
		return mName; //getTopSetLocation();
	}
	
	@Override
    public synchronized int getSubSetCount() { 
		return mMediaSets.size(); 
	}

	@Override
    public synchronized MediaSet getSubSetAt(int index) {
        return index >= 0 && index < mMediaSets.size() ? mMediaSets.get(index) : null;
    }
	
	private synchronized boolean addMediaSet(FlickrAlbum set) { 
		if (set != null) { 
			String albumId = set.getAlbumId();
			if (albumId == null || albumId.length() == 0) { 
				if (LOG.isDebugEnabled())
					LOG.debug("addMediaItem: ignore empty albumId, name=" + set.getName());
				
				return false;
			}
			
			String path = set.getDataPath().toString();
			if (!mMediaPaths.contains(path)) {
				mMediaSets.add(set); 
				mMediaPaths.add(path);
				
				if (LOG.isDebugEnabled())
					LOG.debug("addMediaItem: path=" + path + " name=" + set.getName());
				
				return true;
			}
		}
		return false;
	}
	
	private synchronized void clearMediaSets() { 
		if (LOG.isDebugEnabled())
			LOG.debug("clearMediaSets");
		
		mMediaSets.clear();
		mMediaPaths.clear();
	}
	
	private FlickrAlbum newAlbum(YPhotoSetEntry entry) {
		DataPath path = DataPath.fromString("/" + PREFIX + "/photosets/" 
				+ entry.photosetId);
		return new FlickrAlbum(this, path, entry, mIconRes);
	}

	private String getReloadLocation(ReloadCallback callback) { 
		String location = getTopSetLocation();
		return location;
	}
	
	@Override
	public boolean isDirty() { return isFetchDataDirty(mFetchId); }
	
	private boolean isFetchDataDirty(long fetchId) { 
		if (super.isDirty()) return true;
		
		FetchData fetchData = ContentHelper.getInstance().queryFetch(fetchId);
		if (fetchData == null || fetchData.getStatus() == FetchData.STATUS_DIRTY) 
			return true;
		
		return false;
	}
	
	@Override
    public synchronized long reloadData(final ReloadCallback callback, 
    		ReloadType type) { 
		final String location = getReloadLocation(callback);
		
		final long fetchId = mFetchId;
    	final boolean fetchDirty = getTopSetLocation().equals(location) ? 
    			(isFetchDataDirty(fetchId) && !callback.isActionProcessing()) : false;
    	
    	if (fetchDirty) { 
    		if (LOG.isDebugEnabled())
    			LOG.debug("reloadData: refetch data, fetchId=" + fetchId + " dirty=" + fetchDirty);
    		
    		if (fetchDirty) 
    			type = ReloadType.FORCE;
    		
    		//clearMediaSets();
    		mReloaded = false;
    	}
		
		if (mReloaded == false || type == ReloadType.FORCE || type == ReloadType.NEXTPAGE) { 
			if (type == ReloadType.FORCE || getSubSetCount() <= 0) {
		    	//clearMediaSets();
		    	
				HtmlCallback cb = new HtmlCallback() {
						@Override
						public void onHtmlFetched(String content) {
							onFetched(callback, location, content); 
						}
						@Override
						public void onHttpException(HttpException e) { 
							if (e == null) return;
							callback.onActionError(new ActionError(ActionError.Action.ALBUMSET, e)); 
						}
					};
				
				cb.setRefetchContent(type == ReloadType.FORCE);
				cb.setSaveContent(true);
				
				FetchHelper.removeFailed(location);
				FetchHelper.fetchHtml(location, cb);
	    	}
			
			mDataVersion = nextVersionNumber();
			mReloaded = true;
		}
		
		return mDataVersion;
	}
	
    private synchronized void onFetched(ReloadCallback callback, 
    		String location, String content) { 
    	if (location == null || content == null || content.length() == 0) 
    		return;
    	
    	clearMediaSets();
    	
    	try { 
			NodeXml.Handler handler = new NodeXml.Handler("rsp"); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			NodeXml xml = handler.getEntity(); 
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node rspChild = xml.getChildAt(i);
				if (rspChild != null && "photosets".equalsIgnoreCase(rspChild.getName())) {
					YPhotoSetEntry.ResultInfo resultInfo = YPhotoSetEntry.parseInfo(rspChild);
					mResultInfo = resultInfo;
					
					for (int j=0; j < rspChild.getChildCount(); j++) {
						Node photosetsChild = rspChild.getChildAt(j);
						if (photosetsChild != null && "photoset".equalsIgnoreCase(photosetsChild.getName())) {
							try { 
								YPhotoSetEntry entry = YPhotoSetEntry.parseEntry(photosetsChild); 
								if (entry != null)
									addMediaSet(newAlbum(entry));
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
			callback.onActionError(new ActionError(ActionError.Action.ALBUMSET, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    }
	
    private void saveContent(String location, YPhotoSetEntry.ResultInfo info) { 
    	if (location == null || info == null) 
    		return;
    	
    	long fetchId = mFetchId;
    	long current = System.currentTimeMillis();
    	boolean create = false;
    	FetchData fetchData = null;
    	
    	if (fetchId > 0) 
    		fetchData = ContentHelper.getInstance().queryFetch(fetchId);
    	else
    		fetchData = ContentHelper.getInstance().queryFetch(location);
    	
    	if (fetchData == null) { 
    		fetchData = ContentHelper.getInstance().newFetch();
    		create = true;
    	}
    	
    	FetchData data = fetchData.startUpdate();
    	data.setContentUri(location);
    	data.setContentName(getName());
    	data.setContentType("text/xml");
    	
    	data.setPrefix(PREFIX);
    	data.setAccount(null);
    	//data.setEntryId(info.id);
    	data.setEntryType(0);
    	
    	data.setTotalResults(info.total);
    	data.setStartIndex((info.page - 1) * info.perpage + 1);
    	data.setItemsPerPage(info.perpage);
    	
    	data.setFailedCode(0);
    	data.setStatus(FetchData.STATUS_OK);
    	data.setUpdateTime(current);
    	if (create) data.setCreateTime(current);
    	
    	long updateKey = data.commitUpdates();
    	mFetchId = updateKey;
    	
    	if (LOG.isDebugEnabled()) { 
    		LOG.debug("saveContent: fetchId=" + fetchId + " updateKey=" 
    				+ updateKey + " location=" + location);
    	}
    }
    
	private static DataPath toDataPath(String location) { 
		return DataPath.fromString("/" + PREFIX + "/album/" + Utilities.toMD5(location));
	}
	
	public static FlickrAlbumSet newAlbumSet(DataApp app, String userid, int iconRes) { 
		String location = FlickrHelper.USER_PHOTOSETS_URL + userid;
		return new FlickrAlbumSet(app, location, "UserPhotoSets", iconRes);
	}
	
}
