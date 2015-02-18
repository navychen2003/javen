package org.javenstudio.provider.library;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.provider.library.select.SelectFolderItem;
import org.javenstudio.provider.library.select.SelectOperation;

public class SelectLibraryItem extends SelectFolderItem {

	private final ILibraryData mData;
	
	public SelectLibraryItem(SelectOperation op, 
			SelectFolderItem parent, ILibraryData data) {
		super(op, parent);
		if (data == null) throw new NullPointerException();
		mData = data;
		
		addImageItem(data.getPosterThumbnailURL(), 
				data.getWidth(), data.getHeight());
		
		data.getSectionThumbnails(new IThumbnailCallback() {
				@Override
				public boolean onThumbnail(String imageURL, int imageWidth, int imageHeight) {
					addImageItem(imageURL, imageWidth, imageHeight);
					return getImageCount() < 3;
				}
			});
	}
	
	public ILibraryData getData() { return mData; }
	public String getName() { return getData().getName(); }
	
	@Override
	public String getFolderInfo() { 
		return SectionHelper.getFolderCountInfo(getData()); 
	}
	
	@Override
	public Drawable getFolderIcon() { 
		return AppResources.getInstance().getSectionNavIcon(getData()); 
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + getIdentity() 
				+ "{data=" + getData() + "}";
	}
	
}
