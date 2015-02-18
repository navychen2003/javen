package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.SortType;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.CategoryData;
import org.javenstudio.provider.library.ICategoryData;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.IThumbnailCallback;
import org.javenstudio.provider.library.IVisibleData;
import org.javenstudio.provider.library.SectionHelper;

public class AnyboxSectionFolder extends AnyboxSection 
		implements ISectionFolder, AnyboxHelper.IAnyboxFolder {
	private static final Logger LOG = Logger.getLogger(AnyboxSectionFolder.class);

	private final ArrayList<AnyboxSectionSet> mSectionList = 
			new ArrayList<AnyboxSectionSet>();
	
	private IVisibleData mFirstVisibleItem = null;
	private ICategoryData mFileCategoryData = null;
	private ICategoryData mFolderCategoryData = null;
	
	private long mSectionRequestTime = 0;
	protected SortType.Type mSortType = null;
	protected FilterType.Type mFilterType = null;
	
	protected FileOperation mSubFileOps;
	protected FileOperation mSubFolderOps;
	protected FileOperation mItemOps;
	
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
	
	public AnyboxSectionFolder(AnyboxLibrary library, 
			ISectionFolder parent, AnyboxData data, String id, 
			boolean isfolder, long requestTime) {
		super(library, parent, data, id, isfolder, requestTime);
	}
	
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
	
	public FileOperation getSubFileOps() { return mSubFileOps; }
	public FileOperation getSubFolderOps() { return mSubFolderOps; }
	public FileOperation getOperation() { return mItemOps; }
	
	public String getOwner() { return getUser().getAccountName(); }
	public String getSizeInfo() { return SectionHelper.getFolderCountInfo(this); }
	public String getSizeDetails() { return getSizeInfo(); }
	
	public String getDisplayName() {
		return getName() + " (" + getLibrary().getHostName() + ")";
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
	
	public boolean isRecycleBin() {
		String type = getType();
		if (type != null && type.indexOf("recycle") >= 0) return true;
		return false; 
	}
	
	public boolean isSearchResult() { return false; }
	
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
	
	public boolean shouldReload(SortType.Type sort, FilterType.Type filter) {
		if (getRefreshTime() <= 0 || sort != mSortType || filter != mFilterType)
			return true;
		return false;
	}
	
	public String getRequestAddr() {
		return getLibrary().getRequestAddr(); //getUser().getApp().getRequestAddr(getUser().getHostData(), false);
	}
	
	public String getRequestToken() {
		return getLibrary().getRequestToken(); //getUser().getAuthToken();
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
	
	static class PosterItem {
		private final String mPosterId;
		private final String mExtension;
		private final int mImageWidth;
		private final int mImageHeight;
		
		public PosterItem(String id, String ext, 
				int width, int height) {
			mPosterId = id;
			mExtension = ext;
			mImageWidth = width;
			mImageHeight = height;
		}
		
		public String getPosterId() { return mPosterId; }
		public String getExtension() { return mExtension; }
		public int getImageWidth() { return mImageWidth; }
		public int getImageHeight() { return mImageHeight; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{id=" + getPosterId() 
					+ ",width=" + getImageWidth() + ",height=" + getImageHeight() 
					+ ",extension=" + getExtension() + "}";
		}
	}
	
	private PosterItem mPosterItem = null;
	private PosterItem[] mSubPosterItems = null;
	
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
		
		ArrayList<PosterItem> sublist = new ArrayList<PosterItem>();
		SubItemData[] subitems = getSubItems();
		
		if (subitems != null) {
			for (int i=0; i < subitems.length; i++) {
				SubItemData subitem = subitems[i];
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
					PosterItem pitem = new PosterItem(subitem.getId(), 
							subitem.getExtension(), subitem.getWidth(), subitem.getHeight());
					sublist.add(pitem);
					
				} else if (subposter != null && subposter.length() > 0) {
					PosterItem pitem = new PosterItem(subposter, null, 0, 0);
					sublist.add(pitem);
				}
			}
		}
		
		mPosterItem = new PosterItem(posterId, posterExt, posterWidth, posterHeight);
		mSubPosterItems = sublist.toArray(new PosterItem[sublist.size()]);
	}
	
	@Override
	public String getPhotoURL() {
		return getPosterURL();
	}
	
	@Override
	public String getPosterURL() {
		initPosterItems();
		PosterItem item = mPosterItem;
		if (item != null) { 
			return toImagePosterURL(getRequestWrapper(), 
					item.getPosterId(), item.getExtension());
		}
		return null;
	}
	
	@Override
	public String getPosterThumbnailURL() {
		initPosterItems();
		PosterItem item = mPosterItem;
		if (item != null) { 
			return toImageThumbnailURL(getRequestWrapper(), 
					item.getPosterId(), item.getExtension());
		}
		return null;
	}
	
	@Override
	public void getSectionThumbnails(IThumbnailCallback cb) {
		if (cb == null) return;
		
		initPosterItems();
		PosterItem[] items = mSubPosterItems;
		
		if (items != null) {
			for (PosterItem item : items) {
				if (item == null) continue;
				String imageURL = toImageThumbnailURL(getRequestWrapper(), 
						item.getPosterId(), item.getExtension());
				
				if (LOG.isDebugEnabled())
					LOG.debug("getSectionThumbnails: item=" + item + " imageURL=" + imageURL);
				
				if (cb.onThumbnail(imageURL, item.getImageWidth(), item.getImageHeight()) == false)
					return;
			}
		}
	}
	
}
