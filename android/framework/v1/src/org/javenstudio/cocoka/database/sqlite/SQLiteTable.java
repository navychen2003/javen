package org.javenstudio.cocoka.database.sqlite;

import org.javenstudio.cocoka.database.Constants;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.rdb.DBField;
import org.javenstudio.common.entitydb.rdb.DBOpenHelper;
import org.javenstudio.common.entitydb.rdb.DBTable;
import org.javenstudio.common.entitydb.rdb.DBValues;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class SQLiteTable<T extends SQLiteEntity> extends DBTable<LongIdentity, T> {

	private final String mTableName; 
	
	public SQLiteTable(DBOpenHelper helper, String tableName, Class<T> clazz) { 
		super(helper, clazz); 
		mTableName = tableName;
	}
	
	@Override
	protected DBField addIdentityField() { 
		return addField(Constants.IDENTITY_FIELDNAME, DBField.FieldType.INTEGER, 
				DBField.FieldProperty.PRIMARY_KEY, DBField.FieldProperty.AUTOINCREMENT); 
	}
	
	@Override
	public String getTableName() {
		return mTableName; 
	}
	
	@Override
	protected String getNullColumnHack() {
		return null; 
	}
	
	@Override
	protected DBValues newDBValues() { 
		return new SQLiteValues();
	}
	
	public static <E extends SQLiteEntity> ITable<LongIdentity, E> getTable(IDatabase db, Class<E> entityClass) { 
		return (ITable<LongIdentity, E>)db.getTable(entityClass);
	}
	
}
