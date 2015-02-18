package org.javenstudio.provider.app.picasa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.FetchData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.widget.ResultCallback;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.NodeXml;
import org.javenstudio.common.util.Logger;

public class PicasaAlbumPhotoSet extends PicasaMediaSet 
		implements PicasaSource, ResultCallback {
	private static final Logger LOG = Logger.getLogger(PicasaAlbumPhotoSet.class);
	
	static final String PREFIX = "picasa";
	
	private final List<MediaItem> mMediaItems = new ArrayList<MediaItem>();
	private final Set<String> mMediaPaths = new HashSet<String>();
	
	private final DataApp mApplication;
	private final SystemUser mAccount;
	private final String mLocation;
	private final String mName;
	private final String mAlbumId;
	private final String mUserId;
	private final String mUserName;
	private final String mAvatarLocation;
	private final int mIconRes;
	
	private long mFetchId = -1;
	private GAlbumPhotoEntry.ResultInfo mResultInfo = null;
	private boolean mReloaded = false;
	
	private PicasaAlbumPhotoSet(DataApp app, SystemUser account, 
			String location, String albumId, String userId, String userName, 
			String avatarURL, String name, int iconRes) { 
		super(toDataPath(location), nextVersionNumber());
		mApplication = app;
		mAccount = account;
		mLocation = location;
		mName = name;
		mAlbumId = albumId;
		mUserId = userId;
		mUserName = userName;
		mAvatarLocation = avatarURL;
		mIconRes = iconRes;
		
		FetchData data = ContentHelper.getInstance().queryFetch(location);
		if (data != null) 
			mFetchId = data.getId();
	}
	
	public DataApp getDataApp() { return mApplication; }
	public SystemUser getAccount() { return mAccount; }
	
	public String getTopSetLocation() { return mLocation; }
	public String getAvatarLocation() { return mAvatarLocation; }
	
	public String getUserId() { return mUserId; }
	public String getAuthor() { return mUserName; }
	public String getNickName() { return mResultInfo != null ? mResultInfo.nickName : null; }
	public String getAlbumTitle() { return mResultInfo != null ? mResultInfo.title : null; }
	public String getAlbumId() { return mAlbumId; }
	
	@Override
	public boolean isDeleteEnabled() { 
		return getAccount() != null; 
	}
	
	@Override
	public void onResult(Context context, int code, Object result) { 
		PicasaUploader.enqueueUpload(context, this, code, result);
	}
	
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
	
	private synchronized boolean addMediaItem(PicasaAlbumPhoto item) { 
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
	
	private PicasaAlbumPhoto newPhoto(GAlbumPhotoEntry entry) {
		DataPath path = DataPath.fromString("/" + PREFIX + "/album/item/" 
				+ entry.photoId);
		return new PicasaAlbumPhoto(this, path, entry);
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
    		
    		//clearMediaItems();
    		mReloaded = false;
    	}
    	
    	if (mReloaded == false || type == ReloadType.FORCE || type == ReloadType.NEXTPAGE) {
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
							PicasaHelper.resetAuthToken(getDataApp().getContext(), getAccount());
							callback.onActionError(new ActionError(ActionError.Action.ALBUM_PHOTOSET, e)); 
						}
						@Override 
						public void initRequest(HttpUriRequest request) { 
							PicasaHelper.initAuthRequest(getDataApp().getContext(), 
									request, getAccount());
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
    	
    	clearMediaItems();
    	
    	try { 
			NodeXml.Handler handler = new NodeXml.Handler("feed"); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			NodeXml xml = handler.getEntity(); 
			GAlbumPhotoEntry.ResultInfo resultInfo = GAlbumPhotoEntry.parseInfo(xml);
			mResultInfo = resultInfo;
			
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node child = xml.getChildAt(i);
				if (child == null) continue;
				
				if ("entry".equalsIgnoreCase(child.getName())) {
					try { 
						GAlbumPhotoEntry entry = GAlbumPhotoEntry.parseEntry(child); 
						if (entry != null) {
							entry.user = mUserId;
							addMediaItem(newPhoto(entry));
						}
					} catch (Throwable e) { 
						if (LOG.isWarnEnabled())
							LOG.warn("parse entry error: " + e.toString(), e); 
					}
				}
			}
			
			if (getTopSetLocation().equals(location))
				saveContent(location, resultInfo);
    	} catch (Throwable e) { 
			callback.onActionError(new ActionError(ActionError.Action.ALBUM_PHOTOSET, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    }
	
    private void saveContent(String location, GAlbumPhotoEntry.ResultInfo info) { 
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
    	
    	SystemUser account = getAccount();
    	
    	FetchData data = fetchData.startUpdate();
    	data.setContentUri(location);
    	data.setContentName(getName());
    	data.setContentType("text/xml");
    	
    	data.setPrefix(PREFIX);
    	data.setAccount(account != null ? account.getAccountName() : "");
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
    
	private static DataPath toDataPath(String location) { 
		return DataPath.fromString("/" + PREFIX + "/image/" + Utilities.toMD5(location));
	}
	
	public static PicasaAlbumPhotoSet newPhotoSet(DataApp app, SystemUser account, 
			String userId, String albumId, String userName, String avatarURL, int iconRes) { 
		String location = String.format(GPhotoHelper.ALBUM_PHOTOS_URL, userId, albumId);
		return new PicasaAlbumPhotoSet(app, account, location, albumId, userId, userName, avatarURL, 
				(account != null ? "AccountAlbumPhotos" : "UserAlbumPhotos"), iconRes);
	}
    
}
