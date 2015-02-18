package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.StorageHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.SortType;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.util.StringUtils;

public abstract class AnyboxSection implements ISectionData {
	private static final Logger LOG = Logger.getLogger(AnyboxSection.class);

	public static class SortData {
		private final String mName;
		private final String mTitle;
		private final String mSorted;
		
		public SortData(String name, String title, String sorted) {
			mName = name;
			mTitle = title;
			mSorted = sorted;
		}
		
		public String getName() { return mName; }
		public String getTitle() { return mTitle; }
		public String getSorted() { return mSorted; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{name=" + mName 
					+ ",sorted=" + mSorted + "}";
		}
	}
	
	public static class SubItemData {
		private final String mId;
		private final boolean mIsFolder;
		
		private String mName;
		private String mType;
		private String mExtname;
		private String mPoster;
		private String mBackground;
		private String mOwner;
		private String mChecksum;
		private long mModifiedTime;
		private long mLength;
		private int mWidth;
		private int mHeight;
		private long mTimeLen;
		
		public SubItemData(String id, boolean isfolder) {
			mId = id;
			mIsFolder = isfolder;
		}
		
		public String getId() { return mId; }
		public String getName() { return mName; }
		public boolean isFolder() { return mIsFolder; }
		public String getType() { return mType; }
		public String getExtension() { return mExtname; }
		public String getPoster() { return mPoster; }
		public String getBackground() { return mBackground; }
		public String getOwner() { return mOwner; }
		public String getChecksum() { return mChecksum; }
		public long getModifiedTime() { return mModifiedTime; }
		public long getLength() { return mLength; }
		public int getWidth() { return mWidth; }
		public int getHeight() { return mHeight; }
		public long getTimeLength() { return mTimeLen; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{id=" + mId + ",name=" + mName + "}";
		}
	}
	
	private final AnyboxLibrary mLibrary;
	//private final AnyboxData mData;
	private final ISectionFolder mParent;
	private final long mRequestTime;
	
	private final String mId;
	private final boolean mIsFolder;
	
	private SubItemData[] mSubItems;
	
	private String mName;
	private String mType;
	private String mExtname;
	private String mPath;
	private String mPoster;
	private String mBackground;
	private String mOwner;
	private String mChecksum;
	private long mModifiedTime;
	private long mLength;
	private int mWidth;
	private int mHeight;
	private long mTimeLen;
	private int mSubCount;
	private long mSubLen;
	
	public AnyboxSection(AnyboxLibrary library, 
			ISectionFolder parent, AnyboxData data, String id, 
			boolean isfolder, long requestTime) {
		if (library == null) throw new NullPointerException();
		mLibrary = library;
		//mData = data;
		mParent = parent;
		mId = id;
		mIsFolder = isfolder;
		mRequestTime = requestTime;
	}
	
	public AnyboxLibrary getLibrary() { return mLibrary; }
	public AnyboxAccount getUser() { return getLibrary().getUser(); }
	
	//public AnyboxData getData() { return mData; }
	public ISectionFolder getParent() { return mParent; }
	public long getRequestTime() { return mRequestTime; }
	
	public String getId() { return mId; }
	public String getName() { return mName; }
	public String getDisplayName() { return mName; }
	public boolean isFolder() { return mIsFolder; }
	public String getType() { return mType; }
	public String getExtension() { return mExtname; }
	public String getPath() { return mPath; }
	public String getPoster() { return mPoster; }
	public String getBackground() { return mBackground; }
	public String getOwner() { return mOwner; }
	public String getChecksum() { return mChecksum; }
	public long getModifiedTime() { return mModifiedTime; }
	public long getLength() { return mLength; }
	public int getWidth() { return mWidth; }
	public int getHeight() { return mHeight; }
	public long getTimeLength() { return mTimeLen; }
	public int getSubCount() { return mSubCount; }
	public long getSubLength() { return mSubLen; }
	
	public SubItemData[] getSubItems() { return mSubItems; }
	public FileOperation getOperation() { return null; }
	
