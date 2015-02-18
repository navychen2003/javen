package org.javenstudio.falcon.datum;

import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public abstract class SectionBase extends DataObject 
		implements ISection {
	private static final Logger LOG = Logger.getLogger(SectionBase.class);
	
	protected void onCreated() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onCreated: " + toString());
	}
	
	@Override
	public boolean supportOperation(IData.Operation op) { 
		return false;
	}
	
	public boolean isFolder() { return false; }
	public boolean canRead() { return true; }
	public boolean canMove() { return false; }
	public boolean canDelete() { return false; }
	public boolean canWrite() { return false; }
	public boolean canCopy() { return false; }
	
	@Override
	public boolean equals(Object o) { 
		if (this == o) return true;
		if (o == null || !(o instanceof SectionBase)) 
			return false;
		
		SectionBase other = (SectionBase)o;
		return this.getContentId().equals(other.getContentId());
	}
	
	@Override
	public int getSubCount() { 
		return 0;
	}
	
	@Override
	public long getSubLength() { 
		return 0;
	}
	
	@Override
	public ISectionSet getSubSections(ISectionQuery query) 
			throws ErrorException { 
		return null;
	}
	
	@Override
	public int getWidth() throws ErrorException { 
		return 0;
	}
	
	@Override
	public int getHeight() throws ErrorException { 
		return 0;
	}
	
	@Override
	public long getDuration() throws ErrorException { 
		return 0;
	}
	
	@Override
	public int getMetaTag(Map<String,Object> tags) 
			throws ErrorException { 
		return 0;
	}
	
	@Override
	public int getMetaInfo(Map<String,Object> infos) 
			throws ErrorException { 
		return 0;
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
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + getContentId() 
				+ ",name=" + getName() + "}";
	}
	
}
