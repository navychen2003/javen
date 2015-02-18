package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.TDownload;
import org.javenstudio.cocoka.database.SQLiteContentIterable;
import org.javenstudio.common.util.EntityCursor;

public class DownloadIterable extends SQLiteContentIterable<DownloadData, TDownload> {

	public DownloadIterable(EntityCursor<TDownload> cursor) { 
		super(cursor); 
	}
	
	@Override 
	protected DownloadData newContent(TDownload entity) { 
		return new DownloadDataImpl(entity); 
	}
	
}
