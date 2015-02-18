package org.javenstudio.provider.app.picasa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.drawable.Drawable;

import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.SystemUser;
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

public class PicasaAlbumSet extends PicasaMediaSet 
		implements PicasaSource {
	private static final Logger LOG = Logger.getLogger(PicasaAlbumSet.class);
	
	static final String PREFIX = "picasa";
	
	private final List<MediaSet> mMediaSets = new ArrayList<MediaSet>();
	private final Set<String> mMediaPaths = new HashSet<String>();
	
	private final AccountApp mApplication;
	private final SystemUser mAccount;
	private final String mLocation;
	private final String mName;
	private final String mUserId;
	private final String mUserName;
	private final String mAvatarLocation;
	private final int mIconRes;
	
	private long mFetchId = -1;
	private GAlbumEntry.ResultInfo mResultInfo = null;
	private boolean mReloaded = false;
	
	private PicasaAlbumSet(AccountApp app, SystemUser account, 
			String location, String userId, String userName, 
			String avatarURL, String name, int iconRes) { 
		super(toDataPath(location), nextVersionNumber());
		mApplication = app;
		mAccount = account;
		mLocation = location;
		mName = name;
		mUserId = userId;
		mUserName = userName;
		mAvatarLocation = avatarURL;
		mIconRes = iconRes;
		
		FetchData data = ContentHelper.getInstance().queryFetch(location);
		if (data != null) 
			mFetchId = data.getId();
	}
	
	public AccountApp getAccountApp() { return mApplication; }
	public SystemUser getAccount() { return mAccount; }
	
	public String getUserId() { return mUserId; }
	public String getAuthor() { return mUserName; }
	
	public String getTopSetLocation() { return mLocation; }
	public GAlbumEntry.ResultInfo getResultInfo() { return mResultInfo; }
	
	@Override
	public DataApp getDataApp() { 
		return getAccountApp().getDataApp();
	}
	
	@Override
	public boolean isDeleteEnabled() { 
		return getAccount() != null; 
	}
	
	@Override
	public String getAvatarLocation() { 
		if (mAvatarLocation != null && mAvatarLocation.length() > 0)
			return mAvatarLocation; 
		
		GAlbumEntry.ResultInfo resultInfo = getResultInfo();
		if (resultInfo != null) 
			return resultInfo.authorThumbnail;
		
		return null;
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
    public synchronized int getSubSetCount() { 
		return mMediaSets.size(); 
	}

	@Override
    public synchronized MediaSet getSubSetAt(int index) {
        return index >= 0 && index < mMediaSets.size() ? mMediaSets.get(index) : null;
    }
	
	private synchronized boolean addMediaSet(PicasaAlbum set) { 
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
	
	private PicasaAlbum newAlbum(GAlbumEntry entry) {
		DataPath path = DataPath.fromString("/" + PREFIX + "/album/" 
				+ entry.albumId);
		return new PicasaAlbum(this, path, entry, getAvatarLocation(), mIconRes);
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
							PicasaHelper.resetAuthToken(getDataApp().getContext(), getAccount());
							callback.onActionError(new ActionError(ActionError.Action.ALBUMSET, e)); 
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
    	
    	clearMediaSets();
    	
    	try { 
			NodeXml.Handler handler = new NodeXml.Handler("feed"); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			NodeXml xml = handler.getEntity(); 
			GAlbumEntry.ResultInfo resultInfo = GAlbumEntry.parseInfo(xml);
			mResultInfo = resultInfo;
			
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node child = xml.getChildAt(i);
				if (child != null && "entry".equalsIgnoreCase(child.getName())) {
					try { 
						GAlbumEntry entry = GAlbumEntry.parseEntry(child); 
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
    	} catch (Throwable e) { 
			callback.onActionError(new ActionError(ActionError.Action.ALBUMSET, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    }
	
    private void saveContent(String location, GAlbumEntry.ResultInfo info) { 
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
		return DataPath.fromString("/" + PREFIX + "/album/" + Utilities.toMD5(location));
	}
	
	public static PicasaAlbumSet newAlbumSet(AccountApp app, 
			String userId, String userName, String avatarURL, int iconRes) { 
		String location = String.format(GPhotoHelper.ALBUMS_URL, userId);
		return new PicasaAlbumSet(app, null, location, userId, 
				userName, avatarURL, "UserAlbums", iconRes);
	}
	
	public static PicasaAlbumSet newAccountAlbumSet(AccountApp app, 
			SystemUser account, int iconRes) { 
		String userId = account.getUserId();
		String location = String.format(GPhotoHelper.ACCOUNT_ALBUMS_URL, userId);
		return new PicasaAlbumSet(app, account, location, userId, 
				account.getUserTitle(), null, "AccountAlbums", iconRes);
	}
	
}
