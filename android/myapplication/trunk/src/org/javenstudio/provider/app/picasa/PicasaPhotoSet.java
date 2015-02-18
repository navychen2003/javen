package org.javenstudio.provider.app.picasa;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.CacheReader;
import org.javenstudio.android.data.media.CacheWriter;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.FetchData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.NodeXml;
import org.javenstudio.common.util.Logger;

public class PicasaPhotoSet extends PicasaMediaSet 
		implements PicasaSource {
	private static final Logger LOG = Logger.getLogger(PicasaPhotoSet.class);
	
	static final String PREFIX = "picasa";
	
	private final List<MediaItem> mMediaItems = new ArrayList<MediaItem>();
	private final Set<String> mMediaPaths = new HashSet<String>();
	
	private final DataApp mApplication;
	private final String mLocation;
	private final String mName;
	private final boolean mSearchable;
	private final int mIconRes;
	
	private long mFetchId = -1;
	private GPhotoEntry.ResultInfo mResultInfo = null;
	
	private String mReloadLocation = null;
	private String mSearchText = null;
	private boolean mReloaded = false;
	
	private PicasaPhotoSet(DataApp app, String location, 
			String name, int iconRes, boolean searchable) { 
		super(toDataPath(location), nextVersionNumber());
		mApplication = app;
		mLocation = location;
		mName = name;
		mSearchable = searchable;
		mIconRes = iconRes;
		
		FetchData data = ContentHelper.getInstance().queryFetch(location);
		if (data != null) 
			mFetchId = data.getId();
	}
	
	public DataApp getDataApp() { return mApplication; }
	public SystemUser getAccount() { return null; }
	public String getTopSetLocation() { return mLocation; }
	
	public GPhotoEntry.ResultInfo getResultInfo() { return mResultInfo; }
	
	public boolean isSearchable() { return mSearchable; }
	public String getSearchText() { return mSearchText; }
	public String[] getPhotoTags() { return GPhotoHelper.TAGS; }
	
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
    public synchronized int getItemCount() {
        return mMediaItems.size();
    }
	
	@Override
    public synchronized List<MediaItem> getItemList(int start, int count) {
        List<MediaItem> result = new ArrayList<MediaItem>();
        if (start < 0 || start >= mMediaItems.size() || count <= 0) 
        	return result;
        
        int end = start + count;
        for (int i=start; i < end && i < mMediaItems.size(); i++) { 
        	MediaItem item = mMediaItems.get(i);
        	result.add(item);
        }
        
        return result;
    }
	
	private synchronized boolean addMediaItem(PicasaPhoto item) { 
		if (item != null) { 
			String path = item.getDataPath().toString();
			if (!mMediaPaths.contains(path)) {
				mMediaItems.add(item); 
				mMediaPaths.add(path);
				
				if (LOG.isDebugEnabled())
					LOG.debug("addMediaItem: path=" + path + " name=" + item.getName());
				
				return true;
			}
		}
		return false;
	}
	
	private synchronized void clearMediaItems() { 
		if (LOG.isDebugEnabled())
			LOG.debug("clearMediaItems");
		
		mMediaItems.clear();
		mMediaPaths.clear();
	}
	
	private synchronized boolean addPhoto(GPhotoEntry entry) {
		if (entry == null || entry.photoId == null) 
			return false;
		
		DataPath path = DataPath.fromString("/" + PREFIX + "/photo/" 
				+ entry.photoId);
		
		if (mMediaPaths.contains(path.toString()))
			return false;
		
		return addMediaItem(new PicasaPhoto(this, path, entry));
	}
	
	private String getReloadLocation(ReloadCallback callback) { 
		String queryText = callback.getParam(ReloadCallback.PARAM_QUERYTEXT);
		String queryTag = callback.getParam(ReloadCallback.PARAM_QUERYTAG);
    	String location = getTopSetLocation();
    	
    	if (!Utilities.isEquals(queryText, mSearchText)) {
    		mReloaded = false;
    		//clearMediaItems();
    	}
    	
    	mSearchText = queryText;
		callback.clearParams();
    	
    	if (queryText != null && queryText.length() > 0) { 
    		try {
    			location = GPhotoHelper.SEARCH_URL;
    			location += URLEncoder.encode(queryText, "UTF-8");
    		} catch (Throwable e) { 
    			if (LOG.isWarnEnabled()) 
    				LOG.warn(e.toString(), e);
    		}
    	} else if (queryTag != null && queryTag.length() > 0) { 
    		try {
    			location = GPhotoHelper.TAG_URL;
    			location += URLEncoder.encode(queryTag, "UTF-8");
    		} catch (Throwable e) { 
    			if (LOG.isWarnEnabled()) 
    				LOG.warn(e.toString(), e);
    		}
    	}
    	
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
    	
    	if (fetchDirty || !Utilities.isEquals(location, mReloadLocation)) { 
    		if (LOG.isDebugEnabled())
    			LOG.debug("reloadData: refetch data, fetchId=" + fetchId + " dirty=" + fetchDirty);
    		
    		if (fetchDirty) 
    			type = ReloadType.FORCE;
    		
    		//clearMediaItems();
    		mReloaded = false;
    	}
    	
    	if (mReloaded == false || type == ReloadType.FORCE || type == ReloadType.NEXTPAGE) {
	    	if (type != ReloadType.FORCE) 
	    		loadCache(JobSubmit.newContext(), location);
	    	
	    	if (type == ReloadType.FORCE || getItemCount() <= 0) {
		    	//clearMediaItems();
		    	
				HtmlCallback cb = new HtmlCallback() {
						@Override
						public void onHtmlFetched(String content) {
							onFetched(callback, location, content); 
						}
						@Override
						public void onHttpException(HttpException e) { 
							if (e == null) return;
							callback.onActionError(new ActionError(ActionError.Action.PHOTOSET, e)); 
						}
					};
				
				cb.setRefetchContent(type == ReloadType.FORCE);
				cb.setSaveContent(true);
				
				FetchHelper.removeFailed(location);
				FetchHelper.fetchHtml(location, cb);
				
				saveCache(location);
	    	}
	    	
	    	mReloadLocation = location;
			mDataVersion = nextVersionNumber();
			mReloaded = true;
    	}
    	
    	return mDataVersion;
    }
    
    private synchronized void onFetched(ReloadCallback callback, 
    		String location, String content) { 
    	if (location == null || content == null || content.length() == 0) 
    		return;
    	
    	clearMediaItems();
    	
    	try { 
			NodeXml.Handler handler = new NodeXml.Handler("feed"); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			NodeXml xml = handler.getEntity(); 
			GPhotoEntry.ResultInfo resultInfo = GPhotoEntry.parseInfo(xml);
			mResultInfo = resultInfo;
			
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node child = xml.getChildAt(i);
				if (child != null && "entry".equalsIgnoreCase(child.getName())) {
					try { 
						GPhotoEntry entry = GPhotoEntry.parseEntry(child); 
						if (entry != null)
							addPhoto(entry);
					} catch (Throwable e) { 
						if (LOG.isWarnEnabled())
							LOG.warn("parse entry error: " + e.toString(), e); 
					}
				}
			}
			
			if (getTopSetLocation().equals(location))
				saveContent(location, resultInfo);
    	} catch (Throwable e) { 
			callback.onActionError(new ActionError(ActionError.Action.PHOTOSET, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    }
	
    private void saveContent(String location, GPhotoEntry.ResultInfo info) { 
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
    	data.setEntryId(info.id);
    	data.setEntryType(0);
    	
    	data.setTotalResults(info.totalResults);
    	data.setStartIndex(info.startIndex);
    	data.setItemsPerPage(info.itemsPerPage);
    	
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
    
    private boolean saveCache(final String location) { 
    	CacheWriter writer = new CacheWriter(getDataApp().getCacheData()) {
    		@Override
    		protected String getCacheFileName(MediaSet mediaSet) { 
    			return location;
    		}
    		
			@Override
			protected boolean write(DataOutput out) throws IOException {
		    	writeMediaSet(out, PicasaPhotoSet.this);
				return true;
			}
			
			@Override
			protected void writeMediaItem(DataOutput out, MediaItem item) throws IOException { 
				PicasaPhoto image = (PicasaPhoto)item;
		    	GPhotoEntry.writeEntry(out, image.getPhotoEntry());
		    }
		    
			@Override
			protected void writeMediaSet(DataOutput out, MediaSet set) throws IOException { 
		    	if (set != null && set instanceof PicasaPhotoSet) {
		    		PicasaPhotoSet source = (PicasaPhotoSet)set;
		    		writeItems(out, source);
		    	}
		    }
    	};
    	
    	return writer.saveCache(this);
    }
    
	private boolean loadCache(JobContext jc, final String location) { 
    	CacheReader reader = new CacheReader(getDataApp().getCacheData()) {
    		private int mCount = 0;
    		
    		@Override
    		protected String getCacheFileName(MediaSet mediaSet) { 
    			return location;
    		}
    		
			@Override
			protected boolean read(DataInput in) throws IOException {
				clearMediaItems();
				readMediaSet(in, PicasaPhotoSet.this);
				return true;
			}
			
			@Override
			protected void readMediaItem(DataInput in, MediaSet set) throws IOException {
				GPhotoEntry entry = GPhotoEntry.readEntry(in);
				if (addPhoto(entry)) 
					mCount ++;
			}

			@Override
			protected void readMediaSet(DataInput in, MediaSet set) throws IOException {
				readItems(in, PicasaPhotoSet.this);
			}
			
			@Override
			protected boolean isInterrupt() { 
				return mCount > 20; 
			}
    	};
    	
    	return reader.readCache(this);
    }
    
	private static DataPath toDataPath(String location) { 
		return DataPath.fromString("/" + PREFIX + "/image/" + Utilities.toMD5(location));
	}
	
	public static PicasaPhotoSet newFeatureSet(DataApp app, int iconRes) { 
		return new PicasaPhotoSet(app, GPhotoHelper.FEATURE_URL, "Featured", iconRes, true);
	}
	
}
