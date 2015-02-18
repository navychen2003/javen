package org.javenstudio.falcon.datum.data;

import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.SectionBase;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.io.Text;

public abstract class SqSection extends SectionBase 
		implements ISection {
	private static final Logger LOG = Logger.getLogger(SqSection.class);

	private final SqLibrary mLibrary;
	
	protected SqSection(SqLibrary library) {
		mLibrary = library;
	}
	
	public abstract SqRoot getRoot();
	public abstract NameData getNameData();
	public abstract String getParentKey();
	public abstract String getPathKey();
	
	@Override
	public SqLibrary getLibrary() {
		return mLibrary;
	}

	@Override
	public DataManager getManager() {
		return mLibrary.getManager();
	}
	
	@Override
	public String getOwner() { 
		NameData data = getNameData();
		if (data != null) { 
			Text txt = data.getAttrs().getOwner();
			if (txt != null) 
				return txt.toString();
		}
		return getLibrary().getOwner();
	}
	
	@Override
	public String getChecksum() {
		NameData data = getNameData();
		if (data != null) { 
			Text txt = data.getAttrs().getChecksum();
			if (txt != null) 
				return txt.toString();
		}
		return null;
	}
	
	@Override
	public String getAccessKey() {
		NameData data = getNameData();
		if (data != null) { 
			FileShare share = data.getFileShare();
			if (share != null) {
				Text txt = share.getAccessKey();
				if (txt != null) 
					return txt.toString();
			}
		}
		
		SqRoot root = getRoot();
		if (root != null && root != this)
			return root.getAccessKey();
		
		return null;
	}
	
	@Override
	public final String getContentId() { 
		return getManager().getUserKey() + getLibrary().getContentKey() 
				+ getRoot().getContentKey() + getPathKey();
	}
	
	@Override
	public final boolean canRead() { 
		try {
			return !getRoot().getLock().isLocked(ILockable.Type.WRITE, false);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("canRead: error: " + e, e);
			
			return false;
		}
	}
	
	@Override
	public boolean equals(Object o) { 
		if (this == o) return true;
		if (o == null || !(o instanceof SqSection)) 
			return false;
		
		SqSection other = (SqSection)o;
		return this.getLibrary() == other.getLibrary() && 
				this.getRoot() == other.getRoot() && 
				this.getContentKey().equals(other.getContentKey());
	}
	
	@Override
	public final String[] getPosters() throws ErrorException { 
		ArrayList<String> list = new ArrayList<String>();
		
		NameData data = getNameData();
		FilePoster poster = data != null ? data.getFilePoster() : null;
		
		if (poster != null) { 
			Text[] keys = poster.getPosters();
			for (int i=0; keys != null && i < keys.length; i++) { 
				Text key = keys[i];
				if (key != null) { 
					String value = key.toString();
					if (value != null && value.length() > 0)
						list.add(value);
				}
			}
		}
		
		if (hasScreenshotPoster(data)) 
			list.add(getContentId());
		
		return list.toArray(new String[list.size()]);
	}
	
	protected boolean hasScreenshotPoster(NameData data) { 
		//if (data != null) { 
		//	if (data.getAttrs().getPosterCount() > 0) 
		//		return true;
		//}
		return false;
	}
	
	@Override
	public final String[] getBackgrounds() throws ErrorException { 
		ArrayList<String> list = new ArrayList<String>();
		
		NameData data = getNameData();
		FilePoster poster = data != null ? data.getFilePoster() : null;
		
		if (poster != null) { 
			Text[] keys = poster.getBackgrounds();
			for (int i=0; keys != null && i < keys.length; i++) { 
				Text key = keys[i];
				if (key != null) { 
					String value = key.toString();
					if (value != null && value.length() > 0)
						list.add(value);
				}
			}
		}
		
		return list.toArray(new String[list.size()]);
	}
	
}
