package org.javenstudio.provider.library.list;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class LibraryListDataSet extends AbstractDataSet<LibraryListItem> {

	public LibraryListDataSet(LibraryListDataSets dataSets, LibraryListItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getLibraryListItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public LibraryListItem getLibraryListItem() { 
		return (LibraryListItem)getObject(); 
	}
	
}
