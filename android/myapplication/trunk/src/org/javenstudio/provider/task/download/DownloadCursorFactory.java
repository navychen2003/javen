package org.javenstudio.provider.task.download;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class DownloadCursorFactory implements IDataSetCursorFactory<DownloadItem> {

	private final DownloadCursor.Model mModel;
	
	public DownloadCursorFactory(DownloadCursor.Model model) { 
		mModel = model;
	}
	
	public DownloadCursor createCursor() { 
		return new DownloadCursor(mModel); 
	}
	
	@Override 
	public final IDataSetCursor<DownloadItem> create() { 
		return createCursor(); 
	}
	
}
