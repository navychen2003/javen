package org.javenstudio.provider.media.album;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class AlbumCursor extends ListDataSetCursor<AlbumItem> {

	public AlbumCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<AlbumItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof AlbumDataSet) 
			addAlbumDataSet((AlbumDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<AlbumItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof AlbumDataSet) 
			addAlbumDataSet((AlbumDataSet)data); 
	}
	
	private void addAlbumDataSet(AlbumDataSet dataSet) { 
		if (dataSet != null) { 
			AlbumItem data = dataSet.getAlbumItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
