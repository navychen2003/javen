package org.javenstudio.provider.library.list;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class LibraryListCursor extends ListDataSetCursor<LibraryListItem> {

	public LibraryListCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<LibraryListItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof LibraryListDataSet) 
			addLibraryListDataSet((LibraryListDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<LibraryListItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof LibraryListDataSet) 
			addLibraryListDataSet((LibraryListDataSet)data); 
	}
	
	private void addLibraryListDataSet(LibraryListDataSet dataSet) { 
		if (dataSet != null) { 
			LibraryListItem data = dataSet.getLibraryListItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