	public long getRefreshTime() { return getRequestTime(); }
	public Drawable getTypeIcon() { return getSectionIcon(this); }
	public String getBackgroundURL() { return null; }
	
	public String getTitle() { return getName(); }
	public String getContentId() { return getId(); }
	public String getContentType() { return getType(); }
	public String getContentName() { return getName(); }
	public Uri getContentUri() { return null; }
	
	private String getPosterId() {
		String posterId = getPoster();
		if (posterId == null || posterId.length() == 0) {
			String type = getType();
			if (type != null && type.startsWith("image/")) 
				posterId = getId();
		}
		return posterId;
	}
	
	private String getPosterExt() {
		String posterId = getPoster();
		if (posterId == null || posterId.length() == 0) {
			String type = getType();
			if (type != null && type.startsWith("image/"))
				return getExtension();
		}
		return null;
	}
	
	public AnyboxHelper.IRequestWrapper getRequestWrapper() {
		return getLibrary().getRequestWrapper();
	}
	
	public String getPhotoURL() {
		return toImageLargeURL(getRequestWrapper(), getPosterId(), getPosterExt());
	}
	
	public String getPosterURL() {
		return toImagePosterURL(getRequestWrapper(), getPosterId(), getPosterExt());
	}
	
	public String getPosterThumbnailURL() {
		return toImageThumbnailURL(getRequestWrapper(), getPosterId(), getPosterExt());
	}
	
	public static String toImageLargeURL(AnyboxHelper.IRequestWrapper wrapper, 
			String imageId, String imageExt) {
		if (imageId != null && imageId.length() > 0) 
			return AnyboxHelper.getImageURL(wrapper, imageId, "4096", imageExt);
		return null;
	}
	
	public static String toImagePosterURL(AnyboxHelper.IRequestWrapper wrapper, 
			String imageId, String imageExt) {
		if (imageId != null && imageId.length() > 0) 
			return AnyboxHelper.getImageURL(wrapper, imageId, "1024", imageExt);
		return null;
	}
	
	public static String toImageThumbnailURL(AnyboxHelper.IRequestWrapper wrapper, 
			String imageId, String imageExt) {
		if (imageId != null && imageId.length() > 0) 
			return AnyboxHelper.getImageURL(wrapper, imageId, "256", imageExt);
		return null;
	}
	
	public String getSizeInfo() {
		String text = AppResources.getInstance().formatReadableBytes(getLength());
		String moretxt = null;
		
		int width = getWidth();
		int height = getHeight();
		if (width > 0 && height > 0) {
			moretxt = "" + width + "x" + height;
		} else {
			long timelen = getTimeLength();
			if (timelen > 0) 
				moretxt = AppResources.getInstance().formatDuration(timelen);
		}
		
		if (moretxt != null && moretxt.length() > 0)
			text += "(" + moretxt + ")";
		
		return text;
	}
	
	public String getSizeDetails() {
		return AppResources.getInstance().formatDetailsBytes(getLength());
	}
	
	public boolean supportOperation(FileOperation.Operation op) {
		return AnyboxHelper.supportOperation(this, op);
	}
	
