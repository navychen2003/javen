package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.SortType;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.library.CategoryData;
import org.javenstudio.provider.library.ICategoryData;
import org.javenstudio.provider.library.ISectionSearch;
import org.javenstudio.provider.library.IThumbnailCallback;
import org.javenstudio.provider.library.IVisibleData;
import org.javenstudio.util.StringUtils;

public class AnyboxSectionSearch implements ISectionSearch {
	private static final Logger LOG = Logger.getLogger(AnyboxSectionSearch.class);

	public static class SearchList {
		private final int mMaxCount;
		
		private final TreeMap<String,AnyboxSectionSearch> mMap = 
			new TreeMap<String,AnyboxSectionSearch>();
		
		public SearchList(int maxCount) { 
			mMaxCount = maxCount > 0 ? maxCount : 4;
		}
		
		public AnyboxSectionSearch[] getSearches() {
			synchronized (mMap) {
				return mMap.values().toArray(new AnyboxSectionSearch[mMap.size()]);
			}
		}
		
		public AnyboxSectionSearch getSearch(String query) {
			if (query == null) return null;
			synchronized (mMap) {
				return mMap.get(query);
			}
		}
		
		public void putSearch(AnyboxSectionSearch search) {
			if (search == null) return;
			synchronized (mMap) {
				if (mMap.size() >= mMaxCount) {
					String removeKey = null;
					long oldest = 0;
					for (String key : mMap.keySet()) {
						AnyboxSectionSearch s = mMap.get(key);
						if (s != null) {
							if (oldest <= 0 || oldest > s.getRequestTime()) {
								removeKey = key;
								oldest = s.getRequestTime();
							}
						}
					}
					if (removeKey != null)
						mMap.remove(removeKey);
				}
				mMap.put(search.getQueryText(), search);
			}
		}
		
		public AnyboxSectionSearch getLast() {
			synchronized (mMap) {
				AnyboxSectionSearch last = null;
				long newest = 0;
				for (String key : mMap.keySet()) {
					AnyboxSectionSearch s = mMap.get(key);
					if (s != null) {
						if (newest <= 0 || newest < s.getRequestTime()) {
							last = s;
							newest = s.getRequestTime();
						}
					}
				}
				return last;
			}
		}
	}
	
	private final SearchList mSearchList;
	private final AnyboxLibrary mLibrary;
	private final String mQueryText;
	private final String mName;
	
	private final ArrayList<AnyboxSectionSet> mSectionList = 
			new ArrayList<AnyboxSectionSet>();
	
	private IVisibleData mFirstVisibleItem = null;
	private ICategoryData mFileCategoryData = null;
	private ICategoryData mFolderCategoryData = null;
	
	private long mRequestTime = 0;
	private long mSectionRequestTime = 0;
	private SortType.Type mSortType = null;
	private FilterType.Type mFilterType = null;
	
	public AnyboxSectionSearch(SearchList list, AnyboxLibrary library, 
			String query, long requestTime) {
		mSearchList = list;
		mLibrary = library;
		mQueryText = query;
		mRequestTime = requestTime;
		
		int nameRes = AppResources.getInstance().getStringRes(AppResources.string.section_search_name_title);
		if (nameRes == 0) nameRes = R.string.search_name_label;
		String text = AppResources.getInstance().getResources().getString(nameRes);
		
		if (query == null) query = "";
		mName = String.format(text, "\"" + query + "\"");
	}
	
	public AnyboxLibrary getLibrary() { return mLibrary; }
	public AnyboxAccount getUser() { return getLibrary().getUser(); }
	public long getRequestTime() { return mRequestTime; }
	
	public boolean isRecycleBin() { return false; }
	public boolean isSearchResult() { return true; }
	
	public AnyboxLibrary getParent() { return getLibrary(); }
	public String getName() { return mName; }
	public String getQueryText() { return mQueryText; }
	
