package org.javenstudio.android.data.media.download;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.javenstudio.android.SourceHelper;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.image.http.HttpCache;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.CacheReader;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.android.data.media.CacheWriter;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.FetchData;
import org.javenstudio.cocoka.net.http.fetch.FetchCache;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.Path;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.util.Logger;

final class DownloadAlbumSet extends DownloadMediaSet {
	private static final Logger LOG = Logger.getLogger(DownloadAlbumSet.class);

	private final Set<String> mAlbumPaths = new TreeSet<String>();
	private DownloadAlbum[] mAlbums = null;
	
	private final DownloadMediaSource mSource;
	private final String mStoragePath;
	private final String mName;
	
	private long mFetchId = -1;
	//private boolean mDirty = false;
	
	public DownloadAlbumSet(DownloadMediaSource source, 
			DataPath path, String storagePath) { 
		super(path, nextVersionNumber());
		mName = source.getDataApp().getContext().getResources()
				.getString(R.string.label_download_albums);
		mSource = source;
		mStoragePath = storagePath;
		
		FetchData data = ContentHelper.getInstance().queryFetch(storagePath);
		if (data != null) 
			mFetchId = data.getId();
	}

	public final DownloadMediaSource getMediaSource() { return mSource; }
	public final String getStoragePath() { return mStoragePath; }
	
