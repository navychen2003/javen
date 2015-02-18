package org.javenstudio.provider.library;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;

public class CategoryData implements ICategoryData {

	public static CategoryData createFileCategory(ISectionList parent) {
		return new CategoryData(parent, ResourceHelper.getResources()
				.getString(R.string.files_category_label));
	}
	
	public static CategoryData createFolderCategory(ISectionList parent) {
		return new CategoryData(parent, ResourceHelper.getResources()
				.getString(R.string.folders_category_label));
	}
	
	private final ISectionList mParent;
	private final String mName;
	
	private CategoryData(ISectionList parent, String name) {
		mParent = parent;
		mName = name;
	}
	
	@Override
	public ISectionList getParent() {
		return mParent;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{parent=" + mParent 
				+ ",name=" + mName + "}";
	}
	
}
