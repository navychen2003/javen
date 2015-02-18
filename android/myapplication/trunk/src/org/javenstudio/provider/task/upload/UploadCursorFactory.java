package org.javenstudio.provider.task.upload;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class UploadCursorFactory implements IDataSetCursorFactory<UploadItem> {

	private final UploadCursor.Model mModel;
	
	public UploadCursorFactory(UploadCursor.Model model) { 
		mModel = model;
	}
	
	public UploadCursor createCursor() { 
		return new UploadCursor(mModel); 
	}
	
	@Override 
	public final IDataSetCursor<UploadItem> create() { 
		return createCursor(); 
	}
	
}
