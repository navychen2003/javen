package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITable;

public abstract class UpdateTrigger<K extends IIdentity, T extends IEntity<K>> extends AbstractTrigger<K,T> {

	public void onUpdateBefore(ITable<K,T> table, T data) {
		// do nothing
	}
	
	public void onUpdateAfter(ITable<K,T> table, T data, boolean updated) {
		// do nothing
	}
	
}
