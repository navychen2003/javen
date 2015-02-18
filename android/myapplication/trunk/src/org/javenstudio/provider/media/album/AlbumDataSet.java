package org.javenstudio.provider.media.album;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class AlbumDataSet extends AbstractDataSet<AlbumItem> {

	public AlbumDataSet(AlbumDataSets dataSets, AlbumItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return getAlbumItem().getSource().getOnAlbumItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public AlbumItem getAlbumItem() { 
		return (AlbumItem)getObject(); 
	}
	
}
