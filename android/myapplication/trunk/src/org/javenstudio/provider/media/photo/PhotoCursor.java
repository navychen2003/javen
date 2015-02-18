package org.javenstudio.provider.media.photo;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class PhotoCursor extends ListDataSetCursor<PhotoItem> {

	public PhotoCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<PhotoItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof PhotoDataSet) 
			addImageDataSet((PhotoDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<PhotoItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof PhotoDataSet) 
			addImageDataSet((PhotoDataSet)data); 
	}
	
	private void addImageDataSet(PhotoDataSet dataSet) { 
		if (dataSet != null) { 
			PhotoItem data = dataSet.getPhotoItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
