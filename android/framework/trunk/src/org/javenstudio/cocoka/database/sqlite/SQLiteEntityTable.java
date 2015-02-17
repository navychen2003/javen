package org.javenstudio.cocoka.database.sqlite;

import org.javenstudio.cocoka.database.Constants;
import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.nosql.SimpleCursor;
import org.javenstudio.common.entitydb.nosql.SimpleTable;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class SQLiteEntityTable<K extends IIdentity, T extends IEntity<K>> 
		extends SimpleTable<K, T> {

	public SQLiteEntityTable(SQLiteEntityDatabase db, String name, Class<T> entityClass) {
		super(db, name, entityClass, Constants.IDENTITY_FIELDNAME, null, null);
	}
	
	@Override
	protected SimpleCursor<K,T> createSimpleCursor(IQuery<K,T> query) { 
		return new SQLiteEntityCursor<K,T>(this, query); 
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public K newIdentity(Object value) { 
		if (value != null) { 
			try { 
				long num = 0;
				if (value instanceof Number) 
					num = ((Number)value).longValue();
				else 
					num = Long.valueOf(value.toString()).longValue();
				
				if (num > 0) 
					return (K) new LongIdentity(num);
				
			} catch (Throwable e) { 
				// no throw
			}
			
			throw new DBException("wrong identity value: "+value);
		}
		
		throw new DBException("increment identity not support");
	}
	
}
