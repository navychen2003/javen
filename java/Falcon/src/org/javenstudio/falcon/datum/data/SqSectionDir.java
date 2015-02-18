package org.javenstudio.falcon.datum.data;

import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IFileInfo;
import org.javenstudio.falcon.datum.IFolderData;
import org.javenstudio.falcon.datum.IFolderInfo;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionQuery;
import org.javenstudio.falcon.datum.ISectionSet;
import org.javenstudio.falcon.datum.SectionQuery;

public abstract class SqSectionDir extends SqSection 
		implements IFolderData, IFolderInfo {

	private final SqSectionLists mLists;
	
	protected SqSectionDir(SqLibrary library) {
		super(library);
		mLists = createSectionLists();
	}
	
	private SqSectionLists createSectionLists() { 
		return new SqSectionLists() {
				@Override
				protected SqSection[] loadSections(boolean byfolder)
						throws ErrorException {
					return SqSectionDir.this.loadSections(byfolder);
				}
			};
	}

	@Override
	public final boolean isFolder() { 
		return true; 
	}

	@Override
	public long getContentLength() { 
		return 0; 
	}
	
	@Override
	public String getExtension() {
		return "";
	}
	
	@Override
	public SqSectionSet getSubSections(ISectionQuery query) 
			throws ErrorException { 
		synchronized (mLists) {
			SqSectionList list = query.isByFolder() ? 
					mLists.getListByFolder(query.getCollector()) : 
					mLists.getListByFile(query.getCollector());
			
			return list != null ? list.getSectionSet(query) : null;
		}
	}
	
	@Override
	public synchronized void close() { 
		synchronized (mLists) {
			mLists.close();
		}
		super.close();
	}
	
	public void reset() throws ErrorException { 
		synchronized (mLists) {
			mLists.close();
		}
		synchronized (mFileLock) {
			mFolders = null;
			mFiles = null;
		}
	}
	
	protected abstract SqSection[] loadSections(boolean byfolder) 
			throws ErrorException;
	
	private final Object mFileLock = new Object();
	private IFolderInfo[] mFolders = null;
	private IFileInfo[] mFiles = null;
	
	@Override
	public final boolean isHomeFolder() {
		return false;
	}

	@Override
	public final boolean isRootFolder() {
		return false;
	}
	
	@Override
	public final IFolderInfo getRootInfo() throws ErrorException {
		return getLibrary();
	}
	
	protected ISectionSet getSectionSet() throws ErrorException { 
		return getSubSections(new SectionQuery(0, 0));
	}
	
	protected boolean acceptSection(ISection section) { 
		return section != null && section instanceof IFileInfo;
	}
	
	private void initList(boolean refresh, Filter filter) 
			throws ErrorException { 
		synchronized (mFileLock) {
			if (mFolders == null || mFiles == null || refresh || filter != null) {
				ISectionSet set = getSectionSet();
				
				ArrayList<IFolderInfo> folders = new ArrayList<IFolderInfo>();
				ArrayList<IFileInfo> files = new ArrayList<IFileInfo>();
				
				for (int i=0; set != null && i < set.getSectionCount(); i++) { 
					ISection section = set.getSectionAt(i);
					if (section != null && acceptSection(section)) { 
						IFileInfo info = (IFileInfo)section;
						if (info != null && (filter == null || filter.accept(info))) { 
							if (info instanceof IFolderInfo)
								folders.add((IFolderInfo)info);
							else
								files.add(info);
						}
					}
				}
				
				mFolders = folders.toArray(new IFolderInfo[folders.size()]);
				mFiles = files.toArray(new IFileInfo[files.size()]);
			}
		}
	}
	
	@Override
	public IFolderInfo[] listFolderInfos(boolean refresh, 
			Filter filter) throws ErrorException {
		synchronized (mFileLock) {
			initList(refresh, filter);
			return mFolders;
		}
	}
	
	@Override
	public IFileInfo[] listFileInfos(boolean refresh, 
			Filter filter) throws ErrorException {
		synchronized (mFileLock) {
			initList(refresh, filter);
			return mFiles;
		}
	}
	
}