	public boolean isAccountOwner() {
		String owner = getOwner();
		if (owner == null || owner.length() == 0 || 
			owner.equals(getUser().getAccountName())) {
			return true;
		}
		return false;
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
	
	public void getExifs(IMediaDetails details) {
		if (details == null) return;
		
		AnyboxProperty property = getUser().getApp().getSectionProperty(getId());
		if (property != null) property.getExifs(details);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id=" + getId() 
				+ ",name=" + getName() + ",type=" + getType() + "}";
	}
	
	static Drawable getSectionIcon(ISectionInfoData data) {
		if (data == null) return null;
		//if (data instanceof ILibraryData) return StorageHelper.getLibraryTypeIcon(data.getType());
		if (data.isFolder()) return StorageHelper.getFolderTypeIcon(data.getType());
		return StorageHelper.getFileTypeIcon(data.getName(), 
				data.getType(), data.getExtension());
	}
	
	static AnyboxSection[] loadSections(AnyboxLibrary library, 
			ISectionFolder parent, AnyboxData data) throws IOException {
		if (parent == null || data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadSections: data=" + data);
		
		String[] names = data.getNames();
		if (names != null) {
			ArrayList<AnyboxSection> list = new ArrayList<AnyboxSection>();
			for (String name : names) {
				AnyboxSection item = AnyboxSection.loadSection(library, parent, data.get(name));
				if (item != null) list.add(item);
			}
			
			return list.toArray(new AnyboxSection[list.size()]);
		}
		
		return null;
	}
	
	static AnyboxSection.SortData[] loadSorts(AnyboxLibrary library, 
			ISectionFolder parent, AnyboxData[] datas) throws IOException {
		if (parent == null || datas == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadSorts: datas=" + datas);
		
		ArrayList<AnyboxSection.SortData> list = new ArrayList<AnyboxSection.SortData>();
		
		for (AnyboxData data : datas) {
			AnyboxSection.SortData item = AnyboxSection.loadSort(data);
			if (item != null) list.add(item);
		}
		
		return list.toArray(new SortData[list.size()]);
	}
	
	static AnyboxSection loadSection(AnyboxLibrary library, 
			ISectionFolder parent, AnyboxData data) throws IOException {
		if (library == null || data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadSection: data=" + data);
		
		String id = data.getString("id");
		boolean isfolder = data.getBool("isfolder", false);
		long requestTime = System.currentTimeMillis();
		
		AnyboxSection a = isfolder ? 
				new AnyboxSectionFolder(library, parent, data, id, isfolder, requestTime) :
				new AnyboxSectionFile(library, parent, data, id, isfolder, requestTime);
		
		a.mName = data.getString("name");
		a.mType = data.getString("type");
		a.mExtname = data.getString("extname");
		a.mPath = data.getString("path");
		a.mPoster = data.getString("poster");
		a.mBackground = data.getString("background");
		a.mOwner = data.getString("owner");
		a.mChecksum = data.getString("checksum");
		a.mModifiedTime = data.getLong("mtime", 0);
		a.mLength = data.getLong("length", 0);
		a.mWidth = data.getInt("width", 0);
		a.mHeight = data.getInt("height", 0);
		a.mTimeLen = data.getLong("timelen", 0);
		a.mSubCount = data.getInt("subcount", 0);
		a.mSubLen = data.getLong("sublen", 0);
		
		a.mSubItems = loadSubItems(data.get("subitems"));
		
		if (a instanceof AnyboxSectionFolder) {
			AnyboxSectionFolder af = (AnyboxSectionFolder)a;
			af.mItemOps = AnyboxHelper.createFolderOperation(af);
		}
		
		return a;
	}
	
	static SortData loadSort(AnyboxData data) throws IOException {
		if (data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadSort: data=" + data);
		
		String name = data.getString("name");
		String title = data.getString("title");
		String sorted = data.getString("sorted");
		
		if (name != null && name.length() > 0) 
			return new SortData(name, title, sorted);
		
		return null;
	}
	
	static SubItemData[] loadSubItems(AnyboxData data) throws IOException {
		if (data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadSubItems: data=" + data);
		
		String[] names = data.getNames();
		if (names != null) {
			ArrayList<SubItemData> list = new ArrayList<SubItemData>();
			for (String name : names) {
				SubItemData item = loadSubItem(data.get(name));
				if (item != null) list.add(item);
			}
			
			SubItemData[] items = list.toArray(new SubItemData[list.size()]);
			Arrays.sort(items, new Comparator<SubItemData>() {
					@Override
					public int compare(SubItemData lhs, SubItemData rhs) {
						long llen = lhs.getLength();
						long rlen = rhs.getLength();
						if (llen > rlen) return -1;
						else if (llen < rlen) return 1;
						
						int lsize = lhs.getWidth() * lhs.getHeight();
						int rsize = rhs.getWidth() * rhs.getHeight();
						if (lsize > rsize) return -1;
						else if (lsize < rsize) return 1;
						
						return 0;
					}
				});
			
			return items;
		}
		
		return null;
	}
	
	static SubItemData loadSubItem(AnyboxData data) throws IOException {
		if (data == null) return null;
		if (LOG.isDebugEnabled()) LOG.debug("loadSubItem: data=" + data);
		
		String id = data.getString("id");
		boolean isfolder = data.getBool("isfolder", false);
		
		SubItemData a = new SubItemData(id, isfolder);
		
		a.mName = data.getString("name");
		a.mType = data.getString("type");
		a.mExtname = data.getString("extname");
		a.mPoster = data.getString("poster");
		a.mBackground = data.getString("background");
		a.mOwner = data.getString("owner");
		a.mChecksum = data.getString("checksum");
		a.mModifiedTime = data.getLong("mtime", 0);
		a.mLength = data.getLong("length", 0);
		a.mWidth = data.getInt("width", 0);
		a.mHeight = data.getInt("height", 0);
		a.mTimeLen = data.getLong("timelen", 0);
		
		return a;
	}
	
	public static void reloadSectionList(AnyboxSectionFolder section, 
			ProviderCallback callback, long reloadId, 
			SortType.Type sort, FilterType.Type filter) {
		if (section == null || !section.isFolder()) return;
		if (section.getSectionReloadId() == reloadId) {
			if (LOG.isDebugEnabled())
				LOG.debug("reloadSectionList: already loaded for same reloadId: " + reloadId);
			return;
		}
		
		String sorted = AnyboxSectionSet.getSortTypeString(sort);
		String filtered = AnyboxSectionSet.getFilterTypeString(filter);
		
		section.mSortType = sort;
		section.mFilterType = filter;
		
		getSectionList(section, callback, reloadId, 
				sorted, filtered, true, 0, 50);
	}
	
	public static AnyboxSectionSet reloadSectionListNext(AnyboxSectionFolder section, 
			ProviderCallback callback, long reloadId, FilterType.Type filter) {
		if (section == null || !section.isFolder()) return null;
		if (section.getSectionReloadId() == reloadId) {
			if (LOG.isDebugEnabled())
				LOG.debug("reloadSectionListNext: already loaded for same reloadId: " + reloadId);
			return null;
		}
		
		int listfrom = 0;
		String sorted = null;
		String filtered = AnyboxSectionSet.getFilterTypeString(filter);
		
		AnyboxSectionSet last = section.getSectionSetAt(section.getSectionSetCount() -1);
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
		
		return getSectionList(section, callback, reloadId, 
				sorted, filtered, true, listfrom, 50);
	}
	
	static AnyboxSectionSet getSectionList(AnyboxSectionFolder section, 
			ProviderCallback callback, long reloadId, String sort, String filtertype, 
			boolean byfolder, int from, int count) {
		if (section == null || !section.isFolder()) return null;
		//final AnyboxAccount user = section.getUser();
		
		if (sort == null) sort = "";
		if (filtertype == null) filtertype = "";
		
		String url = section.getRequestAddr() //user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/datum/section?wt=secretjson&subfilecount=3&q=" 
				+ "&filtertype=" + StringUtils.URLEncode(filtertype)
				+ "&sort=" + StringUtils.URLEncode(sort) + "&byfolder=" + byfolder 
				+ "&from=" + from + "&count=" + count 
				+ "&id=" + StringUtils.URLEncode(section.getId()) 
				+ "&token=" + StringUtils.URLEncode(section.getRequestToken()); //user.getAuthToken());
		
		SectionListener listener = new SectionListener(reloadId);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			AnyboxData data = listener.mData;
			if (data != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					error = null;
					return section.loadSectionSet(data, reloadId);
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
	
	static class SectionListener extends AnyboxApi.SecretJSONListener {
		private final long mReloadId;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public SectionListener(long reloadId) {
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
