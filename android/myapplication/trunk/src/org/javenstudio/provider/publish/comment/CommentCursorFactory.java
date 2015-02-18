package org.javenstudio.provider.publish.comment;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class CommentCursorFactory implements IDataSetCursorFactory<CommentItem> {

	private final CommentCursor.Model mModel;
	
	public CommentCursorFactory(CommentCursor.Model model) { 
		mModel = model;
	}
	
	public CommentCursor createCursor() { 
		return new CommentCursor(mModel); 
	}
	
	@Override 
	public final IDataSetCursor<CommentItem> create() { 
		return createCursor(); 
	}

}
