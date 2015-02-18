package org.javenstudio.common.entitydb.rdb;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;

public class DBField {

	public static final Class<?> BYTEARRAY_TYPE = (new byte[0]).getClass(); 
	
	public static enum FieldType {
		INTEGER, FLOAT, DOUBLE, TEXT, BYTEARRAY, STREAM 
	}
	
	public static enum FieldProperty {
		PRIMARY_KEY, AUTOINCREMENT, NOT_NULL 
	}
	
	protected final DBTable<? extends IIdentity, ? extends IEntity<?>> mTable; 
	protected final String mName; 
	protected final FieldType mType; 
	protected FieldProperty[] mProperties; 
	
	public DBField(final DBTable<? extends IIdentity, ? extends IEntity<?>> table, String name, FieldType type, FieldProperty... props) {
		mTable = table; 
		mName = name; 
		mType = type; 
		mProperties = props; 
	}
	
	public DBTable<? extends IIdentity, ? extends IEntity<?>> getTable() {
		return mTable; 
	}
	
	public String getName() {
		return mName; 
	}
	
	public FieldType getType() {
		return mType; 
	}
	
	public FieldProperty[] getProperties() {
		return mProperties; 
	}
	
	public void setProperty(FieldProperty... props) {
		mProperties = props; 
	}
	
}
