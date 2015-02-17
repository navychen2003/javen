package org.javenstudio.cocoka.database.sqlite;

import org.javenstudio.common.entitydb.nosql.SimpleEntity;
import org.javenstudio.common.entitydb.type.LongIdentity;

public abstract class SQLiteEntity extends SimpleEntity<LongIdentity> {

	public SQLiteEntity() {
		super(); 
	}
	
	public SQLiteEntity(long id) {
		super(id > 0 ? new LongIdentity(id) : null); 
	}
	
	public final long getId() { 
		LongIdentity id = getIdentity();
		return id != null ? id.longValue() : -1;
	}
	
}
