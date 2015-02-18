package org.javenstudio.provider.library;

import android.graphics.drawable.Drawable;

import org.javenstudio.provider.library.select.SelectFileItem;
import org.javenstudio.provider.library.select.SelectFolderItem;
import org.javenstudio.provider.library.select.SelectOperation;

public class SelectSectionFileItem extends SelectFileItem {

	private final ISectionData mData;
	
	public SelectSectionFileItem(SelectOperation op, 
			SelectFolderItem parent, ISectionData data) {
		super(op, parent);
		if (data == null) throw new NullPointerException();
		mData = data;
		
		addImageItem(data.getPosterThumbnailURL(), 
				data.getWidth(), data.getHeight());
	}
	
	public ISectionData getData() { return mData; }
	public String getName() { return getData().getName(); }
	
	@Override
	public String getFileInfo() { 
		return SectionHelper.getFileSizeInfo(getData()); 
	}
	
	@Override
	public Drawable getFileIcon() { 
		return getData().getTypeIcon();
		//return AppResources.getInstance().getSectionNavIcon(getData()); 
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + getIdentity() 
				+ "{data=" + getData() + "}";
	}
	
}
