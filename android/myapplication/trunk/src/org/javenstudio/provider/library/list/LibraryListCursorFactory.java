package org.javenstudio.provider.library.list;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class LibraryListCursorFactory implements IDataSetCursorFactory<LibraryListItem> {

	public LibraryListCursorFactory() {}
	
	public LibraryListCursor createCursor() { 
		return new LibraryListCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<LibraryListItem> create() { 
		return createCursor(); 
	}
	
}
