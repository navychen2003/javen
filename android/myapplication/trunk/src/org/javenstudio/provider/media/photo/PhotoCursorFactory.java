package org.javenstudio.provider.media.photo;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class PhotoCursorFactory implements IDataSetCursorFactory<PhotoItem> {

	public PhotoCursorFactory() {}
	
	public PhotoCursor createCursor() { 
		return new PhotoCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<PhotoItem> create() { 
		return createCursor(); 
	}
	
}