	public ISectionSearch[] getSearches() { return mSearchList.getSearches(); }
	public String getType() { return "application/x-search"; }
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{query=" + mQueryText + "}";
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
		return 0;
	}
	
	@Override
	public void getSectionThumbnails(IThumbnailCallback cb) {
		if (cb == null) return;
		
		for (int i=0; i < getSectionSetCount(); i++) {
			AnyboxSectionSet set = getSectionSetAt(i);
			if (set == null) continue;
			
			AnyboxSection[] items = set.getSections();
			if (items == null) continue;
			
			for (AnyboxSection item : items) {
				if (item == null || item.isFolder()) continue;
				
				String imageURL = item.getPosterThumbnailURL();
				if (imageURL == null || imageURL.length() == 0) continue;
				
				if (LOG.isDebugEnabled())
					LOG.debug("getSectionThumbnails: item=" + item + " imageURL=" + imageURL);
				
				if (cb.onThumbnail(imageURL, item.getWidth(), item.getHeight()) == false)
					return;
			}
		}
	}
	
	public AnyboxSectionSet loadSectionSet(AnyboxData data, 
			long reloadId) throws IOException {
		if (data == null) return null;

		AnyboxSectionSet set = AnyboxSectionSet.loadSectionSet(
				getLibrary(), getLibrary(), data, reloadId);
		
		if (set != null) { 
			if (set.getSectionFrom() <= 0) clearSectionSet();
			addSectionSet(set);
		}
		
		mSectionRequestTime = System.currentTimeMillis();
		
		return set;
	}
	
	public boolean shouldReload(SortType.Type sort, FilterType.Type filter) {
		if (getRefreshTime() <= 0 || sort != mSortType || filter != mFilterType)
			return true;
		return false;
	}
	
	public static void reloadSectionList(AnyboxSectionSearch search, 
			ProviderCallback callback, long reloadId, 
			SortType.Type sort, FilterType.Type filter) {
		if (search == null) return;
		if (search.getSectionReloadId() == reloadId) {
			if (LOG.isDebugEnabled())
				LOG.debug("reloadSectionList: already loaded for same reloadId: " + reloadId);
			return;
		}
		
		String sorted = null;
		String filtered = AnyboxSectionSet.getFilterTypeString(filter);
		
		search.mSortType = sort;
		search.mFilterType = filter;
		
		getSectionList(search, callback, reloadId, 
				sorted, filtered, true, 0, 50);
	}
	
	public static AnyboxSectionSet reloadSectionListNext(AnyboxSectionSearch search, 
			ProviderCallback callback, long reloadId, FilterType.Type filter) {
		if (search == null) return null;
		if (search.getSectionReloadId() == reloadId) {
			if (LOG.isDebugEnabled())
				LOG.debug("reloadSectionListNext: already loaded for same reloadId: " + reloadId);
			return null;
		}
		
		int listfrom = 0;
		String sorted = null;
		String filtered = AnyboxSectionSet.getFilterTypeString(filter);
		
		AnyboxSectionSet last = search.getSectionSetAt(search.getSectionSetCount() -1);
		if (last != null) {
			int from = last.getSectionFrom();
			int count = last.getSectionCount();
			if (from >= 0 && count >= 0) {
				listfrom = from + count;
				if (listfrom >= last.getTotalCount())
					return null;
			}
		}
		
		return getSectionList(search, callback, reloadId, 
				sorted, filtered, true, listfrom, 50);
	}
	
	static AnyboxSectionSet getSectionList(AnyboxSectionSearch search, 
			ProviderCallback callback, long reloadId, String sort, String filtertype,
			boolean byfolder, int from, int count) {
		if (search == null) return null;
		final AnyboxAccount user = search.getUser();
		
		String query = search.getQueryText();
		if (query == null) query = "";
		if (sort == null) sort = "";
		if (filtertype == null) filtertype = "";
		
		String url = user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/datum/section?wt=secretjson&q=" + StringUtils.URLEncode(query)
				+ "&filtertype=" + StringUtils.URLEncode(filtertype)
				+ "&sort=" + StringUtils.URLEncode(sort) + "&byfolder=" + byfolder 
				+ "&from=" + from + "&count=" + count
				+ "&id=" + StringUtils.URLEncode("all") 
				+ "&token=" + StringUtils.URLEncode(user.getAuthToken());
		
		SearchListener listener = new SearchListener(reloadId);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			AnyboxData data = listener.mData;
			if (data != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					error = null;
					return search.loadSectionSet(data, reloadId);
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
	
	static class SearchListener extends AnyboxApi.SecretJSONListener {
		private final long mReloadId;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public SearchListener(long reloadId) {
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
