package org.javenstudio.provider.publish.discuss;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class ReplyCursorFactory implements IDataSetCursorFactory<ReplyItem> {

	public ReplyCursorFactory() {}
	
	public ReplyCursor createCursor() { 
		return new ReplyCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<ReplyItem> create() { 
		return createCursor(); 
	}
	
}
