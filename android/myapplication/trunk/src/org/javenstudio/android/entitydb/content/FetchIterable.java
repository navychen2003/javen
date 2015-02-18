package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.TFetch;
import org.javenstudio.cocoka.database.SQLiteContentIterable;
import org.javenstudio.common.util.EntityCursor;

public class FetchIterable extends SQLiteContentIterable<FetchData, TFetch> {

	public FetchIterable(EntityCursor<TFetch> cursor) { 
		super(cursor); 
	}
	
	@Override 
	protected FetchData newContent(TFetch entity) { 
		return new FetchDataImpl(entity); 
	}
	
}
