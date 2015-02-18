package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.SelectSectionFileItem;
import org.javenstudio.provider.library.select.SelectFolderItem;
import org.javenstudio.provider.library.select.SelectOperation;

public class AnyboxSelectFileItem extends SelectSectionFileItem {

	public AnyboxSelectFileItem(SelectOperation op, 
			SelectFolderItem parent, ISectionData data) {
		super(op, parent, data);
	}
	
}
