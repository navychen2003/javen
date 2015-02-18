package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.SelectSectionFolderItem;
import org.javenstudio.provider.library.select.SelectFolderItem;
import org.javenstudio.provider.library.select.SelectOperation;

public class AnyboxSelectFolderItem extends SelectSectionFolderItem {
	//private static final Logger LOG = Logger.getLogger(AnyboxSelectFolderItem.class);
	
	public AnyboxSelectFolderItem(SelectOperation op, 
			SelectFolderItem parent, ISectionFolder data) {
		super(op, parent, data);
	}
	
	@Override
	public AnyboxSelectOperation getOperation() {
		return (AnyboxSelectOperation)super.getOperation();
	}
	
}
