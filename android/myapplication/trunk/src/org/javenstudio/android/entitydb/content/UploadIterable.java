package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.TUpload;
import org.javenstudio.cocoka.database.SQLiteContentIterable;
import org.javenstudio.common.util.EntityCursor;

public class UploadIterable extends SQLiteContentIterable<UploadData, TUpload> {

	public UploadIterable(EntityCursor<TUpload> cursor) { 
		super(cursor); 
	}
	
	@Override 
	protected UploadData newContent(TUpload entity) { 
		return new UploadDataImpl(entity); 
	}
	
}
