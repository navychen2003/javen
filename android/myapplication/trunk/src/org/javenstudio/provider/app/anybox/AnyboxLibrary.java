package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.SortType;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.library.CategoryData;
import org.javenstudio.provider.library.ICategoryData;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.IThumbnailCallback;
import org.javenstudio.provider.library.IVisibleData;
import org.javenstudio.provider.library.SectionHelper;
import org.javenstudio.provider.library.select.ISelectData;
import org.javenstudio.util.StringUtils;

public class AnyboxLibrary implements ILibraryData, ISelectData, 
		AnyboxHelper.IAnyboxFolder {
	private static final Logger LOG = Logger.getLogger(AnyboxLibrary.class);

	private final AnyboxAccount mUser;
	//private final AnyboxData mData;
	private final long mRequestTime;
	
	private final ArrayList<AnyboxSectionSet> mSectionList = 
			new ArrayList<AnyboxSectionSet>();
	
	private IVisibleData mFirstVisibleItem = null;
	private ICategoryData mFileCategoryData = null;
	private ICategoryData mFolderCategoryData = null;
	
	//private AnyboxSectionSearch mSectionSearch = null;
	private long mSectionRequestTime = 0;
	private SortType.Type mSortType = null;
	private FilterType.Type mFilterType = null;
	
	private AnyboxSection.SubItemData[] mSubItems;
	private FileOperation mSubFileOps;
	private FileOperation mSubFolderOps;
	
	private final String mId;
	private String mName;
	private String mHostName;
	private String mType;
	private String mPoster;
	private String mBackground;
	private long mCreatedTime;
	private long mModifiedTime;
	private long mIndexedTime;
	private int mSubCount;
	private long mSubLen;
	
	private String mHostname;
	private String mPerms;
	private String mOps;
	private String mParentId;
	private String mParentName;
	private String mParentType;
	private String mRootId;
	private String mRootName;
	private String mRootType;
	private String mLibraryId;
	private String mLibraryName;
	private String mLibraryType;
	
	public AnyboxLibrary(AnyboxAccount user, 
			AnyboxData data, String id, long requestTime) {
		if (user == null) throw new NullPointerException();
		mUser = user;
		//mData = data;
		mId = id;
		mRequestTime = requestTime;
	}
	
	public AnyboxAccount getUser() { return mUser; }
	//public AnyboxData getData() { return mData; }
	public long getRequestTime() { return mRequestTime; }
	
	public String getId() { return mId; }
	public String getName() { return mName; }
	public String getHostName() { return mHostName; }
	public String getType() { return mType; }
	public String getExtension() { return null; }
	public String getPoster() { return mPoster; }
	public String getBackground() { return mBackground; }
	public long getCreatedTime() { return mCreatedTime; }
	public long getModifiedTime() { return mModifiedTime; }
	public long getIndexedTime() { return mIndexedTime; }
	public int getSubCount() { return mSubCount; }
	public long getSubLength() { return mSubLen; }
	
	public String getHostname() { return mHostname; }
	public String getPerms() { return mPerms; }
	public String getOps() { return mOps; }
	public String getParentId() { return mParentId; }
	public String getParentName() { return mParentName; }
	public String getParentType() { return mParentType; }
	public String getRootId() { return mRootId; }
	public String getRootName() { return mRootName; }
	public String getRootType() { return mRootType; }
	public String getLibraryId() { return mLibraryId; }
	public String getLibraryName() { return mLibraryName; }
	public String getLibraryType() { return mLibraryType; }
	
	public AnyboxLibrary getLibrary() { return this; }
	public ISectionFolder getParent() { return null; }
	public FileOperation getSubFileOps() { return mSubFileOps; }
	public FileOperation getSubFolderOps() { return mSubFolderOps; }
	public String getPath() { return "/"; }
	public String getChecksum() { return null; }
	
	public boolean isFolder() { return true; }
	public boolean isRecycleBin() { return false; }
	public boolean isSearchResult() { return false; }
	
	public int getWidth() { return 0; }
	public int getHeight() { return 0; }
	public long getLength() { return 0; }
	
	public String getOwner() { return getUser().getAccountName(); }
	public String getSizeInfo() { return SectionHelper.getFolderCountInfo(this); }
	public String getSizeDetails() { return getSizeInfo(); }
	
	public String getDisplayName() {
		return getName() + " (" + getHostName() + ")";
	}
	
	public AnyboxHelper.IRequestWrapper getRequestWrapper() {
		return getUser();
	}
	
	public IVisibleData getFirstVisibleItem() { 
		IVisibleData item = mFirstVisibleItem; 
		if (LOG.isDebugEnabled()) LOG.debug("getFirstVisibleItem: item=" + item);
		return item;
	}
	
	public void setFirstVisibleItem(IVisibleData item) { 
		if (item == null || item.getParent() != this) return;
		if (LOG.isDebugEnabled()) LOG.debug("setFirstVisibleItem: item=" + item);
		mFirstVisibleItem = item; 
	}
	
	public synchronized ICategoryData getFileCategory() {
		if (mFileCategoryData == null) 
			mFileCategoryData = CategoryData.createFileCategory(this);
		return mFileCategoryData;
	}
	
	public synchronized ICategoryData getFolderCategory() {
		if (mFolderCategoryData == null) 
			mFolderCategoryData = CategoryData.createFolderCategory(this);
		return mFolderCategoryData;
	}
	
	public AnyboxSectionSearch[] getSectionSearches() {
		return getUser().getSearchList().getSearches();
	}
	
	public AnyboxSectionSearch getLastSearch() {
		return getUser().getSearchList().getLast();
	}
	
	public AnyboxSectionSearch getSectionSearch(String query) {
		if (query == null || query.length() == 0) return null;
		AnyboxSectionSearch search = getUser().getSearchList().getSearch(query);
		if (search == null) {
			search = new AnyboxSectionSearch(getUser().getSearchList(), 
					this, query, System.currentTimeMillis());
			getUser().getSearchList().putSearch(search);
		}
		return search;
	}
	
	public Drawable getTypeIcon() { 
		return AnyboxSection.getSectionIcon(this); 
	}
	
	public void getDetails(IMediaDetails details) {
		if (details == null) return;
		
		ISectionData data = this;
		String owner = getOwner();
		if (owner == null || owner.length() == 0)
			owner = getUser().getAccountName();
		
		details.add(R.string.details_filename, data.getName());
		details.add(R.string.details_filepath, data.getPath());
		details.add(R.string.details_contenttype, data.getType());
		details.add(R.string.details_owner, owner);
		details.add(R.string.details_filesize, data.getSizeDetails());
		details.add(R.string.details_lastmodified, 
				AppResources.getInstance().formatReadableTime(data.getModifiedTime()));
		details.add(R.string.details_checksum, data.getChecksum());
	}
	
	public Uri getContentUri() { return null; }
	public void getExifs(IMediaDetails details) {}
	
	public AnyboxSection.SubItemData[] getSubItems() { return mSubItems; }
	public String getBackgroundURL() { return null; }
	
	private AnyboxSectionFolder.PosterItem mPosterItem = null;
	private AnyboxSectionFolder.PosterItem[] mSubPosterItems = null;
	
	private synchronized void initPosterItems() {
		if (mPosterItem != null || mSubPosterItems != null) return;
		if (LOG.isDebugEnabled()) LOG.debug("initPosterItems: folder=" + this);
		
		String posterId = getPoster();
		String posterExt = null;
		int posterWidth = 0;
		int posterHeight = 0;
		
		if (posterId == null || posterId.length() == 0) {
			String type = getType();
			if (type != null && type.startsWith("image/")) {
				posterId = getId();
				posterExt = getExtension();
				posterWidth = getWidth();
				posterHeight = getHeight();
			}
		}
		
		ArrayList<AnyboxSectionFolder.PosterItem> sublist = 
				new ArrayList<AnyboxSectionFolder.PosterItem>();
		AnyboxSection.SubItemData[] subitems = getSubItems();
		
		if (subitems != null) {
			for (int i=0; i < subitems.length; i++) {
				AnyboxSection.SubItemData subitem = subitems[i];
				if (subitem == null) continue;
				
				String subtype = subitem.getType();
				String subposter = subitem.getPoster();
				
				if (posterId == null || posterId.length() == 0) {
					if (subtype != null && subtype.startsWith("image/")) {
						posterId = subitem.getId();
						posterExt = subitem.getExtension();
						posterWidth = subitem.getWidth();
						posterHeight = subitem.getHeight();
						continue;
						
					} else if (subposter != null && subposter.length() > 0) {
						posterId = subposter;
						posterExt = null;
						posterWidth = 0;
						posterHeight = 0;
						continue;
					}
				}
				
				if (subtype != null && subtype.startsWith("image/")) {
					AnyboxSectionFolder.PosterItem pitem = 
							new AnyboxSectionFolder.PosterItem(subitem.getId(), 
									subitem.getExtension(), subitem.getWidth(), subitem.getHeight());
					sublist.add(pitem);
					
				} else if (subposter != null && subposter.length() > 0) {
					AnyboxSectionFolder.PosterItem pitem = 
							new AnyboxSectionFolder.PosterItem(subposter, null, 0, 0);
					sublist.add(pitem);
				}
			}
		}
		
		mPosterItem = new AnyboxSectionFolder.PosterItem(posterId, 
				posterExt, posterWidth, posterHeight);
		mSubPosterItems = sublist.toArray(
				new AnyboxSectionFolder.PosterItem[sublist.size()]);
	}
	
	@Override
	public String getPhotoURL() {
		return getPosterURL();
	}
	
	@Override
	public String getPosterURL() {
		initPosterItems();
		AnyboxSectionFolder.PosterItem item = mPosterItem;
		if (item != null) {
			return AnyboxSection.toImagePosterURL(getRequestWrapper(), 
					item.getPosterId(), item.getExtension());
		}
		return null;
	}
	
	@Override
	public String getPosterThumbnailURL() {
		initPosterItems();
		AnyboxSectionFolder.PosterItem item = mPosterItem;
		if (item != null) { 
			return AnyboxSection.toImageThumbnailURL(getRequestWrapper(), 
					item.getPosterId(), item.getExtension());
		}
		return null;
	}
	
	@Override
	public void getSectionThumbnails(IThumbnailCallback cb) {
		if (cb == null) return;
		
		initPosterItems();
		AnyboxSectionFolder.PosterItem[] items = mSubPosterItems;
		
		if (items != null) {
			for (AnyboxSectionFolder.PosterItem item : items) {
				if (item == null) continue;
				String imageURL = AnyboxSection.toImageThumbnailURL(getRequestWrapper(), 
						item.getPosterId(), item.getExtension());
				
				if (LOG.isDebugEnabled())
					LOG.debug("getSectionThumbnails: item=" + item + " imageURL=" + imageURL);
				
				if (cb.onThumbnail(imageURL, item.getImageWidth(), item.getImageHeight()) == false)
					return;
			}
		}
	}
	
	public boolean supportOperation(FileOperation.Operation op) {
		return AnyboxHelper.supportOperation(this, op);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id=" + getId() 
				+ ",name=" + getName() + ",type=" + getType() + "}";
	}
	
	static void loadLibraries(AnyboxAccount user, AnyboxData data) throws IOException {
		if (user == null || data == null) return;
		AnyboxLibrary[] libraries = loadLibraries0(user, data.get("libraries"), null);
		user.setLibraries(libraries);
		user.setLibraryRequestTime(System.currentTimeMillis());
	}
	
	static interface ILibraryFactory {
		public AnyboxLibrary create(AnyboxAccount user, 
				AnyboxData data, String id, long requestTime);
	}
	
	static AnyboxLibrary[] loadLibraries0(AnyboxAccount user, 
			AnyboxData data, ILibraryFactory factory) throws IOException {
		if (user == null || data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadLibraries0: data=" + data);
		
		ArrayList<AnyboxLibrary> list = new ArrayList<AnyboxLibrary>();
		String[] names = data.getNames();
		if (names != null) {
			for (String name : names) {
				AnyboxLibrary item = loadLibrary(user, data.get(name), factory);
				if (item != null) list.add(item);
			}
		}
		
		return list.toArray(new AnyboxLibrary[list.size()]);
	}
	
	private static AnyboxLibrary loadLibrary(AnyboxAccount user, 
			AnyboxData data, ILibraryFactory factory) throws IOException {
		if (user == null || data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadLibrary: data=" + data);
		
		String key = data.getString("id");
		AnyboxLibrary a = factory != null ? 
				factory.create(user, data, key, System.currentTimeMillis()) :
				new AnyboxLibrary(user, data, key, System.currentTimeMillis());
		
		a.mName = data.getString("name");
		a.mHostName = data.getString("hostname");
		a.mType = data.getString("type");
		a.mPoster = data.getString("poster");
		a.mBackground = data.getString("background");
		a.mCreatedTime = data.getLong("ctime", 0);
		a.mModifiedTime = data.getLong("mtime", 0);
		a.mIndexedTime = data.getLong("itime", 0);
		a.mSubCount = data.getInt("subcount", 0);
		a.mSubLen = data.getLong("sublen", 0);
		
		a.mSubItems = AnyboxSection.loadSubItems(data.get("sections"));
		
		return a;
	}
	
	void addSectionSet(AnyboxSectionSet sectionSet) {
		if (sectionSet == null) return;
		synchronized (mSectionList) {
			for (AnyboxSectionSet set : mSectionList) {
				if (set == sectionSet) return;
			}
			mSectionList.add(sectionSet);
		}
	}
	
	@Override
	public void clearSectionSet() {
		synchronized (mSectionList) {
			mSectionList.clear();
			mSectionRequestTime = 0;
		}
	}
	
	public AnyboxSectionSet[] getSectionList() {
		synchronized (mSectionList) {
			return mSectionList.toArray(new AnyboxSectionSet[mSectionList.size()]);
		}
	}
	
	public int getSectionSetCount() {
		synchronized (mSectionList) {
			return mSectionList.size();
		}
	}
	
	public AnyboxSectionSet getSectionSetAt(int index) {
		synchronized (mSectionList) {
			if (index >= 0 && index < mSectionList.size())
				return mSectionList.get(index);
			return null;
		}
	}
	
	public long getSectionReloadId() {
		synchronized (mSectionList) {
			if (mSectionList.size() > 0) {
				AnyboxSectionSet set = mSectionList.get(mSectionList.size()-1);
				if (set != null) return set.getReloadId();
			}
			return 0;
		}
	}
	
	public long getRefreshTime() {
		synchronized (mSectionList) {
			if (mSectionList.size() > 0) {
				AnyboxSectionSet set = mSectionList.get(mSectionList.size()-1);
				if (set != null) return set.getRequestTime();
			}
			return mSectionRequestTime;
		}
	}
	
	public int getTotalCount() {
		AnyboxSectionSet set = getSectionSetAt(0);
		if (set != null) return set.getTotalCount();
		return getSubCount();
	}
	
	public AnyboxSectionSet loadSectionSet(AnyboxData data, 
			long reloadId) throws IOException {
		if (data == null) return null;

		AnyboxSectionSet set = AnyboxSectionSet.loadSectionSet(
				getLibrary(), this, data, reloadId);
		
		if (set != null) { 
			if (set.getSectionFrom() <= 0) clearSectionSet();
			addSectionSet(set);
		}
		
		mHostname = data.getString("hostname");
		mPerms = data.getString("perms");
		mOps = data.getString("ops");
		mParentId = data.getString("parent_id");
		mParentName = data.getString("parent_name");
		mParentType = data.getString("parent_type");
		mRootId = data.getString("root_id");
		mRootName = data.getString("root_name");
		mRootType = data.getString("root_type");
		mLibraryId = data.getString("library_id");
		mLibraryName = data.getString("library_name");
		mLibraryType = data.getString("library_type");
		
		mSectionRequestTime = System.currentTimeMillis();
		mSubFileOps = AnyboxHelper.createSubFileOperation(this);
		mSubFolderOps = AnyboxHelper.createSubFolderOperation(this);
		
		return set;
	}
	
	public boolean shouldReload(SortType.Type sort, FilterType.Type filter) {
		if (getRefreshTime() <= 0 || sort != mSortType || filter != mFilterType)
			return true;
		return false;
	}
	
	public static void reloadSectionList(AnyboxLibrary library, 
			ProviderCallback callback, long reloadId, 
			SortType.Type sort, FilterType.Type filter) {
		if (library == null) return;
		if (library.getSectionReloadId() == reloadId) {
			if (LOG.isDebugEnabled())
				LOG.debug("reloadSectionList: already loaded for same reloadId: " + reloadId);
			return;
		}
		
		String sorted = AnyboxSectionSet.getSortTypeString(sort);
		String filtered = AnyboxSectionSet.getFilterTypeString(filter);
		
		library.mSortType = sort;
		library.mFilterType = filter;
		
		getSectionList(library, callback, reloadId, 
				sorted, filtered, true, 0, 50);
	}
	
	public static AnyboxSectionSet reloadSectionListNext(AnyboxLibrary library, 
			ProviderCallback callback, long reloadId, FilterType.Type filter) {
		if (library == null || !library.isFolder()) return null;
		if (library.getSectionReloadId() == reloadId) {
			if (LOG.isDebugEnabled())
				LOG.debug("reloadSectionListNext: already loaded for same reloadId: " + reloadId);
			return null;
		}
		
		int listfrom = 0;
		String sorted = null;
		String filtered = AnyboxSectionSet.getFilterTypeString(filter);
		
		AnyboxSectionSet last = library.getSectionSetAt(library.getSectionSetCount() -1);
		if (last != null) {
			sorted = last.getSorted();
			
			int from = last.getSectionFrom();
			int count = last.getSectionCount();
			if (from >= 0 && count >= 0) {
				listfrom = from + count;
				if (listfrom >= last.getTotalCount())
					return null;
			}
		}
		
		return getSectionList(library, callback, reloadId, 
				sorted, filtered, true, listfrom, 50);
	}
	
	public String getRequestAddr() {
		return getUser().getApp().getRequestAddr(getUser().getHostData(), false);
	}
	
	public String getRequestToken() {
		return getUser().getAuthToken();
	}
	
	static AnyboxSectionSet getSectionList(AnyboxLibrary library, 
			ProviderCallback callback, long reloadId, String sort, String filtertype,
			boolean byfolder, int from, int count) {
		if (library == null) return null;
		//final AnyboxAccount user = library.getUser();
		
		if (sort == null) sort = "";
		if (filtertype == null) filtertype = "";
		
		String url = library.getRequestAddr() //user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/datum/section?wt=secretjson&subfilecount=3&q=" 
				+ "&filtertype=" + StringUtils.URLEncode(filtertype)
				+ "&sort=" + StringUtils.URLEncode(sort) + "&byfolder=" + byfolder 
				+ "&from=" + from + "&count=" + count
				+ "&id=" + StringUtils.URLEncode(library.getId()) 
				+ "&token=" + StringUtils.URLEncode(library.getRequestToken()); //user.getAuthToken());
		
		LibraryListener listener = new LibraryListener(reloadId);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			AnyboxData data = listener.mData;
			if (data != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					error = null;
					return library.loadSectionSet(data, reloadId);
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isErrorEnabled())
				LOG.error("getSectionList: error: " + e, e);
		}
		
		if (callback != null && error != null) 
			callback.onActionError(error);
		
		return null;
	}
	
	static class LibraryListener extends AnyboxApi.SecretJSONListener {
		private final long mReloadId;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public LibraryListener(long reloadId) {
			mReloadId = reloadId;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			
			if (LOG.isDebugEnabled())
				LOG.debug("handleData: reloadId=" + mReloadId + " data=" + data);
			
			if (error != null) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("handleData: response error: " + error, 
							error.getException());
				}
			}
		}

		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.SECTION_LIST;
		}
	}
	
}
