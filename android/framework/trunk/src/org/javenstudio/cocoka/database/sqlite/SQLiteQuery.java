package org.javenstudio.cocoka.database.sqlite;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.nosql.SimpleQuery;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class SQLiteQuery<T extends SQLiteEntity> extends SimpleQuery<LongIdentity, T> {

	public SQLiteQuery(IDatabase db, Class<T> entityClass) { 
		super(db, entityClass);
	}

}
