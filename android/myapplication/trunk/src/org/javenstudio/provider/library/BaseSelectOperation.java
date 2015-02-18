package org.javenstudio.provider.library;

import org.javenstudio.android.app.R;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.worker.work.Work;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderModel;
import org.javenstudio.provider.library.select.ISelectData;
import org.javenstudio.provider.library.select.LocalFileItem;
import org.javenstudio.provider.library.select.LocalFolderItem;
import org.javenstudio.provider.library.select.SelectAppItem;
import org.javenstudio.provider.library.select.SelectFolderItem;
import org.javenstudio.provider.library.select.SelectListItem;
import org.javenstudio.provider.library.select.SelectOperation;

public abstract class BaseSelectOperation extends SelectOperation {
	private static final Logger LOG = Logger.getLogger(BaseSelectOperation.class);
	
	private final Object mThreadLock = new Object();
	private SelectFolderItem mFolderItem = null;
	private volatile boolean mLoading = false;
	
	public boolean isLoading() { return mLoading; }
	public SelectFolderItem getCurrentFolder() { return mFolderItem; }
	
	@Override
	public void startLoader(final SelectFolderItem folder, 
			final ReloadType type) {
		if (folder == null) return;
		try {
			ResourceHelper.getScheduler().post(new Work("LoadSelectData") {
					@Override
					public void onRun() {
						mLoading = true;
						postShowProgress(true);
						try {
							synchronized (mThreadLock) {
								mFolderItem = folder;
								
								reloadOnThread(folder, type, 
										ProviderModel.sCounter.getAndIncrement());
							}
						} finally {
							postShowProgress(false);
							mLoading = false;
						}
					}
				});
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("startLoader: error: " + e, e);
		}
	}
	
	protected boolean reloadOnThread(SelectFolderItem folder, 
			ReloadType type, long reloadId) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("reloadOnThread: folder=" + folder 
					+ " reloadType=" + type + " reloadId=" + reloadId);
		}
		
		if (folder != null && folder instanceof LocalFolderItem) {
			LocalFolderItem localFolder = (LocalFolderItem)folder;
			postClearData();
			
			ISelectData[] dataList = localFolder.listFiles();
			postAddDataList(localFolder, dataList);
			
			if (dataList == null || dataList.length == 0)
				onEmptyState(folder);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	protected SelectListItem createCategoryItem(SelectFolderItem folder, 
			ISelectData data, ISelectData prev) {
		if (data == null) return null;
		
		if (data instanceof ILibraryData) {
			if (prev == null || !(prev instanceof ILibraryData)) {
				return createCategoryItem(folder, R.string.library_category_label);
			}
		} else if (data instanceof ISectionFolder) {
			if (prev == null || !(prev instanceof ISectionFolder)) {
				return createCategoryItem(folder, R.string.folders_category_label);
			}
		} else if (data instanceof ISectionData) {
			if (prev == null || !(prev instanceof ISectionData) || prev instanceof ISectionFolder) {
				return createCategoryItem(folder, R.string.files_category_label);
			}
		} else if (data instanceof LocalFolderItem) {
			LocalFolderItem item = (LocalFolderItem)data;
			if (item.getParent() == null) {
				if (prev == null || !(prev instanceof LocalFolderItem)) {
					return createCategoryItem(folder, R.string.local_category_label);
				}
			} else {
				if (prev == null || !(prev instanceof LocalFolderItem)) {
					return createCategoryItem(folder, R.string.folders_category_label);
				}
			}
		} else if (data instanceof LocalFileItem) {
			if (prev == null || !(prev instanceof LocalFileItem)) {
				return createCategoryItem(folder, R.string.files_category_label);
			}
		} else if (data instanceof SelectAppItem) {
			if (prev == null || !(prev instanceof SelectAppItem)) {
				return createCategoryItem(folder, R.string.application_category_label);
			}
		}
		
		return null;
	}
	
	@Override
	protected SelectListItem createSelectItem(SelectFolderItem folder, 
			ISelectData data) {
		if (data == null) return null;
		
		if (data instanceof ILibraryData) {
			return createLibraryItem(folder, (ILibraryData)data);
			
		} else if (data instanceof ISectionFolder) {
			return createFolderItem(folder, (ISectionFolder)data);
			
		} else if (data instanceof ISectionData) {
			return createFileItem(folder, (ISectionData)data);
			
		} else if (data instanceof LocalFileItem) {
			return (LocalFileItem)data;
			
		} else if (data instanceof LocalFolderItem) {
			return (LocalFolderItem)data;
			
		} else if (data instanceof SelectAppItem) {
			return (SelectAppItem)data;
		}
		
		return null;
	}
	
	protected SelectListItem createLibraryItem(SelectFolderItem folder, ILibraryData data) {
		return new SelectLibraryItem(this, folder, data);
	}
	
	protected SelectListItem createFolderItem(SelectFolderItem folder, ISectionFolder data) {
		return new SelectSectionFolderItem(this, folder, data);
	}
	
	protected SelectListItem createFileItem(SelectFolderItem folder, ISectionData data) {
		return new SelectSectionFileItem(this, folder, data);
	}
	
}
