package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.ITable;

public abstract class QueryTrigger<K extends IIdentity, T extends IEntity<K>> extends AbstractTrigger<K,T> {

	public void onQueryBefore(ITable<K,T> table, IQuery<K,T> query) {
		// do nothing
	}
	
	public void onQueryAfter(ITable<K,T> table, IQuery<K,T> query, ICursor<K,T> cursor) {
		// do nothing
	}
	
}
