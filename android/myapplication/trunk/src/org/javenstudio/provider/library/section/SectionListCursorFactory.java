package org.javenstudio.provider.library.section;

import org.javenstudio.android.app.SelectManager;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class SectionListCursorFactory implements IDataSetCursorFactory<SectionListItem> {

	public SectionListCursorFactory() {}
	
	public SectionListCursor createCursor() { 
		return new SectionListCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<SectionListItem> create() { 
		return createCursor(); 
	}
	
	public SelectManager createSelectManager() {
		return new SelectManager();
	}
	
}
