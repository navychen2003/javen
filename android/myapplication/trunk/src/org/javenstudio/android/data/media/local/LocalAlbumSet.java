package org.javenstudio.android.data.media.local;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataManager;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.MediaObject;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.android.data.media.MediaSource;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.FetchData;
import org.javenstudio.cocoka.data.MediaHelper;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.util.Logger;

final class LocalAlbumSet extends LocalMediaSet {
	private static final Logger LOG = Logger.getLogger(LocalAlbumSet.class);

	public static final DataPath PATH_ALL = DataPath.fromString("/local/all");
    public static final DataPath PATH_IMAGE = DataPath.fromString("/local/image");
    public static final DataPath PATH_VIDEO = DataPath.fromString("/local/video");
	
    //private static final Uri[] mWatchUris =
    //    {Images.Media.EXTERNAL_CONTENT_URI, Video.Media.EXTERNAL_CONTENT_URI};
    
    private static int getTypeFromPath(DataPath path) {
        String name[] = path.split();
        if (name.length < 2) 
            throw new IllegalArgumentException(path.toString());
        
        return MediaHelper.getTypeFromString(name[1]);
    }
    
    private List<MediaSet> mAlbums = new ArrayList<MediaSet>();
	//private final MediaChangeNotifier mNotifier;
    private boolean mReloaded = false;
    
    private final LocalMediaSource mSource;
    //private final Handler mHandler;
    private final String mName;
    private final int mType;
    
    private long mFetchId = -1;
    
	public LocalAlbumSet(LocalMediaSource source, DataPath path) { 
		super(path, nextVersionNumber());
		mSource = source;
		//mHandler = new Handler(source.getApplication().getMainLooper());
		mType = getTypeFromPath(path);
		//mNotifier = new MediaChangeNotifier(this, mWatchUris, source.getApplication());
		mName = source.getDataApp().getContext().getResources().getString(R.string.label_local_albums);
		
		FetchData data = ContentHelper.getInstance().queryFetch(path.toString());
		if (data != null) 
			mFetchId = data.getId();
	}
	
	@Override
	public String getName() { 
		return mName;
	}
	
	@Override
	public DataApp getDataApp() { 
		return mSource.getDataApp();
	}
	
    @Override
    public MediaSet getSubSetAt(int index) {
        return mAlbums.get(index);
    }

    @Override
    public int getSubSetCount() {
        return mAlbums.size();
    }
	
	@Override
	public boolean isDeleteEnabled() { return true; }
    
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
    public synchronized long reloadData(ReloadCallback callback, ReloadType type) { 
    	final long fetchId = mFetchId;
    	final boolean fetchDirty = isFetchDataDirty(fetchId);
    	
    	if (fetchDirty && !callback.isActionProcessing()) { 
    		if (LOG.isDebugEnabled())
    			LOG.debug("reloadData: reload data, fetchId=" + fetchId + " dirty=" + fetchDirty);
    		
    		if (fetchDirty) 
    			type = ReloadType.FORCE;
    		
    		//clearMediaItems();
    		mReloaded = false;
    	}
    	
	    if (/*mNotifier.isDirty()*/ !mReloaded || type == ReloadType.FORCE) { 
	    	final JobContext jc = JobSubmit.newContext();
		    List<MediaSet> albums = loadAlbums(jc);
		    if (albums != null) { 
		    	if (type == ReloadType.FORCE) {
			    	try { 
			    		callback.showProgressDialog(getDataApp().getContext()
	            				.getString(R.string.dialog_localalbum_scanning_message));
			    		
			    		reloadLocalAlbums(albums, callback, type);
			    		saveContent(getPath().toString(), albums.size());
			    	} finally { 
			    		callback.hideProgressDialog();
			    	}
		    	}
		    	
		    	mAlbums = albums;
		    	mDataVersion = nextVersionNumber();
		    	mReloaded = true;
		    	
		    	setDirty(false);
		    }
	    }
	    
	    return mDataVersion;
    }
    
