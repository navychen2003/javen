package org.javenstudio.provider.app.anybox.library;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.SelectDialogHelper;

public class AnyboxSelectCreate extends SelectDialogHelper {

	public class CreateItem extends SelectItem {
		private int mIconRes = 0;
		public void setIconRes(int resId) { mIconRes = resId; }
		
		@Override
		public Drawable getIcon() {
			if (mIconRes != 0) 
				return ResourceHelper.getResources().getDrawable(mIconRes);
			return null;
		}
	}
	
	private final AnyboxAccount mAccount;
	
	public AnyboxSelectCreate(AnyboxAccount account) {
		if (account == null) throw new NullPointerException();
		mAccount = account;
	}
	
	public AnyboxAccount getAccountUser() { return mAccount; }
	
	protected CreateItem createLibraryItem(ISectionFolder folder) {
		CreateItem item = new CreateItem();
		item.setTitle(ResourceHelper.getResources().getString(R.string.select_create_library_title));
		
		String text = ResourceHelper.getResources().getString(R.string.select_create_library_subtitle);
		item.setSubTitle(String.format(text, "\""+getAccountUser().getAccountFullname()+"\""));
		
		return item;
	}
	
	protected CreateItem createFolderItem(ISectionFolder folder) {
		if (folder == null) return null;
		
		CreateItem item = new CreateItem();
		item.setTitle(ResourceHelper.getResources().getString(R.string.select_create_folder_title));
		
		String text = ResourceHelper.getResources().getString(R.string.select_create_folder_subtitle);
		item.setSubTitle(String.format(text, "\""+folder.getName()+"\""));
		
		return item;
	}
	
	protected CreateItem createTextItem(ISectionFolder folder) {
		if (folder == null) return null;
		
		CreateItem item = new CreateItem();
		item.setTitle(ResourceHelper.getResources().getString(R.string.select_create_textfile_title));
		
		String text = ResourceHelper.getResources().getString(R.string.select_create_textfile_subtitle);
		item.setSubTitle(String.format(text, "\""+folder.getName()+"\""));
		
		return item;
	}
	
	public ISelectItem[] getSelectItems(ISectionFolder folder) {
		ArrayList<ISelectItem> list = new ArrayList<ISelectItem>();
		
		CreateItem folderItem = createFolderItem(folder);
		CreateItem textItem = createTextItem(folder);
		CreateItem libraryItem = createLibraryItem(folder);
		
		if (folderItem != null) list.add(folderItem);
		if (textItem != null) list.add(textItem);
		if (libraryItem != null) list.add(libraryItem);
		
		return list.toArray(new ISelectItem[list.size()]);
	}
	
	public void showSelectDialog(IActivity activity, ISectionFolder folder) {
		if (activity == null) return;
		
		showSelectDialog(activity.getActivity(), 
				getSelectItems(folder), R.string.select_create_title);
	}
	
}
