package org.javenstudio.falcon.datum.data;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.ISectionCollector;

abstract class SqSectionLists {

	private class SqListAdapterImpl extends SqListAdapter {
		private final boolean mByFolder;
		
		public SqListAdapterImpl(boolean byfolder) { 
			mByFolder = byfolder;
		}
		
		@Override
		protected SqSection[] loadSections() throws ErrorException {
			return SqSectionLists.this.loadSections(mByFolder);
		}
	}
	
	private final SqListAdapter mListByFolder = new SqListAdapterImpl(true);
	private final SqListAdapter mListByFile = new SqListAdapterImpl(false);
	
	protected abstract SqSection[] loadSections(boolean byfolder) 
			throws ErrorException;
	
	public SqSectionList getListByFolder(
			ISectionCollector collector) throws ErrorException { 
		return mListByFolder.getSectionList(collector);
	}
	
	public SqSectionList getListByFile(
			ISectionCollector collector) throws ErrorException { 
		return mListByFile.getSectionList(collector);
	}
	
	public void close() { 
		mListByFolder.close();
		mListByFile.close();
	}
	
}
