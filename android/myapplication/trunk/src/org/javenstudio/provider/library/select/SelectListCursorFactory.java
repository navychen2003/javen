package org.javenstudio.provider.library.select;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class SelectListCursorFactory implements IDataSetCursorFactory<SelectListItem> {

	public SelectListCursorFactory() {}
	
	public SelectListCursor createCursor() { 
		return new SelectListCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<SelectListItem> create() { 
		return createCursor(); 
	}
	
}
