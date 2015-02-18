package org.javenstudio.provider.account.space;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class SpaceCursorFactory implements IDataSetCursorFactory<SpaceItem> {

	public SpaceCursorFactory() {}
	
	public SpaceCursor createCursor() { 
		return new SpaceCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<SpaceItem> create() { 
		return createCursor(); 
	}
	
}