    private void reloadLocalAlbums(List<MediaSet> albums, 
    		ReloadCallback callback, ReloadType type) { 
    	for (int i=0; i < albums.size();) { 
    		MediaSet album = albums.get(i);
    		album.reloadData(callback, type);
    		if (album.getItemCount() == 0 && album.getSubSetCount() == 0) { 
    			albums.remove(i);
    			continue;
    		}
    		i++;
    	}
    }
    
    private void saveContent(String location, int itemCount) { 
    	if (location == null) 
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
    	data.setContentName("LocalAlbumSet");
    	data.setContentType("application/*");
    	
    	data.setPrefix(LocalMediaSource.PREFIX);
    	data.setAccount(null);
    	data.setEntryId(null);
    	data.setEntryType(0);
    	
    	data.setTotalResults(itemCount);
    	data.setStartIndex(0);
    	data.setItemsPerPage(itemCount);
    	
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
    
    private MediaSet getLocalAlbum(int type, DataPath parent, int id, String name) {
        synchronized (DataManager.LOCK) {
            DataPath path = parent.getChild(id);
            MediaObject object = mSource.peekMediaObject(path);
            if (object != null) 
            	return (MediaSet) object;
            
            switch (type) {
                case MEDIA_TYPE_IMAGE:
                    return new LocalAlbum(mSource, path, id, true, name);
                case MEDIA_TYPE_VIDEO:
                    return new LocalAlbum(mSource, path, id, false, name);
                    
                case MEDIA_TYPE_ALL:
                    Comparator<MediaItem> comp = MediaSource.sDateTakenComparator;
                    return new LocalMergeAlbum(mSource, path, comp, new MediaSet[] {
                            getLocalAlbum(MEDIA_TYPE_IMAGE, PATH_IMAGE, id, name),
                            getLocalAlbum(MEDIA_TYPE_VIDEO, PATH_VIDEO, id, name)}, id);
            }
            
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }
    
    private List<MediaSet> loadAlbums(JobContext jc) {
        // Note: it will be faster if we only select media_type and bucket_id.
        //       need to test the performance if that is worth
    	BucketHelper.BucketEntry[] entries = BucketHelper.loadBucketEntries(
                jc, mSource.getDataApp().getContentResolver(), mType);
    	
        if (jc.isCancelled()) return null;
        int offset = 0;
        
        // Move camera and download bucket to the front, while keeping the
        // order of others.
        int index = findBucket(entries, LocalHelper.CAMERA_BUCKET_ID);
        if (index != -1) {
            circularShiftRight(entries, offset++, index);
        }
        
        index = findBucket(entries, LocalHelper.DOWNLOAD_BUCKET_ID);
        if (index != -1) {
            circularShiftRight(entries, offset++, index);
        }

        ArrayList<MediaSet> albums = new ArrayList<MediaSet>();
        for (BucketHelper.BucketEntry entry : entries) {
            MediaSet album = getLocalAlbum(mType, getDataPath(), entry.bucketId, entry.bucketName);
            albums.add(album);
        }
        
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("loadAlbums: load " + (entries != null ? entries.length : 0) 
    				+ " entries and " + albums.size() + " albums, type=" + mType);
    	}
        
        return albums;
    }
    
    private static int findBucket(BucketHelper.BucketEntry entries[], int bucketId) {
        for (int i = 0, n = entries.length; i < n; ++i) {
            if (entries[i].bucketId == bucketId) return i;
        }
        return -1;
    }
    
    // Circular shift the array range from a[i] to a[j] (inclusive). That is,
    // a[i] -> a[i+1] -> a[i+2] -> ... -> a[j], and a[j] -> a[i]
    private static <T> void circularShiftRight(T[] array, int i, int j) {
        T temp = array[j];
        for (int k = j; k > i; k--) {
            array[k] = array[k - 1];
        }
        array[i] = temp;
    }
    
}
