package org.javenstudio.falcon.datum.index;

import java.util.ArrayList;
import java.util.LinkedList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.IDatumCore;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionCollector;
import org.javenstudio.falcon.datum.SectionQuery;

public abstract class IndexDocs<T> {
	private static final Logger LOG = Logger.getLogger(IndexDocs.class);

	private final LinkedList<ISection> mFolders;
	private final LinkedList<ISection> mSections;
	private final LinkedList<String> mDeleted;
	
	private final IDatumCore mCore;
	private boolean mDelta = false;
	private boolean mDeepScan = false;
	
	public IndexDocs(IDatumCore core) { 
		if (core == null) throw new NullPointerException();
		mCore = core;
		mFolders = new LinkedList<ISection>();
		mSections = new LinkedList<ISection>();
		mDeleted = new LinkedList<String>();
	}
	
	public final IDatumCore getCore() { return mCore; }
	public final boolean isDelta() { return mDelta; }
	public final boolean isDeepScan() { return mDeepScan; }
	
	public synchronized String[] init(IData[] datas, 
			boolean delta, boolean deepscan) throws ErrorException { 
		if (datas == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("init: delta=" + delta);
		
		mFolders.clear();
		mSections.clear();
		mDeleted.clear();
		
		mDelta = delta;
		mDeepScan = deepscan;
		
		ArrayList<String> names = new ArrayList<String>();
		
		for (int i=0; datas != null && i < datas.length; i++) { 
    		IData data = datas[i];
    		if (data == null) continue;
    		
    		if (addData(data, true)) { 
    			
    			if (LOG.isDebugEnabled())
    	    		LOG.debug("init: add data=" + data);
    			
    			names.add(data.getName());
    		}
		}
		
		return names.toArray(new String[names.size()]);
	}
	
	private boolean addData(IData data, boolean addfolder) throws ErrorException { 
		if (data == null) return false;
		
		if (data instanceof ISection) { 
    		addSection((ISection)data, addfolder);
    		return true;
    	}
    	
    	if (data instanceof ILibrary) { 
    		addLibrary((ILibrary)data);
    		return true;
    	}
    	
    	return false;
	}
	
	private void addLibrary(ILibrary library) throws ErrorException { 
		if (library == null) return;
		
		library.getSections(new IndexQuery());
		library.onIndexed(System.currentTimeMillis());
		//library.saveMetaInfo();
	}
	
	private void listSections(ISection dir) throws ErrorException { 
		if (dir == null || !dir.isFolder())
			return;
		
		dir.getSubSections(new IndexQuery());
		//dir.setIndexedTime(System.currentTimeMillis());
		//dir.saveMetaInfo();
	}
	
	private final void addDeleted(String sectionId) { 
		if (sectionId == null || sectionId.length() == 0)
			return;
		
		synchronized (this) { 
			mDeleted.add(sectionId);
		}
	}
	
	private final void addSection(ISection section, boolean addfolder) { 
		if (section == null) return;
		
		synchronized(this) {
			if (section.isFolder()) {
				if (addfolder) mFolders.add(section);
			} else
				mSections.add(section);
		}
	}
	
    public final synchronized T nextDoc() throws ErrorException {
    	if (!isDelta()) return nextItem();
    	return null;
    }
	
    public final synchronized T nextModifiedRow() throws ErrorException {
    	if (isDelta()) return nextItem();
    	return null;
    }
    
    public final synchronized T nextDeletedRow() throws ErrorException {
    	if (isDelta()) return nextDeleted();
    	return null;
    }
    
	public synchronized void close() { 
		mFolders.clear();
		mSections.clear();
		mDeleted.clear();
	}
	
	private synchronized T nextItem() throws ErrorException {
    	T row = nextSection();
		if (row != null) 
			return row;
		
		while (mFolders.size() > 0) { 
			ISection dir = mFolders.removeFirst();
			listSections(dir);
			
			if (mSections.size() > 0)
				break;
		}
		
    	return nextSection();
    }
	
	private T nextSection() throws ErrorException { 
		while (mSections.size() > 0) { 
			ISection file = mSections.removeFirst();
			if (file == null) continue;
			
			if (file.isFolder()) { 
				listSections(file);
				continue;
			}
			
			T doc = wrapDoc(file);
			if (doc != null)
				return doc;
		}
		
		return null;
	}
	
	private T nextDeleted() throws ErrorException { 
		while (mDeleted.size() > 0) { 
			String sectionId = mDeleted.removeFirst();
			if (sectionId == null) continue;
			
			T doc = wrapDoc(sectionId);
			if (doc != null)
				return doc;
		}
		
		return null;
	}
	
	protected abstract T wrapDoc(ISection file) throws ErrorException;
	protected abstract T wrapDoc(String id) throws ErrorException;
	
	private class IndexQuery extends SectionQuery 
			implements ISectionCollector {
		
		public IndexQuery() { 
			super(0, 0);
		}
		
		@Override
		public ISectionCollector getCollector() { 
			return this;
		}
		
		public IndexQuery(long start, int count) {
			super(start, count);
		}
		
		@Override
		public void addSection(ISection section) {
			if (!isDelta() || section.isFolder()) { 
				//if (LOG.isDebugEnabled())
				//	LOG.debug("addSection: section=" + section);
				
				IndexDocs.this.addSection(section, isDeepScan());
			}
		}
		
		@Override
		public void addModified(ISection section) {
			if (isDelta() && !section.isFolder()) {
				//if (LOG.isDebugEnabled())
				//	LOG.debug("addModified: section=" + section);
				
				IndexDocs.this.addSection(section, isDeepScan());
			}
		}
		
		@Override
		public void addDeleted(String sectionId) {
			if (isDelta()) { 
				//if (LOG.isDebugEnabled())
				//	LOG.debug("addDeleted: sectionId=" + sectionId);
				
				IndexDocs.this.addDeleted(sectionId); 
			}
		}
	}
	
}
