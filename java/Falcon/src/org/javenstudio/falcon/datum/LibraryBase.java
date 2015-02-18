package org.javenstudio.falcon.datum;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public abstract class LibraryBase implements ILibrary {
	private static final Logger LOG = Logger.getLogger(LibraryBase.class);
	
	private final DataManager mManager;
	
	protected LibraryBase(DataManager manager) { 
		if (manager == null) throw new NullPointerException();
		mManager = manager;
	}
	
	protected void onCreated() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onCreated: " + toString());
	}
	
	@Override
	public boolean supportOperation(IData.Operation op) { 
		return false;
	}
	
	public boolean canRead() { return true; }
	public boolean canMove() { return false; }
	public boolean canDelete() { return false; }
	public boolean canWrite() { return false; }
	public boolean canCopy() { return false; }
	
	@Override
	public final DataManager getManager() { 
		return mManager; 
	}
	
	@Override
	public final String getContentId() { 
		return SectionHelper.newContentId(getManager().getUserKey() 
				+ getContentKey());
	}
	
	@Override
	public boolean equals(Object o) { 
		if (this == o) return true;
		if (o == null || !(o instanceof LibraryBase)) 
			return false;
		
		LibraryBase other = (LibraryBase)o;
		return this.getContentId().equals(other.getContentId());
	}
	
	@Override
	public String getExtension() {
		return null;
	}
	
	@Override
	public int getTotalFolderCount() { 
		return 0;
	}
	
	@Override
	public int getTotalFileCount() { 
		return 0;
	}
	
	@Override
	public long getTotalFileLength() { 
		return 0;
	}
	
	@Override
	public int getSectionCount() throws ErrorException { 
		return 0;
	}
	
	@Override
	public ISectionRoot getSectionAt(int index) throws ErrorException { 
		return null;
	}
	
	@Override
	public ISectionSet getSections(ISectionQuery query) 
			throws ErrorException { 
		return null;
	}
	
	@Override
	public ISection getSection(String key) throws ErrorException { 
		return null;
	}
	
	@Override
	public String[] getPosters() throws ErrorException { 
		return null;
	}
	
	@Override
	public String[] getBackgrounds() throws ErrorException { 
		return null;
	}
	
	@Override
	public void close() {}
	
	@Override
	public void removeAndClose() throws ErrorException {
		close();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + getContentId() 
				+ ",name=" + getName() + "}";
	}
	
}
