package org.javenstudio.provider.publish.discuss;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class TopicCursorFactory implements IDataSetCursorFactory<TopicItem> {

	public TopicCursorFactory() {}
	
	public TopicCursor createCursor() { 
		return new TopicCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<TopicItem> create() { 
		return createCursor(); 
	}
	
}
