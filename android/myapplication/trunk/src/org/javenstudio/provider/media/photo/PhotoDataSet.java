package org.javenstudio.provider.media.photo;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class PhotoDataSet extends AbstractDataSet<PhotoItem> {

	public PhotoDataSet(PhotoDataSets dataSets, PhotoItem data) {
		super(dataSets, data); 
		
		if (data != null)
			data.setDataSet(this);
	}
	
	@Override 
	public boolean isEnabled() { 
		return getPhotoItem().getSource().getOnPhotoItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public PhotoItem getPhotoItem() { 
		return (PhotoItem)getObject(); 
	}
	
}
