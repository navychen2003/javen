package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.THost;
import org.javenstudio.cocoka.database.SQLiteContentIterable;
import org.javenstudio.common.util.EntityCursor;

public class HostIterable extends SQLiteContentIterable<HostData, THost> {

	public HostIterable(EntityCursor<THost> cursor) { 
		super(cursor); 
	}
	
	@Override 
	protected HostData newContent(THost entity) { 
		return new HostDataImpl(entity); 
	}
	
}