	@Override
	public boolean isDeleteEnabled() { return true; }
	
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
        return mAlbums != null && index >= 0 && index < mAlbums.length ? 
        		mAlbums[index] : null;
    }

    @Override
    public int getSubSetCount() {
        return mAlbums != null ? mAlbums.length : 0;
    }
	
    @Override
	public boolean isDirty() { return isFetchDataDirty(mFetchId); }
    
	private synchronized boolean isFetchDataDirty(long fetchId) { 
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
    		//mReloaded = false;
    	}
    	
    	if (mAlbums == null || type == ReloadType.FORCE || type == ReloadType.NEXTPAGE) { 
    		Collection<DownloadAlbum> albums = null;
    		boolean reload = false;
    		
    		if (type != ReloadType.FORCE) { 
    			DownloadAlbum[] loaded = mAlbums;
    			mAlbumPaths.clear();
    			
    			albums = new ArrayList<DownloadAlbum>();
    			for (int i=0; loaded != null && i < loaded.length; i++) { 
    				DownloadAlbum album = loaded[i];
    				if (album != null) {
    					mAlbumPaths.add(album.getDataPath().toString());
    					albums.add(album);
    				}
    			}
    			
    			List<DownloadAlbum> albumList = new ArrayList<DownloadAlbum>();
    			loadCache(JobSubmit.newContext(), albumList);
    			
    			for (DownloadAlbum album : albumList) { 
    				final String albumPath = album.getDataPath().toString();
		    		if (mAlbumPaths.contains(albumPath))
		    			continue;
		    		
    				album.reloadData(callback, type);
		    		if (album.getItemCount() > 0) {
			    		mAlbumPaths.add(albumPath);
			    		albums.add(album);
		    		}
    	    	}
    			
    			if (albums.size() == 0) 
    				albums = null;
    		}
    		
    		if (albums == null || type == ReloadType.FORCE) {
    			mAlbumPaths.clear();
    			reload = true;
    			
    			List<DownloadAlbum> albumList = new ArrayList<DownloadAlbum>();
			    reloadAlbums(JobSubmit.newContext(), callback, albumList);
			    
			    albums = new TreeSet<DownloadAlbum>(
	        		new Comparator<DownloadAlbum>() {
	    				@Override
	    				public int compare(DownloadAlbum lhs, DownloadAlbum rhs) {
	    					long lm = lhs.getDateInMs();
	    					long rm = rhs.getDateInMs();
	    					return lm == rm ? (lhs.getDataPath().compareTo(rhs.getDataPath())) 
	    							: (lm < rm ? 1 : -1);
	    				}
	    	    	});
			    
		    	for (DownloadAlbum album : albumList) { 
		    		final String albumPath = album.getDataPath().toString();
		    		if (mAlbumPaths.contains(albumPath))
		    			continue;
		    		
		    		album.reloadData(callback, type);
		    		if (album.getItemCount() > 0) {
			    		mAlbumPaths.add(albumPath);
			    		albums.add(album);
		    		}
		    	}
		    	
		    	saveContent(mStoragePath, albums.size());
    		}
	    	
	    	mAlbums = albums.toArray(new DownloadAlbum[albums.size()]);
	    	mDataVersion = nextVersionNumber();
	    	
	    	setDirty(false);
	    	if (reload) saveCache();
    	}
    	
    	return mDataVersion;
    }
    
    synchronized void removeMediaItem(MediaItem item) { 
    	if (item == null) return;
    	
    	DownloadAlbum[] albums = mAlbums;
    	if (albums == null || albums.length == 0) 
    		return;
    	
    	for (DownloadAlbum album : albums) { 
    		if (album != null && album.removeMediaItem(item)) { 
    			setDirty(true);
    			mSource.setDirty(true);
    		}
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
    	data.setContentName("DownloadAlbumSet");
    	data.setContentType("application/*");
    	
    	data.setPrefix(DownloadMediaSource.PREFIX);
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
    
    private boolean saveCache() { 
    	CacheWriter writer = new CacheWriter(getDataApp().getCacheData()) {
			@Override
			protected boolean write(DataOutput out) throws IOException {
				out.writeUTF(getDataPath().toString());
		    	out.writeUTF(getStoragePath());
		    	writeItems(out, DownloadAlbumSet.this);
				return true;
			}
			
			@Override
			protected void writeMediaItem(DataOutput out, MediaItem item) throws IOException { 
		    	DownloadImage image = (DownloadImage)item;
		    	out.writeUTF(getImageSourceName(image.getSourceName()));
		    	out.writeUTF(image.getFile().getAbsolutePath());
		    	out.writeLong(image.getDateInMs());
		    }
		    
			@Override
			protected void writeMediaSet(DataOutput out, MediaSet set) throws IOException { 
		    	DownloadAlbum album = (DownloadAlbum)set;
		    	out.writeUTF(album.getName());
		    	out.writeUTF(getImageSourceName(album.getSourceName()));
		    	out.writeUTF(album.getDataPath().toString());
		    	out.writeLong(album.getDateInMs());
		    	writeItems(out, album);
		    }
    	};
    	
    	return writer.saveCache(this);
    }
    
    @SuppressWarnings("unused")
    private boolean loadCache(JobContext jc, final List<DownloadAlbum> albums) { 
    	final FetchCache cache = mSource.getImageResource().getFetchCache();
    	
    	CacheReader reader = new CacheReader(getDataApp().getCacheData()) {
			@Override
			protected boolean read(DataInput in) throws IOException {
				String path = in.readUTF();
				String storagePath = in.readUTF();
				readItems(in, DownloadAlbumSet.this);
				return true;
			}

			@Override
			protected void readMediaItem(DataInput in, MediaSet set) throws IOException {
				String sourceName = in.readUTF();
				String storagePath = in.readUTF();
				long lastModified = in.readLong();
				
				if (set == null) return;
				
				IFile file = cache.getStorage().getFileSystem().getFile(new Path(storagePath));
    			if (file != null && file.exists() && file.isFile()) {
    				DownloadImage image = mSource.newImage(DownloadAlbumSet.this, file, sourceName);
    				if (set instanceof DownloadAlbum) {
    					DownloadAlbum album = (DownloadAlbum)set;
    					album.addMediaItem(image, file.lastModified());
    				}
    			}
			}

			@Override
			protected void readMediaSet(DataInput in, MediaSet set) throws IOException {
				String albumName = in.readUTF();
				String sourceName = in.readUTF();
				String dataPath = in.readUTF();
				long lastModified = in.readLong();
				
				set = null;
				
				if (!mAlbumPaths.contains(dataPath)) {
					DownloadAlbum album = mSource.newAlbum(DownloadAlbumSet.this, 
							dataPath, albumName, sourceName, lastModified);
					albums.add(album);
					set = album;
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("add Album: name=" + albumName + " sourceName=" 
								+ sourceName + " path=" + dataPath);
					}
				}
				
				readItems(in, set);
			}
			
			@Override
			protected boolean isInterrupt() { 
				return albums.size() > 20; 
			}
    	};
    	
    	return reader.readCache(this);
    }
    
    private void reloadAlbums(JobContext jc, ReloadCallback callback, 
    		List<DownloadAlbum> albums) {
    	FetchCache cache = mSource.getImageResource().getFetchCache();
    	try { 
    		callback.showProgressDialog(getDataApp().getContext()
    				.getString(R.string.dialog_downloadalbum_scanning_message));
    		
    		IFile[] files = cache.listFsFiles(mStoragePath);
    		Map<String,List<IFile>> albumDirs = new HashMap<String,List<IFile>>();
    		
    		for (int i=0; files != null && i < files.length; i++) { 
    			final IFile albumDir = files[i];
    			if (jc.isCancelled()) break;
    			if (albumDir != null && albumDir.isDirectory()) {
    				String albumPath = getAlbumPath(albumDir.getAbsolutePath());
    				
    				List<IFile> dirs = albumDirs.get(albumPath);
    				if (dirs == null ) { 
    					dirs = new ArrayList<IFile>();
    					albumDirs.put(albumPath, dirs);
    				}
    				
    				dirs.add(albumDir);
    			}
    		}
    		
    		for (Map.Entry<String, List<IFile>> entry : albumDirs.entrySet()) { 
    			String albumPath = entry.getKey();
    			List<IFile> dirs = entry.getValue();
    			if (albumPath == null || dirs == null) 
    				continue;
    			
    			listAlbumDir(jc, callback, cache, albums, albumPath, 
    					dirs.toArray(new IFile[dirs.size()]));
    		}
    	} catch (Throwable e) { 
    		if (LOG.isErrorEnabled())
    			LOG.error("load Albums error: " + e.toString(), e);
    		
    	} finally { 
    		callback.hideProgressDialog();
    	}
    }
    
    private static class AlbumDirItem { 
    	public final String mAlbumPath;
    	public final TreeSet<DownloadImage> mImageSet;
    	
    	public AlbumDirItem(String albumPath) { 
    		mAlbumPath = albumPath;
    		mImageSet = new TreeSet<DownloadImage>(
    			new Comparator<DownloadImage>() {
					@Override
					public int compare(DownloadImage lhs, DownloadImage rhs) {
						long lm = lhs.getDateInMs();
						long rm = rhs.getDateInMs();
						return lm == rm ? (lhs.getDataPath().compareTo(rhs.getDataPath())) 
								: (lm < rm ? 1 : -1);
					}
		    	});
    	}
    }
    
    private void listAlbumDir(JobContext jc, ReloadCallback callback, FetchCache cache, 
    		List<DownloadAlbum> albums, String albumPath, IFile... albumDirs) 
    		throws IOException { 
    	if (albumDirs == null) return;
    	
    	Map<String,AlbumDirItem> albumMap = new HashMap<String,AlbumDirItem>();
    	for (IFile albumDir : albumDirs) { 
    		if (LOG.isDebugEnabled()) {
    			LOG.debug("listAlbumFiles: albumPath=" + albumPath 
    					+ " dir=" + albumDir.getAbsolutePath());
    		}
    		
    		listAlbumFiles(jc, callback, cache, albumMap, albumPath, albumDir);
    	}
    	
    	for (Map.Entry<String,AlbumDirItem> entry : albumMap.entrySet()) { 
    		String albumName = entry.getKey();
    		AlbumDirItem albumItem = entry.getValue();
    		
	    	DownloadAlbum album = mSource.newAlbum(DownloadAlbumSet.this, 
	    			albumItem.mAlbumPath, albumName, 
	    			getImageSourceName(albumItem.mAlbumPath));
	    	
	    	for (DownloadImage image : albumItem.mImageSet) { 
	    		album.addMediaItem(image, image.getDateInMs());
	    	}
	    	
			if (album.getItemCount() > 0) 
				albums.add(album);
    	}
    }
    
    private void listAlbumFiles(JobContext jc, ReloadCallback callback, FetchCache cache, 
    		Map<String,AlbumDirItem> albumMap, String albumPath, IFile file) 
    		throws IOException { 
    	if (file == null || jc.isCancelled()) 
    		return;
    	
    	if (file.isDirectory()) {
    		if (jc.isCancelled()) return;
    		callback.showContentMessage(file.getPath());
    		
    		IFile[] subFiles = cache.listFsFiles(file);
    		
    		for (int i=0; subFiles != null && i < subFiles.length; i++) { 
    			IFile subFile = subFiles[i];
    			if (subFile == null || !subFile.exists()) continue;
    			if (jc.isCancelled()) return;
    			
    			if (subFile.isDirectory()) {
    				listAlbumFiles(jc, callback, cache, albumMap, albumPath, subFile);
    				continue;
    			}
    			
    			if (HttpCache.isDownloadImage(cache, subFile)) {
    				if (subFile.length() < 20480) {
    					if (LOG.isDebugEnabled()) {
    						LOG.debug("listAlbumFiles: ignore small file: " + subFile.getAbsolutePath() 
    								+ " length=" + subFile.length());
    					}
    					
    					if ((System.currentTimeMillis() - subFile.lastModified()) > 24 * 60 * 60 * 1000)
    						subFile.getFileSystem().delete(subFile);
    					
        				continue;
        			}
    				
    				DownloadImage image = mSource.newImage(DownloadAlbumSet.this, 
    						subFile, getImageSourceName(albumPath));
    				addImage(albumMap, albumPath, image);
    				
    				//if (LOG.isDebugEnabled()) {
					//	LOG.debug("listAlbumFiles: add image file: " + subFile.getAbsolutePath() 
					//			+ " length=" + subFile.length() + " albumPath=" + albumPath);
					//}
    				
    			} else { 
    				if (LOG.isDebugEnabled()) {
						LOG.debug("listAlbumFiles: ignore not-image file: " + subFile.getAbsolutePath() 
								+ " length=" + subFile.length());
					}
    				
    				if ((System.currentTimeMillis() - subFile.lastModified()) > 24 * 60 * 60 * 1000)
    					subFile.getFileSystem().delete(subFile);
    			}
    		}
    	}
    }
    
    private void addImage(Map<String,AlbumDirItem> albumMap, String albumPath, 
    		DownloadImage image) { 
    	String albumName = getAlbumName(image);
    	
    	AlbumDirItem albumItem = albumMap.get(albumName);
    	if (albumItem == null) { 
    		albumItem = new AlbumDirItem(albumPath);
    		albumMap.put(albumName, albumItem);
    	}
		
    	albumItem.mImageSet.add(image);
    }
    
    static String getAlbumPath(String albumPath) { 
    	if (albumPath == null) return albumPath;
    	
		if (albumPath.endsWith("/") || albumPath.endsWith("\\")) 
			albumPath = albumPath.substring(0, albumPath.length()-1);
		
		int pos1 = albumPath.lastIndexOf('/');
		int pos2 = albumPath.lastIndexOf('\\');
		
		int pos = pos1 > pos2 ? pos1 : pos2;
		if (pos > 0 && pos < albumPath.length() - 1) { 
			String path = albumPath.substring(0, pos+1);
			String name = albumPath.substring(pos+1);
			
			albumPath = path + SourceHelper.toSourceName(name);
		}
		
		return albumPath;
    }
    
    static String getImageSourceName(String albumPath) { 
    	String name = albumPath;
    	if (name != null) {
	    	int pos1 = name.lastIndexOf('/');
	    	int pos2 = name.lastIndexOf('\\');
	    	
	    	int pos = pos1 > pos2 ? pos1 : pos2;
	    	if (pos >= 0) 
	    		name = name.substring(pos+1);
    	}
    	
    	if (name == null) name = "";
    	return name;
    }
    
    static final SimpleDateFormat sFormater  = new SimpleDateFormat("yyyy-MM-dd"); 
    
    static String getAlbumName(DownloadImage image) { 
    	return sFormater.format(new Date(image.getDateInMs()));
    }
    
}
