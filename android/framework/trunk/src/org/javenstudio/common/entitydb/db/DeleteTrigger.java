package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITable;

public abstract class DeleteTrigger<K extends IIdentity, T extends IEntity<K>> extends AbstractTrigger<K,T> {

	public void onDeleteBefore(ITable<K,T> table, T data) {
		// do nothing
	}
	
	public void onDeleteAfter(ITable<K,T> table, T data) {
		// do nothing
	}
	
}
