package org.anybox.android.library.app;

import org.anybox.android.library.R;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.library.AnyboxSelectCreate;
import org.javenstudio.provider.library.ISectionFolder;

public class MySelectCreate extends AnyboxSelectCreate {

	public MySelectCreate(AnyboxAccount account) {
		super(account);
	}
	
	@Override
	protected CreateItem createLibraryItem(ISectionFolder folder) {
		CreateItem item = super.createLibraryItem(folder);
		if (item != null) {
			item.setIconRes(R.drawable.ic_nav_library);
		}
		return item;
	}
	
	@Override
	protected CreateItem createFolderItem(ISectionFolder folder) {
		CreateItem item = super.createFolderItem(folder);
		if (item != null) {
			item.setIconRes(R.drawable.ic_nav_folder);
		}
		return item;
	}
	
	@Override
	protected CreateItem createTextItem(ISectionFolder folder) {
		CreateItem item = super.createTextItem(folder);
		if (item != null) {
			item.setIconRes(R.drawable.ic_nav_file_text);
		}
		return item;
	}
	
}
