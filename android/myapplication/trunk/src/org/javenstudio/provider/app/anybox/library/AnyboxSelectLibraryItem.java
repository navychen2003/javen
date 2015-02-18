package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.SelectLibraryItem;
import org.javenstudio.provider.library.select.SelectFolderItem;
import org.javenstudio.provider.library.select.SelectOperation;

public class AnyboxSelectLibraryItem extends SelectLibraryItem {
	//private static final Logger LOG = Logger.getLogger(AnyboxSelectLibraryItem.class);

	public AnyboxSelectLibraryItem(SelectOperation op, 
			SelectFolderItem parent, ILibraryData data) {
		super(op, parent, data);
	}
	
	@Override
	public AnyboxSelectOperation getOperation() {
		return (AnyboxSelectOperation)super.getOperation();
	}
	
}
