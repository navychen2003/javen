package org.javenstudio.provider.media.album;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class AlbumCursorFactory implements IDataSetCursorFactory<AlbumItem> {

	public AlbumCursorFactory() {}
	
	public AlbumCursor createCursor() { 
		return new AlbumCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<AlbumItem> create() { 
		return createCursor(); 
	}
	
}
