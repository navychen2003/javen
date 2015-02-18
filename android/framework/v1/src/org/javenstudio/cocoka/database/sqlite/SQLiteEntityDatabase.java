package org.javenstudio.cocoka.database.sqlite;

import org.javenstudio.cocoka.database.Constants;
import org.javenstudio.common.entitydb.IDatabaseManager;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IIdentityGenerator;
import org.javenstudio.common.entitydb.IMapCreator;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.nosql.SimpleDatabase;
import org.javenstudio.common.entitydb.nosql.SimpleTable;

public class SQLiteEntityDatabase extends SimpleDatabase {

	public SQLiteEntityDatabase(String dbname, IDatabaseManager manager) {
		super(dbname, manager); 
	}
	
	@Override
	protected <K extends IIdentity, T extends IEntity<K>> 
			SimpleTable<K,T> createSimpleTable(String name, Class<T> entityClass, 
					String identityFieldName, IIdentityGenerator<K> idGenerator, IMapCreator<K,T> mapCreator) { 
		return new SQLiteEntityTable<K,T>(this, name, entityClass); 
	}
	
	public <K extends IIdentity, T extends IEntity<K>> 
			ITable<K,T> createTable(String name, Class<T> entityClass, boolean cacheEnabled) {
		return createTable(name, entityClass, Constants.IDENTITY_FIELDNAME, null, null, cacheEnabled);
	}
	
}
